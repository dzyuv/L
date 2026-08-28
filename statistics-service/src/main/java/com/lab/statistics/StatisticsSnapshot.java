package com.lab.statistics;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("statistics_snapshot")
public class StatisticsSnapshot {
    @TableId(type=IdType.AUTO) public Long id;
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
