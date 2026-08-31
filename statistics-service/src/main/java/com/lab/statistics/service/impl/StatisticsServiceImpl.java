package com.lab.statistics.service.impl;

import com.lab.common.api.RoleGuard;
import com.lab.common.api.Roles;
import com.lab.statistics.StatisticsSnapshot;
import com.lab.statistics.StatisticsSnapshotRepository;
import com.lab.statistics.service.StatisticsAggregationService;
import com.lab.statistics.service.StatisticsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class StatisticsServiceImpl implements StatisticsService {
    private final StatisticsSnapshotRepository snapshots;
    private final StatisticsAggregationService aggregation;
    private final RoleGuard roleGuard;

    public StatisticsServiceImpl(StatisticsSnapshotRepository snapshots, StatisticsAggregationService aggregation, RoleGuard roleGuard) {
        this.snapshots = snapshots;
        this.aggregation = aggregation;
        this.roleGuard = roleGuard;
    }

    public Map<String, Object> usage(HttpServletRequest request) {
        roleGuard.requireAny(request, Roles.SYSTEM_ADMIN, Roles.LAB_ADMIN);
        List<StatisticsSnapshot> items = snapshots.findTop100ByOrderByPeriodEndDesc();
        if (items.isEmpty()) {
            aggregation.refresh();
            items = snapshots.findTop100ByOrderByPeriodEndDesc();
        }
        Map<Long, String> names = aggregation.resourceNames();
        List<Map<String, Object>> utilization = snapshots.findByMetricType("RESOURCE_USAGE").stream()
                .sorted(Comparator.comparing((StatisticsSnapshot item) -> Objects.requireNonNullElse(item.metricValue, BigDecimal.ZERO)).reversed())
                .map(item -> resourceRow(item, names))
                .toList();
        List<Map<String, Object>> ranking = snapshots.findByMetricType("RESOURCE_RANKING").stream()
                .sorted(Comparator.comparing((StatisticsSnapshot item) -> Objects.requireNonNullElse(item.numerator, BigDecimal.ZERO)).reversed())
                .map(item -> {
                    Map<String, Object> row = resourceRow(item, names);
                    row.put("bookingCount", item.numerator);
                    return row;
                })
                .toList();
        List<Map<String, Object>> trend = snapshots.findByMetricType("BOOKING_TREND").stream()
                .sorted(Comparator.comparing((StatisticsSnapshot item) -> item.periodStart))
                .map(item -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("date", item.periodStart.toLocalDate());
                    row.put("count", item.numerator);
                    return row;
                })
                .toList();
        List<Map<String, Object>> userBookings = snapshots.findByMetricType("USER_BOOKING_COUNT").stream()
                .sorted(Comparator.comparing((StatisticsSnapshot item) -> Objects.requireNonNullElse(item.numerator, BigDecimal.ZERO)).reversed())
                .map(item -> Map.<String, Object>of("userId", item.userId, "count", item.numerator))
                .toList();
        List<Map<String, Object>> userViolations = snapshots.findByMetricType("USER_VIOLATION_COUNT").stream()
                .sorted(Comparator.comparing((StatisticsSnapshot item) -> Objects.requireNonNullElse(item.numerator, BigDecimal.ZERO)).reversed())
                .map(item -> Map.<String, Object>of("userId", item.userId, "count", item.numerator))
                .toList();
        StatisticsSnapshot overall = snapshots.findByMetricType("OVERALL_USAGE").stream().findFirst().orElse(null);
        StatisticsSnapshot bookingCount = snapshots.findByMetricType("BOOKING_COUNT").stream().findFirst().orElse(null);
        StatisticsSnapshot violationCount = snapshots.findByMetricType("VIOLATION_COUNT").stream().findFirst().orElse(null);
        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("usedMinutes", overall == null ? 0 : overall.numerator);
        totals.put("bookableMinutes", overall == null ? 0 : overall.denominator);
        totals.put("utilization", overall == null ? BigDecimal.ZERO : overall.metricValue);
        totals.put("bookingCount", bookingCount == null ? 0 : bookingCount.numerator);
        totals.put("violationCount", violationCount == null ? 0 : violationCount.numerator);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("periodStart", overall == null ? null : overall.periodStart);
        result.put("periodEnd", overall == null ? null : overall.periodEnd);
        result.put("calculatedUntil", items.stream().map(item -> item.calculatedUntil).filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null));
        result.put("dataVersion", items.stream().map(item -> item.dataVersion).filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(0L));
        result.put("utilization", utilization);
        result.put("ranking", ranking);
        result.put("trend", trend);
        result.put("userBookings", userBookings);
        result.put("userViolations", userViolations);
        result.put("totals", totals);
        result.put("items", items);
        return result;
    }

    private Map<String, Object> resourceRow(StatisticsSnapshot item, Map<Long, String> names) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("resourceId", item.resourceId);
        row.put("resourceName", names.getOrDefault(item.resourceId, "资源 " + item.resourceId));
        row.put("usedMinutes", item.numerator);
        row.put("bookableMinutes", item.denominator);
        BigDecimal rate = item.metricValue == null ? BigDecimal.ZERO : item.metricValue;
        row.put("rate", rate);
        row.put("ratePercent", rate.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP));
        return row;
    }
}
