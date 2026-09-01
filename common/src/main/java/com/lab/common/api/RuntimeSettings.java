package com.lab.common.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/** Reads live business parameters from system_config, falling back to local defaults. */
@Component
public class RuntimeSettings {
    private static final Logger log = LoggerFactory.getLogger(RuntimeSettings.class);
    private static final long CACHE_MS = 5000;

    private final RestClient client;
    private final String internalToken;
    private final int defaultCheckinBefore;
    private final int defaultCheckinAfter;
    private final int defaultViolationMax;
    private final int defaultRestrictionDays;
    private final int defaultApprovalTimeoutMinutes;
    private final AtomicReference<Cache> cache = new AtomicReference<>(new Cache(Map.of(), 0));

    public RuntimeSettings(@Value("${services.system.base-url:http://localhost:8087}") String baseUrl,
                           @Value("${security.internal-token:}") String internalToken,
                           @Value("${booking.checkin.before-minutes:15}") int defaultCheckinBefore,
                           @Value("${booking.checkin.after-minutes:30}") int defaultCheckinAfter,
                           @Value("${booking.violation.max-count:3}") int defaultViolationMax,
                           @Value("${booking.violation.restriction-days:30}") int defaultRestrictionDays,
                           @Value("${approval.timeout-hours:24}") int defaultTimeoutHours) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
        this.internalToken = internalToken;
        this.defaultCheckinBefore = defaultCheckinBefore;
        this.defaultCheckinAfter = defaultCheckinAfter;
        this.defaultViolationMax = defaultViolationMax;
        this.defaultRestrictionDays = defaultRestrictionDays;
        this.defaultApprovalTimeoutMinutes = Math.max(1, defaultTimeoutHours) * 60;
    }

    public int checkinBeforeMinutes() {
        return intValue("checkin.window.before_minutes", defaultCheckinBefore, 0, 24 * 60);
    }

    public int checkinAfterMinutes() {
        return intValue("checkin.window.after_minutes", defaultCheckinAfter, 0, 24 * 60);
    }

    public int violationMaxCount() {
        return intValue("violation.max_count", defaultViolationMax, 1, 100);
    }

    public int restrictionDays() {
        return intValue("violation.restriction_days", defaultRestrictionDays, 1, 3650);
    }

    public int approvalTimeoutMinutes() {
        return intValue("approval.timeout_minutes", defaultApprovalTimeoutMinutes, 1, 60 * 24 * 30);
    }

    private int intValue(String key, int fallback, int min, int max) {
        String raw = snapshot().get(key);
        if (raw == null || raw.isBlank()) return fallback;
        try {
            int value = Integer.parseInt(raw.trim());
            if (value < min || value > max) return fallback;
            return value;
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private Map<String, String> snapshot() {
        Cache current = cache.get();
        long now = System.currentTimeMillis();
        if (now - current.at < CACHE_MS) return current.values;
        try {
            ApiResponse<Map<String, String>> response = client.get()
                    .uri("/api/v1/internal/configs")
                    .header(InternalServiceGuard.HEADER, internalToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            Map<String, String> values = response == null || response.data() == null ? Map.of() : response.data();
            cache.set(new Cache(values, now));
            return values;
        } catch (RuntimeException exception) {
            log.warn("Cannot refresh system_config, using {} defaults/cache: {}", current.values.isEmpty() ? "yml" : "cached", exception.getMessage());
            if (current.values.isEmpty()) cache.set(new Cache(Map.of(), now));
            else cache.set(new Cache(current.values, now));
            return cache.get().values;
        }
    }

    private record Cache(Map<String, String> values, long at) {}
}
