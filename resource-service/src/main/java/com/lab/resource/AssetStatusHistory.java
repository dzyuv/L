package com.lab.resource;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "asset_status_history")
public class AssetStatusHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false) public Long assetId;
    public String fromStatus;
    @Column(nullable = false) public String toStatus;
    public String reason;
    public Long operatorId;
    public LocalDateTime createdAt = LocalDateTime.now();
}
