package com.lab.resource.entity;
import com.lab.resource.repository.*;
import com.lab.resource.client.*;
import com.lab.resource.seed.*;
import com.lab.resource.controller.*;
import com.lab.resource.service.*;
import com.lab.resource.service.impl.*;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("resource_manager")
public class ResourceManager {
    @TableId(type=IdType.AUTO) public Long id;
    public Long resourceId;
    public Long userId;
    public String managerType="OWNER";
    public String scopeType="RESOURCE";
    public String scopeValue="";
    public LocalDateTime createdAt=LocalDateTime.now();
}
