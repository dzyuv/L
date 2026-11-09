package com.lab.gateway;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Distributed fixed-window rate limiter backed by Redis, with an in-process fallback. */
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {
    private static final long WINDOW_MS = 60_000;
    private static final int MAX_TRACKED_CLIENTS = 10_000;
    private static final long REDIS_RETRY_MS = 30_000;
    private static final RedisScript<Long> INCREMENT_WITH_TTL = new DefaultRedisScript<>(
            "local current = redis.call('INCR', KEYS[1]) "
                    + "if current == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end "
                    + "return current", Long.class);

    private final int requestsPerMinute;
    private final ReactiveStringRedisTemplate redis;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private volatile long redisDisabledUntil;

    public RateLimitFilter(int requestsPerMinute) {
        this(requestsPerMinute, (ReactiveStringRedisTemplate) null);
    }

    @Autowired
    public RateLimitFilter(@Value("${gateway.rate-limit.requests-per-minute:120}") int requestsPerMinute,
                           ObjectProvider<ReactiveStringRedisTemplate> redisProvider) {
        this(requestsPerMinute, redisProvider.getIfAvailable());
    }

    private RateLimitFilter(int requestsPerMinute, ReactiveStringRedisTemplate redis) {
        this.requestsPerMinute = Math.max(1, requestsPerMinute);
        this.redis = redis;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (path.startsWith("/actuator/")) return chain.filter(exchange);
        String client = clientKey(exchange);
        if (redis == null || System.currentTimeMillis() < redisDisabledUntil) {
            return localFilter(client, exchange, chain);
        }
        long window = System.currentTimeMillis() / WINDOW_MS;
        String key = "rate:limit:" + window + ":" + client;
        return redis.execute(INCREMENT_WITH_TTL, List.of(key), List.of(String.valueOf(WINDOW_MS)))
                .next()
                .defaultIfEmpty(-1L)
                .onErrorResume(error -> {
                    redisDisabledUntil = System.currentTimeMillis() + REDIS_RETRY_MS;
                    return Mono.just(-1L);
                })
                .flatMap(count -> count < 0
                        ? localFilter(client, exchange, chain)
                        : count > requestsPerMinute ? tooMany(exchange) : chain.filter(exchange));
    }

    private Mono<Void> localFilter(String client, ServerWebExchange exchange, GatewayFilterChain chain) {
        long now = System.currentTimeMillis();
        Window window = windows.compute(client, (key, current) ->
                current == null || now - current.start >= WINDOW_MS ? new Window(now, new AtomicInteger()) : current);
        if (window.count.incrementAndGet() > requestsPerMinute) return tooMany(exchange);
        if (windows.size() > MAX_TRACKED_CLIENTS) {
            windows.entrySet().removeIf(entry -> now - entry.getValue().start >= WINDOW_MS);
        }
        return chain.filter(exchange);
    }

    private Mono<Void> tooMany(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        return exchange.getResponse().setComplete();
    }

    private String clientKey(ServerWebExchange exchange) {
        String remote = remoteHost(exchange);
        if (!isTrustedProxy(remote)) return remote;
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        String realIp = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) return realIp.trim();
        return remote;
    }

    private String remoteHost(ServerWebExchange exchange) {
        return exchange.getRequest().getRemoteAddress() == null
                || exchange.getRequest().getRemoteAddress().getAddress() == null
                ? "unknown"
                : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
    }

    private boolean isTrustedProxy(String host) {
        if (host == null || "unknown".equals(host)) return false;
        if ("127.0.0.1".equals(host) || "::1".equals(host)) return true;
        if (host.startsWith("10.") || host.startsWith("192.168.")) return true;
        if (host.startsWith("172.")) {
            String[] parts = host.split("\\.");
            if (parts.length > 1) {
                try {
                    int second = Integer.parseInt(parts[1]);
                    return second >= 16 && second <= 31;
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }
        }
        return host.startsWith("fc") || host.startsWith("fd") || host.startsWith("fe80:");
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    private record Window(long start, AtomicInteger count) {}
}
