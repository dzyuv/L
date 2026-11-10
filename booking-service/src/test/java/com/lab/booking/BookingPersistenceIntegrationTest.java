package com.lab.booking;
import com.lab.booking.entity.Booking;
import com.lab.booking.repository.BookingRepository;
import com.lab.booking.scheduler.BookingScheduler;
import com.lab.booking.entity.BookingSlot;
import com.lab.booking.repository.BookingSlotRepository;
import com.lab.booking.entity.BookingStatusHistory;
import com.lab.booking.repository.BookingStatusHistoryRepository;

import com.lab.booking.approval.entity.ApprovalRecord;
import com.lab.booking.approval.repository.ApprovalRecordRepository;
import com.lab.booking.approval.entity.ApprovalTask;
import com.lab.booking.approval.repository.ApprovalTaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.nacos.config.enabled=false",
        "security.jwt-secret=0123456789abcdef0123456789abcdef",
        "security.internal-token=0123456789abcdef0123456789abcdef",
        "services.system.base-url=http://localhost:1",
        "services.resource.base-url=http://localhost:1"
})
@Testcontainers(disabledWithoutDocker = true)
@Transactional
class BookingPersistenceIntegrationTest {
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8")
            .withDatabaseName("lab_booking")
            .withUsername("lab_app")
            .withPassword("lab_app");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @MockBean BookingScheduler bookingScheduler;

    @Autowired BookingRepository bookings;
    @Autowired BookingSlotRepository slots;
    @Autowired ApprovalTaskRepository approvalTasks;
    @Autowired ApprovalRecordRepository approvalRecords;
    @Autowired BookingStatusHistoryRepository histories;

    @Test
    void enforcesUniqueActiveSlotPerResourceAndTime() {
        Booking booking = booking(1L, "BK-1", "req-1", LocalDateTime.of(2026, 2, 1, 9, 0), LocalDateTime.of(2026, 2, 1, 10, 0));
        bookings.save(booking);

        BookingSlot first = slot(booking.id, 1L, LocalDateTime.of(2026, 2, 1, 9, 0));
        slots.save(first);
        BookingSlot duplicate = slot(booking.id, 1L, LocalDateTime.of(2026, 2, 1, 9, 0));

        assertThatThrownBy(() -> slots.save(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findsActiveOccupancyWithinRequestedWindow() {
        Booking booking = booking(2L, "BK-2", "req-2", LocalDateTime.of(2026, 2, 2, 9, 0), LocalDateTime.of(2026, 2, 2, 10, 0));
        bookings.save(booking);

        List<Booking> overlapping = bookings.findActiveOccupancy(2L,
                LocalDateTime.of(2026, 2, 2, 9, 15), LocalDateTime.of(2026, 2, 2, 9, 45));
        List<Booking> after = bookings.findActiveOccupancy(2L,
                LocalDateTime.of(2026, 2, 2, 10, 0), LocalDateTime.of(2026, 2, 2, 11, 0));

        assertThat(overlapping).extracting(item -> item.id).containsExactly(booking.id);
        assertThat(after).isEmpty();
    }

    @Test
    void approvalTasksPersistInBookingDatabase() {
        ApprovalTask task = new ApprovalTask();
        task.bookingId = 9L;
        task.applicantUserId = 1L;
        task.applicantName = "Tester";
        task.resourceId = 2L;
        task.resourceName = "Lab";
        task.startTime = LocalDateTime.of(2026, 2, 3, 9, 0);
        task.endTime = LocalDateTime.of(2026, 2, 3, 10, 0);
        task.level = 1;
        task.totalLevels = 1;
        task.approverRole = "TEACHER";
        task.status = "PENDING";
        task.deadline = LocalDateTime.of(2026, 2, 2, 18, 0);
        approvalTasks.save(task);

        assertThat(task.id).isNotNull();
        assertThat(approvalTasks.findByBookingIdAndLevel(9L, 1)).isPresent();
    }

    @Test
    void persistsApprovalCommentUpToValidationLimit() {
        String comment = "a".repeat(300);

        ApprovalTask task = new ApprovalTask();
        task.bookingId = 11L;
        task.applicantUserId = 1L;
        task.applicantName = "Tester";
        task.resourceId = 2L;
        task.resourceName = "Lab";
        task.startTime = LocalDateTime.of(2026, 2, 4, 9, 0);
        task.endTime = LocalDateTime.of(2026, 2, 4, 10, 0);
        task.level = 1;
        task.totalLevels = 1;
        task.approverRole = "TEACHER";
        task.status = "APPROVED";
        task.deadline = LocalDateTime.of(2026, 2, 3, 18, 0);
        task.comment = comment;
        approvalTasks.save(task);

        ApprovalRecord record = new ApprovalRecord();
        record.taskId = task.id;
        record.bookingId = task.bookingId;
        record.approverId = 9L;
        record.result = "APPROVED";
        record.comment = comment;
        approvalRecords.save(record);

        BookingStatusHistory history = new BookingStatusHistory();
        history.bookingId = task.bookingId;
        history.fromStatus = "PENDING_APPROVAL";
        history.toStatus = "APPROVED";
        history.operatorId = 9L;
        history.reason = comment;
        histories.save(history);

        assertThat(approvalTasks.selectById(task.id).comment).isEqualTo(comment);
        assertThat(approvalRecords.selectById(record.id).comment).isEqualTo(comment);
        assertThat(histories.selectById(history.id).reason).isEqualTo(comment);
    }

    private Booking booking(Long resourceId, String bookingNo, String requestId, LocalDateTime start, LocalDateTime end) {
        Booking booking = new Booking();
        booking.bookingNo = bookingNo;
        booking.clientRequestId = requestId;
        booking.userId = 1L;
        booking.resourceId = resourceId;
        booking.resourceNameSnapshot = "Lab";
        booking.applicantNameSnapshot = "Tester";
        booking.startTime = start;
        booking.endTime = end;
        booking.slotMinutesSnapshot = 30;
        booking.approvalLevelSnapshot = 0;
        booking.needCheckinSnapshot = true;
        booking.participants = 1;
        booking.status = "APPROVED";
        return booking;
    }

    private BookingSlot slot(Long bookingId, Long resourceId, LocalDateTime start) {
        BookingSlot slot = new BookingSlot();
        slot.bookingId = bookingId;
        slot.resourceId = resourceId;
        slot.slotStart = start;
        return slot;
    }
}
