package com.lab.approval;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("approval_record")
public class ApprovalRecord {
    @TableId(type=IdType.AUTO) public Long id;
    public Long taskId;
    public Long bookingId;
    public Long approverId;
    public String result;
    public String comment;
    public String requestId;
    public LocalDateTime createdAt = LocalDateTime.now();
}
