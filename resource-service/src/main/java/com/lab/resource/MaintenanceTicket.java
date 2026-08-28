package com.lab.resource;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("maintenance_ticket")
public class MaintenanceTicket {
    @TableId(type=IdType.AUTO) public Long id;
    public String ticketNo;
    public Long assetId;
    public Long resourceId;
    public String locationSnapshot;
    public String assetClue;
    public Long reportedBy;
    public String previousAssetStatus;
    public String reportType = "MALFUNCTION";
    public String severity = "MEDIUM";
    public String description;
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
