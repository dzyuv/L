package com.lab.common.feign;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.common.api.ApiResponse;
import com.lab.common.exception.BusinessException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.http.HttpStatus;

import java.io.InputStream;

/** Converts the shared ApiResponse error contract into BusinessException. */
public class ApiErrorDecoder implements ErrorDecoder {
    private final ObjectMapper objectMapper;
    private final ErrorDecoder defaultDecoder = new ErrorDecoder.Default();

    public ApiErrorDecoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.body() != null) {
            try (InputStream body = response.body().asInputStream()) {
                ApiResponse<?> error = objectMapper.readValue(body, ApiResponse.class);
                if (error.code() != null && error.message() != null) {
                    return new BusinessException(error.code(), error.message(), HttpStatus.valueOf(response.status()));
                }
            } catch (Exception ignored) {
                // Fall back to Feign's default decoder for non-API error bodies.
            }
        }
        return defaultDecoder.decode(methodKey, response);
    }
}
