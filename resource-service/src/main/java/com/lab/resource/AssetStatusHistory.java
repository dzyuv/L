package com.lab.resource;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("asset_status_history")
public class AssetStatusHistory {
    @TableId(type=IdType.AUTO) public Long id;
    public Long assetId;
    public String fromStatus;
    public String toStatus;
    public String reason;
    public Long operatorId;
    public LocalDateTime createdAt = LocalDateTime.now();
}
