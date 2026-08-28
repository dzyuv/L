package com.lab.booking;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
@TableName("booking_slot") public class BookingSlot{
    @TableId(type=IdType.AUTO) public Long id;
    public Long resourceId;
    public Long bookingId;
    public LocalDateTime slotStart;
    public LocalDateTime releasedAt;
    public String releaseReason;
    public LocalDateTime createdAt=LocalDateTime.now();
}
