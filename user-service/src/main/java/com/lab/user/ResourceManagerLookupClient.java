package com.lab.user;

import com.lab.common.api.ApiResponse;
import com.lab.common.api.InternalServiceGuard;
import com.lab.common.api.RemoteClientFactory;
import com.lab.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Component
public class ResourceManagerLookupClient {
    private final RestClient client;
    private final String internalToken;

    ResourceManagerLookupClient(@Value("${services.resource.base-url:http://localhost:8082}") String baseUrl,
                                @Value("${security.internal-token:}") String token) {
        this.client = RemoteClientFactory.create(baseUrl);
        this.internalToken = token;
    }

    public boolean isResourceManager(Long userId) {
        try {
            ApiResponse<Map<String, Object>> response = client.get()
                    .uri("/api/v1/internal/resources/managers/{id}/exists", userId)
                    .header(InternalServiceGuard.HEADER, internalToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {});
            Object exists = response == null || response.data() == null ? null : response.data().get("exists");
            return Boolean.TRUE.equals(exists);
        } catch (ResourceAccessException exception) {
            throw new BusinessException("RESOURCE_SERVICE_UNAVAILABLE", "Resource service is unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        } catch (RestClientResponseException exception) {
            throw new BusinessException("RESOURCE_SERVICE_UNAVAILABLE", "Resource service is unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
