package com.lab.common.api;

import com.lab.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Guards endpoints intended only for service-to-service calls. */
@Component
public class InternalServiceGuard {
    public static final String HEADER = "X-Internal-Service-Token";
    private final byte[] token;

    public InternalServiceGuard(@Value("${security.internal-token:}") String configuredToken) {
        if (configuredToken.length() < 32) {
            throw new IllegalStateException("INTERNAL_SERVICE_TOKEN must contain at least 32 characters");
        }
        token = configuredToken.getBytes(StandardCharsets.UTF_8);
    }

    public void require(HttpServletRequest request) {
        String supplied = request.getHeader(HEADER);
        if (supplied == null || !MessageDigest.isEqual(token, supplied.getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException("INTERNAL_AUTH_REQUIRED", "Service authentication is required", HttpStatus.FORBIDDEN);
        }
    }
}
