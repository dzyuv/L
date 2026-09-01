package com.lab.user.service.impl;

import com.lab.common.api.AdminOperationLogger;
import com.lab.common.api.RoleGuard;
import com.lab.common.api.Roles;
import com.lab.common.exception.BusinessException;
import com.lab.user.RefreshTokenRepository;
import com.lab.user.Role;
import com.lab.user.RoleRepository;
import com.lab.user.User;
import com.lab.user.UserRepository;
import com.lab.user.service.AdminUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class AdminUserServiceImpl implements AdminUserService {
    private static final Set<String> USER_STATUSES = Set.of("ACTIVE", "DISABLED", "LOCKED");
    private static final Set<String> ASSIGNABLE_ROLES = Set.of(Roles.STUDENT, Roles.TEACHER, Roles.LAB_ADMIN, Roles.SYSTEM_ADMIN);
    private static final int IMPORT_MAX_ROWS = 200;
    private static final String DEFAULT_IMPORT_PASSWORD = "12345678";
    private final UserRepository users;
    private final RoleRepository roles;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final RoleGuard roleGuard;
    private final AdminOperationLogger operationLogger;

    public AdminUserServiceImpl(UserRepository users, RoleRepository roles, RefreshTokenRepository refreshTokens,
                                PasswordEncoder passwordEncoder, RoleGuard roleGuard,
                                AdminOperationLogger operationLogger) {
        this.users = users; this.roles = roles; this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder; this.roleGuard = roleGuard;
        this.operationLogger = operationLogger;
    }

    @Override
    public Map<String, Object> list(String query, String status, HttpServletRequest request) {
        roleGuard.requireSystemAdmin(request);
        String keyword = Objects.toString(query, "").trim().toLowerCase();
        List<Map<String, Object>> items = users.findByDeletedFalseOrderByCreatedAtDesc().stream()
                .filter(user -> status == null || status.isBlank() || status.equals(user.status))
                .filter(user -> keyword.isBlank() || user.username.toLowerCase().contains(keyword)
                        || user.realName.toLowerCase().contains(keyword)
                        || Objects.toString(user.email, "").toLowerCase().contains(keyword))
                .map(this::view).toList();
        return Map.of("items", items, "total", items.size());
    }

    @Override
    public Map<String, Object> teachers(HttpServletRequest request) {
        roleGuard.requireAny(request, Roles.LAB_ADMIN, Roles.SYSTEM_ADMIN);
        List<Map<String, Object>> items = users.findByDeletedFalseOrderByCreatedAtDesc().stream()
                .filter(user -> "ACTIVE".equals(user.status))
                .filter(user -> user.roles.stream().anyMatch(role -> Roles.TEACHER.equals(role.code)))
                .map(user -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", user.id);
                    item.put("username", user.username);
                    item.put("employeeNo", user.employeeNo);
                    item.put("realName", user.realName);
                    return item;
                })
                .toList();
        return Map.of("items", items, "total", items.size());
    }

    @Override
    public Map<String, Object> roles(HttpServletRequest request) {
        roleGuard.requireSystemAdmin(request);
        List<Map<String, Object>> items = roles.findAll().stream()
                .filter(role -> "ACTIVE".equals(role.status))
                .map(role -> Map.<String, Object>of("id", role.id, "code", role.code, "name", role.name))
                .toList();
        return Map.of("items", items, "total", items.size());
    }

    @Override
    @Transactional
    public Map<String, Object> updateStatus(Long userId, String status, HttpServletRequest request) {
        roleGuard.requireSystemAdmin(request);
        if (!USER_STATUSES.contains(status)) throw new BusinessException("INVALID_STATUS", "User status is invalid", HttpStatus.BAD_REQUEST);
        if (Objects.equals(roleGuard.currentUserId(request), userId) && !"ACTIVE".equals(status)) {
            throw new BusinessException("SELF_DISABLE_FORBIDDEN", "Current administrator cannot disable their own account", HttpStatus.CONFLICT);
        }
        User user = find(userId);
        user.status = status;
        user.failedLoginCount = 0;
        user.tokenVersion++;
        refreshTokens.deleteByUserId(user.id);
        Map<String, Object> result = view(users.save(user));
        operationLogger.success(request, "USER_STATUS_UPDATED", "USER", user.id,
                Map.of("username", user.username, "status", status));
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> updateRoles(Long userId, Set<String> roleCodes, HttpServletRequest request) {
        roleGuard.requireSystemAdmin(request);
        if (roleCodes == null || roleCodes.isEmpty() || !ASSIGNABLE_ROLES.containsAll(roleCodes)) {
            throw new BusinessException("INVALID_ROLE", "At least one valid role is required", HttpStatus.BAD_REQUEST);
        }
        if (Objects.equals(roleGuard.currentUserId(request), userId) && !roleCodes.contains(Roles.SYSTEM_ADMIN)) {
            throw new BusinessException("SELF_ROLE_CHANGE_FORBIDDEN", "Current administrator cannot remove their own system role", HttpStatus.CONFLICT);
        }
        User user = find(userId);
        user.roles.clear();
        roleCodes.forEach(code -> user.roles.add(roles.findByCode(code)
                .orElseThrow(() -> new BusinessException("ROLE_NOT_FOUND", "Role does not exist: " + code, HttpStatus.NOT_FOUND))));
        user.tokenVersion++;
        refreshTokens.deleteByUserId(user.id);
        Map<String, Object> result = view(users.save(user));
        operationLogger.success(request, "USER_ROLES_UPDATED", "USER", user.id,
                Map.of("username", user.username, "roles", roleCodes));
        return result;
    }

    @Override
    public Map<String, Object> importUsers(String csv, HttpServletRequest request) {
        roleGuard.requireSystemAdmin(request);
        List<String> lines = parseImportLines(csv);
        if (lines.isEmpty()) throw new BusinessException("IMPORT_EMPTY", "Import file has no user rows", HttpStatus.BAD_REQUEST);
        ColumnMap columns = detectColumns(lines.get(0));
        int start = columns.header ? 1 : 0;
        if (lines.size() - start > IMPORT_MAX_ROWS) {
            throw new BusinessException("IMPORT_TOO_LARGE", "Import cannot exceed " + IMPORT_MAX_ROWS + " rows", HttpStatus.BAD_REQUEST);
        }
        if (start >= lines.size()) throw new BusinessException("IMPORT_EMPTY", "Import file has no user rows", HttpStatus.BAD_REQUEST);
        List<Map<String, Object>> created = new ArrayList<>();
        List<Map<String, Object>> skipped = new ArrayList<>();
        List<Map<String, Object>> failed = new ArrayList<>();
        for (int index = start; index < lines.size(); index++) {
            int lineNumber = index + 1;
            try {
                List<String> cells = parseCsvLine(lines.get(index));
                String employeeNo = cell(cells, columns.employeeNo);
                String realName = cell(cells, columns.realName);
                String email = cell(cells, columns.email);
                String phone = cell(cells, columns.phone);
                String roleValue = cell(cells, columns.role);
                String password = cell(cells, columns.password);
                if (roleValue.isBlank()) {
                    for (String candidate : cells) {
                        if (resolveRoleCode(candidate) != null) {
                            roleValue = candidate.trim();
                            break;
                        }
                    }
                }
                if (employeeNo.isBlank() && realName.isBlank() && roleValue.isBlank()) continue;
                if (employeeNo.isBlank() || employeeNo.length() > 50) {
                    failed.add(importIssue(lineNumber, employeeNo, "工号不能为空且不超过 50 个字符"));
                    continue;
                }
                if (realName.isBlank() || realName.length() > 50) {
                    failed.add(importIssue(lineNumber, employeeNo, "姓名不能为空且不超过 50 个字符"));
                    continue;
                }
                String roleCode = resolveRoleCode(roleValue);
                if (roleCode == null) {
                    failed.add(importIssue(lineNumber, employeeNo, "导入只支持学生或教师角色"));
                    continue;
                }
                if (!email.isBlank() && (email.length() > 100 || !email.contains("@"))) {
                    failed.add(importIssue(lineNumber, employeeNo, "邮箱格式不正确"));
                    continue;
                }
                if (!phone.isBlank() && (phone.length() < 6 || phone.length() > 30)) {
                    failed.add(importIssue(lineNumber, employeeNo, "手机号长度须为 6 至 30 位"));
                    continue;
                }
                if (!password.isBlank() && (password.length() < 8 || password.length() > 72)) {
                    failed.add(importIssue(lineNumber, employeeNo, "初始密码须为 8 至 72 位，留空则使用 12345678"));
                    continue;
                }
                if (users.existsByEmployeeNo(employeeNo) || users.existsByUsername(employeeNo)) {
                    skipped.add(importIssue(lineNumber, employeeNo, "工号已存在，已跳过"));
                    continue;
                }
                Role role = roles.findByCode(roleCode)
                        .orElseThrow(() -> new BusinessException("ROLE_NOT_FOUND", "Role does not exist: " + roleCode, HttpStatus.NOT_FOUND));
                User user = new User();
                user.employeeNo = employeeNo;
                user.username = employeeNo;
                user.realName = realName;
                user.email = email.isBlank() ? null : email;
                user.phone = phone.isBlank() ? null : phone;
                user.passwordHash = passwordEncoder.encode(password.isBlank() ? DEFAULT_IMPORT_PASSWORD : password);
                user.roles.add(role);
                users.save(user);
                created.add(Map.of("line", lineNumber, "username", employeeNo, "realName", realName, "role", roleCode));
            } catch (RuntimeException exception) {
                failed.add(importIssue(lineNumber, "", "该行无法导入"));
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("createdCount", created.size());
        result.put("skippedCount", skipped.size());
        result.put("failedCount", failed.size());
        result.put("created", created);
        result.put("skipped", skipped);
        result.put("failed", failed);
        operationLogger.success(request, "USER_IMPORTED", "USER", null,
                Map.of("createdCount", created.size(), "skippedCount", skipped.size(), "failedCount", failed.size()));
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> resetPassword(Long userId, String password, HttpServletRequest request) {
        roleGuard.requireSystemAdmin(request);
        if (password == null || password.length() < 8 || password.length() > 72) {
            throw new BusinessException("INVALID_PASSWORD", "Password length must be between 8 and 72", HttpStatus.BAD_REQUEST);
        }
        User user = find(userId);
        user.passwordHash = passwordEncoder.encode(password);
        user.failedLoginCount = 0;
        user.status = "ACTIVE";
        user.tokenVersion++;
        refreshTokens.deleteByUserId(user.id);
        users.save(user);
        operationLogger.success(request, "USER_PASSWORD_RESET", "USER", user.id,
                Map.of("username", user.username));
        return Map.of("id", user.id, "reset", true);
    }

    @Override
    @Transactional
    public Map<String, Object> delete(Long userId, HttpServletRequest request) {
        roleGuard.requireSystemAdmin(request);
        if (Objects.equals(roleGuard.currentUserId(request), userId)) {
            throw new BusinessException("SELF_DELETE_FORBIDDEN", "Current administrator cannot delete their own account", HttpStatus.CONFLICT);
        }
        User user = find(userId);
        user.deleted = true;
        user.status = "DISABLED";
        user.tokenVersion++;
        refreshTokens.deleteByUserId(user.id);
        users.save(user);
        operationLogger.success(request, "USER_DELETED", "USER", user.id,
                Map.of("username", user.username, "realName", Objects.toString(user.realName, "")));
        return Map.of("id", user.id, "deleted", true);
    }

    private User find(Long id) {
        return users.findById(id).filter(user -> !user.deleted)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "User does not exist", HttpStatus.NOT_FOUND));
    }

    private List<String> parseImportLines(String csv) {
        String text = Objects.toString(csv, "").replace("\uFEFF", "").trim();
        if (text.isBlank()) return List.of();
        List<String> lines = new ArrayList<>();
        for (String line : text.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.isBlank() || trimmed.startsWith("#")) continue;
            lines.add(trimmed);
        }
        return lines;
    }

    private ColumnMap detectColumns(String firstLine) {
        List<String> cells = parseCsvLine(firstLine);
        ColumnMap map = new ColumnMap();
        if (cells.isEmpty()) return map;
        String first = cells.get(0).toLowerCase(Locale.ROOT);
        if (first.contains("工号") || first.contains("employeeno") || first.contains("username") || "账号".equals(cells.get(0))) {
            map.header = true;
            for (int index = 0; index < cells.size(); index++) {
                String header = cells.get(index).toLowerCase(Locale.ROOT).replace(" ", "");
                if (header.contains("工号") || header.contains("employeeno") || header.contains("username") || header.contains("账号")) map.employeeNo = index;
                else if (header.contains("姓名") || header.contains("realname") || header.contains("name")) map.realName = index;
                else if (header.contains("邮箱") || header.contains("email")) map.email = index;
                else if (header.contains("手机") || header.contains("电话") || header.contains("phone")) map.phone = index;
                else if (header.contains("角色") || header.contains("role")) map.role = index;
                else if (header.contains("密码") || header.contains("password")) map.password = index;
            }
        }
        return map;
    }

    private List<String> parseCsvLine(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (quoted) {
                if (character == '"') {
                    if (index + 1 < line.length() && line.charAt(index + 1) == '"') {
                        current.append('"');
                        index++;
                    } else quoted = false;
                } else current.append(character);
            } else if (character == '"') quoted = true;
            else if (character == ',') {
                cells.add(current.toString().trim());
                current.setLength(0);
            } else current.append(character);
        }
        cells.add(current.toString().trim());
        return cells;
    }

    private String cell(List<String> cells, int index) {
        if (index < 0 || index >= cells.size()) return "";
        return Objects.toString(cells.get(index), "").trim();
    }

    private String resolveRoleCode(String value) {
        String normalized = Objects.toString(value, "").trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "STUDENT", "学生" -> Roles.STUDENT;
            case "TEACHER", "教师", "老师" -> Roles.TEACHER;
            default -> null;
        };
    }

    private Map<String, Object> importIssue(int line, String username, String reason) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("line", line);
        item.put("username", username);
        item.put("reason", reason);
        return item;
    }

    private static final class ColumnMap {
        boolean header;
        int employeeNo;
        int realName = 1;
        int email = 2;
        int phone = 3;
        int role = 4;
        int password = 5;
    }

    private Map<String, Object> view(User user) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.id); result.put("username", user.username); result.put("employeeNo", user.employeeNo);
        result.put("realName", user.realName); result.put("email", maskEmail(user.email)); result.put("phone", maskPhone(user.phone));
        result.put("status", user.status); result.put("roles", user.roles.stream().map(role -> role.code).sorted().toList());
        result.put("lastLoginAt", user.lastLoginAt); result.put("createdAt", user.createdAt);
        return result;
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "";
        int at = email.indexOf('@');
        return email.substring(0, Math.min(2, at)) + "***" + email.substring(at);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return Objects.toString(phone, "");
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
