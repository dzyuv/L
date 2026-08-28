package com.lab.system.service;

import com.lab.common.api.AdminOperationLogger;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface OperationLogService {
    Map<String, Object> list(String operationType, String result, String keyword, HttpServletRequest request);
    Object record(AdminOperationLogger.OperationLogRequest body, HttpServletRequest request);
    void recordLocal(HttpServletRequest request, String operationType, String targetType,
                     Long targetId, Map<String, Object> detail);
}
