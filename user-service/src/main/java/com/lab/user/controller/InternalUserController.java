package com.lab.user.controller;

import com.lab.common.api.ApiResponse;
import com.lab.common.api.InternalServiceGuard;
import com.lab.common.api.RequestIdFilter;
import com.lab.user.User;
import com.lab.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/internal/users")
public class InternalUserController {
    private final UserRepository users;
    private final InternalServiceGuard internalServices;

    public InternalUserController(UserRepository users, InternalServiceGuard internalServices) {
        this.users = users;
        this.internalServices = internalServices;
    }

    public record TokenVersion(int version, boolean active) {}

    @GetMapping("/{id}/token-version")
    public ApiResponse<?> tokenVersion(@PathVariable("id") Long id, HttpServletRequest request) {
        internalServices.require(request);
        User user = users.selectById(id);
        if (user == null || user.deleted) {
            return ApiResponse.success(new TokenVersion(-1, false), requestId(request));
        }
        return ApiResponse.success(new TokenVersion(user.tokenVersion, "ACTIVE".equals(user.status)), requestId(request));
    }

    private String requestId(HttpServletRequest request) {
        return Objects.toString(request.getAttribute(RequestIdFilter.HEADER), "");
    }
}
