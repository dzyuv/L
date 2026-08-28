package com.lab.resource;
import com.baomidou.mybatisplus.annotation.*;
@TableName("resource") public class Resource{
    @TableId(type=IdType.AUTO) public Long id;
    public Long typeId;
    public String name;
    public String location;
    public int capacity;
    public String status="ACTIVE";
    public String description;
    public Long ownerUserId;
    public String imageUrl;
    public boolean needCheckin=true;
    public int maxDurationMinutes=120;
    public int slotMinutes=30;
    public Boolean approvalRequiredOverride;
    public Integer approvalLevelOverride;
    @Version public int version;
    public boolean deleted;
}
