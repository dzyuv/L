package com.lab.booking.entity;
import com.lab.booking.repository.*;
import com.lab.booking.client.*;
import com.lab.booking.scheduler.*;
import com.lab.booking.config.*;
import com.lab.booking.service.*;
import com.lab.booking.service.impl.*;
import com.lab.booking.controller.*;
import com.lab.booking.approval.entity.*;
import com.lab.booking.approval.repository.*;
import com.lab.booking.approval.service.*;
import com.lab.booking.approval.service.impl.*;
import com.lab.booking.approval.controller.*;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("violation_record")
public class ViolationRecord {
    @TableId(type=IdType.AUTO) public Long id;
    public Long bookingId;
    public Long userId;
    public String violationType;
    public String status="OPEN";
    public String comment;
    public Long processedBy;
    public LocalDateTime processedAt;
    public LocalDateTime createdAt=LocalDateTime.now();
}
