package com.lab.approval;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("approval_flow")
public class ApprovalFlow {
    @TableId(type=IdType.AUTO) public Long id;
    public Long resourceTypeId;
    public int version;
    public boolean enabled = true;
    public Long createdBy;
    public LocalDateTime createdAt = LocalDateTime.now();
}
