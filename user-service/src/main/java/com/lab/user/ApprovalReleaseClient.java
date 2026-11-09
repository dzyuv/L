package com.lab.user;

import com.lab.common.api.InternalServiceGuard;
import com.lab.common.api.RemoteClientFactory;
import com.lab.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class ApprovalReleaseClient {
    private final RestClient client;
    private final String internalToken;

    ApprovalReleaseClient(@Value("${services.booking.base-url:http://localhost:8083}") String baseUrl,
                          @Value("${security.internal-token:}") String token) {
        this.client = RemoteClientFactory.create(baseUrl);
        this.internalToken = token;
    }

    public void release(Long userId) {
        try {
            client.post().uri("/api/v1/internal/approvals/release/{id}", userId)
                    .header(InternalServiceGuard.HEADER, internalToken)
                    .retrieve().toBodilessEntity();
        } catch (ResourceAccessException exception) {
            throw new BusinessException("BOOKING_SERVICE_UNAVAILABLE", "Booking service is unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        } catch (RestClientResponseException exception) {
            throw new BusinessException("BOOKING_SERVICE_UNAVAILABLE", "Booking service is unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
