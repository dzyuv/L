package com.lab.booking.approval.entity;
import com.lab.booking.entity.*;
import com.lab.booking.repository.*;
import com.lab.booking.client.*;
import com.lab.booking.scheduler.*;
import com.lab.booking.config.*;
import com.lab.booking.service.*;
import com.lab.booking.service.impl.*;
import com.lab.booking.controller.*;
import com.lab.booking.approval.repository.*;
import com.lab.booking.approval.service.*;
import com.lab.booking.approval.service.impl.*;
import com.lab.booking.approval.controller.*;

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
