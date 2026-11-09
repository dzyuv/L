package com.lab.common.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registers the shared request-id and JWT filters for every servlet service. */
@Configuration
public class WebFilterConfig {
    @Bean
    RequestIdFilter requestIdFilter() {
        return new RequestIdFilter();
    }

    @Bean
    JwtUserFilter jwtUserFilter(JwtKeyProvider keys, TokenVersionResolver tokenVersions) {
        return new JwtUserFilter(keys, tokenVersions);
    }
}
