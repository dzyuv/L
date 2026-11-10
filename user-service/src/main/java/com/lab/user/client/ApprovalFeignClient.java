package com.lab.user.client;

import com.lab.common.api.ApiResponse;
import com.lab.common.feign.FeignInternalConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Map;

@FeignClient(name = "booking-service",
        url = "${services.booking.base-url:http://localhost:8083}",
        configuration = FeignInternalConfig.class)
public interface ApprovalFeignClient {
    @PostMapping("/api/v1/internal/approvals/release/{id}")
    ApiResponse<Map<String, Object>> release(@PathVariable("id") Long userId);
}
