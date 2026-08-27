package com.lab.user.service.impl;

import com.lab.common.api.JwtKeyProvider;
import com.lab.common.exception.BusinessException;
import com.lab.user.*;
import com.lab.user.controller.AuthController;
import com.lab.user.service.UserService;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;
    private final JwtKeyProvider jwtKeys;

    public UserServiceImpl(UserRepository users, RoleRepository roles, PasswordEncoder encoder, JwtKeyProvider jwtKeys) {
        this.users = users;
        this.roles = roles;
        this.encoder = encoder;
        this.jwtKeys = jwtKeys;
    }

    @Override
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
        user.roles.add(roles.findByCode("STUDENT").orElseThrow(() -> new IllegalStateException("STUDENT role is not initialized")));
        users.save(user);
        return profile(user);
    }

    @Override
    public Map<String, Object> login(AuthController.Login request) {
        User user = users.findByUsername(request.username()).orElseThrow(() -> new BusinessException("LOGIN_FAILED", "invalid username or password", HttpStatus.UNAUTHORIZED));
        if (!"ACTIVE".equals(user.status)) {
            throw new BusinessException("USER_DISABLED", "user is not active", HttpStatus.FORBIDDEN);
        }
        if (!encoder.matches(request.password(), user.passwordHash)) {
            user.failedLoginCount++;
            if (user.failedLoginCount >= 5) user.status = "LOCKED";
            users.save(user);
            throw new BusinessException("LOGIN_FAILED", "invalid username or password", HttpStatus.UNAUTHORIZED);
        }
        user.failedLoginCount = 0;
        user.lastLoginAt = Instant.now();
        users.save(user);
        List<String> roleCodes = roleCodes(user);
        String token = Jwts.builder().subject(user.id.toString())
                .claim("username", user.username).claim("realName", user.realName)
                .claim("roles", roleCodes).issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 7200000))
                .signWith(jwtKeys.key()).compact();
        return Map.of("accessToken", token, "refreshToken", token, "user", profile(user));
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
}
