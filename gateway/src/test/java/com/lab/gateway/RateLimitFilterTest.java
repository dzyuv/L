package com.lab.gateway;
import com.lab.gateway.filter.RateLimitFilter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class RateLimitFilterTest {
    private final GatewayFilterChain chain = mock(GatewayFilterChain.class);

    @Test
    void usesRedisCounterWhenAvailable() {
        RateLimitFilter filter = filterWithRedis(Flux.just(2L), 1);

        assertThat(call(filter, "1.2.3.4", "172.18.0.2").getResponse().getStatusCode())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void fallsBackToLocalCounterWhenRedisFails() {
        RateLimitFilter filter = filterWithRedis(Flux.error(new IllegalStateException("redis down")), 1);

        assertThat(call(filter, "4.3.2.1", "172.18.0.2").getResponse().getStatusCode()).isNull();
        assertThat(call(filter, "4.3.2.1", "172.18.0.2").getResponse().getStatusCode())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @SuppressWarnings("unchecked")
    private RateLimitFilter filterWithRedis(Flux<Long> result, int limit) {
        ReactiveStringRedisTemplate redis = mock(ReactiveStringRedisTemplate.class);
        doReturn(result).when(redis).execute(any(), anyList(), anyList());
        ObjectProvider<ReactiveStringRedisTemplate> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(redis);
        return new RateLimitFilter(limit, provider);
    }

    @Test
    void allowsRequestsWithinLimit() {
        RateLimitFilter filter = new RateLimitFilter(2);

        assertThat(call(filter, "1.2.3.4", "172.18.0.2").getResponse().getStatusCode()).isNull();
        assertThat(call(filter, "1.2.3.4", "172.18.0.2").getResponse().getStatusCode()).isNull();
    }

    @Test
    void rejectsRequestsAboveLimit() {
        RateLimitFilter filter = new RateLimitFilter(1);

        call(filter, "5.6.7.8", "172.18.0.2");

        assertThat(call(filter, "5.6.7.8", "172.18.0.2").getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void tracksClientsSeparately() {
        RateLimitFilter filter = new RateLimitFilter(1);

        call(filter, "9.9.9.9", "172.18.0.2");

        assertThat(call(filter, "8.8.8.8", "172.18.0.2").getResponse().getStatusCode()).isNull();
    }

    @Test
    void ignoresForwardedForFromUntrustedClients() {
        RateLimitFilter filter = new RateLimitFilter(1);

        call(filter, "1.1.1.1", "203.0.113.10");

        assertThat(call(filter, "2.2.2.2", "203.0.113.10").getResponse().getStatusCode())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    private MockServerWebExchange call(RateLimitFilter filter, String forwardedFor, String remoteIp) {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/resources")
                .remoteAddress(new InetSocketAddress(remoteIp, 12345))
                .header("X-Forwarded-For", forwardedFor).build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        when(chain.filter(exchange)).thenReturn(Mono.empty());
        filter.filter(exchange, chain).block();
        return exchange;
    }
}
