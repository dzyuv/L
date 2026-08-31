package com.lab.resource.controller;

import com.lab.common.api.ApiResponse;
import com.lab.resource.*;
import com.lab.resource.service.AssetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1")
public class AssetController {
    private final AssetService service;
    public AssetController(AssetService service) { this.service = service; }

    public record CategoryRequest(@NotBlank @Size(max = 100) String name, boolean serialized,
                                  boolean highValue, boolean enabled, @Size(max = 500) String description) {}
    public record AssetRequest(@NotBlank @Size(max = 50) String assetNo, @NotBlank @Size(max = 100) String name,
                               @NotNull Long categoryId, Long resourceId, @Size(max = 100) String serialNo,
                               @Size(max = 100) String brand, @Size(max = 100) String model,
                               @Size(max = 500) String specification, @Size(max = 30) String status,
                               @Size(max = 200) String location, Long custodianUserId, LocalDate purchaseDate,
                               LocalDate warrantyUntil, @DecimalMin(value = "0.0", inclusive = true) BigDecimal originalCost,
                               @Size(max = 1000) String remark) {}
    public record StatusRequest(@NotBlank @Size(max = 30) String status, @Size(max = 500) String reason) {}
    public record AssignRequest(Long custodianUserId, @Size(max = 200) String location, @Size(max = 500) String reason) {}
    public record BatchAssetItem(@Size(max = 50) String assetNo, @Size(max = 100) String serialNo,
                                 @Size(max = 200) String location, Long resourceId,
                                 @DecimalMin(value = "0.0", inclusive = true) BigDecimal originalCost,
                                 @Size(max = 1000) String remark) {}
    public record BatchCreateRequest(@NotNull Long categoryId, @NotBlank @Size(max = 100) String name,
                                     @Size(max = 100) String brand, @Size(max = 100) String model,
                                     @Size(max = 500) String specification, Long resourceId,
                                     @Size(max = 30) String status, boolean autoNumber,
                                     @Size(max = 30) String numberPrefix, @NotNull @Size(min = 1, max = 200) List<BatchAssetItem> items) {}
    public record MoveAssetsRequest(@NotNull @Size(min = 1, max = 200) List<Long> assetIds, Long resourceId,
                                    @Size(max = 200) String location) {}
    public record ReportRequest(Long assetId, Long resourceId, @Size(max = 200) String location,
                                @Size(max = 500) String assetClue, @NotBlank @Size(max = 30) String reportType,
                                @NotBlank @Size(max = 2000) String description, @Size(max = 20) String severity) {}
    public record TicketUpdateRequest(@NotBlank @Size(max = 30) String status, Long assetId,
                                      @Size(max = 30) String assignedTo,
                                      @Size(max = 2000) String resolution,
                                      @Size(max = 500) String assetClue,
                                      Boolean unlistedDevice,
                                      @DecimalMin(value = "0.0", inclusive = true) BigDecimal estimatedCost,
                                      @DecimalMin(value = "0.0", inclusive = true) BigDecimal actualCost) {}

    @GetMapping("/assets/categories") public ApiResponse<?> categories(HttpServletRequest request) { return ok(service.categories(false, request), request); }
    @GetMapping("/admin/assets/categories") public ApiResponse<?> adminCategories(HttpServletRequest request) { return ok(service.categories(true, request), request); }
    @PostMapping("/admin/assets/categories") public ApiResponse<?> createCategory(@Valid @RequestBody CategoryRequest body, HttpServletRequest request) { return ok(service.createCategory(body, request), request); }
    @PutMapping("/admin/assets/categories/{id}") public ApiResponse<?> updateCategory(@PathVariable("id") Long id, @Valid @RequestBody CategoryRequest body, HttpServletRequest request) { return ok(service.updateCategory(id, body, request), request); }

    @GetMapping("/assets/catalog") public ApiResponse<?> catalog(HttpServletRequest request) { return ok(service.catalog(request), request); }
    @GetMapping("/assets/types") public ApiResponse<?> publicTypes(HttpServletRequest request) { return ok(service.publicTypes(request), request); }
    @GetMapping("/admin/assets/next-no") public ApiResponse<?> nextNo(@RequestParam(value = "prefix", required = false) String prefix, HttpServletRequest request) { return ok(service.nextAssetNo(prefix, request), request); }
    @PostMapping("/admin/assets/batch") public ApiResponse<?> batchCreate(@Valid @RequestBody BatchCreateRequest body, HttpServletRequest request) { return ok(service.batchCreate(body, request), request); }
    @PostMapping("/admin/assets/move") public ApiResponse<?> moveAssets(@Valid @RequestBody MoveAssetsRequest body, HttpServletRequest request) { return ok(service.moveAssets(body, request), request); }
    @GetMapping("/admin/assets") public ApiResponse<?> assets(@RequestParam(value = "query", required = false) String query, @RequestParam(value = "status", required = false) String status, @RequestParam(value = "categoryId", required = false) Long categoryId, HttpServletRequest request) { return ok(service.listAssets(query, status, categoryId, request), request); }
    @GetMapping("/admin/assets/{id}") public ApiResponse<?> asset(@PathVariable("id") Long id, HttpServletRequest request) { return ok(service.getAsset(id, request), request); }
    @PostMapping("/admin/assets") public ApiResponse<?> createAsset(@Valid @RequestBody AssetRequest body, HttpServletRequest request) { return ok(service.createAsset(body, request), request); }
    @PutMapping("/admin/assets/{id}") public ApiResponse<?> updateAsset(@PathVariable("id") Long id, @Valid @RequestBody AssetRequest body, HttpServletRequest request) { return ok(service.updateAsset(id, body, request), request); }
    @PutMapping("/admin/assets/{id}/status") public ApiResponse<?> status(@PathVariable("id") Long id, @Valid @RequestBody StatusRequest body, HttpServletRequest request) { return ok(service.updateStatus(id, body, request), request); }
    @PostMapping("/admin/assets/{id}/assign") public ApiResponse<?> assign(@PathVariable("id") Long id, @Valid @RequestBody AssignRequest body, HttpServletRequest request) { return ok(service.assign(id, body, request), request); }
    @GetMapping("/admin/assets/{id}/history") public ApiResponse<?> history(@PathVariable("id") Long id, HttpServletRequest request) { return ok(service.history(id, request), request); }

    @PostMapping("/maintenance/tickets") public ApiResponse<?> report(@Valid @RequestBody ReportRequest body, HttpServletRequest request) { return ok(service.report(body, request), request); }
    @GetMapping("/maintenance/tickets/mine") public ApiResponse<?> mine(HttpServletRequest request) { return ok(service.myTickets(request), request); }
    @GetMapping("/admin/maintenance/tickets") public ApiResponse<?> tickets(@RequestParam(value = "status", required = false) String status, @RequestParam(value = "assetId", required = false) Long assetId, HttpServletRequest request) { return ok(service.listTickets(status, assetId, request), request); }
    @PutMapping("/admin/maintenance/tickets/{id}") public ApiResponse<?> updateTicket(@PathVariable("id") Long id, @Valid @RequestBody TicketUpdateRequest body, HttpServletRequest request) { return ok(service.updateTicket(id, body, request), request); }

    private <T> ApiResponse<T> ok(T data, HttpServletRequest request) { return ApiResponse.success(data, Objects.toString(request.getAttribute("X-Request-Id"), "")); }
}
