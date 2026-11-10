package com.lab.resource.entity;
import com.lab.resource.repository.*;
import com.lab.resource.client.*;
import com.lab.resource.seed.*;
import com.lab.resource.controller.*;
import com.lab.resource.service.*;
import com.lab.resource.service.impl.*;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
@TableName("resource_closure") public class ResourceClosure{
    @TableId(type=IdType.AUTO) public Long id;
    public Long resourceId;
    public LocalDateTime startTime;
    public LocalDateTime endTime;
    public String reason;
    public String status="PLANNED";
    public String handledBookingPolicy;
    public Long createdBy;
    public LocalDateTime updatedAt=LocalDateTime.now();
    @Version public int version;
    public LocalDateTime createdAt=LocalDateTime.now();
}
