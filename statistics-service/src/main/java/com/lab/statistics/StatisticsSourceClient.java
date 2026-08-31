package com.lab.statistics;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.common.api.ApiResponse;
import com.lab.common.api.InternalServiceGuard;
import com.lab.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
public class StatisticsSourceClient {
    private final RestClient resources;
    private final RestClient bookings;
    private final ObjectMapper json;
    private final String internalToken;

    StatisticsSourceClient(@Value("${services.resource.base-url:http://localhost:8082}") String resourceBaseUrl,
                           @Value("${services.booking.base-url:http://localhost:8083}") String bookingBaseUrl,
                           @Value("${security.internal-token:}") String token,
                           ObjectMapper objectMapper) {
        resources = RestClient.builder().baseUrl(resourceBaseUrl).build();
        bookings = RestClient.builder().baseUrl(bookingBaseUrl).build();
        json = objectMapper;
        internalToken = token;
    }

    public List<Map<String, Object>> resourceCatalog() {
        try {
            return list(resources.get().uri("/api/v1/internal/resources/statistics-catalog")
                    .header(InternalServiceGuard.HEADER, internalToken)
                    .retrieve().body(String.class));
        } catch (RestClientException exception) {
            throw new BusinessException("RESOURCE_SERVICE_UNAVAILABLE", "Resource service is unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> bookingSource(LocalDateTime start, LocalDateTime end) {
        try {
            String body = bookings.get()
                    .uri(builder -> builder.path("/api/v1/internal/bookings/statistics-source")
                            .queryParam("start", start)
                            .queryParam("end", end)
                            .build())
                    .header(InternalServiceGuard.HEADER, internalToken)
                    .retrieve().body(String.class);
            ApiResponse<Map<String, Object>> response = json.readValue(body, new TypeReference<>() {});
            if (response == null || response.data() == null) return Map.of("bookings", List.of(), "violations", List.of());
            return response.data();
        } catch (RestClientException exception) {
            throw new BusinessException("BOOKING_SERVICE_UNAVAILABLE", "Booking service is unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        } catch (Exception exception) {
            throw new BusinessException("INTERNAL_ERROR", "Cannot read booking statistics source", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private List<Map<String, Object>> list(String body) {
        try {
            ApiResponse<List<Map<String, Object>>> response = json.readValue(body, new TypeReference<>() {});
            if (response == null || response.data() == null) return List.of();
            return response.data();
        } catch (RestClientException exception) {
            throw new BusinessException("RESOURCE_SERVICE_UNAVAILABLE", "Resource service is unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        } catch (Exception exception) {
            throw new BusinessException("INTERNAL_ERROR", "Cannot read resource statistics catalog", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
