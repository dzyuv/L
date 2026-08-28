package com.lab.booking;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("booking_status_history")
public class BookingStatusHistory {
    @TableId(type=IdType.AUTO) public Long id;
    public Long bookingId;
    public String fromStatus;
    public String toStatus;
    public Long operatorId;
    public String reason;
    public String requestId;
    public LocalDateTime createdAt=LocalDateTime.now();
}
