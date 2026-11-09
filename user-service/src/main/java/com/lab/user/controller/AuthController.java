package com.lab.user.controller;

import com.lab.common.api.ApiResponse;
import com.lab.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/user")
public class AuthController {
    private static final String REFRESH_COOKIE = "refreshToken";
    private static final String REFRESH_COOKIE_PATH = "/api/v1/user";

    private final UserService userService;
    private final int refreshTokenDays;
    private final boolean refreshCookieSecure;
    private final String refreshCookieSameSite;

    public AuthController(UserService userService,
                          @Value("${security.jwt.refresh-token-days:30}") int refreshTokenDays,
                          @Value("${security.jwt.refresh-cookie-secure:false}") boolean refreshCookieSecure,
                          @Value("${security.jwt.refresh-cookie-same-site:Lax}") String refreshCookieSameSite) {
        this.userService = userService;
        this.refreshTokenDays = refreshTokenDays;
        this.refreshCookieSecure = refreshCookieSecure;
        this.refreshCookieSameSite = refreshCookieSameSite;
    }

    public record Register(@NotBlank @Size(max=50) String employeeNo,
                           @NotBlank @Size(max=50) String realName,
                           @NotBlank @Size(min=8,max=72) String password,
                           @NotBlank @Email @Size(max=100) String email,
                           @NotBlank @Size(max=30) String phone) {}

    public record Login(@NotBlank String username, @NotBlank String password) {}

    public record Refresh(String refreshToken) {}

    @PostMapping("/register")
    public ApiResponse<?> register(@Valid @RequestBody Register request, HttpServletRequest servletRequest) {
        return ApiResponse.success(userService.register(request), requestId(servletRequest));
    }

    @PostMapping("/login")
    public ApiResponse<?> login(@Valid @RequestBody Login request, HttpServletResponse servletResponse,
                                HttpServletRequest servletRequest) {
        Map<String, Object> result = new LinkedHashMap<>(userService.login(request));
        setRefreshCookie(servletResponse, Objects.toString(result.remove(REFRESH_COOKIE), ""));
        return ApiResponse.success(result, requestId(servletRequest));
    }

    @PostMapping("/token/refresh")
    public ApiResponse<?> refresh(@CookieValue(value = REFRESH_COOKIE, required = false) String cookieToken,
                                  @RequestBody(required = false) Refresh request,
                                  HttpServletResponse servletResponse, HttpServletRequest servletRequest) {
        Map<String, Object> result = new LinkedHashMap<>(userService.refresh(
                firstNonBlank(cookieToken, request == null ? null : request.refreshToken())));
        setRefreshCookie(servletResponse, Objects.toString(result.remove(REFRESH_COOKIE), ""));
        return ApiResponse.success(result, requestId(servletRequest));
    }

    @PostMapping("/logout")
    public ApiResponse<?> logout(@CookieValue(value = REFRESH_COOKIE, required = false) String cookieToken,
                                 @RequestBody(required = false) Refresh request,
                                 HttpServletResponse servletResponse, HttpServletRequest servletRequest) {
        try {
            String refreshToken = firstNonBlank(cookieToken, request == null ? null : request.refreshToken());
            return ApiResponse.success(userService.logout(refreshToken, servletRequest), requestId(servletRequest));
        } finally {
            clearRefreshCookie(servletResponse);
        }
    }

    @GetMapping("/me")
    public ApiResponse<?> me(HttpServletRequest servletRequest) {
        return ApiResponse.success(userService.me(servletRequest), requestId(servletRequest));
    }

    private void setRefreshCookie(HttpServletResponse response, String token) {
        if (token == null || token.isBlank()) return;
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, token)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path(REFRESH_COOKIE_PATH)
                .maxAge(Duration.ofDays(refreshTokenDays))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path(REFRESH_COOKIE_PATH)
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private String requestId(HttpServletRequest request) {
        Object id = request.getAttribute("X-Request-Id");
        return id == null ? "" : id.toString();
    }
}
