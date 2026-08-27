package com.lab.statistics.service.impl;

import com.lab.statistics.service.StatisticsService;
import com.lab.statistics.StatisticsSnapshot;
import com.lab.statistics.StatisticsSnapshotRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import java.util.*;
import com.lab.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import com.lab.common.api.RoleGuard;

@Service
public class StatisticsServiceImpl implements StatisticsService {
    private final StatisticsSnapshotRepository snapshots;
    private final RoleGuard roleGuard;
    public StatisticsServiceImpl(StatisticsSnapshotRepository snapshots, RoleGuard roleGuard) { this.snapshots = snapshots; this.roleGuard = roleGuard; }
    public Map<String, Object> usage(HttpServletRequest request) {
        roleGuard.requireSystemAdmin(request);
        List<StatisticsSnapshot> items = snapshots.findTop100ByOrderByPeriodEndDesc();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("calculatedUntil", items.stream().map(item -> item.calculatedUntil).filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null));
        result.put("dataVersion", items.stream().map(item -> item.dataVersion).filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(0L));
        return result;
    }
}
