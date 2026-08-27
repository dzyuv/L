package com.lab.statistics.service.impl;

import com.lab.statistics.service.StatisticsService;
import com.lab.statistics.StatisticsSnapshot;
import com.lab.statistics.StatisticsSnapshotRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class StatisticsServiceImpl implements StatisticsService {
    private final StatisticsSnapshotRepository snapshots;
    public StatisticsServiceImpl(StatisticsSnapshotRepository snapshots) { this.snapshots = snapshots; }
    public Map<String, Object> usage(HttpServletRequest request) {
        List<StatisticsSnapshot> items = snapshots.findTop100ByOrderByPeriodEndDesc();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("calculatedUntil", items.stream().map(item -> item.calculatedUntil).filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null));
        result.put("dataVersion", items.stream().map(item -> item.dataVersion).filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(0L));
        return result;
    }
}
