package com.lab.resource.service.impl;

import com.lab.common.api.InternalServiceGuard;
import com.lab.common.api.RoleGuard;
import com.lab.common.exception.BusinessException;
import com.lab.resource.repository.ClosureRepository;
import com.lab.resource.entity.Resource;
import com.lab.resource.entity.ResourceManager;
import com.lab.resource.repository.ResourceManagerRepository;
import com.lab.resource.repository.ResourceRepository;
import com.lab.resource.entity.ResourceSchedule;
import com.lab.resource.entity.ResourceType;
import com.lab.resource.repository.ResourceTypeRepository;
import com.lab.resource.repository.ScheduleRepository;
import com.lab.resource.client.UserLookupClient;
import com.lab.resource.controller.ResourceController;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
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

    @Test
    void skipsIneligibleManagerWhenRoutingBooking() {
        Resource resource = resource();
        resource.typeId = 2L;
        resource.status = "ACTIVE";
        resource.capacity = 10;
        resource.maxDurationMinutes = 120;
        resource.needCheckin = true;
        resource.approvalLevelOverride = 1;
        resource.name = "Lab";
        when(resources.findById(1L)).thenReturn(Optional.of(resource));

        ResourceType type = new ResourceType();
        type.id = 2L;
        type.deleted = false;
        type.enabled = true;
        type.defaultApprovalLevel = 1;
        when(types.findById(2L)).thenReturn(Optional.of(type));

        LocalDate date = LocalDate.now().plusDays(1).with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        ResourceSchedule schedule = new ResourceSchedule();
        schedule.resourceId = 1L;
        schedule.weekday = date.getDayOfWeek().getValue();
        schedule.openTime = LocalTime.of(9, 0);
        schedule.closeTime = LocalTime.of(17, 0);
        schedule.slotMinutes = 30;
        schedule.maxDurationMinutes = 120;
        when(schedules.findByResourceIdAndWeekdayAndEnabledTrue(1L, schedule.weekday)).thenReturn(List.of(schedule));
        when(closures.findByResourceIdAndStatusNot(1L, "CANCELED")).thenReturn(List.of());

        ResourceManager manager = new ResourceManager();
        manager.resourceId = 1L;
        manager.userId = 9L;
        manager.managerType = "OWNER";
        when(managers.findByResourceIdOrderByIdAsc(1L)).thenReturn(List.of(manager));
        when(userLookup.lookupForRouting(9L)).thenReturn(new UserLookupClient.LookupResult(
                new UserLookupClient.UserSummary(9L, "S", "Student", "ACTIVE", false, List.of("STUDENT")), true));

        ResourceController.BookingRule rule = service.bookingRule(
                1L, date.atTime(10, 0), date.atTime(11, 0), 5, 100L, request);

        assertThat(rule.approverUserId()).isNull();
        assertThat(rule.approverRole()).isEqualTo("LAB_ADMIN");
    }

    private Resource resource() {
        Resource resource = new Resource();
        resource.id = 1L;
        resource.deleted = false;
        return resource;
    }
}
