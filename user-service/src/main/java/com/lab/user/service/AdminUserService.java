package com.lab.user.service;

import com.lab.common.api.PageQuery;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface AdminUserService {
    Map<String, Object> list(String query, String status, PageQuery pageQuery, HttpServletRequest request);
    Map<String, Object> stats(HttpServletRequest request);
    List<Map<String, Object>> directory(HttpServletRequest request);
    Map<String, Object> teachers(HttpServletRequest request);
    Map<String, Object> roles(HttpServletRequest request);
    Map<String, Object> updateStatus(Long userId, String status, HttpServletRequest request);
    Map<String, Object> updateRoles(Long userId, Set<String> roleCodes, HttpServletRequest request);
    Map<String, Object> importUsers(String csv, HttpServletRequest request);
    Map<String, Object> resetPassword(Long userId, String password, HttpServletRequest request);
    Map<String, Object> delete(Long userId, HttpServletRequest request);
}
