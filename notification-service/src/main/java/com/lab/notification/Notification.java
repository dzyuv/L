package com.lab.notification;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity @Table(name = "notification")
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public Long userId;
    public String type;
    public String title;
    public String content;
    public boolean isRead;
    public LocalDateTime createdAt = LocalDateTime.now();
    public LocalDateTime readAt;
}
