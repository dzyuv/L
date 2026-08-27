package com.lab.statistics;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StatisticsSnapshotRepository extends JpaRepository<StatisticsSnapshot, Long> {
    List<StatisticsSnapshot> findTop100ByOrderByPeriodEndDesc();
}
