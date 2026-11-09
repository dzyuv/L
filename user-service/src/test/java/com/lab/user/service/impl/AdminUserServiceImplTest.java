package com.lab.user.service.impl;

import com.lab.common.api.AdminOperationLogger;
import com.lab.common.api.RoleGuard;
import com.lab.common.exception.BusinessException;
import com.lab.user.RefreshTokenRepository;
import com.lab.user.ResourceManagerLookupClient;
import com.lab.user.Role;
import com.lab.user.RoleRepository;
import com.lab.user.User;
import com.lab.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {
    @Mock UserRepository users;
    @Mock RoleRepository roles;
    @Mock RefreshTokenRepository refreshTokens;
    @Mock PasswordEncoder encoder;
    @Mock RoleGuard roleGuard;
    @Mock AdminOperationLogger operationLogger;
    @Mock ResourceManagerLookupClient resourceManagers;
    @Mock HttpServletRequest request;
    @InjectMocks AdminUserServiceImpl service;

    @Test
    void rejectsRemovingTeacherRoleFromResourceManager() {
        User user = new User();
        user.id = 5L;
        Role teacher = new Role();
        teacher.id = 2L;
        teacher.code = "TEACHER";
        user.roles.add(teacher);
        when(users.findById(5L)).thenReturn(Optional.of(user));
        when(roleGuard.currentUserId(request)).thenReturn(1L);
        when(resourceManagers.isResourceManager(5L)).thenReturn(true);

        assertThatThrownBy(() -> service.updateRoles(5L, Set.of("STUDENT"), request))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).code())
                .isEqualTo("USER_IS_RESOURCE_MANAGER");

        verify(users, never()).save(any());
    }
}
