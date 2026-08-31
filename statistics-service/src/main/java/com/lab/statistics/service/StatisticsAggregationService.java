package com.lab.statistics.service;

import com.lab.statistics.StatisticsSnapshot;
import com.lab.statistics.StatisticsSnapshotRepository;
import com.lab.statistics.StatisticsSourceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StatisticsAggregationService {
    private static final Set<String> USAGE_STATUSES = Set.of("APPROVED", "CHECKED_IN", "COMPLETED");
    private static final Set<String> COUNTED_STATUSES = Set.of("PENDING_APPROVAL", "APPROVED", "CHECKED_IN", "COMPLETED");
    private static final long ALL = 0L;

    private final StatisticsSourceClient sources;
    private final StatisticsSnapshotRepository snapshots;
    private final int windowDays;
    private final Map<Long, String> resourceNames = new ConcurrentHashMap<>();

    public StatisticsAggregationService(StatisticsSourceClient sources, StatisticsSnapshotRepository snapshots,
                                        @Value("${statistics.window-days:7}") int windowDays) {
        this.sources = sources;
        this.snapshots = snapshots;
        this.windowDays = Math.max(1, windowDays);
    }

    @Scheduled(initialDelay = 5000, fixedDelayString = "${statistics.refresh-ms:600000}")
    @Transactional
    public synchronized LocalDateTime refresh() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime periodStart = LocalDate.now().minusDays(windowDays - 1L).atStartOfDay();
        LocalDateTime periodEnd = now;
        List<Map<String, Object>> catalog = sources.resourceCatalog();
        resourceNames.clear();
        for (Map<String, Object> resource : catalog) {
            Long resourceId = number(resource.get("id"));
            if (resourceId != null) resourceNames.put(resourceId, String.valueOf(resource.getOrDefault("name", "资源 " + resourceId)));
        }
        Map<String, Object> bookingSource = sources.bookingSource(periodStart, periodEnd);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> bookings = (List<Map<String, Object>>) bookingSource.getOrDefault("bookings", List.of());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> violations = (List<Map<String, Object>>) bookingSource.getOrDefault("violations", List.of());

        long version = now.toEpochSecond(java.time.ZoneOffset.UTC);
        snapshots.deleteCalculatedMetrics();
        List<StatisticsSnapshot> rows = new ArrayList<>();

        long totalUsed = 0;
        long totalBookable = 0;
        for (Map<String, Object> resource : catalog) {
            Long resourceId = number(resource.get("id"));
            if (resourceId == null) continue;
            long bookable = bookableMinutes(resource, periodStart, periodEnd);
            long used = bookings.stream()
                    .filter(item -> resourceId.equals(number(item.get("resourceId"))))
                    .filter(item -> USAGE_STATUSES.contains(String.valueOf(item.get("status"))))
                    .mapToLong(item -> overlapMinutes(time(item.get("startTime")), time(item.get("endTime")), periodStart, periodEnd))
                    .sum();
            totalUsed += used;
            totalBookable += bookable;
            rows.add(snapshot("RESOURCE_USAGE", resourceId, ALL, periodStart, periodEnd, used, bookable, rate(used, bookable), now, version));
            long bookingCount = bookings.stream()
                    .filter(item -> resourceId.equals(number(item.get("resourceId"))))
                    .filter(item -> COUNTED_STATUSES.contains(String.valueOf(item.get("status"))))
                    .count();
            rows.add(snapshot("RESOURCE_RANKING", resourceId, ALL, periodStart, periodEnd, bookingCount, 1, BigDecimal.valueOf(bookingCount), now, version));
        }
        rows.add(snapshot("OVERALL_USAGE", ALL, ALL, periodStart, periodEnd, totalUsed, totalBookable, rate(totalUsed, totalBookable), now, version));
        rows.add(snapshot("BOOKING_COUNT", ALL, ALL, periodStart, periodEnd,
                bookings.stream().filter(item -> COUNTED_STATUSES.contains(String.valueOf(item.get("status")))).count(),
                1, null, now, version));
        rows.add(snapshot("VIOLATION_COUNT", ALL, ALL, periodStart, periodEnd, violations.size(), 1, null, now, version));

        Map<Long, Long> userBookings = new LinkedHashMap<>();
        for (Map<String, Object> item : bookings) {
            if (!COUNTED_STATUSES.contains(String.valueOf(item.get("status")))) continue;
            Long userId = number(item.get("userId"));
            if (userId == null) continue;
            userBookings.merge(userId, 1L, Long::sum);
        }
        userBookings.forEach((userId, count) -> rows.add(snapshot("USER_BOOKING_COUNT", ALL, userId, periodStart, periodEnd, count, 1, BigDecimal.valueOf(count), now, version)));

        Map<Long, Long> userViolations = new LinkedHashMap<>();
        for (Map<String, Object> item : violations) {
            Long userId = number(item.get("userId"));
            if (userId == null) continue;
            userViolations.merge(userId, 1L, Long::sum);
        }
        userViolations.forEach((userId, count) -> rows.add(snapshot("USER_VIOLATION_COUNT", ALL, userId, periodStart, periodEnd, count, 1, BigDecimal.valueOf(count), now, version)));

        for (int offset = 0; offset < windowDays; offset++) {
            LocalDate day = periodStart.toLocalDate().plusDays(offset);
            LocalDateTime dayStart = day.atStartOfDay();
            LocalDateTime dayEnd = day.plusDays(1).atStartOfDay();
            long count = bookings.stream()
                    .filter(item -> COUNTED_STATUSES.contains(String.valueOf(item.get("status"))))
                    .filter(item -> {
                        LocalDateTime start = time(item.get("startTime"));
                        return start != null && !start.isBefore(dayStart) && start.isBefore(dayEnd);
                    })
                    .count();
            rows.add(snapshot("BOOKING_TREND", ALL, ALL, dayStart, dayEnd, count, 1, BigDecimal.valueOf(count), now, version));
        }

        rows.forEach(snapshots::save);
        return now;
    }

    public Map<Long, String> resourceNames() {
        return Map.copyOf(resourceNames);
    }

    private long bookableMinutes(Map<String, Object> resource, LocalDateTime periodStart, LocalDateTime periodEnd) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> schedules = (List<Map<String, Object>>) resource.getOrDefault("schedules", List.of());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> closures = (List<Map<String, Object>>) resource.getOrDefault("closures", List.of());
        long total = 0;
        for (LocalDate date = periodStart.toLocalDate(); !date.isAfter(periodEnd.toLocalDate()); date = date.plusDays(1)) {
            int weekday = date.getDayOfWeek().getValue();
            for (Map<String, Object> schedule : schedules) {
                if (!asBoolean(schedule.get("enabled"))) continue;
                Long scheduleWeekday = number(schedule.get("weekday"));
                if (scheduleWeekday == null || scheduleWeekday.intValue() != weekday) continue;
                LocalDate from = dateValue(schedule.get("effectiveFrom"));
                LocalDate to = dateValue(schedule.get("effectiveTo"));
                if (from != null && date.isBefore(from)) continue;
                if (to != null && date.isAfter(to)) continue;
                LocalTime open = timeOfDay(schedule.get("openTime"));
                LocalTime close = timeOfDay(schedule.get("closeTime"));
                if (open == null || close == null || !open.isBefore(close)) continue;
                LocalDateTime windowStart = date.atTime(open);
                LocalDateTime windowEnd = date.atTime(close);
                long minutes = overlapMinutes(windowStart, windowEnd, periodStart, periodEnd);
                for (Map<String, Object> closure : closures) {
                    minutes -= overlapMinutes(windowStart, windowEnd, time(closure.get("startTime")), time(closure.get("endTime")));
                }
                total += Math.max(0, minutes);
            }
        }
        return total;
    }

    private boolean asBoolean(Object value) {
        if (value instanceof Boolean flag) return flag;
        return "true".equalsIgnoreCase(String.valueOf(value));
    }

    private StatisticsSnapshot snapshot(String type, Long resourceId, Long userId, LocalDateTime start, LocalDateTime end,
                                        long numerator, long denominator, BigDecimal value, LocalDateTime until, long version) {
        StatisticsSnapshot item = new StatisticsSnapshot();
        item.metricType = type;
        item.resourceId = resourceId;
        item.userId = userId;
        item.periodStart = start;
        item.periodEnd = end;
        item.numerator = BigDecimal.valueOf(numerator);
        item.denominator = BigDecimal.valueOf(denominator);
        item.metricValue = value;
        item.calculatedUntil = until;
        item.dataVersion = version;
        return item;
    }

    private BigDecimal rate(long used, long bookable) {
        if (bookable <= 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(used).divide(BigDecimal.valueOf(bookable), 6, RoundingMode.HALF_UP);
    }

    private long overlapMinutes(LocalDateTime leftStart, LocalDateTime leftEnd, LocalDateTime rightStart, LocalDateTime rightEnd) {
        if (leftStart == null || leftEnd == null || rightStart == null || rightEnd == null) return 0;
        LocalDateTime start = leftStart.isAfter(rightStart) ? leftStart : rightStart;
        LocalDateTime end = leftEnd.isBefore(rightEnd) ? leftEnd : rightEnd;
        if (!start.isBefore(end)) return 0;
        return Math.max(0, Duration.between(start, end).toMinutes());
    }

    private Long number(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.longValue();
        try { return Long.parseLong(String.valueOf(value)); } catch (NumberFormatException exception) { return null; }
    }

    @SuppressWarnings("unchecked")
    private LocalDateTime time(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDateTime time) return time;
        if (value instanceof List<?> parts && parts.size() >= 5) {
            return LocalDateTime.of(number(parts.get(0)).intValue(), number(parts.get(1)).intValue(), number(parts.get(2)).intValue(),
                    number(parts.get(3)).intValue(), number(parts.get(4)).intValue(), parts.size() > 5 ? number(parts.get(5)).intValue() : 0);
        }
        String text = String.valueOf(value).trim().replace(" ", "T");
        if (text.length() == 16) text = text + ":00";
        try { return LocalDateTime.parse(text); } catch (Exception exception) { return null; }
    }

    private LocalTime timeOfDay(Object value) {
        if (value == null) return null;
        if (value instanceof LocalTime time) return time;
        if (value instanceof List<?> parts && parts.size() >= 2) {
            return LocalTime.of(number(parts.get(0)).intValue(), number(parts.get(1)).intValue());
        }
        String text = String.valueOf(value);
        if (text.length() == 5) return LocalTime.parse(text);
        if (text.length() >= 8) return LocalTime.parse(text.substring(0, 8));
        try { return LocalTime.parse(text); } catch (Exception exception) { return null; }
    }

    private LocalDate dateValue(Object value) {
        if (value == null || "null".equals(String.valueOf(value))) return null;
        if (value instanceof LocalDate date) return date;
        if (value instanceof List<?> parts && parts.size() >= 3) {
            return LocalDate.of(number(parts.get(0)).intValue(), number(parts.get(1)).intValue(), number(parts.get(2)).intValue());
        }
        try { return LocalDate.parse(String.valueOf(value).substring(0, 10)); } catch (Exception exception) { return null; }
    }
}
