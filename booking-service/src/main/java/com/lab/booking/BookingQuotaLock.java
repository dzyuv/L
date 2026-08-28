package com.lab.booking;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("booking_quota_lock")
public class BookingQuotaLock {
    @TableId(value="user_id", type=IdType.INPUT)
    public Long userId;
}
