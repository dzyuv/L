package com.lab.system.service.impl;

import com.lab.system.service.SystemConfigService;
import com.lab.system.SystemConfig;
import com.lab.system.SystemConfigRepository;
import com.lab.system.service.OperationLogService;
import com.lab.common.api.RoleGuard;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import com.lab.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import java.util.*;

@Service
public class SystemConfigServiceImpl implements SystemConfigService {
    private final SystemConfigRepository configs;
    private final RoleGuard roleGuard;
    private final OperationLogService operationLogs;
    public SystemConfigServiceImpl(SystemConfigRepository configs, RoleGuard roleGuard, OperationLogService operationLogs) {
        this.configs = configs; this.roleGuard = roleGuard; this.operationLogs = operationLogs;
    }
    public List<Map<String, String>> configs(HttpServletRequest request) {
        roleGuard.requireSystemAdmin(request);
        return configs.findAll().stream().map(item -> {
            Map<String, String> row = new LinkedHashMap<>();
            row.put("key", item.configKey);
            row.put("value", Objects.toString(item.configValue, ""));
            row.put("type", Objects.toString(item.valueType, "STRING"));
            row.put("description", Objects.toString(item.description, ""));
            return row;
        }).toList();
    }
    public Map<String, String> update(String key, String value, HttpServletRequest request) {
        roleGuard.requireSystemAdmin(request);
        if (key == null || key.isBlank() || key.length() > 100 || value == null) throw new BusinessException("INVALID_ARGUMENT", "Config key and value are required", HttpStatus.BAD_REQUEST);
        SystemConfig item = configs.findByConfigKey(key).orElseGet(SystemConfig::new);
        String normalized = value.trim();
        if ("INT".equalsIgnoreCase(item.valueType) || key.endsWith("_minutes") || key.endsWith("_days") || key.endsWith("_count") || key.endsWith("_duration")) {
            try {
                Integer.parseInt(normalized);
            } catch (NumberFormatException exception) {
                throw new BusinessException("INVALID_ARGUMENT", "该配置必须是整数", HttpStatus.BAD_REQUEST);
            }
        }
        item.configKey = key; item.configValue = normalized; item.updatedBy = request.getAttribute("userId") instanceof Long id ? id : 0L; item.updatedAt = java.time.LocalDateTime.now();
        configs.save(item);
        operationLogs.recordLocal(request, "SYSTEM_CONFIG_UPDATED", "SYSTEM_CONFIG", item.id,
                Map.of("key", item.configKey));
        return Map.of("key", item.configKey, "value", item.configValue);
    }

    public Map<String, String> allValues() {
        Map<String, String> values = new LinkedHashMap<>();
        for (SystemConfig item : configs.findAll()) {
            if (item.configKey != null) values.put(item.configKey, Objects.toString(item.configValue, ""));
        }
        return values;
    }

    public Map<String, Integer> runtime() {
        Map<String, String> values = allValues();
        Map<String, Integer> runtime = new LinkedHashMap<>();
        runtime.put("checkinBeforeMinutes", intValue(values, "checkin.window.before_minutes", 15));
        runtime.put("checkinAfterMinutes", intValue(values, "checkin.window.after_minutes", 30));
        runtime.put("approvalTimeoutMinutes", intValue(values, "approval.timeout_minutes", 1440));
        runtime.put("violationMaxCount", intValue(values, "violation.max_count", 3));
        runtime.put("violationRestrictionDays", intValue(values, "violation.restriction_days", 30));
        return runtime;
    }

    private int intValue(Map<String, String> values, String key, int fallback) {
        String raw = values.get(key);
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
