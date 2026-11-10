package com.lab.system.controller;

import com.lab.common.api.ApiResponse;
import com.lab.system.service.SystemConfigService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import java.util.Objects;
import java.util.Map;

@RestController @RequestMapping("/api/v1/admin")
public class SystemController {
    private final SystemConfigService service;
    public SystemController(SystemConfigService service) { this.service = service; }
    @GetMapping("/configs")
    public ApiResponse<?> configs(HttpServletRequest request) { return ApiResponse.success(service.configs(request), Objects.toString(request.getAttribute("X-Request-Id"), "")); }
    @PutMapping("/configs/{key}")
    public ApiResponse<?> update(@PathVariable("key") String key, @RequestBody Map<String, String> body, HttpServletRequest request) { return ApiResponse.success(service.update(key, body.get("value"), request), Objects.toString(request.getAttribute("X-Request-Id"), "")); }
}
