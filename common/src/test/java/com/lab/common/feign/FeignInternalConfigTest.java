package com.lab.common.feign;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.common.api.InternalServiceGuard;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.codec.ErrorDecoder;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class FeignInternalConfigTest {
    @Test
    void addsInternalServiceTokenHeader() {
        FeignInternalConfig config = new FeignInternalConfig("secret-token-value");
        RequestTemplate template = new RequestTemplate();

        config.internalHeaders().apply(template);

        assertThat(template.headers().get(InternalServiceGuard.HEADER))
                .containsExactly("secret-token-value");
    }

    @Test
    void registersBeansAsLiteConfiguration() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(ObjectMapper.class);
            context.register(FeignInternalConfig.class);
            context.refresh();

            assertThat(context.getBean(RequestInterceptor.class)).isNotNull();
            assertThat(context.getBean(ErrorDecoder.class)).isNotNull();
        }
    }
}
