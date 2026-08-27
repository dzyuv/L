package com.lab.user.controller;

import com.lab.common.api.ApiResponse;
import com.lab.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user")
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    public record Register(@NotBlank @Size(max=50) String employeeNo,
                           @NotBlank @Size(max=50) String realName,
                           @NotBlank @Size(min=8,max=72) String password,
                           @NotBlank @Email @Size(max=100) String email,
                           @NotBlank @Size(max=30) String phone) {}
    public record Login(@NotBlank String username, @NotBlank String password) {}
    public record Refresh(@NotBlank String refreshToken) {}

    @PostMapping("/register")
    public ApiResponse<?> register(@Valid @RequestBody Register request, HttpServletRequest servletRequest) {
        return ApiResponse.success(userService.register(request), requestId(servletRequest));
    }

    @PostMapping("/login")
    public ApiResponse<?> login(@Valid @RequestBody Login request, HttpServletRequest servletRequest) {
        return ApiResponse.success(userService.login(request), requestId(servletRequest));
    }

    @PostMapping("/token/refresh")
    public ApiResponse<?> refresh(@Valid @RequestBody Refresh request,HttpServletRequest servletRequest){
        return ApiResponse.success(userService.refresh(request.refreshToken()),requestId(servletRequest));
    }

    @PostMapping("/logout")
    public ApiResponse<?> logout(@Valid @RequestBody Refresh request,HttpServletRequest servletRequest){
        return ApiResponse.success(userService.logout(request.refreshToken(),servletRequest),requestId(servletRequest));
    }

    @GetMapping("/me")
    public ApiResponse<?> me(HttpServletRequest servletRequest) {
        return ApiResponse.success(userService.me(servletRequest), requestId(servletRequest));
    }

    private String requestId(HttpServletRequest request) {
        Object id = request.getAttribute("X-Request-Id");
        return id == null ? "" : id.toString();
    }
}
