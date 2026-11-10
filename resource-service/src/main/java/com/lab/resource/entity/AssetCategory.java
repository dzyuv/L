package com.lab.resource.entity;
import com.lab.resource.repository.*;
import com.lab.resource.client.*;
import com.lab.resource.seed.*;
import com.lab.resource.controller.*;
import com.lab.resource.service.*;
import com.lab.resource.service.impl.*;

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
