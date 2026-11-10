package com.lab.booking.service.impl;

import com.lab.booking.entity.Booking;
import com.lab.booking.service.BookingLifecycleService;
import com.lab.booking.entity.BookingQuotaLock;
import com.lab.booking.repository.BookingQuotaLockRepository;
import com.lab.booking.repository.BookingRepository;
import com.lab.booking.entity.BookingSlot;
import com.lab.booking.repository.BookingSlotRepository;
import com.lab.booking.repository.BookingStatusHistoryRepository;
import com.lab.booking.client.ResourceRuleClient;
import com.lab.booking.repository.ViolationRecordRepository;
import com.lab.booking.approval.service.ApprovalInternalService;
import com.lab.booking.controller.BookingController;
import com.lab.common.api.RoleGuard;
import com.lab.common.api.Roles;
import com.lab.common.api.RuntimeSettings;
import com.lab.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookingServiceImplTest {
    private static final Long USER_ID = 7L;
    private static final String KEY = "idem-key-1";
    private static final Long RESOURCE_ID = 11L;

    private static final ResourceRuleClient.BookingRule DIRECT_RULE =
            new ResourceRuleClient.BookingRule("Lab A", 3L, 10, 30, 120, false, 0, null, null);
    private static final ResourceRuleClient.BookingRule APPROVAL_RULE =
            new ResourceRuleClient.BookingRule("Lab A", 3L, 10, 30, 120, true, 1, null, null);

    @Mock BookingRepository bookings;
    @Mock BookingSlotRepository slots;
    @Mock BookingQuotaLockRepository quotaLocks;
    @Mock ResourceRuleClient resourceRules;
    @Mock ApprovalInternalService approvals;
    @Mock BookingLifecycleService lifecycle;
    @Mock BookingStatusHistoryRepository histories;
    @Mock ViolationRecordRepository violations;
    @Mock RoleGuard roleGuard;
    @Mock RuntimeSettings settings;
    @Mock HttpServletRequest servletRequest;

    private BookingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BookingServiceImpl(bookings, slots, quotaLocks, resourceRules, approvals, lifecycle,
                histories, violations, roleGuard, settings,
                2, 4, 1, 2, 1, 2, 120, 240, 30);

        when(servletRequest.getAttribute("userId")).thenReturn(USER_ID);
        when(servletRequest.getAttribute("realName")).thenReturn("Tester");
        when(servletRequest.getHeader("Authorization")).thenReturn("Bearer token");
        when(bookings.findByUserIdAndClientRequestId(USER_ID, KEY)).thenReturn(Optional.empty());
        when(bookings.findByUserIdOrderByStartTimeDesc(USER_ID)).thenReturn(List.of());
        when(bookings.findActiveOccupancy(any(), any(), any())).thenReturn(List.of());
        when(bookings.saveAndFlush(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.id = 100L;
            return booking;
        });
        when(quotaLocks.lockByUserId(USER_ID)).thenReturn(Optional.of(new BookingQuotaLock()));
        when(roleGuard.hasRole(servletRequest, Roles.TEACHER)).thenReturn(false);
        when(roleGuard.hasRole(servletRequest, Roles.LAB_ADMIN)).thenReturn(false);
        when(resourceRules.getRule(any(), any(), any(), anyInt(), any(), any())).thenReturn(DIRECT_RULE);
    }

    @Test
    void createRequiresIdempotencyKey() {
        BusinessException exception = catchThrowableOfType(
                () -> service.create(request(futureStart(), futureStart().plusHours(1), RESOURCE_ID), " ", servletRequest),
                BusinessException.class);

        assertThat(exception.code()).isEqualTo("IDEMPOTENCY_REQUIRED");
    }

    @Test
    void createReturnsExistingBookingForSameIdempotencyKey() {
        Booking existing = existing("APPROVED", RESOURCE_ID, futureStart(), futureStart().plusHours(1));
        when(bookings.findByUserIdAndClientRequestId(USER_ID, KEY)).thenReturn(Optional.of(existing));

        Booking result = service.create(request(futureStart(), futureStart().plusHours(1), RESOURCE_ID), KEY, servletRequest);

        assertThat(result).isSameAs(existing);
        verify(quotaLocks, never()).lockByUserId(any());
    }

    @Test
    void createReturnsBookingCommittedByConcurrentRequest() {
        Booking existing = existing("APPROVED", RESOURCE_ID, futureStart(), futureStart().plusHours(1));
        when(bookings.findByUserIdAndClientRequestId(USER_ID, KEY))
                .thenReturn(Optional.empty(), Optional.of(existing));

        Booking result = service.create(request(futureStart(), futureStart().plusHours(1), RESOURCE_ID), KEY, servletRequest);

        assertThat(result).isSameAs(existing);
        verify(bookings, never()).saveAndFlush(any(Booking.class));
    }

    @Test
    void createRejectsStartTimeNotBeforeEndTime() {
        LocalDateTime start = futureStart();

        BusinessException exception = catchThrowableOfType(
                () -> service.create(request(start, start, RESOURCE_ID), KEY, servletRequest),
                BusinessException.class);

        assertThat(exception.code()).isEqualTo("INVALID_TIME");
    }

    @Test
    void createRejectsPastStartTime() {
        LocalDateTime start = LocalDateTime.now().minusMinutes(5);

        BusinessException exception = catchThrowableOfType(
                () -> service.create(request(start, start.plusHours(1), RESOURCE_ID), KEY, servletRequest),
                BusinessException.class);

        assertThat(exception.code()).isEqualTo("BOOKING_ADVANCE_LIMIT");
    }

    @Test
    void createRejectsStartTimeBeyondAdvanceWindow() {
        LocalDateTime start = LocalDateTime.now().plusDays(31);

        BusinessException exception = catchThrowableOfType(
                () -> service.create(request(start, start.plusHours(1), RESOURCE_ID), KEY, servletRequest),
                BusinessException.class);

        assertThat(exception.code()).isEqualTo("BOOKING_ADVANCE_LIMIT");
    }

    @Test
    void createRejectsWhenActiveLimitReached() {
        LocalDateTime first = futureStart();
        LocalDateTime second = futureStart().plusHours(2);
        when(bookings.findByUserIdOrderByStartTimeDesc(USER_ID)).thenReturn(List.of(
                existing("APPROVED", 101L, first, first.plusHours(1)),
                existing("APPROVED", 102L, second, second.plusHours(1))));

        BusinessException exception = catchThrowableOfType(
                () -> service.create(request(futureStart().plusHours(5), futureStart().plusHours(6), RESOURCE_ID), KEY, servletRequest),
                BusinessException.class);

        assertThat(exception.code()).isEqualTo("BOOKING_ACTIVE_LIMIT");
    }

    @Test
    void createRejectsWhenPendingLimitReached() {
        LocalDateTime start = futureStart().plusHours(3);
        when(bookings.findByUserIdOrderByStartTimeDesc(USER_ID)).thenReturn(List.of(
                existing("PENDING_APPROVAL", 101L, start, start.plusHours(1))));

        BusinessException exception = catchThrowableOfType(
                () -> service.create(request(futureStart(), futureStart().plusHours(1), RESOURCE_ID), KEY, servletRequest),
                BusinessException.class);

        assertThat(exception.code()).isEqualTo("BOOKING_PENDING_LIMIT");
    }

    @Test
    void createRejectsWhenPerResourceLimitReached() {
        LocalDateTime start = futureStart().plusHours(3);
        when(bookings.findByUserIdOrderByStartTimeDesc(USER_ID)).thenReturn(List.of(
                existing("APPROVED", RESOURCE_ID, start, start.plusHours(1))));

        BusinessException exception = catchThrowableOfType(
                () -> service.create(request(futureStart(), futureStart().plusHours(1), RESOURCE_ID), KEY, servletRequest),
                BusinessException.class);

        assertThat(exception.code()).isEqualTo("BOOKING_RESOURCE_LIMIT");
    }

    @Test
    void createRejectsWhenUserAlreadyHasOverlappingBooking() {
        LocalDateTime overlappingStart = futureStart().minusMinutes(30);
        when(bookings.findByUserIdOrderByStartTimeDesc(USER_ID)).thenReturn(List.of(
                existing("APPROVED", 101L, overlappingStart, overlappingStart.plusHours(1))));

        BusinessException exception = catchThrowableOfType(
                () -> service.create(request(futureStart(), futureStart().plusHours(1), RESOURCE_ID), KEY, servletRequest),
                BusinessException.class);

        assertThat(exception.code()).isEqualTo("USER_TIME_CONFLICT");
    }

    @Test
    void createRejectsWhenDailyDurationLimitExceeded() {
        LocalDateTime existingStart = futureStart().toLocalDate().atTime(13, 0);
        when(bookings.findByUserIdOrderByStartTimeDesc(USER_ID)).thenReturn(List.of(
                existing("APPROVED", 101L, existingStart, existingStart.plusHours(2))));

        BusinessException exception = catchThrowableOfType(
                () -> service.create(request(futureStart(), futureStart().plusHours(1), RESOURCE_ID), KEY, servletRequest),
                BusinessException.class);

        assertThat(exception.code()).isEqualTo("BOOKING_DAILY_DURATION_LIMIT");
    }

    @Test
    void createRejectsWhenResourceSlotAlreadyOccupied() {
        when(bookings.findActiveOccupancy(eq(RESOURCE_ID), any(), any())).thenReturn(List.of(new Booking()));

        BusinessException exception = catchThrowableOfType(
                () -> service.create(request(futureStart(), futureStart().plusHours(1), RESOURCE_ID), KEY, servletRequest),
                BusinessException.class);

        assertThat(exception.code()).isEqualTo("BOOKING_CONFLICT");
    }

    @Test
    void createMapsConcurrentSlotConflictToBookingConflict() {
        when(bookings.saveAndFlush(any(Booking.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate slot"));

        BusinessException exception = catchThrowableOfType(
                () -> service.create(request(futureStart(), futureStart().plusHours(1), RESOURCE_ID), KEY, servletRequest),
                BusinessException.class);

        assertThat(exception.code()).isEqualTo("BOOKING_CONFLICT");
    }

    @Test
    void createSavesBookingAndSlotsWhenRuleNeedsNoApproval() {
        Booking result = service.create(request(futureStart(), futureStart().plusHours(1), RESOURCE_ID), KEY, servletRequest);

        assertThat(result.status).isEqualTo("APPROVED");
        assertThat(result.bookingNo).startsWith("BK");
        assertThat(result.slotMinutesSnapshot).isEqualTo(30);
        verify(lifecycle).recordInitial(result, USER_ID, "");
        verify(slots, times(2)).saveAndFlush(any(BookingSlot.class));
        verify(approvals, never()).create(any());
    }

    @Test
    void createMarksPendingAndCreatesApprovalTaskWhenRuleNeedsApproval() {
        when(resourceRules.getRule(any(), any(), any(), anyInt(), any(), any())).thenReturn(APPROVAL_RULE);

        Booking result = service.create(request(futureStart(), futureStart().plusHours(1), RESOURCE_ID), KEY, servletRequest);

        assertThat(result.status).isEqualTo("PENDING_APPROVAL");
        verify(lifecycle).refreshApprovalDeadline(result);
        ArgumentCaptor<ApprovalInternalService.CreateTask> captor = ArgumentCaptor.forClass(ApprovalInternalService.CreateTask.class);
        verify(approvals).create(captor.capture());
        ApprovalInternalService.CreateTask task = captor.getValue();
        assertThat(task.bookingId()).isEqualTo(result.id);
        assertThat(task.level()).isEqualTo(1);
        assertThat(task.totalLevels()).isEqualTo(1);
        assertThat(task.approverRole()).isNull();
    }

    @Test
    void teacherUsesHigherQuotaLimits() {
        when(roleGuard.hasRole(servletRequest, Roles.TEACHER)).thenReturn(true);
        LocalDateTime morning = futureStart().withHour(7);
        LocalDateTime next = futureStart().withHour(8);
        when(bookings.findByUserIdOrderByStartTimeDesc(USER_ID)).thenReturn(List.of(
                existing("APPROVED", 101L, morning, morning.plusHours(1)),
                existing("APPROVED", 102L, next, next.plusHours(1))));

        Booking result = service.create(request(futureStart(), futureStart().plusHours(1), RESOURCE_ID), KEY, servletRequest);

        assertThat(result.status).isEqualTo("APPROVED");
    }

    private BookingController.Create request(LocalDateTime start, LocalDateTime end, Long resourceId) {
        return new BookingController.Create(resourceId, start, end, "实验", 1);
    }

    private Booking existing(String status, Long resourceId, LocalDateTime start, LocalDateTime end) {
        Booking booking = new Booking();
        booking.id = 200L;
        booking.userId = USER_ID;
        booking.resourceId = resourceId;
        booking.status = status;
        booking.startTime = start;
        booking.endTime = end;
        return booking;
    }

    private LocalDateTime futureStart() {
        return LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
    }
}
