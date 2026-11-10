package com.lab.user.entity;
import com.lab.user.repository.*;
import com.lab.user.client.*;
import com.lab.user.seed.*;
import com.lab.user.scheduler.*;
import com.lab.user.security.*;
import com.lab.user.config.*;
import com.lab.user.controller.*;
import com.lab.user.service.*;
import com.lab.user.service.impl.*;

import com.baomidou.mybatisplus.annotation.*;
import java.util.LinkedHashSet;
import java.util.Set;

@TableName("role")
public class Role {
    @TableId(type=IdType.AUTO)
    public Long id;
    public String code;
    public String name;
    public String status="ACTIVE";
    @TableField(exist=false)
    public Set<Permission> permissions=new LinkedHashSet<>();
}
