package com.lab.resource;
import com.baomidou.mybatisplus.annotation.*;
@TableName("resource_type") public class ResourceType{
    @TableId(type=IdType.AUTO) public Long id;
    public String name;
    public int defaultApprovalLevel=1;
    public boolean defaultNeedCheckin=true;
    public boolean enabled=true;
    @Version public int version;
    public boolean deleted;
}
