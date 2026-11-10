package com.lab.common.feign;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.common.exception.BusinessException;
import feign.Response;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiErrorDecoderTest {
    @Test
    void mapsApiResponseErrorToBusinessException() throws Exception {
        Response response = mock(Response.class);
        Response.Body body = mock(Response.Body.class);
        when(response.status()).thenReturn(422);
        when(response.body()).thenReturn(body);
        when(body.asInputStream()).thenReturn(new ByteArrayInputStream(
                "{\"code\":\"INVALID_SLOT\",\"message\":\"bad slot\"}".getBytes(StandardCharsets.UTF_8)));

        Exception decoded = new ApiErrorDecoder(new ObjectMapper()).decode("Test#method()", response);

        assertThat(decoded).isInstanceOf(BusinessException.class);
        BusinessException business = (BusinessException) decoded;
        assertThat(business.code()).isEqualTo("INVALID_SLOT");
        assertThat(business.status()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
