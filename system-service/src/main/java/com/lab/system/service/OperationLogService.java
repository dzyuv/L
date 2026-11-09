package com.lab.system.service;

import com.lab.common.api.AdminOperationLogger;
import com.lab.common.api.PageQuery;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;

public interface OperationLogService {
    Map<String, Object> list(String operationType, String result, String keyword, PageQuery pageQuery, HttpServletRequest request);
    List<String> operationTypes(HttpServletRequest request);
    Object record(AdminOperationLogger.OperationLogRequest body, HttpServletRequest request);
    void recordLocal(HttpServletRequest request, String operationType, String targetType,
                     Long targetId, Map<String, Object> detail);
}
