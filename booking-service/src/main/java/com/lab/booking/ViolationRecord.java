package com.lab.booking;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("violation_record")
public class ViolationRecord {
    @TableId(type=IdType.AUTO) public Long id;
    public Long bookingId;
    public Long userId;
    public String violationType;
    public String status="OPEN";
    public String comment;
    public Long processedBy;
    public LocalDateTime processedAt;
    public LocalDateTime createdAt=LocalDateTime.now();
}
