package com.lab.system;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Table(name = "operation_log")
public class OperationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Long operatorId;
    public String operationType;
    public String targetType;
    public Long targetId;
    public String result = "SUCCESS";
    public String reason;
    public String requestId;
    public String ip;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    public Map<String, Object> detail = new LinkedHashMap<>();
    public LocalDateTime createdAt = LocalDateTime.now();
}
