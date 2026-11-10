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

@TableName("user_restriction")
public class UserRestriction {
    @TableId(type=IdType.AUTO) public Long id;
    public Long userId;
    public LocalDateTime restrictedUntil;
    public String reason;
    public int sourceViolationCount;
    public String status="ACTIVE";
    public Long createdBy;
    public LocalDateTime createdAt=LocalDateTime.now();
    public LocalDateTime updatedAt=LocalDateTime.now();
}
