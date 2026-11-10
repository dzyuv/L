package com.lab.resource.entity;
import com.lab.resource.repository.*;
import com.lab.resource.client.*;
import com.lab.resource.seed.*;
import com.lab.resource.controller.*;
import com.lab.resource.service.*;
import com.lab.resource.service.impl.*;
import com.baomidou.mybatisplus.annotation.*;
@TableName("resource_type") public class ResourceType{
    @TableId(type=IdType.AUTO) public Long id;
    public String name;
    public int defaultApprovalLevel=1;
    public boolean defaultNeedCheckin=true;
    public boolean enabled=true;
    @Version public int version;
    public boolean deleted;
}
