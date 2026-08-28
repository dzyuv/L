package com.lab.resource;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("asset_category")
public class AssetCategory {
    @TableId(type=IdType.AUTO) public Long id;
    public String name;
    public boolean serialized = true;
    public boolean highValue = false;
    public boolean enabled = true;
    public String description;
    public LocalDateTime createdAt = LocalDateTime.now();
    public LocalDateTime updatedAt = LocalDateTime.now();
    @Version public int version;
}
