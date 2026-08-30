package com.lab.user.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Set;

public interface AdminUserService {
    Map<String, Object> list(String query, String status, HttpServletRequest request);
    Map<String, Object> teachers(HttpServletRequest request);
    Map<String, Object> roles(HttpServletRequest request);
    Map<String, Object> updateStatus(Long userId, String status, HttpServletRequest request);
    Map<String, Object> updateRoles(Long userId, Set<String> roleCodes, HttpServletRequest request);
    Map<String, Object> resetPassword(Long userId, String password, HttpServletRequest request);
}
