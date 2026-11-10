package com.lab.common.feign;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.common.api.InternalServiceGuard;
import com.lab.common.api.RequestIdFilter;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Per-client Feign configuration. Kept as a lite configuration class (no @Configuration)
 * so it is only instantiated inside each Feign child context, not component-scanned globally.
 */
public class FeignInternalConfig {
    private final String internalToken;

    public FeignInternalConfig(@Value("${security.internal-token:}") String internalToken) {
        this.internalToken = internalToken;
    }

    @Bean
    public RequestInterceptor internalHeaders() {
        return template -> {
            template.header(InternalServiceGuard.HEADER, internalToken);
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                Object requestId = attributes.getRequest().getAttribute(RequestIdFilter.HEADER);
                if (requestId != null && !requestId.toString().isBlank()) {
                    template.header(RequestIdFilter.HEADER, requestId.toString());
                }
            }
        };
    }

    @Bean
    public ErrorDecoder apiErrorDecoder(ObjectMapper objectMapper) {
        return new ApiErrorDecoder(objectMapper);
    }
}
