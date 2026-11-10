package com.lab.resource;

import com.lab.common.api.ApiResponse;
import com.lab.common.exception.BusinessException;
import com.lab.resource.client.UserSummaryFeignClient;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class UserLookupClient {
    private static final Logger log = LoggerFactory.getLogger(UserLookupClient.class);
    private static final long ROUTING_CACHE_MS = 30_000;
    public record UserSummary(Long id, String username, String realName, String status,
                              boolean deleted, List<String> roles) {}
    public record LookupResult(UserSummary summary, boolean available) {}

    private final UserSummaryFeignClient feign;
    private final ConcurrentHashMap<Long, CacheEntry> routingCache = new ConcurrentHashMap<>();

    UserLookupClient(UserSummaryFeignClient feign) {
        this.feign = feign;
    }

    public UserSummary find(Long userId) {
        try {
            ApiResponse<UserSummary> response = feign.summary(userId);
            if (response == null || response.data() == null) {
                throw new BusinessException("USER_SERVICE_UNAVAILABLE", "User service returned an empty response", HttpStatus.SERVICE_UNAVAILABLE);
            }
            return response.data();
        } catch (BusinessException exception) {
            throw exception;
        } catch (FeignException exception) {
            if (exception.status() == 404) {
                throw new BusinessException("USER_NOT_FOUND", "User does not exist", HttpStatus.NOT_FOUND);
            }
            throw new BusinessException("USER_SERVICE_UNAVAILABLE", "User service is unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    /** Used on the booking hot path: caches successful lookups and fails open when user-service is unavailable. */
    public LookupResult lookupForRouting(Long userId) {
        long now = System.currentTimeMillis();
        CacheEntry cached = routingCache.get(userId);
        if (cached != null && now - cached.at < ROUTING_CACHE_MS) {
            return new LookupResult(cached.summary, true);
        }
        try {
            UserSummary summary = find(userId);
            routingCache.put(userId, new CacheEntry(summary, now));
            return new LookupResult(summary, true);
        } catch (BusinessException exception) {
            if ("USER_NOT_FOUND".equals(exception.code())) {
                routingCache.put(userId, new CacheEntry(null, now));
                return new LookupResult(null, true);
            }
            log.warn("Cannot verify approval eligibility for user {}: {}", userId, exception.getMessage());
            return new LookupResult(null, false);
        }
    }

    private record CacheEntry(UserSummary summary, long at) {}
}
