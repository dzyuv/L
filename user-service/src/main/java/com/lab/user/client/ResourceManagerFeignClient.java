package com.lab.user.client;
import com.lab.user.entity.*;
import com.lab.user.repository.*;
import com.lab.user.seed.*;
import com.lab.user.scheduler.*;
import com.lab.user.security.*;
import com.lab.user.config.*;
import com.lab.user.controller.*;
import com.lab.user.service.*;
import com.lab.user.service.impl.*;

import com.lab.common.api.ApiResponse;
import com.lab.common.feign.FeignInternalConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "resource-service",
        url = "${services.resource.base-url:http://localhost:8082}",
        configuration = FeignInternalConfig.class)
public interface ResourceManagerFeignClient {
    @GetMapping("/api/v1/internal/resources/managers/{id}/exists")
    ApiResponse<Map<String, Object>> managerExists(@PathVariable("id") Long userId);
}
