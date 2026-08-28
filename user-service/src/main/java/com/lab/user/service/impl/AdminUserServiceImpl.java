package com.lab.user.service.impl;

import com.lab.common.api.AdminOperationLogger;
import com.lab.common.api.RoleGuard;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class AdminUserServiceImpl implements AdminUserService {
    private static final Set<String> USER_STATUSES = Set.of("ACTIVE", "DISABLED", "LOCKED");
    private static final Set<String> ASSIGNABLE_ROLES = Set.of("STUDENT", "TEACHER", "LAB_ADMIN", "SYSTEM_ADMIN");
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
        if (Objects.equals(roleGuard.currentUserId(request), userId) && !roleCodes.contains("SYSTEM_ADMIN")) {
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

    private User find(Long id) {
        return users.findById(id).filter(user -> !user.deleted)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "User does not exist", HttpStatus.NOT_FOUND));
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
