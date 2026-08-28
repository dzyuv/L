package com.lab.user;

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
