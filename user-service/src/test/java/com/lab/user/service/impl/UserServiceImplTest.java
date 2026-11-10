package com.lab.user.service.impl;
import com.lab.user.entity.*;
import com.lab.user.repository.*;
import com.lab.user.client.*;
import com.lab.user.seed.*;
import com.lab.user.scheduler.*;
import com.lab.user.security.*;
import com.lab.user.config.*;
import com.lab.user.controller.*;
import com.lab.user.service.*;

import com.lab.common.api.JwtKeyProvider;
import com.lab.common.exception.BusinessException;
import com.lab.user.client.ApprovalReleaseClient;
import com.lab.user.repository.RefreshTokenRepository;
import com.lab.user.repository.RoleRepository;
import com.lab.user.entity.User;
import com.lab.user.repository.UserRepository;
import com.lab.user.controller.AuthController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock UserRepository users;
    @Mock RoleRepository roles;
    @Mock PasswordEncoder encoder;
    @Mock RefreshTokenRepository refreshTokens;
    @Mock ApprovalReleaseClient approvals;

    private final JwtKeyProvider keys = new JwtKeyProvider("0123456789abcdef0123456789abcdef");
    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(users, roles, encoder, keys, refreshTokens, approvals, 2, 30, 3, 15);
    }

    @Test
    void failedPasswordBelowThresholdIncrementsCounter() {
        User user = user("ACTIVE", 0, null);
        when(users.findByUsernameAndDeletedFalseForUpdate("u")).thenReturn(Optional.of(user));
        when(encoder.matches("bad", "hash")).thenReturn(false);

        BusinessException exception = catchThrowableOfType(() -> service.login(login("bad")), BusinessException.class);

        assertThat(exception.code()).isEqualTo("LOGIN_FAILED");
        assertThat(exception.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(user.failedLoginCount).isEqualTo(1);
        assertThat(user.status).isEqualTo("ACTIVE");
        verify(users).updateLoginState(eq(1L), eq("ACTIVE"), isNull(), eq(1), isNull());
    }

    @Test
    void failedPasswordAtThresholdLocksAccountTemporarily() {
        User user = user("ACTIVE", 2, null);
        when(users.findByUsernameAndDeletedFalseForUpdate("u")).thenReturn(Optional.of(user));
        when(encoder.matches("bad", "hash")).thenReturn(false);

        BusinessException exception = catchThrowableOfType(() -> service.login(login("bad")), BusinessException.class);

        assertThat(exception.code()).isEqualTo("USER_LOCKED");
        assertThat(exception.status()).isEqualTo(HttpStatus.LOCKED);
        assertThat(user.status).isEqualTo("LOCKED");
        assertThat(user.lockedUntil).isAfter(Instant.now());
        verify(users).updateLoginState(eq(1L), eq("LOCKED"), any(Instant.class), eq(3), isNull());
        verify(approvals).release(1L);
    }

    @Test
    void lockedAccountRejectsLoginBeforeExpiry() {
        User user = user("LOCKED", 0, Instant.now().plusSeconds(300));
        when(users.findByUsernameAndDeletedFalseForUpdate("u")).thenReturn(Optional.of(user));
        when(encoder.matches("good", "hash")).thenReturn(true);

        BusinessException exception = catchThrowableOfType(() -> service.login(login("good")), BusinessException.class);

        assertThat(exception.code()).isEqualTo("USER_LOCKED");
        assertThat(exception.status()).isEqualTo(HttpStatus.LOCKED);
        verify(encoder).matches("good", "hash");
    }

    @Test
    void lockedAccountWithWrongPasswordReturnsGenericFailure() {
        User user = user("LOCKED", 0, Instant.now().plusSeconds(300));
        when(users.findByUsernameAndDeletedFalseForUpdate("u")).thenReturn(Optional.of(user));
        when(encoder.matches("bad", "hash")).thenReturn(false);

        BusinessException exception = catchThrowableOfType(() -> service.login(login("bad")), BusinessException.class);

        assertThat(exception.code()).isEqualTo("LOGIN_FAILED");
        assertThat(exception.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void unknownUsernameReturnsGenericFailure() {
        when(users.findByUsernameAndDeletedFalseForUpdate("u")).thenReturn(Optional.empty());

        BusinessException exception = catchThrowableOfType(() -> service.login(login("good")), BusinessException.class);

        assertThat(exception.code()).isEqualTo("LOGIN_FAILED");
        assertThat(exception.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void legacyLockedAccountWithoutExpiryRequiresAdminUnlock() {
        User user = user("LOCKED", 0, null);
        when(users.findByUsernameAndDeletedFalseForUpdate("u")).thenReturn(Optional.of(user));
        when(encoder.matches("good", "hash")).thenReturn(true);

        BusinessException exception = catchThrowableOfType(() -> service.login(login("good")), BusinessException.class);

        assertThat(exception.code()).isEqualTo("USER_LOCKED");
        assertThat(exception.status()).isEqualTo(HttpStatus.LOCKED);
    }

    @Test
    void expiredLockAllowsLoginAndResetsState() {
        User user = user("LOCKED", 3, Instant.now().minusSeconds(1));
        when(users.findByUsernameAndDeletedFalseForUpdate("u")).thenReturn(Optional.of(user));
        when(encoder.matches("good", "hash")).thenReturn(true);

        Map<String, Object> result = service.login(login("good"));

        assertThat(result.get("accessToken")).isNotNull();
        assertThat(user.status).isEqualTo("ACTIVE");
        assertThat(user.lockedUntil).isNull();
        assertThat(user.failedLoginCount).isZero();
    }

    @Test
    void successfulLoginResetsFailedCounter() {
        User user = user("ACTIVE", 2, null);
        when(users.findByUsernameAndDeletedFalseForUpdate("u")).thenReturn(Optional.of(user));
        when(encoder.matches("good", "hash")).thenReturn(true);

        service.login(login("good"));

        assertThat(user.failedLoginCount).isZero();
        assertThat(user.lockedUntil).isNull();
        assertThat(user.status).isEqualTo("ACTIVE");
        verify(users).updateLoginState(eq(1L), eq("ACTIVE"), isNull(), eq(0), any(Instant.class));
    }

    private User user(String status, int failedLoginCount, Instant lockedUntil) {
        User user = new User();
        user.id = 1L;
        user.username = "u";
        user.realName = "User";
        user.passwordHash = "hash";
        user.status = status;
        user.failedLoginCount = failedLoginCount;
        user.lockedUntil = lockedUntil;
        return user;
    }

    private AuthController.Login login(String password) {
        return new AuthController.Login("u", password);
    }
}
