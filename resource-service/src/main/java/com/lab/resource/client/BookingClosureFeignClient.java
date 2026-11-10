package com.lab.resource.client;

import com.lab.common.api.ApiResponse;
import com.lab.common.feign.FeignInternalConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDateTime;
import java.util.Map;

@FeignClient(name = "booking-service",
        url = "${services.booking.base-url:http://localhost:8083}",
        configuration = FeignInternalConfig.class)
public interface BookingClosureFeignClient {
    @PostMapping("/api/v1/internal/bookings/close-for-maintenance")
    ApiResponse<Map<String, Object>> closeForMaintenance(@RequestBody CloseForMaintenance body);

    @PostMapping("/api/v1/internal/bookings/restore-for-maintenance")
    ApiResponse<Map<String, Object>> restoreForMaintenance(@RequestBody CloseForMaintenance body);

    record CloseForMaintenance(Long resourceId, LocalDateTime startTime, LocalDateTime endTime, String reason) {}
}
