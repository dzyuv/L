package com.lab.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("permission")
public class Permission {
    @TableId(type=IdType.AUTO)
    public Long id;
    public String code;
    public String name;
}
