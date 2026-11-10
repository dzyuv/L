package com.lab.booking;
import com.lab.booking.entity.*;
import com.lab.booking.repository.*;
import com.lab.booking.client.*;
import com.lab.booking.scheduler.*;
import com.lab.booking.config.*;
import com.lab.booking.service.*;
import com.lab.booking.service.impl.*;
import com.lab.booking.controller.*;
import com.lab.booking.approval.entity.*;
import com.lab.booking.approval.repository.*;
import com.lab.booking.approval.service.*;
import com.lab.booking.approval.service.impl.*;
import com.lab.booking.approval.controller.*;

import com.lab.common.api.RuntimeSettings;
import com.lab.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingLifecycleServiceTest {
    @Mock BookingStatusHistoryRepository histories;
    @Mock BookingSlotRepository slots;
    @Mock ViolationRecordRepository violations;
    @Mock UserRestrictionRepository restrictions;
    @Mock RuntimeSettings settings;

    private BookingLifecycleService service;

    @BeforeEach
    void setUp() {
        service = new BookingLifecycleService(histories, slots, violations, restrictions, settings, 30);
    }

    @Test
    void assertCanCreateRejectsActiveRestriction() {
        UserRestriction restriction = new UserRestriction();
        restriction.restrictedUntil = LocalDateTime.now().plusDays(3);
        when(restrictions.findFirstByUserIdAndStatusAndRestrictedUntilAfterOrderByRestrictedUntilDesc(
                eq(7L), eq("ACTIVE"), any(LocalDateTime.class))).thenReturn(Optional.of(restriction));

        BusinessException exception = catchThrowableOfType(
                () -> service.assertCanCreate(7L), BusinessException.class);

        assertThat(exception.code()).isEqualTo("USER_RESTRICTED");
        assertThat(exception.status()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void assertCanCreateAllowsUserWithoutRestriction() {
        when(restrictions.findFirstByUserIdAndStatusAndRestrictedUntilAfterOrderByRestrictedUntilDesc(
                eq(7L), eq("ACTIVE"), any(LocalDateTime.class))).thenReturn(Optional.empty());

        assertThatCode(() -> service.assertCanCreate(7L)).doesNotThrowAnyException();
    }

    @Test
    void transitionUpdatesStatusAndRecordsHistory() {
        Booking booking = new Booking();
        booking.id = 1L;
        booking.status = "PENDING_APPROVAL";

        service.transition(booking, "APPROVED", 9L, "ok", "req-1");

        assertThat(booking.status).isEqualTo("APPROVED");
        ArgumentCaptor<BookingStatusHistory> captor = ArgumentCaptor.forClass(BookingStatusHistory.class);
        verify(histories).save(captor.capture());
        BookingStatusHistory history = captor.getValue();
        assertThat(history.bookingId).isEqualTo(1L);
        assertThat(history.fromStatus).isEqualTo("PENDING_APPROVAL");
        assertThat(history.toStatus).isEqualTo("APPROVED");
        assertThat(history.operatorId).isEqualTo(9L);
        assertThat(history.reason).isEqualTo("ok");
        assertThat(history.requestId).isEqualTo("req-1");
    }

    @Test
    void releaseSlotsMarksUnreleasedSlots() {
        BookingSlot first = new BookingSlot();
        first.id = 1L;
        BookingSlot second = new BookingSlot();
        second.id = 2L;
        when(slots.findByBookingIdAndReleasedAtIsNull(5L)).thenReturn(List.of(first, second));

        service.releaseSlots(5L, "canceled");

        assertThat(first.releasedAt).isNotNull();
        assertThat(first.releaseReason).isEqualTo("canceled");
        assertThat(second.releasedAt).isNotNull();
        verify(slots).save(first);
        verify(slots).save(second);
    }

    @Test
    void recordNoShowCreatesOpenViolation() {
        Booking booking = new Booking();
        booking.id = 11L;
        booking.userId = 22L;

        service.recordNoShow(booking);

        ArgumentCaptor<ViolationRecord> captor = ArgumentCaptor.forClass(ViolationRecord.class);
        verify(violations).save(captor.capture());
        ViolationRecord record = captor.getValue();
        assertThat(record.bookingId).isEqualTo(11L);
        assertThat(record.userId).isEqualTo(22L);
        assertThat(record.violationType).isEqualTo("NO_SHOW");
        assertThat(record.status).isEqualTo("OPEN");
        assertThat(record.comment).isEqualTo("Missed required check-in");
    }

    @Test
    void applyRestrictionCreatesRestrictionWhenThresholdReached() {
        when(settings.violationMaxCount()).thenReturn(3);
        when(settings.restrictionDays()).thenReturn(14);
        when(violations.countActiveNoShows(eq(22L), any(LocalDateTime.class))).thenReturn(3L);
        when(restrictions.findFirstByUserIdAndStatusAndRestrictedUntilAfterOrderByRestrictedUntilDesc(
                eq(22L), eq("ACTIVE"), any(LocalDateTime.class))).thenReturn(Optional.empty());

        service.applyRestrictionIfNeeded(22L);

        ArgumentCaptor<UserRestriction> captor = ArgumentCaptor.forClass(UserRestriction.class);
        verify(restrictions).save(captor.capture());
        UserRestriction restriction = captor.getValue();
        assertThat(restriction.userId).isEqualTo(22L);
        assertThat(restriction.sourceViolationCount).isEqualTo(3);
        assertThat(restriction.reason).isEqualTo("Repeated no-show bookings");
        assertThat(restriction.restrictedUntil).isAfter(LocalDateTime.now().plusDays(13));
    }

    @Test
    void applyRestrictionSkipsWhenUserAlreadyRestricted() {
        when(settings.violationMaxCount()).thenReturn(3);
        when(violations.countActiveNoShows(eq(22L), any(LocalDateTime.class))).thenReturn(5L);
        when(restrictions.findFirstByUserIdAndStatusAndRestrictedUntilAfterOrderByRestrictedUntilDesc(
                eq(22L), eq("ACTIVE"), any(LocalDateTime.class))).thenReturn(Optional.of(new UserRestriction()));

        service.applyRestrictionIfNeeded(22L);

        verify(restrictions, never()).save(any());
    }

    @Test
    void applyRestrictionSkipsBelowThreshold() {
        when(settings.violationMaxCount()).thenReturn(3);
        when(violations.countActiveNoShows(eq(22L), any(LocalDateTime.class))).thenReturn(2L);

        service.applyRestrictionIfNeeded(22L);

        verify(restrictions, never()).save(any());
    }

    @Test
    void refreshRestrictionLiftsRestrictionWhenBelowThreshold() {
        when(settings.violationMaxCount()).thenReturn(3);
        when(violations.countActiveNoShows(eq(22L), any(LocalDateTime.class))).thenReturn(1L);
        UserRestriction restriction = new UserRestriction();
        restriction.userId = 22L;
        restriction.status = "ACTIVE";
        restriction.restrictedUntil = LocalDateTime.now().plusDays(5);
        when(restrictions.findByUserIdAndStatus(22L, "ACTIVE")).thenReturn(List.of(restriction));

        service.refreshRestriction(22L);

        assertThat(restriction.status).isEqualTo("LIFTED");
        verify(restrictions).save(restriction);
    }

    @Test
    void refreshRestrictionKeepsRestrictionWhenThresholdStillMet() {
        when(settings.violationMaxCount()).thenReturn(3);
        when(violations.countActiveNoShows(eq(22L), any(LocalDateTime.class))).thenReturn(3L);

        service.refreshRestriction(22L);

        verify(restrictions, never()).findByUserIdAndStatus(anyLong(), anyString());
        verify(restrictions, never()).save(any());
    }

    @Test
    void reclaimSlotsUnreleasesExistingSlots() {
        Booking booking = new Booking();
        booking.id = 5L;
        BookingSlot slot = new BookingSlot();
        slot.releasedAt = LocalDateTime.now();
        slot.releaseReason = "canceled";
        when(slots.findByBookingId(5L)).thenReturn(List.of(slot));

        service.reclaimSlots(booking);

        assertThat(slot.releasedAt).isNull();
        assertThat(slot.releaseReason).isNull();
        verify(slots).save(slot);
        verify(slots, times(1)).save(any());
    }

    @Test
    void reclaimSlotsCreatesSlotsWhenNoneExist() {
        Booking booking = new Booking();
        booking.id = 5L;
        booking.resourceId = 8L;
        booking.slotMinutesSnapshot = 30;
        booking.startTime = LocalDateTime.of(2026, 1, 1, 9, 0);
        booking.endTime = LocalDateTime.of(2026, 1, 1, 10, 0);
        when(slots.findByBookingId(5L)).thenReturn(List.of());

        service.reclaimSlots(booking);

        ArgumentCaptor<BookingSlot> captor = ArgumentCaptor.forClass(BookingSlot.class);
        verify(slots, times(2)).save(captor.capture());
        List<BookingSlot> created = captor.getAllValues();
        assertThat(created).hasSize(2);
        assertThat(created.get(0).slotStart).isEqualTo(LocalDateTime.of(2026, 1, 1, 9, 0));
        assertThat(created.get(1).slotStart).isEqualTo(LocalDateTime.of(2026, 1, 1, 9, 30));
        assertThat(created).allSatisfy(slot -> {
            assertThat(slot.resourceId).isEqualTo(8L);
            assertThat(slot.bookingId).isEqualTo(5L);
        });
    }

    @Test
    void refreshApprovalDeadlineUsesStartTimeWhenItComesFirst() {
        when(settings.approvalTimeoutMinutes()).thenReturn(60);
        Booking booking = new Booking();
        booking.startTime = LocalDateTime.now().plusMinutes(10);

        service.refreshApprovalDeadline(booking);

        assertThat(booking.approvalDeadline).isEqualTo(booking.startTime);
    }

    @Test
    void refreshApprovalDeadlineUsesHoldWindowWhenStartTimeIsLater() {
        when(settings.approvalTimeoutMinutes()).thenReturn(60);
        Booking booking = new Booking();
        booking.startTime = LocalDateTime.now().plusDays(2);
        LocalDateTime lowerBound = LocalDateTime.now().plusMinutes(59);

        service.refreshApprovalDeadline(booking);

        assertThat(booking.approvalDeadline).isAfter(lowerBound);
        assertThat(booking.approvalDeadline).isBefore(booking.startTime);
    }
}
