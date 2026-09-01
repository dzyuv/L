package com.lab.resource.service.impl;

import com.lab.common.api.RoleGuard;
import com.lab.common.exception.BusinessException;
import com.lab.resource.*;
import com.lab.resource.controller.AssetController;
import com.lab.resource.service.AssetService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AssetServiceImpl implements AssetService {
    private static final Set<String> ASSET_STATUSES = Set.of("IN_STOCK", "IN_USE", "REPORTED", "MAINTENANCE", "LOST", "SCRAPPED");
    private static final Set<String> TICKET_STATUSES = Set.of("REPORTED", "TRIAGED", "REPAIRING", "WAITING_ACCEPTANCE", "CLOSED", "REJECTED");
    private static final Set<String> REPORT_TYPES = Set.of("DAMAGE", "MALFUNCTION", "LOSS", "OTHER");
    private static final Set<String> SEVERITIES = Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL");
    private final AssetCategoryRepository categories;
    private final AssetRepository assets;
    private final AssetStatusHistoryRepository history;
    private final MaintenanceTicketRepository tickets;
    private final ResourceRepository resources;
    private final RoleGuard roles;

    public AssetServiceImpl(AssetCategoryRepository categories, AssetRepository assets, AssetStatusHistoryRepository history,
                            MaintenanceTicketRepository tickets, ResourceRepository resources, RoleGuard roles) {
        this.categories = categories; this.assets = assets; this.history = history; this.tickets = tickets; this.resources = resources; this.roles = roles;
    }

    public List<AssetCategory> categories(boolean admin, HttpServletRequest request) {
        if (admin) roles.requireLabAdmin(request);
        return admin ? categories.findAll().stream().sorted(Comparator.comparing(item -> item.name, String.CASE_INSENSITIVE_ORDER)).toList() : categories.findByEnabledTrueOrderByNameAsc();
    }
    public AssetCategory createCategory(AssetController.CategoryRequest body, HttpServletRequest request) {
        roles.requireLabAdmin(request); categories.findByNameIgnoreCase(body.name()).ifPresent(x -> { throw new BusinessException("CATEGORY_EXISTS", "Asset category already exists", HttpStatus.CONFLICT); });
        AssetCategory item = new AssetCategory(); apply(item, body); return categories.save(item);
    }
    public AssetCategory updateCategory(Long id, AssetController.CategoryRequest body, HttpServletRequest request) {
        roles.requireLabAdmin(request); AssetCategory item = categories.findById(id).orElseThrow(() -> notFound("Asset category does not exist"));
        categories.findByNameIgnoreCase(body.name()).filter(x -> !Objects.equals(x.id, id)).ifPresent(x -> { throw new BusinessException("CATEGORY_EXISTS", "Asset category already exists", HttpStatus.CONFLICT); });
        apply(item, body); return categories.save(item);
    }
    public List<?> catalog(HttpServletRequest request) {
        roles.requireAny(request, "TEACHER", "LAB_ADMIN");
        return assets.findByDeletedFalseOrderByCreatedAtDesc().stream().filter(item -> !Set.of("SCRAPPED", "LOST").contains(item.status))
                .map(item -> Map.of("id", item.id, "assetNo", item.assetNo, "name", item.name, "categoryId", item.categoryId,
                        "resourceId", item.resourceId == null ? "" : item.resourceId, "serialNo", Objects.toString(item.serialNo, ""),
                        "brand", Objects.toString(item.brand, ""), "model", Objects.toString(item.model, ""),
                        "status", item.status, "location", Objects.toString(item.location, "")))
                .toList();
    }
    public List<?> publicTypes(HttpServletRequest request) {
        roles.currentUserId(request);
        Map<String, Map<String, Object>> grouped = new LinkedHashMap<>();
        for (Asset item : assets.findByDeletedFalseOrderByCreatedAtDesc()) {
            if (Set.of("SCRAPPED", "LOST").contains(item.status) || item.resourceId == null) continue;
            String key = item.categoryId + "\u0001" + item.name + "\u0001" + Objects.toString(item.brand, "") + "\u0001" + Objects.toString(item.model, "");
            Map<String, Object> row = grouped.computeIfAbsent(key, ignored -> {
                Map<String, Object> created = new LinkedHashMap<>();
                created.put("key", key);
                created.put("name", item.name);
                created.put("brand", Objects.toString(item.brand, ""));
                created.put("model", Objects.toString(item.model, ""));
                created.put("categoryId", item.categoryId);
                created.put("resourceIds", new LinkedHashSet<Long>());
                created.put("count", 0);
                return created;
            });
            @SuppressWarnings("unchecked")
            Set<Long> resourceIds = (Set<Long>) row.get("resourceIds");
            resourceIds.add(item.resourceId);
            row.put("count", ((Integer) row.get("count")) + 1);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : grouped.values()) {
            @SuppressWarnings("unchecked")
            Set<Long> resourceIds = (Set<Long>) row.get("resourceIds");
            Map<String, Object> copy = new LinkedHashMap<>(row);
            copy.put("resourceIds", List.copyOf(resourceIds));
            result.add(copy);
        }
        return result;
    }
    public Map<String, Object> nextAssetNo(String prefix, HttpServletRequest request) {
        roles.requireLabAdmin(request);
        return Map.of("assetNo", generateAssetNo(prefix), "prefix", normalizePrefix(prefix));
    }
    @Transactional
    public Map<String, Object> batchCreate(AssetController.BatchCreateRequest body, HttpServletRequest request) {
        roles.requireLabAdmin(request);
        validateCategory(body.categoryId());
        validateResource(body.resourceId());
        validateStatus(body.status());
        List<Asset> created = new ArrayList<>();
        String prefix = normalizePrefix(body.numberPrefix() != null && !body.numberPrefix().isBlank() ? body.numberPrefix() : suggestPrefix(body.name()));
        int generated = 0;
        for (AssetController.BatchAssetItem row : body.items()) {
            Asset item = new Asset();
            item.categoryId = body.categoryId();
            item.name = body.name().trim();
            item.brand = body.brand();
            item.model = body.model();
            item.specification = body.specification();
            item.status = body.status() == null || body.status().isBlank() ? "IN_STOCK" : body.status();
            item.resourceId = row.resourceId() != null ? row.resourceId() : body.resourceId();
            validateResource(item.resourceId);
            item.location = row.location();
            item.originalCost = row.originalCost();
            item.remark = row.remark();
            item.serialNo = row.serialNo() == null || row.serialNo().isBlank() ? null : row.serialNo().trim();
            if (row.assetNo() != null && !row.assetNo().isBlank()) item.assetNo = row.assetNo().trim();
            else if (body.autoNumber()) {
                item.assetNo = generateAssetNo(prefix);
                generated++;
            } else throw new BusinessException("ASSET_NO_REQUIRED", "Asset number is required when auto numbering is disabled", HttpStatus.BAD_REQUEST);
            try { created.add(assets.saveAndFlush(item)); }
            catch (DataIntegrityViolationException e) { throw new BusinessException("ASSET_EXISTS", "Asset number or serial number already exists", HttpStatus.CONFLICT); }
            record(item, null, item.status, "Batch asset created", roles.currentUserId(request));
        }
        return Map.of("items", created, "total", created.size(), "generated", generated);
    }
    @Transactional
    public Map<String, Object> moveAssets(AssetController.MoveAssetsRequest body, HttpServletRequest request) {
        roles.requireLabAdmin(request);
        validateResource(body.resourceId());
        List<Asset> moved = new ArrayList<>();
        for (Long id : body.assetIds()) {
            Asset item = findAsset(id);
            item.resourceId = body.resourceId();
            if (body.location() != null && !body.location().isBlank()) item.location = body.location().trim();
            item.updatedAt = LocalDateTime.now();
            moved.add(assets.save(item));
            record(item, item.status, item.status, "Moved to resource " + body.resourceId(), roles.currentUserId(request));
        }
        return Map.of("items", moved, "total", moved.size());
    }
    private String normalizePrefix(String prefix) {
        String value = prefix == null ? "" : prefix.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9-]", "");
        if (value.isBlank()) return "AST";
        return value.endsWith("-") ? value.substring(0, value.length() - 1) : value;
    }
    private String suggestPrefix(String name) {
        if (name == null || name.isBlank()) return "AST";
        String letters = name.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        if (letters.length() >= 3) return letters.substring(0, 3);
        return "AST";
    }
    private String generateAssetNo(String prefix) {
        String normalized = normalizePrefix(prefix);
        Pattern pattern = Pattern.compile("^" + Pattern.quote(normalized) + "-(\\d+)$", Pattern.CASE_INSENSITIVE);
        int max = 0;
        int width = 3;
        for (Asset item : assets.findByDeletedFalseOrderByCreatedAtDesc()) {
            Matcher matcher = pattern.matcher(item.assetNo);
            if (!matcher.matches()) continue;
            String digits = matcher.group(1);
            width = Math.max(width, digits.length());
            max = Math.max(max, Integer.parseInt(digits));
        }
        return normalized + "-" + String.format("%0" + width + "d", max + 1);
    }
    public Map<String, Object> listAssets(String query, String status, Long categoryId, HttpServletRequest request) {
        roles.requireLabAdmin(request);
        List<Asset> items = assets.findByDeletedFalseOrderByCreatedAtDesc().stream()
                .filter(item -> query == null || query.isBlank() || item.name.toLowerCase().contains(query.toLowerCase()) || item.assetNo.toLowerCase().contains(query.toLowerCase()) || Objects.toString(item.serialNo, "").toLowerCase().contains(query.toLowerCase()))
                .filter(item -> status == null || status.isBlank() || status.equals(item.status))
                .filter(item -> categoryId == null || Objects.equals(categoryId, item.categoryId)).toList();
        return Map.of("items", items, "total", items.size());
    }
    public Asset getAsset(Long id, HttpServletRequest request) { roles.requireLabAdmin(request); return findAsset(id); }
    @Transactional
    public Asset createAsset(AssetController.AssetRequest body, HttpServletRequest request) {
        roles.requireLabAdmin(request); validateCategory(body.categoryId()); validateResource(body.resourceId()); validateStatus(body.status());
        Asset item = new Asset(); apply(item, body); validateSerialized(item); Asset saved;
        try { saved = assets.saveAndFlush(item); } catch (DataIntegrityViolationException e) { throw new BusinessException("ASSET_EXISTS", "Asset number or serial number already exists", HttpStatus.CONFLICT); }
        record(saved, null, saved.status, "Asset created", roles.currentUserId(request)); return saved;
    }
    @Transactional
    public Asset updateAsset(Long id, AssetController.AssetRequest body, HttpServletRequest request) {
        roles.requireLabAdmin(request); validateCategory(body.categoryId()); validateResource(body.resourceId()); validateStatus(body.status());
        Asset item = findAsset(id); String previous = item.status; apply(item, body); validateSerialized(item); Asset saved;
        try { saved = assets.saveAndFlush(item); } catch (DataIntegrityViolationException e) { throw new BusinessException("ASSET_EXISTS", "Asset number or serial number already exists", HttpStatus.CONFLICT); }
        if (!Objects.equals(previous, saved.status)) record(saved, previous, saved.status, "Asset updated", roles.currentUserId(request)); return saved;
    }
    @Transactional
    public Asset updateStatus(Long id, AssetController.StatusRequest body, HttpServletRequest request) {
        roles.requireLabAdmin(request); validateStatus(body.status()); Asset item = findAsset(id); String previous = item.status;
        item.status = body.status(); Asset saved = assets.save(item); record(saved, previous, saved.status, Objects.toString(body.reason(), "Status changed"), roles.currentUserId(request)); return saved;
    }
    @Transactional
    public Asset assign(Long id, AssetController.AssignRequest body, HttpServletRequest request) {
        roles.requireLabAdmin(request); Asset item = findAsset(id); item.custodianUserId = body.custodianUserId(); if (body.location() != null) item.location = body.location();
        Asset saved = assets.save(item); record(saved, saved.status, saved.status, Objects.toString(body.reason(), "Custodian changed"), roles.currentUserId(request)); return saved;
    }
    public List<AssetStatusHistory> history(Long id, HttpServletRequest request) { roles.requireLabAdmin(request); findAsset(id); return history.findByAssetIdOrderByCreatedAtDesc(id); }

    @Transactional
    public MaintenanceTicket report(AssetController.ReportRequest body, HttpServletRequest request) {
        Long userId = roles.currentUserId(request); Asset asset = body.assetId() == null ? null : findAsset(body.assetId());
        if (asset == null && body.resourceId() == null && (body.location() == null || body.location().isBlank())) throw new BusinessException("LOCATION_REQUIRED", "Select a resource or provide the problem location", HttpStatus.BAD_REQUEST);
        if (asset != null && Set.of("SCRAPPED", "LOST").contains(asset.status)) throw new BusinessException("ASSET_UNAVAILABLE", "This asset cannot receive a maintenance report", HttpStatus.UNPROCESSABLE_ENTITY);
        if (body.resourceId() != null) validateResource(body.resourceId());
        if (!REPORT_TYPES.contains(body.reportType())) throw new BusinessException("INVALID_REPORT_TYPE", "Maintenance report type is invalid", HttpStatus.BAD_REQUEST);
        String severity = body.severity() == null || body.severity().isBlank() ? "MEDIUM" : body.severity();
        if (!SEVERITIES.contains(severity)) throw new BusinessException("INVALID_SEVERITY", "Maintenance severity is invalid", HttpStatus.BAD_REQUEST);
        if (asset != null && tickets.existsByAssetIdAndStatusIn(asset.id, Set.of("REPORTED", "TRIAGED", "REPAIRING", "WAITING_ACCEPTANCE"))) throw new BusinessException("OPEN_TICKET_EXISTS", "This asset already has an open maintenance ticket", HttpStatus.CONFLICT);
        MaintenanceTicket ticket = new MaintenanceTicket(); ticket.ticketNo = "MT" + UUID.randomUUID().toString().replace("-", "").substring(0, 24); ticket.assetId = asset == null ? null : asset.id; ticket.resourceId = body.resourceId() != null ? body.resourceId() : asset == null ? null : asset.resourceId; ticket.locationSnapshot = firstNonBlank(body.location(), asset == null ? null : asset.location); ticket.assetClue = body.assetClue(); ticket.reportedBy = userId; ticket.previousAssetStatus = asset == null ? null : asset.status; ticket.reportType = body.reportType(); ticket.severity = severity; ticket.description = body.description();
        ticket = tickets.save(ticket);
        syncAssetWithTicket(asset, ticket, ticket.status, userId);
        return ticket;
    }
    public List<MaintenanceTicket> myTickets(HttpServletRequest request) { return tickets.findByReportedByOrderByCreatedAtDesc(roles.currentUserId(request)); }
    public Map<String, Object> listTickets(String status, Long assetId, HttpServletRequest request) {
        roles.requireLabAdmin(request); List<MaintenanceTicket> items = tickets.findAllByOrderByCreatedAtDesc().stream().filter(x -> status == null || status.isBlank() || status.equals(x.status)).filter(x -> assetId == null || Objects.equals(assetId, x.assetId)).toList(); return Map.of("items", items, "total", items.size());
    }
    @Transactional
    public MaintenanceTicket updateTicket(Long id, AssetController.TicketUpdateRequest body, HttpServletRequest request) {
        roles.requireLabAdmin(request); if (!TICKET_STATUSES.contains(body.status())) throw new BusinessException("INVALID_STATUS", "Maintenance ticket status is invalid", HttpStatus.BAD_REQUEST);
        MaintenanceTicket ticket = tickets.findById(id).orElseThrow(() -> notFound("Maintenance ticket does not exist"));
        validateTransition(ticket.status, body.status());
        if (ticket.assetId == null && body.assetId() != null) {
            Asset selected = findAsset(body.assetId());
            if (tickets.existsByAssetIdAndStatusIn(selected.id, Set.of("REPORTED", "TRIAGED", "REPAIRING", "WAITING_ACCEPTANCE"))) {
                throw new BusinessException("OPEN_TICKET_EXISTS", "This asset already has an open maintenance ticket", HttpStatus.CONFLICT);
            }
            ticket.assetId = selected.id;
            ticket.previousAssetStatus = selected.status;
            if (ticket.resourceId == null) ticket.resourceId = selected.resourceId;
            if (ticket.locationSnapshot == null || ticket.locationSnapshot.isBlank()) ticket.locationSnapshot = selected.location;
        }
        boolean unlisted = Boolean.TRUE.equals(body.unlistedDevice());
        if (unlisted && body.assetClue() != null && !body.assetClue().isBlank()) {
            ticket.assetClue = body.assetClue().trim();
        }
        Asset asset = ticket.assetId == null ? null : findAsset(ticket.assetId);
        if (asset == null && !unlisted && Set.of("REPAIRING", "WAITING_ACCEPTANCE", "CLOSED").contains(body.status())) {
            throw new BusinessException("ASSET_REQUIRED", "Bind the concrete asset before starting repair", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        ticket.status = body.status();
        ticket.assignedTo = normalizeAssigneePhone(body.assignedTo());
        ticket.resolution = body.resolution();
        ticket.estimatedCost = body.estimatedCost();
        ticket.actualCost = body.actualCost();
        ticket.processedBy = roles.currentUserId(request);
        ticket.processedAt = LocalDateTime.now();
        if ("CLOSED".equals(body.status()) || "REJECTED".equals(body.status())) ticket.closedAt = LocalDateTime.now();
        MaintenanceTicket saved = tickets.save(ticket);
        syncAssetWithTicket(asset, saved, saved.status, saved.processedBy);
        return saved;
    }

    private void apply(AssetCategory item, AssetController.CategoryRequest body) { item.name = body.name(); item.serialized = body.serialized(); item.highValue = body.highValue(); item.enabled = body.enabled(); item.description = body.description(); item.updatedAt = LocalDateTime.now(); }
    private void apply(Asset item, AssetController.AssetRequest body) { item.assetNo = body.assetNo(); item.name = body.name(); item.categoryId = body.categoryId(); item.resourceId = body.resourceId(); item.serialNo = body.serialNo(); item.brand = body.brand(); item.model = body.model(); item.specification = body.specification(); item.status = body.status() == null || body.status().isBlank() ? "IN_STOCK" : body.status(); item.location = body.location(); item.custodianUserId = body.custodianUserId(); item.purchaseDate = body.purchaseDate(); item.warrantyUntil = body.warrantyUntil(); item.originalCost = body.originalCost(); item.remark = body.remark(); item.updatedAt = LocalDateTime.now(); }
    private void validateSerialized(Asset item) { AssetCategory category = categories.findById(item.categoryId).orElseThrow(() -> notFound("Asset category does not exist")); if ((category.serialized || category.highValue) && (item.serialNo == null || item.serialNo.isBlank())) throw new BusinessException("SERIAL_NO_REQUIRED", "Serialized or high-value assets require a serial number", HttpStatus.BAD_REQUEST); }
    private void validateCategory(Long id) { categories.findById(id).filter(x -> x.enabled).orElseThrow(() -> notFound("Asset category does not exist")); }
    private void validateResource(Long id) { if (id != null && resources.findById(id).filter(x -> !x.deleted).isEmpty()) throw new BusinessException("RESOURCE_NOT_FOUND", "Linked resource does not exist", HttpStatus.NOT_FOUND); }
    private void validateStatus(String status) { if (status != null && !status.isBlank() && !ASSET_STATUSES.contains(status)) throw new BusinessException("INVALID_STATUS", "Asset status is invalid", HttpStatus.BAD_REQUEST); }
    private Asset findAsset(Long id) { return assets.findById(id).filter(x -> !x.deleted).orElseThrow(() -> notFound("Asset does not exist")); }
    private BusinessException notFound(String message) { return new BusinessException("NOT_FOUND", message, HttpStatus.NOT_FOUND); }
    private void record(Asset item, String from, String to, String reason, Long operator) { AssetStatusHistory row = new AssetStatusHistory(); row.assetId = item.id; row.fromStatus = from; row.toStatus = to; row.reason = reason; row.operatorId = operator; history.save(row); }
    private void syncAssetWithTicket(Asset asset, MaintenanceTicket ticket, String ticketStatus, Long operator) {
        if (asset == null) return;
        String nextStatus = assetStatusForTicket(ticket.reportType, ticketStatus, ticket.previousAssetStatus);
        if (nextStatus == null) return;
        Asset latest = findAsset(asset.id);
        setAssetStatus(latest, nextStatus, "工单 " + ticket.ticketNo + " " + ticketStatus, operator);
    }

    private String assetStatusForTicket(String reportType, String ticketStatus, String previousAssetStatus) {
        if ("LOSS".equals(reportType) && Set.of("REPORTED", "TRIAGED", "REPAIRING", "WAITING_ACCEPTANCE", "CLOSED").contains(ticketStatus)) {
            return "LOST";
        }
        return switch (ticketStatus) {
            case "REPORTED", "TRIAGED" -> "REPORTED";
            case "REPAIRING", "WAITING_ACCEPTANCE" -> "MAINTENANCE";
            case "CLOSED", "REJECTED" -> restoreAssetStatus(previousAssetStatus);
            default -> null;
        };
    }

    private String restoreAssetStatus(String previous) {
        if (previous == null || previous.isBlank() || Set.of("REPORTED", "MAINTENANCE", "LOST", "SCRAPPED").contains(previous)) {
            return "IN_STOCK";
        }
        return previous;
    }

    private void setAssetStatus(Asset item, String status, String reason, Long operator) {
        if (Objects.equals(item.status, status)) return;
        String previous = item.status;
        item.status = status;
        item.updatedAt = LocalDateTime.now();
        assets.save(item);
        record(item, previous, status, reason, operator);
    }
    private void validateTransition(String from, String to) { if (Objects.equals(from, to)) return; Map<String, Set<String>> allowed = Map.of("REPORTED", Set.of("TRIAGED", "REPAIRING", "REJECTED"), "TRIAGED", Set.of("REPAIRING", "REJECTED"), "REPAIRING", Set.of("WAITING_ACCEPTANCE", "REJECTED"), "WAITING_ACCEPTANCE", Set.of("REPAIRING", "CLOSED")); if (!allowed.getOrDefault(from, Set.of()).contains(to)) throw new BusinessException("INVALID_TRANSITION", "Maintenance ticket status transition is not allowed", HttpStatus.UNPROCESSABLE_ENTITY); }
    private String firstNonBlank(String first, String second) { return first != null && !first.isBlank() ? first.trim() : second; }

    private String normalizeAssigneePhone(String value) {
        if (value == null) return null;
        String phone = value.trim().replaceAll("[\\s-]", "");
        if (phone.isBlank()) return null;
        if (phone.startsWith("+86")) phone = phone.substring(3);
        else if (phone.startsWith("86") && phone.length() == 13) phone = phone.substring(2);
        if (!phone.matches("1\\d{10}") && !phone.matches("\\d{7,12}")) {
            throw new BusinessException("INVALID_PHONE", "请填写有效的维修负责人电话", HttpStatus.BAD_REQUEST);
        }
        return phone;
    }
}
