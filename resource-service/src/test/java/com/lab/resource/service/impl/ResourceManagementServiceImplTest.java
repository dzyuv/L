package com.lab.resource.service.impl;

import com.lab.common.api.InternalServiceGuard;
import com.lab.common.api.RoleGuard;
import com.lab.common.exception.BusinessException;
import com.lab.resource.ClosureRepository;
import com.lab.resource.Resource;
import com.lab.resource.ResourceManagerRepository;
import com.lab.resource.ResourceRepository;
import com.lab.resource.ResourceTypeRepository;
import com.lab.resource.ScheduleRepository;
import com.lab.resource.UserLookupClient;
import com.lab.resource.controller.ResourceController;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceManagementServiceImplTest {
    @Mock ResourceRepository resources;
    @Mock ResourceTypeRepository types;
    @Mock ScheduleRepository schedules;
    @Mock ClosureRepository closures;
    @Mock ResourceManagerRepository managers;
    @Mock RoleGuard roleGuard;
    @Mock InternalServiceGuard internalServices;
    @Mock UserLookupClient userLookup;
    @Mock HttpServletRequest request;
    @InjectMocks ResourceManagementServiceImpl service;

    @Test
    void rejectsManagerWithoutTeacherRole() {
        when(resources.findById(1L)).thenReturn(Optional.of(resource()));
        when(userLookup.find(9L)).thenReturn(new UserLookupClient.UserSummary(
                9L, "S20260001", "Student", "ACTIVE", false, List.of("STUDENT")));

        assertThatThrownBy(() -> service.addManager(1L, new ResourceController.ManagerRequest(9L), request))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).code())
                .isEqualTo("MANAGER_MUST_BE_TEACHER");
    }

    @Test
    void acceptsActiveTeacherAsManager() {
        when(resources.findById(1L)).thenReturn(Optional.of(resource()));
        when(userLookup.find(9L)).thenReturn(new UserLookupClient.UserSummary(
                9L, "T20260001", "Teacher", "ACTIVE", false, List.of("TEACHER")));
        when(managers.findByResourceIdOrderByIdAsc(1L)).thenReturn(List.of());
        when(managers.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.addManager(1L, new ResourceController.ManagerRequest(9L), request))
                .isNotNull();
    }

    private Resource resource() {
        Resource resource = new Resource();
        resource.id = 1L;
        resource.deleted = false;
        return resource;
    }
}
