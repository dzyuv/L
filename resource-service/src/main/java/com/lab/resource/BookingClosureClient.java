package com.lab.resource;

import com.lab.common.api.ApiResponse;
import com.lab.common.api.InternalServiceGuard;
import com.lab.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class BookingClosureClient {
    private final RestClient client;
    private final String internalToken;

    BookingClosureClient(@Value("${services.booking.base-url:http://localhost:8083}") String baseUrl,
                         @Value("${security.internal-token:}") String token) {
        client = RestClient.builder().baseUrl(baseUrl).build();
        internalToken = token;
    }

    public int cancelOverlapping(Long resourceId, LocalDateTime startTime, LocalDateTime endTime, String reason) {
        try {
            ApiResponse<Map<String, Object>> response = client.post()
                    .uri("/api/v1/internal/bookings/close-for-maintenance")
                    .header(InternalServiceGuard.HEADER, internalToken)
                    .body(new CloseForMaintenance(resourceId, startTime, endTime, reason))
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {});
            if (response == null || response.data() == null) return 0;
            Object count = response.data().get("cancelledCount");
            if (count instanceof Number number) return number.intValue();
            return 0;
        } catch (RestClientException exception) {
            throw new BusinessException("BOOKING_SERVICE_UNAVAILABLE", "Booking service is unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private record CloseForMaintenance(Long resourceId, LocalDateTime startTime, LocalDateTime endTime, String reason) {}
}
