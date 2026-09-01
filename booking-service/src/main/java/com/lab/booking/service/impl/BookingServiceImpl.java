package com.lab.booking.service.impl;

import com.lab.booking.*;
import com.lab.booking.controller.BookingController;
import com.lab.booking.service.BookingService;
import com.lab.common.exception.BusinessException;
import com.lab.common.api.RoleGuard;
import com.lab.common.api.Roles;
import com.lab.common.api.RuntimeSettings;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class BookingServiceImpl implements BookingService {
    private static final Logger log = LoggerFactory.getLogger(BookingServiceImpl.class);
    private static final Set<String> QUOTA_STATUSES = Set.of("PENDING_APPROVAL", "APPROVED", "CHECKED_IN");
    private static final Set<String> DAILY_MINUTE_STATUSES = Set.of("PENDING_APPROVAL", "APPROVED", "CHECKED_IN", "COMPLETED", "NO_SHOW");
    private final BookingRepository bookings;
    private final BookingSlotRepository slots;
    private final BookingQuotaLockRepository quotaLocks;
    private final ResourceRuleClient resourceRules;
    private final ApprovalTaskClient approvalTasks;
    private final BookingLifecycleService lifecycle;
    private final ViolationRecordRepository violations;
    private final RoleGuard roleGuard;
    private final int maxActive;
    private final int teacherMaxActive;
    private final int maxPending;
    private final int teacherMaxPending;
    private final int maxPerResource;
    private final int teacherMaxPerResource;
    private final int maxDailyMinutes;
    private final int teacherMaxDailyMinutes;
    private final int maxAdvanceDays;
    private final RuntimeSettings settings;

    public BookingServiceImpl(BookingRepository bookings, BookingSlotRepository slots,
                              BookingQuotaLockRepository quotaLocks, ResourceRuleClient resourceRules,
                              ApprovalTaskClient approvalTasks, BookingLifecycleService lifecycle,
                              ViolationRecordRepository violations, RoleGuard roleGuard, RuntimeSettings settings,
                              @Value("${booking.fair-use.max-active:5}") int maxActive,
                              @Value("${booking.fair-use.teacher-max-active:10}") int teacherMaxActive,
                              @Value("${booking.fair-use.max-pending:3}") int maxPending,
                              @Value("${booking.fair-use.teacher-max-pending:5}") int teacherMaxPending,
                              @Value("${booking.fair-use.max-per-resource:2}") int maxPerResource,
                              @Value("${booking.fair-use.teacher-max-per-resource:3}") int teacherMaxPerResource,
                              @Value("${booking.fair-use.max-daily-minutes:240}") int maxDailyMinutes,
                              @Value("${booking.fair-use.teacher-max-daily-minutes:480}") int teacherMaxDailyMinutes,
                              @Value("${booking.fair-use.max-advance-days:30}") int maxAdvanceDays) {
        this.bookings = bookings;
        this.slots = slots;
        this.quotaLocks = quotaLocks;
        this.resourceRules = resourceRules;
        this.approvalTasks = approvalTasks;
        this.lifecycle = lifecycle;
        this.violations = violations;
        this.roleGuard = roleGuard;
        this.maxActive = maxActive;
        this.teacherMaxActive = teacherMaxActive;
        this.maxPending = maxPending;
        this.teacherMaxPending = teacherMaxPending;
        this.maxPerResource = maxPerResource;
        this.teacherMaxPerResource = teacherMaxPerResource;
        this.maxDailyMinutes = maxDailyMinutes;
        this.teacherMaxDailyMinutes = teacherMaxDailyMinutes;
        this.maxAdvanceDays = maxAdvanceDays;
        this.settings = settings;
    }

    @Override
    @Transactional
    public Booking create(BookingController.Create request, String key, HttpServletRequest servletRequest) {
        Long userId = currentUser(servletRequest);
        roleGuard.requireAny(servletRequest, Roles.STUDENT, Roles.TEACHER);
        if (key == null || key.isBlank() || key.length() > 64) {
            throw new BusinessException("IDEMPOTENCY_REQUIRED", "Idempotency-Key is required", HttpStatus.BAD_REQUEST);
        }
        Optional<Booking> existing = bookings.findByUserIdAndClientRequestId(userId, key);
        if (existing.isPresent()) return existing.get();
        lifecycle.assertCanCreate(userId);
        if (!request.startTime().isBefore(request.endTime())) {
            throw new BusinessException("INVALID_TIME", "Start time must be before end time", HttpStatus.BAD_REQUEST);
        }
        quotaLocks.ensureExists(userId);
        quotaLocks.lockByUserId(userId).orElseThrow(() -> new IllegalStateException("Booking quota lock was not created"));
        Optional<Booking> lockedExisting = bookings.findByUserIdAndClientRequestId(userId, key);
        if (lockedExisting.isPresent()) return lockedExisting.get();
        assertFairUse(userId, request, servletRequest);
        ResourceRuleClient.BookingRule rule = resourceRules.getRule(request.resourceId(), request.startTime(), request.endTime(), request.participants(), userId, servletRequest.getHeader("Authorization"));
        if (!bookings.findActiveOccupancy(request.resourceId(), request.startTime(), request.endTime()).isEmpty()) {
            throw new BusinessException("BOOKING_CONFLICT", "The selected time is already occupied", HttpStatus.CONFLICT);
        }
        Booking booking = new Booking();
        booking.bookingNo = "BK" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        booking.userId = userId;
        booking.resourceId = request.resourceId();
        booking.resourceNameSnapshot = rule.resourceName();
        booking.applicantNameSnapshot = Objects.toString(servletRequest.getAttribute("realName"), "User" + userId);
        booking.startTime = request.startTime();
        booking.endTime = request.endTime();
        booking.purpose = request.purpose();
        booking.participants = request.participants();
        booking.slotMinutesSnapshot = rule.slotMinutes();
        booking.approvalLevelSnapshot = rule.approvalLevel();
        booking.needCheckinSnapshot = rule.needCheckin();
        booking.clientRequestId = key;
        booking.status = rule.approvalLevel() == 0 ? "APPROVED" : "PENDING_APPROVAL";
        if (rule.approvalLevel() > 0) {
            LocalDateTime holdDeadline = LocalDateTime.now().plusMinutes(settings.approvalTimeoutMinutes());
            booking.approvalDeadline = holdDeadline.isBefore(request.startTime()) ? holdDeadline : request.startTime();
        }
        try {
            booking = bookings.saveAndFlush(booking);
        } catch (DataIntegrityViolationException exception) {
            return bookings.findByUserIdAndClientRequestId(userId, key).orElseThrow(() ->
                    new BusinessException("BOOKING_CONFLICT", "The selected time is already occupied", HttpStatus.CONFLICT));
        }
        lifecycle.recordInitial(booking, userId, requestId(servletRequest));
        try {
            for (LocalDateTime slotTime = request.startTime(); slotTime.isBefore(request.endTime()); slotTime = slotTime.plusMinutes(rule.slotMinutes())) {
                BookingSlot slot = new BookingSlot();
                slot.resourceId = request.resourceId();
                slot.bookingId = booking.id;
                slot.slotStart = slotTime;
                slots.saveAndFlush(slot);
            }
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException("BOOKING_CONFLICT", "The selected time is already occupied", HttpStatus.CONFLICT);
        }
        if (rule.approvalLevel() > 0) {
            approvalTasks.create(booking, rule, servletRequest.getHeader("Authorization"));
        }
        return booking;
    }

    @Override
    public List<Booking> my(HttpServletRequest servletRequest) {
        roleGuard.requireAny(servletRequest, Roles.STUDENT, Roles.TEACHER);
        LocalDateTime now = LocalDateTime.now();
        return bookings.findByUserIdOrderByStartTimeDesc(currentUser(servletRequest)).stream()
                .sorted((left, right) -> compareByProximity(left, right, now))
                .toList();
    }

    @Override
    public List<BookingController.OccupiedInterval> occupied(Long resourceId, LocalDateTime start, LocalDateTime end, HttpServletRequest servletRequest) {
        if (start == null || end == null || !start.isBefore(end)) {
            throw new BusinessException("INVALID_TIME", "Start time must be before end time", HttpStatus.BAD_REQUEST);
        }
        return bookings.findActiveOccupancy(resourceId, start, end).stream()
                .sorted(Comparator.comparing((Booking item) -> item.startTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(item -> new BookingController.OccupiedInterval(item.startTime, item.endTime))
                .toList();
    }

    @Override
    public Booking get(Long bookingId, HttpServletRequest servletRequest) {
        roleGuard.requireAny(servletRequest, Roles.STUDENT, Roles.TEACHER);
        Booking booking = find(bookingId);
        if (!Objects.equals(booking.userId, currentUser(servletRequest))) {
            throw new BusinessException("FORBIDDEN", "You cannot view this booking", HttpStatus.FORBIDDEN);
        }
        return booking;
    }

    @Override
    @Transactional
    public Booking cancel(Long bookingId, HttpServletRequest servletRequest) {
        roleGuard.requireAny(servletRequest, Roles.STUDENT, Roles.TEACHER);
        Long userId = currentUser(servletRequest);
        Booking booking = find(bookingId);
        if (!Objects.equals(booking.userId, userId)) throw new BusinessException("FORBIDDEN", "You cannot cancel this booking", HttpStatus.FORBIDDEN);
        if (!List.of("PENDING_APPROVAL", "APPROVED").contains(booking.status) || !booking.startTime.isAfter(LocalDateTime.now())) {
            throw new BusinessException("INVALID_STATUS", "Booking cannot be canceled", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        boolean closeApproval = "PENDING_APPROVAL".equals(booking.status);
        lifecycle.transition(booking, "CANCELED", userId, "Canceled by applicant", requestId(servletRequest));
        booking.canceledAt = LocalDateTime.now();
        lifecycle.releaseSlots(booking.id, "CANCELED");
        Booking saved = bookings.save(booking);
        if (closeApproval) closeApprovalTasksAfterCommit(List.of(saved.id), "CANCELED");
        return saved;
    }

    @Override
    @Transactional
    public Booking checkin(Long bookingId, HttpServletRequest servletRequest) {
        roleGuard.requireAny(servletRequest, Roles.STUDENT, Roles.TEACHER);
        Long userId = currentUser(servletRequest);
        Booking booking = find(bookingId);
        if (!Objects.equals(booking.userId, userId)) throw new BusinessException("FORBIDDEN", "You cannot check in for this booking", HttpStatus.FORBIDDEN);
        LocalDateTime now = LocalDateTime.now();
        if (!"APPROVED".equals(booking.status) || now.isBefore(booking.startTime.minusMinutes(settings.checkinBeforeMinutes())) || now.isAfter(booking.startTime.plusMinutes(settings.checkinAfterMinutes()))) {
            throw new BusinessException("CHECKIN_WINDOW", "Check-in is not currently available", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        lifecycle.transition(booking, "CHECKED_IN", userId, null, requestId(servletRequest));
        booking.checkinAt = now;
        return bookings.save(booking);
    }

    @Override
    @Transactional
    public void autoComplete() {
        LocalDateTime now = LocalDateTime.now();
        bookings.findByStatus("APPROVED").stream().filter(booking -> !booking.needCheckinSnapshot && booking.endTime != null && !booking.endTime.isAfter(now)).forEach(booking -> {
            lifecycle.transition(booking, "COMPLETED", null, "Automatically completed", null);
            booking.completedAt = now;
            bookings.save(booking);
            lifecycle.releaseSlots(booking.id, "COMPLETED");
        });
        bookings.findByStatus("CHECKED_IN").stream().filter(booking -> booking.endTime != null && !booking.endTime.isAfter(now)).forEach(booking -> {
            lifecycle.transition(booking, "COMPLETED", null, "Automatically completed after check-in", null);
            booking.completedAt = now;
            bookings.save(booking);
            lifecycle.releaseSlots(booking.id, "COMPLETED");
        });
    }

    @Override
    @Transactional
    public void markNoShow() {
        LocalDateTime now = LocalDateTime.now();
        bookings.findByStatus("APPROVED").stream().filter(booking -> booking.needCheckinSnapshot && booking.startTime != null && now.isAfter(booking.startTime.plusMinutes(settings.checkinAfterMinutes()))).forEach(booking -> {
            lifecycle.transition(booking, "NO_SHOW", null, "Check-in deadline passed", null);
            bookings.save(booking);
            lifecycle.releaseSlots(booking.id, "NO_SHOW");
            lifecycle.recordNoShow(booking);
        });
    }

    private Booking find(Long bookingId) { return bookings.findById(bookingId).orElseThrow(() -> new BusinessException("NOT_FOUND", "Booking does not exist", HttpStatus.NOT_FOUND)); }
    private int compareByProximity(Booking left, Booking right, LocalDateTime now) {
        boolean leftCurrentOrUpcoming = left.endTime != null && !left.endTime.isBefore(now);
        boolean rightCurrentOrUpcoming = right.endTime != null && !right.endTime.isBefore(now);
        if (leftCurrentOrUpcoming != rightCurrentOrUpcoming) return leftCurrentOrUpcoming ? -1 : 1;
        Comparator<LocalDateTime> times = leftCurrentOrUpcoming
                ? Comparator.nullsLast(Comparator.naturalOrder())
                : Comparator.nullsLast(Comparator.reverseOrder());
        int result = times.compare(left.startTime, right.startTime);
        if (result != 0) return result;
        return Comparator.nullsLast(Comparator.<Long>reverseOrder()).compare(left.id, right.id);
    }
    private Long currentUser(HttpServletRequest request) {
        if (request.getAttribute("userId") instanceof Long userId) return userId;
        throw new BusinessException("UNAUTHORIZED", "Login required", HttpStatus.UNAUTHORIZED);
    }

    @Override
    @Transactional
    public void expirePendingApprovals() {
        LocalDateTime now = LocalDateTime.now();
        List<Long> expiredIds = new ArrayList<>();
        bookings.findByStatus("PENDING_APPROVAL").forEach(booking -> {
            if (booking.approvalDeadline == null) {
                LocalDateTime createdDeadline = booking.createdAt.plusMinutes(settings.approvalTimeoutMinutes());
                booking.approvalDeadline = createdDeadline.isBefore(booking.startTime)
                        ? createdDeadline : booking.startTime;
            }
            if (!booking.approvalDeadline.isAfter(now)) {
                lifecycle.transition(booking, "EXPIRED", null, "Approval hold expired", null);
                lifecycle.releaseSlots(booking.id, "APPROVAL_TIMEOUT");
                expiredIds.add(booking.id);
            }
            bookings.save(booking);
        });
        closeApprovalTasksAfterCommit(expiredIds, "EXPIRED");
    }

    private void closeApprovalTasksAfterCommit(List<Long> bookingIds, String reason) {
        if (bookingIds == null || bookingIds.isEmpty()) return;
        Runnable close = () -> {
            for (Long bookingId : bookingIds) {
                try {
                    approvalTasks.closePending(bookingId, reason);
                } catch (RuntimeException exception) {
                    log.warn("Failed to close approval task for booking {}: {}", bookingId, exception.getMessage());
                }
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    close.run();
                }
            });
        } else {
            close.run();
        }
    }

    private void assertFairUse(Long userId, BookingController.Create request, HttpServletRequest servletRequest) {
        LocalDateTime now = LocalDateTime.now();
        if (!request.startTime().isAfter(now) || request.startTime().isAfter(now.plusDays(maxAdvanceDays))) {
            throw new BusinessException("BOOKING_ADVANCE_LIMIT",
                    "预约开始时间必须在未来 " + maxAdvanceDays + " 天内", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        boolean teacher = roleGuard.hasRole(servletRequest, Roles.TEACHER) || roleGuard.hasRole(servletRequest, Roles.LAB_ADMIN);
        int activeLimit = teacher ? teacherMaxActive : maxActive;
        int pendingLimit = teacher ? teacherMaxPending : maxPending;
        int resourceLimit = teacher ? teacherMaxPerResource : maxPerResource;
        int dailyMinutesLimit = teacher ? teacherMaxDailyMinutes : maxDailyMinutes;
        List<Booking> userBookings = bookings.findByUserIdOrderByStartTimeDesc(userId);
        List<Booking> active = userBookings.stream()
                .filter(item -> QUOTA_STATUSES.contains(item.status) && item.endTime != null && item.endTime.isAfter(now))
                .toList();
        if (active.size() >= activeLimit) {
            throw new BusinessException("BOOKING_ACTIVE_LIMIT",
                    "你当前最多可以保留 " + activeLimit + " 个未来有效预约，请先取消或完成已有预约", HttpStatus.TOO_MANY_REQUESTS);
        }
        long pending = active.stream().filter(item -> "PENDING_APPROVAL".equals(item.status)).count();
        if (pending >= pendingLimit) {
            throw new BusinessException("BOOKING_PENDING_LIMIT",
                    "你最多可以同时提交 " + pendingLimit + " 个待审批预约，请等待审批后再提交", HttpStatus.TOO_MANY_REQUESTS);
        }
        long sameResource = active.stream().filter(item -> Objects.equals(item.resourceId, request.resourceId())).count();
        if (sameResource >= resourceLimit) {
            throw new BusinessException("BOOKING_RESOURCE_LIMIT",
                    "同一资源最多保留 " + resourceLimit + " 个未来预约", HttpStatus.TOO_MANY_REQUESTS);
        }
        boolean overlaps = active.stream().anyMatch(item -> item.startTime.isBefore(request.endTime())
                && item.endTime.isAfter(request.startTime()));
        if (overlaps) {
            throw new BusinessException("USER_TIME_CONFLICT", "你在该时间段已有其他预约", HttpStatus.CONFLICT);
        }
        long existingMinutes = userBookings.stream()
                .filter(item -> DAILY_MINUTE_STATUSES.contains(item.status))
                .filter(item -> item.startTime != null && item.startTime.toLocalDate().equals(request.startTime().toLocalDate()))
                .mapToLong(item -> java.time.Duration.between(item.startTime, item.endTime).toMinutes())
                .sum();
        long requestedMinutes = java.time.Duration.between(request.startTime(), request.endTime()).toMinutes();
        if (existingMinutes + requestedMinutes > dailyMinutesLimit) {
            throw new BusinessException("BOOKING_DAILY_DURATION_LIMIT",
                    "同一用户每天累计预约时长不能超过 " + dailyMinutesLimit + " 分钟", HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    @Override
    public Map<String, Object> adminList(Long resourceId, Long userId, String status, HttpServletRequest servletRequest) {
        roleGuard.requireLabAdmin(servletRequest);
        List<Booking> items = bookings.findAll().stream()
                .filter(item -> resourceId == null || Objects.equals(item.resourceId, resourceId))
                .filter(item -> userId == null || Objects.equals(item.userId, userId))
                .filter(item -> status == null || status.isBlank() || status.equals(item.status))
                .sorted(Comparator.comparing((Booking item) -> item.startTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        return Map.of("items", items, "total", items.size());
    }

    @Override
    public Map<String, Object> violations(HttpServletRequest servletRequest) {
        roleGuard.requireLabAdmin(servletRequest);
        List<Map<String, Object>> items = violations.findAllByOrderByCreatedAtDesc().stream().map(item -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", item.id);
            row.put("bookingId", item.bookingId);
            row.put("userId", item.userId);
            row.put("violationType", item.violationType);
            row.put("status", item.status);
            row.put("comment", item.comment);
            row.put("processedBy", item.processedBy);
            row.put("processedAt", item.processedAt);
            row.put("createdAt", item.createdAt);
            bookings.findById(item.bookingId).ifPresent(booking -> {
                row.put("bookingNo", booking.bookingNo);
                row.put("applicantName", booking.applicantNameSnapshot);
                row.put("resourceName", booking.resourceNameSnapshot);
                row.put("startTime", booking.startTime);
                row.put("endTime", booking.endTime);
            });
            return row;
        }).toList();
        return Map.of("items", items, "total", items.size());
    }

    @Override
    @Transactional
    public Object processViolation(Long violationId, String status, String comment, HttpServletRequest servletRequest) {
        roleGuard.requireLabAdmin(servletRequest);
        if (!Set.of("CONFIRMED", "DISMISSED").contains(status)) {
            throw new BusinessException("INVALID_STATUS", "Violation status is invalid", HttpStatus.BAD_REQUEST);
        }
        ViolationRecord item = violations.findById(violationId)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Violation record does not exist", HttpStatus.NOT_FOUND));
        if (!"OPEN".equals(item.status)) {
            throw new BusinessException("VIOLATION_ALREADY_PROCESSED", "Violation has already been processed", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        String normalizedComment = comment == null ? "" : comment.trim();
        if ("DISMISSED".equals(status) && normalizedComment.isBlank()) {
            throw new BusinessException("VIOLATION_REASON_REQUIRED", "撤销违约时必须填写原因", HttpStatus.BAD_REQUEST);
        }
        item.status = status;
        item.comment = normalizedComment.isBlank() ? item.comment : normalizedComment;
        item.processedBy = roleGuard.currentUserId(servletRequest);
        item.processedAt = LocalDateTime.now();
        ViolationRecord saved = violations.save(item);
        if ("DISMISSED".equals(status)) lifecycle.refreshRestriction(item.userId);
        return saved;
    }

    @Override
    @Transactional
    public Map<String, Object> cancelOverlappingForClosure(Long resourceId, LocalDateTime start, LocalDateTime end, String reason, HttpServletRequest servletRequest) {
        if (resourceId == null || start == null || end == null || !start.isBefore(end)) {
            throw new BusinessException("INVALID_CLOSURE", "Closure interval is invalid", HttpStatus.BAD_REQUEST);
        }
        Long operatorId = servletRequest.getAttribute("userId") instanceof Long value ? value : null;
        String history = reason == null || reason.isBlank() ? "Resource closed for maintenance" : reason.trim();
        List<Long> pendingIds = new ArrayList<>();
        List<Booking> overlapping = bookings.findOverlapping(resourceId, start, end);
        for (Booking booking : overlapping) {
            boolean pending = "PENDING_APPROVAL".equals(booking.status);
            lifecycle.transition(booking, "CANCELED", operatorId, history, requestId(servletRequest));
            booking.canceledAt = LocalDateTime.now();
            booking.cancelReason = history;
            lifecycle.releaseSlots(booking.id, "RESOURCE_CLOSED");
            bookings.save(booking);
            if (pending) pendingIds.add(booking.id);
        }
        closeApprovalTasksAfterCommit(pendingIds, "CANCELED");
        return Map.of("cancelledCount", overlapping.size());
    }

    private String requestId(HttpServletRequest request) { return Objects.toString(request.getAttribute("X-Request-Id"), ""); }
}
