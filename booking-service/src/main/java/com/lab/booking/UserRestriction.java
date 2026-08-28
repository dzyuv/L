package com.lab.booking;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("user_restriction")
public class UserRestriction {
    @TableId(type=IdType.AUTO) public Long id;
    public Long userId;
    public LocalDateTime restrictedUntil;
    public String reason;
    public int sourceViolationCount;
    public String status="ACTIVE";
    public Long createdBy;
    public LocalDateTime createdAt=LocalDateTime.now();
    public LocalDateTime updatedAt=LocalDateTime.now();
}
