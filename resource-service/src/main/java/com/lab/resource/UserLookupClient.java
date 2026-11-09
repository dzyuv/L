package com.lab.resource;

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

import java.util.List;

@Component
public class UserLookupClient {
    public record UserSummary(Long id, String username, String realName, String status,
                              boolean deleted, List<String> roles) {}

    private final RestClient client;
    private final String internalToken;

    UserLookupClient(@Value("${services.user.base-url:http://localhost:8081}") String baseUrl,
                     @Value("${security.internal-token:}") String token) {
        this.client = RemoteClientFactory.create(baseUrl);
        this.internalToken = token;
    }

    public UserSummary find(Long userId) {
        try {
            ApiResponse<UserSummary> response = client.get()
                    .uri("/api/v1/internal/users/{id}/summary", userId)
                    .header(InternalServiceGuard.HEADER, internalToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<UserSummary>>() {});
            if (response == null || response.data() == null) {
                throw new BusinessException("USER_SERVICE_UNAVAILABLE", "User service returned an empty response", HttpStatus.SERVICE_UNAVAILABLE);
            }
            return response.data();
        } catch (ResourceAccessException exception) {
            throw new BusinessException("USER_SERVICE_UNAVAILABLE", "User service is unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) {
                throw new BusinessException("USER_NOT_FOUND", "User does not exist", HttpStatus.NOT_FOUND);
            }
            throw new BusinessException("USER_SERVICE_UNAVAILABLE", "User service is unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
