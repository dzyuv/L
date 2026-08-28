package com.lab.system.service.impl;

import com.lab.common.api.AdminOperationLogger;
import com.lab.common.api.InternalServiceGuard;
import com.lab.common.api.RequestIdFilter;
import com.lab.common.api.RoleGuard;
import com.lab.common.exception.BusinessException;
import com.lab.system.OperationLog;
import com.lab.system.OperationLogRepository;
import com.lab.system.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class OperationLogServiceImpl implements OperationLogService {
    private static final int MAX_RESULT_SIZE = 500;
    private final OperationLogRepository logs;
    private final RoleGuard roleGuard;
    private final InternalServiceGuard internalServices;

    public OperationLogServiceImpl(OperationLogRepository logs, RoleGuard roleGuard,
                                   InternalServiceGuard internalServices) {
        this.logs = logs;
        this.roleGuard = roleGuard;
        this.internalServices = internalServices;
    }

    @Override
    public Map<String, Object> list(String operationType, String result, String keyword,
                                    HttpServletRequest request) {
        roleGuard.requireSystemAdmin(request);
        String term = Objects.toString(keyword, "").trim().toLowerCase();
        List<Map<String, Object>> items = logs.findAllByOrderByCreatedAtDesc().stream()
                .filter(item -> operationType == null || operationType.isBlank() || operationType.equals(item.operationType))
                .filter(item -> result == null || result.isBlank() || result.equals(item.result))
                .filter(item -> term.isBlank() || searchable(item).contains(term))
                .limit(MAX_RESULT_SIZE)
                .map(this::view)
                .toList();
        return Map.of("items", items, "total", items.size());
    }

    @Override
    public OperationLog record(AdminOperationLogger.OperationLogRequest body, HttpServletRequest request) {
        internalServices.require(request);
        if (body == null || body.operatorId() == null || body.operationType() == null
                || body.operationType().isBlank() || body.operationType().length() > 50) {
            throw new BusinessException("INVALID_OPERATION_LOG", "Operation log fields are invalid", HttpStatus.BAD_REQUEST);
        }
        OperationLog item = new OperationLog();
        item.operatorId = body.operatorId();
        item.operationType = body.operationType();
        item.targetType = truncate(body.targetType(), 50);
        item.targetId = body.targetId();
        item.result = body.result() == null || body.result().isBlank() ? "SUCCESS" : truncate(body.result(), 20);
        item.reason = truncate(body.reason(), 500);
        item.requestId = truncate(body.requestId(), 64);
        item.ip = truncate(body.ip(), 50);
        item.detail = body.detail() == null ? Map.of() : body.detail();
        return logs.save(item);
    }

    @Override
    public void recordLocal(HttpServletRequest request, String operationType, String targetType,
                            Long targetId, Map<String, Object> detail) {
        OperationLog item = new OperationLog();
        item.operatorId = roleGuard.currentUserId(request);
        item.operationType = operationType;
        item.targetType = targetType;
        item.targetId = targetId;
        item.requestId = truncate(Objects.toString(request.getAttribute(RequestIdFilter.HEADER), ""), 64);
        item.ip = truncate(clientIp(request), 50);
        item.detail = detail == null ? Map.of() : detail;
        logs.save(item);
    }

    private String searchable(OperationLog item) {
        return (Objects.toString(item.operationType, "") + " "
                + Objects.toString(item.targetType, "") + " "
                + Objects.toString(item.targetId, "") + " "
                + Objects.toString(item.operatorId, "") + " "
                + Objects.toString(item.requestId, "") + " "
                + Objects.toString(item.reason, "") + " "
                + Objects.toString(item.detail, "")).toLowerCase();
    }

    private Map<String, Object> view(OperationLog item) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", item.id);
        result.put("operatorId", item.operatorId);
        result.put("operationType", item.operationType);
        result.put("targetType", item.targetType);
        result.put("targetId", item.targetId);
        result.put("result", item.result);
        result.put("reason", item.reason);
        result.put("requestId", item.requestId);
        result.put("ip", item.ip);
        result.put("detail", item.detail == null ? Map.of() : item.detail);
        result.put("createdAt", item.createdAt);
        return result;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded == null || forwarded.isBlank() ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
    }

    private String truncate(String value, int length) {
        if (value == null) return null;
        return value.length() <= length ? value : value.substring(0, length);
    }
}
