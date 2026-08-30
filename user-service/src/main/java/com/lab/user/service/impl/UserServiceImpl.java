package com.lab.user.service.impl;

import com.lab.common.api.JwtKeyProvider;
import com.lab.common.api.Roles;
import com.lab.common.exception.BusinessException;
import com.lab.user.*;
import com.lab.user.controller.AuthController;
import com.lab.user.service.UserService;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;
    private final JwtKeyProvider jwtKeys;
    private final RefreshTokenRepository refreshTokens;
    private final SecureRandom random = new SecureRandom();
    private final long accessTokenMillis;
    private final long refreshTokenSeconds;
    private final int maxLoginFailures;

    public UserServiceImpl(UserRepository users, RoleRepository roles, PasswordEncoder encoder,
                           JwtKeyProvider jwtKeys, RefreshTokenRepository refreshTokens,
                           @Value("${security.jwt.access-token-hours:2}") int accessTokenHours,
                           @Value("${security.jwt.refresh-token-days:30}") int refreshTokenDays,
                           @Value("${security.login.max-failures:5}") int maxLoginFailures) {
        this.users = users; this.roles = roles; this.encoder = encoder;
        this.jwtKeys = jwtKeys; this.refreshTokens = refreshTokens;
        this.accessTokenMillis = accessTokenHours * 60L * 60L * 1000L;
        this.refreshTokenSeconds = refreshTokenDays * 24L * 60L * 60L;
        this.maxLoginFailures = maxLoginFailures;
    }

    @Override
    @Transactional
    public Map<String, Object> register(AuthController.Register request) {
        if (users.existsByEmployeeNo(request.employeeNo())) {
            throw new BusinessException("USER_EXISTS", "employee number already exists", HttpStatus.CONFLICT);
        }
        User user = new User();
        user.employeeNo = request.employeeNo();
        user.username = request.employeeNo();
        user.realName = request.realName();
        user.passwordHash = encoder.encode(request.password());
        user.email = request.email();
        user.phone = request.phone();
        user.roles.add(roles.findByCode(Roles.STUDENT).orElseThrow(() -> new IllegalStateException("STUDENT role is not initialized")));
        users.save(user);
        return profile(user);
    }

    @Override
    @Transactional
    public Map<String, Object> login(AuthController.Login request) {
        User user = users.findByUsernameAndDeletedFalse(request.username()).orElseThrow(() -> new BusinessException("LOGIN_FAILED", "invalid username or password", HttpStatus.UNAUTHORIZED));
        if (!"ACTIVE".equals(user.status)) {
            throw new BusinessException("USER_DISABLED", "user is not active", HttpStatus.FORBIDDEN);
        }
        if (!encoder.matches(request.password(), user.passwordHash)) {
            user.failedLoginCount++;
            if (user.failedLoginCount >= maxLoginFailures) user.status = "LOCKED";
            users.save(user);
            throw new BusinessException("LOGIN_FAILED", "invalid username or password", HttpStatus.UNAUTHORIZED);
        }
        user.failedLoginCount = 0;
        user.lastLoginAt = Instant.now();
        users.save(user);
        return tokenResponse(user);
    }

    @Override
    @Transactional
    public Map<String, Object> refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException("REFRESH_TOKEN_INVALID", "Refresh token is invalid", HttpStatus.UNAUTHORIZED);
        }
        Instant now = Instant.now();
        RefreshToken stored = refreshTokens.findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(hash(refreshToken), now)
                .orElseThrow(() -> new BusinessException("REFRESH_TOKEN_INVALID", "Refresh token is invalid or expired", HttpStatus.UNAUTHORIZED));
        User user = users.findById(stored.userId)
                .orElseThrow(() -> new BusinessException("REFRESH_TOKEN_INVALID", "User does not exist", HttpStatus.UNAUTHORIZED));
        if (!"ACTIVE".equals(user.status) || stored.tokenVersion != user.tokenVersion) {
            stored.revokedAt = now;
            refreshTokens.save(stored);
            throw new BusinessException("REFRESH_TOKEN_INVALID", "Refresh token is no longer valid", HttpStatus.UNAUTHORIZED);
        }
        stored.revokedAt = now;
        refreshTokens.save(stored);
        return tokenResponse(user);
    }

    @Override
    @Transactional
    public Map<String, Object> logout(String refreshToken, HttpServletRequest request) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException("REFRESH_TOKEN_INVALID", "Refresh token is invalid", HttpStatus.UNAUTHORIZED);
        }
        RefreshToken stored = refreshTokens.findByTokenHash(hash(refreshToken))
                .orElseThrow(() -> new BusinessException("REFRESH_TOKEN_INVALID", "Refresh token is invalid", HttpStatus.UNAUTHORIZED));
        Object currentUser = request.getAttribute("userId");
        if (currentUser instanceof Long id && !Objects.equals(id, stored.userId)) {
            throw new BusinessException("FORBIDDEN", "Refresh token does not belong to current user", HttpStatus.FORBIDDEN);
        }
        if (stored.revokedAt == null) {
            stored.revokedAt = Instant.now();
            refreshTokens.save(stored);
        }
        return Map.of("loggedOut", true);
    }

    @Override
    public Map<String, Object> me(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) throw new BusinessException("UNAUTHORIZED", "login required", HttpStatus.UNAUTHORIZED);
        User user = users.findById(userId).orElseThrow(() -> new BusinessException("UNAUTHORIZED", "user does not exist", HttpStatus.UNAUTHORIZED));
        Map<String, Object> result = new LinkedHashMap<>(profile(user));
        result.put("employeeNo", user.employeeNo);
        result.put("email", Objects.toString(user.email, ""));
        result.put("phone", Objects.toString(user.phone, ""));
        return result;
    }

    private Map<String, Object> profile(User user) {
        return Map.of("id", user.id, "username", user.username, "realName", user.realName, "roles", roleCodes(user));
    }

    private List<String> roleCodes(User user) {
        return user.roles.stream().map(role -> role.code).sorted().toList();
    }

    private Map<String, Object> tokenResponse(User user) {
        String accessToken = Jwts.builder().subject(user.id.toString())
                .claim("tokenType", "access")
                .claim("username", user.username).claim("realName", user.realName)
                .claim("roles", roleCodes(user)).issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenMillis))
                .signWith(jwtKeys.key()).compact();
        String refreshToken = randomToken();
        RefreshToken stored = new RefreshToken();
        stored.tokenHash = hash(refreshToken);
        stored.userId = user.id;
        stored.tokenVersion = user.tokenVersion;
        stored.expiresAt = Instant.now().plusSeconds(refreshTokenSeconds);
        refreshTokens.save(stored);
        return Map.of("accessToken", accessToken, "refreshToken", refreshToken, "expiresIn", accessTokenMillis / 1000,
                "user", profile(user));
    }

    private String randomToken() {
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) result.append(String.format("%02x", item));
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
