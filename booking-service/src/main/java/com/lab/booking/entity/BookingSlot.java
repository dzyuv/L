package com.lab.booking.entity;
import com.lab.booking.repository.*;
import com.lab.booking.client.*;
import com.lab.booking.scheduler.*;
import com.lab.booking.config.*;
import com.lab.booking.service.*;
import com.lab.booking.service.impl.*;
import com.lab.booking.controller.*;
import com.lab.booking.approval.entity.*;
import com.lab.booking.approval.repository.*;
import com.lab.booking.approval.service.*;
import com.lab.booking.approval.service.impl.*;
import com.lab.booking.approval.controller.*;
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
