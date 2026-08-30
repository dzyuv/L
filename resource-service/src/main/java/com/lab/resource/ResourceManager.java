package com.lab.resource;

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
