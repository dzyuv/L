package com.lab.common.api;

import com.lab.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InternalServiceGuardTest {
    private static final String TOKEN = "0123456789abcdef0123456789abcdef";

    @Test
    void constructorRejectsShortToken() {
        assertThatThrownBy(() -> new InternalServiceGuard("too-short"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }

    @Test
    void requireAcceptsMatchingToken() {
        InternalServiceGuard guard = new InternalServiceGuard(TOKEN);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(InternalServiceGuard.HEADER)).thenReturn(TOKEN);

        assertThatCode(() -> guard.require(request)).doesNotThrowAnyException();
    }

    @Test
    void requireRejectsMissingToken() {
        InternalServiceGuard guard = new InternalServiceGuard(TOKEN);
        HttpServletRequest request = mock(HttpServletRequest.class);

        BusinessException exception = catchThrowableOfType(() -> guard.require(request), BusinessException.class);

        assertThat(exception.code()).isEqualTo("INTERNAL_AUTH_REQUIRED");
        assertThat(exception.status()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void requireRejectsWrongToken() {
        InternalServiceGuard guard = new InternalServiceGuard(TOKEN);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(InternalServiceGuard.HEADER)).thenReturn(TOKEN + "-different");

        BusinessException exception = catchThrowableOfType(() -> guard.require(request), BusinessException.class);

        assertThat(exception.code()).isEqualTo("INTERNAL_AUTH_REQUIRED");
    }
}
