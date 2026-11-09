package com.lab.resource;

import com.lab.common.api.ApiResponse;
import com.lab.common.api.InternalServiceGuard;
import com.lab.common.api.RemoteClientFactory;
import com.lab.common.exception.BusinessException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
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
        client = RemoteClientFactory.create(baseUrl);
        internalToken = token;
    }

    @CircuitBreaker(name = "bookingService", fallbackMethod = "cancelFallback")
    @Retry(name = "bookingService")
    public int cancelOverlapping(Long resourceId, LocalDateTime startTime, LocalDateTime endTime, String reason) {
        return postCount("/api/v1/internal/bookings/close-for-maintenance",
                new CloseForMaintenance(resourceId, startTime, endTime, reason), "cancelledCount");
    }

    @CircuitBreaker(name = "bookingService", fallbackMethod = "restoreFallback")
    @Retry(name = "bookingService")
    public int restoreOverlapping(Long resourceId, LocalDateTime startTime, LocalDateTime endTime) {
        return postCount("/api/v1/internal/bookings/restore-for-maintenance",
                new CloseForMaintenance(resourceId, startTime, endTime, null), "restoredCount");
    }

    private int postCount(String uri, CloseForMaintenance body, String countKey) {
        try {
            ApiResponse<Map<String, Object>> response = client.post()
                    .uri(uri)
                    .header(InternalServiceGuard.HEADER, internalToken)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {});
            if (response == null || response.data() == null) return 0;
            Object count = response.data().get(countKey);
            if (count instanceof Number number) return number.intValue();
            return 0;
        } catch (ResourceAccessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new BusinessException("BOOKING_SERVICE_UNAVAILABLE", "Booking service is unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private int cancelFallback(Long resourceId, LocalDateTime startTime, LocalDateTime endTime, String reason, Throwable throwable) {
        throw new BusinessException("BOOKING_SERVICE_UNAVAILABLE", "Booking service is unavailable", HttpStatus.SERVICE_UNAVAILABLE);
    }

    private int restoreFallback(Long resourceId, LocalDateTime startTime, LocalDateTime endTime, Throwable throwable) {
        throw new BusinessException("BOOKING_SERVICE_UNAVAILABLE", "Booking service is unavailable", HttpStatus.SERVICE_UNAVAILABLE);
    }

    private record CloseForMaintenance(Long resourceId, LocalDateTime startTime, LocalDateTime endTime, String reason) {}
}
