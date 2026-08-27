package com.lab.notification.service.impl;

import com.lab.notification.service.NotificationService;
import com.lab.notification.Notification;
import com.lab.notification.NotificationRepository;
import com.lab.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notifications;
    public NotificationServiceImpl(NotificationRepository notifications) { this.notifications = notifications; }
    public Map<String, Object> my(HttpServletRequest request) {
        Long userId = request.getAttribute("userId") instanceof Long value ? value : null;
        if (userId == null) return Map.of("items", List.of(), "page", 1, "size", 20, "total", 0);
        List<Notification> items = notifications.findByUserIdOrderByCreatedAtDesc(userId);
        return Map.of("items", items, "page", 1, "size", items.size(), "total", items.size());
    }
    public Map<String, Object> markRead(Long id, HttpServletRequest request) {
        Long userId = request.getAttribute("userId") instanceof Long value ? value : null;
        Notification item = notifications.findById(id).orElseThrow(() -> new BusinessException("NOT_FOUND", "Notification does not exist", HttpStatus.NOT_FOUND));
        if (userId == null || !Objects.equals(item.userId, userId)) throw new BusinessException("FORBIDDEN", "Notification does not belong to current user", HttpStatus.FORBIDDEN);
        item.isRead = true; item.readAt = java.time.LocalDateTime.now(); notifications.save(item);
        return Map.of("id", item.id, "isRead", true);
    }
}
