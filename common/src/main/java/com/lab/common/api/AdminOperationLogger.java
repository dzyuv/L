package com.lab.common.api;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.Objects;

/** Sends non-sensitive administrator operation summaries to system-service. */
@Component
public class AdminOperationLogger {
    private static final Logger log = LoggerFactory.getLogger(AdminOperationLogger.class);
    private final RestClient client;
    private final String internalToken;

    public AdminOperationLogger(@Value("${services.system.base-url:http://localhost:8087}") String baseUrl,
                                @Value("${security.internal-token:}") String internalToken) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
        this.internalToken = internalToken;
    }

    public void success(HttpServletRequest request, String operationType, String targetType,
                        Long targetId, Map<String, Object> detail) {
        if (!(request.getAttribute("userId") instanceof Long operatorId)) return;
        OperationLogRequest body = new OperationLogRequest(
                operatorId,
                operationType,
                targetType,
                targetId,
                "SUCCESS",
                null,
                Objects.toString(request.getAttribute(RequestIdFilter.HEADER), ""),
                clientIp(request),
                detail == null ? Map.of() : detail
        );
        try {
            client.post().uri("/api/v1/internal/operation-logs")
                    .header(HttpHeaders.AUTHORIZATION, Objects.toString(request.getHeader(HttpHeaders.AUTHORIZATION), ""))
                    .header(InternalServiceGuard.HEADER, internalToken)
                    .body(body).retrieve().toBodilessEntity();
        } catch (RestClientException exception) {
            // An audit-service outage must not roll back the completed business operation.
            log.warn("Unable to persist administrator operation log: {}", operationType, exception);
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        return request.getRemoteAddr();
    }

    public record OperationLogRequest(Long operatorId, String operationType, String targetType,
                                      Long targetId, String result, String reason, String requestId,
                                      String ip, Map<String, Object> detail) {}
}
