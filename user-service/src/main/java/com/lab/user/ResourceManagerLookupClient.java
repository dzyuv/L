package com.lab.user;

import com.lab.common.api.ApiResponse;
import com.lab.common.exception.BusinessException;
import com.lab.user.client.ResourceManagerFeignClient;
import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ResourceManagerLookupClient {
    private final ResourceManagerFeignClient feign;

    ResourceManagerLookupClient(ResourceManagerFeignClient feign) {
        this.feign = feign;
    }

    public boolean isResourceManager(Long userId) {
        try {
            ApiResponse<Map<String, Object>> response = feign.managerExists(userId);
            Object exists = response == null || response.data() == null ? null : response.data().get("exists");
            return Boolean.TRUE.equals(exists);
        } catch (BusinessException | FeignException exception) {
            throw new BusinessException("RESOURCE_SERVICE_UNAVAILABLE", "Resource service is unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
