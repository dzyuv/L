package com.lab.booking.client;

import com.lab.booking.ResourceRuleClient;
import com.lab.common.api.ApiResponse;
import com.lab.common.feign.FeignInternalConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "resource-service",
        url = "${services.resource.base-url:http://localhost:8082}",
        configuration = FeignInternalConfig.class)
public interface ResourceRuleFeignClient {
    @GetMapping("/api/v1/internal/resources/{id}/booking-rule")
    ApiResponse<ResourceRuleClient.BookingRule> bookingRule(
            @PathVariable("id") Long id,
            @RequestParam("startTime") String startTime,
            @RequestParam("endTime") String endTime,
            @RequestParam("participants") int participants,
            @RequestParam("applicantUserId") Long applicantUserId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization);
}
