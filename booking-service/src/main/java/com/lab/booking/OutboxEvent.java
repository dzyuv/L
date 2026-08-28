package com.lab.booking;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("outbox_event")
public class OutboxEvent {
    @TableId(type=IdType.AUTO) public Long id;
    public String eventId;
    public String eventType;
    public String aggregateType;
    public Long aggregateId;
    public String payload;
    public String status="PENDING";
    public int retryCount;
    public String lastError;
    public LocalDateTime nextRetryAt;
    public LocalDateTime sentAt;
    public LocalDateTime createdAt=LocalDateTime.now();
}
