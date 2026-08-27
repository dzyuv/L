package com.lab.system.service.impl;

import com.lab.system.service.SystemConfigService;
import com.lab.system.SystemConfig;
import com.lab.system.SystemConfigRepository;
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
    public SystemConfigServiceImpl(SystemConfigRepository configs, RoleGuard roleGuard) { this.configs = configs; this.roleGuard = roleGuard; }
    public List<Map<String, String>> configs(HttpServletRequest request) {
        roleGuard.requireSystemAdmin(request);
        return configs.findAll().stream().map(item -> Map.of("key", item.configKey, "value", item.configValue, "type", Objects.toString(item.valueType, "STRING"))).toList();
    }
    public Map<String, String> update(String key, String value, HttpServletRequest request) {
        roleGuard.requireSystemAdmin(request);
        if (key == null || key.isBlank() || key.length() > 100 || value == null) throw new BusinessException("INVALID_ARGUMENT", "Config key and value are required", HttpStatus.BAD_REQUEST);
        SystemConfig item = configs.findByConfigKey(key).orElseGet(SystemConfig::new);
        item.configKey = key; item.configValue = value; item.updatedBy = request.getAttribute("userId") instanceof Long id ? id : 0L; item.updatedAt = java.time.LocalDateTime.now();
        configs.save(item);
        return Map.of("key", item.configKey, "value", item.configValue);
    }
}
