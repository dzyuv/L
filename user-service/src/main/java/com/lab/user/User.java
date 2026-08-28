package com.lab.user;
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
