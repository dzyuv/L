package com.lab.booking.approval;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;

import java.time.LocalDateTime;

@TableName("approval_task")
public class ApprovalTask {
    @TableId(type = IdType.AUTO) public Long id;
    public Long bookingId;
    public Long applicantUserId;
    public String applicantName;
    public Long resourceId;
    public Long resourceTypeId;
    public String resourceName;
    public LocalDateTime startTime;
    public LocalDateTime endTime;
    public int level;
    public int totalLevels = 1;
    public String approverRole;
    public Long assignedUserId;
    public String status = "PENDING";
    public LocalDateTime deadline;
    public LocalDateTime completedAt;
    public String comment;
    @Version public int version;
    public LocalDateTime createdAt = LocalDateTime.now();
}
