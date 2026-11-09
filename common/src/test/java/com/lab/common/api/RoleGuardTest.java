package com.lab.common.api;

import com.lab.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoleGuardTest {
    private final RoleGuard guard = new RoleGuard();
    private final HttpServletRequest request = mock(HttpServletRequest.class);

    @Test
    void requireAnyAllowsMatchingRole() {
        when(request.getAttribute("roles")).thenReturn(List.of(Roles.STUDENT, Roles.TEACHER));

        assertThatCode(() -> guard.requireAny(request, Roles.TEACHER, Roles.LAB_ADMIN))
                .doesNotThrowAnyException();
    }

    @Test
    void requireAnyRejectsMissingRole() {
        when(request.getAttribute("roles")).thenReturn(List.of(Roles.STUDENT));

        BusinessException exception = catchThrowableOfType(
                () -> guard.requireAny(request, Roles.LAB_ADMIN), BusinessException.class);

        assertThat(exception.code()).isEqualTo("FORBIDDEN");
        assertThat(exception.status()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void currentUserIdReturnsAuthenticatedUser() {
        when(request.getAttribute("userId")).thenReturn(42L);

        assertThat(guard.currentUserId(request)).isEqualTo(42L);
    }

    @Test
    void currentUserIdRejectsAnonymousRequest() {
        BusinessException exception = catchThrowableOfType(
                () -> guard.currentUserId(request), BusinessException.class);

        assertThat(exception.code()).isEqualTo("UNAUTHORIZED");
        assertThat(exception.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rolesTreatMissingAttributeAsEmpty() {
        assertThat(guard.roles(request)).isEmpty();
        assertThat(guard.hasRole(request, Roles.STUDENT)).isFalse();
    }
}
