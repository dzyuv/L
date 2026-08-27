package com.lab.statistics;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "statistics_snapshot")
public class StatisticsSnapshot {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public String metricType;
    public Long resourceId;
    public Long userId;
    public LocalDateTime periodStart;
    public LocalDateTime periodEnd;
    public BigDecimal numerator;
    public BigDecimal denominator;
    public BigDecimal metricValue;
    public LocalDateTime calculatedUntil;
    public Long dataVersion;
    public LocalDateTime createdAt = LocalDateTime.now();
}
