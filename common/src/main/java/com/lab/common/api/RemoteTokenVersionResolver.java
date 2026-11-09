package com.lab.common.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class RemoteTokenVersionResolver implements TokenVersionResolver {
    private static final Logger log = LoggerFactory.getLogger(RemoteTokenVersionResolver.class);
    private static final long SUCCESS_CACHE_MS = 30_000;
    private static final long FAILURE_CACHE_MS = 3_000;

    private final RestClient client;
    private final String internalToken;
    private final ConcurrentHashMap<Long, CacheEntry> cache = new ConcurrentHashMap<>();

    public RemoteTokenVersionResolver(
            @Value("${USER_SERVICE_URL:http://localhost:8081}") String baseUrl,
            @Value("${security.internal-token:}") String internalToken) {
        this.client = RemoteClientFactory.create(baseUrl);
        this.internalToken = internalToken;
    }

    @Override
    public Integer currentVersion(Long userId) {
        long now = System.currentTimeMillis();
        CacheEntry cached = cache.get(userId);
        if (cached != null && now - cached.at < cached.ttl) return cached.version;
        try {
            ApiResponse<TokenVersion> response = client.get()
                    .uri("/api/v1/internal/users/{id}/token-version", userId)
                    .header(InternalServiceGuard.HEADER, internalToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<TokenVersion>>() {});
            TokenVersion value = response == null ? null : response.data();
            Integer version = value == null || !value.active() ? -1 : value.version();
            cache.put(userId, new CacheEntry(version, now, SUCCESS_CACHE_MS));
            return version;
        } catch (ResourceAccessException exception) {
            log.warn("User service is unavailable while resolving token version for user {}: {}", userId, exception.getMessage());
            cache.put(userId, new CacheEntry(null, now, FAILURE_CACHE_MS));
            return null;
        } catch (RestClientResponseException exception) {
            log.warn("User service rejected token version lookup for user {}: {}", userId, exception.getStatusCode());
            Integer version = exception.getStatusCode().is4xxClientError() ? -1 : null;
            cache.put(userId, new CacheEntry(version, now, version == null ? FAILURE_CACHE_MS : SUCCESS_CACHE_MS));
            return version;
        }
    }

    public record TokenVersion(int version, boolean active) {}

    private record CacheEntry(Integer version, long at, long ttl) {}
}
