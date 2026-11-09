package com.lab.user.service.impl;

import com.lab.common.api.JwtKeyProvider;
import com.lab.common.exception.BusinessException;
import com.lab.user.RefreshTokenRepository;
import com.lab.user.RoleRepository;
import com.lab.user.User;
import com.lab.user.UserRepository;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock UserRepository users;
    @Mock RoleRepository roles;
    @Mock PasswordEncoder encoder;
    @Mock RefreshTokenRepository refreshTokens;

    private final JwtKeyProvider keys = new JwtKeyProvider("0123456789abcdef0123456789abcdef");
    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(users, roles, encoder, keys, refreshTokens, 2, 30, 3, 15);
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
    }

    @Test
    void lockedAccountRejectsLoginBeforeExpiry() {
        User user = user("LOCKED", 0, Instant.now().plusSeconds(300));
        when(users.findByUsernameAndDeletedFalseForUpdate("u")).thenReturn(Optional.of(user));

        BusinessException exception = catchThrowableOfType(() -> service.login(login("good")), BusinessException.class);

        assertThat(exception.code()).isEqualTo("USER_LOCKED");
        assertThat(exception.status()).isEqualTo(HttpStatus.LOCKED);
        verifyNoInteractions(encoder);
    }

    @Test
    void legacyLockedAccountWithoutExpiryRequiresAdminUnlock() {
        User user = user("LOCKED", 0, null);
        when(users.findByUsernameAndDeletedFalseForUpdate("u")).thenReturn(Optional.of(user));

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
