package com.lab.statistics.controller;

import com.lab.common.api.ApiResponse;
import com.lab.statistics.service.StatisticsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import java.util.Objects;

@RestController @RequestMapping("/api/v1/statistics")
public class StatisticsController {
    private final StatisticsService service;
    public StatisticsController(StatisticsService service) { this.service = service; }
    @GetMapping({"/usage", "/occupancy", "/trend"})
    public ApiResponse<?> usage(HttpServletRequest request) { return ApiResponse.success(service.usage(request), Objects.toString(request.getAttribute("X-Request-Id"), "")); }
}
