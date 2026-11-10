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
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
@TableName("`user`") public class User {
    @TableId(type=IdType.AUTO) public Long id;
    public String employeeNo;
    public String username;
    public String passwordHash;
    public String realName;
    public String email;
    public String phone;
    public String status="ACTIVE";
    public int failedLoginCount;
    public Instant lockedUntil;
    public Instant lastLoginAt;
    public int tokenVersion;
    @Version public int version;
    public boolean deleted;
    public Instant createdAt=Instant.now();
    public Instant updatedAt=Instant.now();
    @TableField(exist=false)
    public Set<Role> roles=new LinkedHashSet<>();
}
