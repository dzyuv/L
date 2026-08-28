package com.lab.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("system_config")
public class SystemConfig {
    @TableId(type=IdType.AUTO) public Long id;
    public String configKey;
    public String configValue;
    public String valueType = "STRING";
    public String description;
    public Long updatedBy;
    public LocalDateTime updatedAt = LocalDateTime.now();
    public int version;
}
