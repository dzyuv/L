package com.lab.statistics;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lab.common.persistence.CrudMapper;
import java.util.List;

public interface StatisticsSnapshotRepository extends CrudMapper<StatisticsSnapshot> {
    default List<StatisticsSnapshot> findTop100ByOrderByPeriodEndDesc() {
        return selectList(Wrappers.<StatisticsSnapshot>query().orderByDesc("period_end").last("LIMIT 100"));
    }
    default void deleteCalculatedMetrics() {
        delete(Wrappers.<StatisticsSnapshot>query().in("metric_type", "RESOURCE_USAGE", "RESOURCE_RANKING",
                "OVERALL_USAGE", "BOOKING_COUNT", "VIOLATION_COUNT", "USER_BOOKING_COUNT", "USER_VIOLATION_COUNT", "BOOKING_TREND"));
    }
    default List<StatisticsSnapshot> findByMetricType(String metricType) {
        return selectList(Wrappers.<StatisticsSnapshot>query().eq("metric_type", metricType).orderByDesc("period_end"));
    }
}
