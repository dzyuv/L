package com.lab.resource;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "maintenance_ticket", uniqueConstraints = @UniqueConstraint(columnNames = "ticketNo"))
public class MaintenanceTicket {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false) public String ticketNo;
    public Long assetId;
    public Long resourceId;
    public String locationSnapshot;
    public String assetClue;
    public Long reportedBy;
    public String previousAssetStatus;
    public String reportType = "MALFUNCTION";
    public String severity = "MEDIUM";
    @Column(nullable = false, length = 2000) public String description;
    public String status = "REPORTED";
    public Long assignedTo;
    public BigDecimal estimatedCost;
    public BigDecimal actualCost;
    public String resolution;
    public Long processedBy;
    public LocalDateTime reportedAt = LocalDateTime.now();
    public LocalDateTime processedAt;
    public LocalDateTime closedAt;
    public LocalDateTime createdAt = LocalDateTime.now();
    public LocalDateTime updatedAt = LocalDateTime.now();
    @Version public int version;
}
