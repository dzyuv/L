package com.lab.notification;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("notification")
public class Notification {
    @TableId(type=IdType.AUTO) public Long id;
    public Long userId;
    public String type;
    public String title;
    public String content;
    public boolean isRead;
    public LocalDateTime createdAt = LocalDateTime.now();
    public LocalDateTime readAt;
}
