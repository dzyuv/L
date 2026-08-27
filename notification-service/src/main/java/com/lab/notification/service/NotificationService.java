package com.lab.notification.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
public interface NotificationService {
    Map<String, Object> my(HttpServletRequest request);
    Map<String, Object> markRead(Long id, HttpServletRequest request);
}
