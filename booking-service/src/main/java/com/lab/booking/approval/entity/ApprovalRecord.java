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

import java.time.LocalDateTime;

@TableName("approval_record")
public class ApprovalRecord {
    @TableId(type = IdType.AUTO) public Long id;
    public Long taskId;
    public Long bookingId;
    public Long approverId;
    public String result;
    public String comment;
    public String requestId;
    public LocalDateTime createdAt = LocalDateTime.now();
}
