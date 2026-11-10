package com.lab.resource.client;

import com.lab.common.api.ApiResponse;
import com.lab.common.feign.FeignInternalConfig;
import com.lab.resource.client.UserLookupClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service",
        url = "${services.user.base-url:http://localhost:8081}",
        configuration = FeignInternalConfig.class)
public interface UserSummaryFeignClient {
    @GetMapping("/api/v1/internal/users/{id}/summary")
    ApiResponse<UserLookupClient.UserSummary> summary(@PathVariable("id") Long id);
}
