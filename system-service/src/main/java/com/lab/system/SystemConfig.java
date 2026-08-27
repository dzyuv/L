package com.lab.system;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity @Table(name = "system_config")
public class SystemConfig {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public String configKey;
    public String configValue;
    public String valueType = "STRING";
    public String description;
    public Long updatedBy;
    public LocalDateTime updatedAt = LocalDateTime.now();
    public int version;
}
