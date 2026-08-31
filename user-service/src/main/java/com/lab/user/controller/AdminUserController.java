package com.lab.user.controller;

import com.lab.common.api.ApiResponse;
import com.lab.user.service.AdminUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminUserController {
    private final AdminUserService service;
    public AdminUserController(AdminUserService service) { this.service = service; }

    public record StatusRequest(@NotBlank String status) {}
    public record RolesRequest(@NotEmpty Set<@NotBlank String> roles) {}
    public record PasswordRequest(@NotBlank @Size(min = 8, max = 72) String password) {}

    @GetMapping("/users")
    public ApiResponse<?> users(@RequestParam(value = "query", required = false) String query,
                                @RequestParam(value = "status", required = false) String status,
                                HttpServletRequest request) { return ok(service.list(query, status, request), request); }
    @GetMapping("/users/teachers")
    public ApiResponse<?> teachers(HttpServletRequest request) { return ok(service.teachers(request), request); }
    @GetMapping("/roles") public ApiResponse<?> roles(HttpServletRequest request) { return ok(service.roles(request), request); }
    @PutMapping("/users/{id}/status") public ApiResponse<?> status(@PathVariable("id") Long id, @Valid @RequestBody StatusRequest body, HttpServletRequest request) { return ok(service.updateStatus(id, body.status(), request), request); }
    @PutMapping("/users/{id}/roles") public ApiResponse<?> roles(@PathVariable("id") Long id, @Valid @RequestBody RolesRequest body, HttpServletRequest request) { return ok(service.updateRoles(id, body.roles(), request), request); }
    @PostMapping("/users/{id}/reset-password") public ApiResponse<?> reset(@PathVariable("id") Long id, @Valid @RequestBody PasswordRequest body, HttpServletRequest request) { return ok(service.resetPassword(id, body.password(), request), request); }
    @DeleteMapping("/users/{id}") public ApiResponse<?> delete(@PathVariable("id") Long id, HttpServletRequest request) { return ok(service.delete(id, request), request); }

    private <T> ApiResponse<T> ok(T data, HttpServletRequest request) {
        return ApiResponse.success(data, Objects.toString(request.getAttribute("X-Request-Id"), ""));
    }
}
