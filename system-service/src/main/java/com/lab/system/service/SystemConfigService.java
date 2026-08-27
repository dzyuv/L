package com.lab.system.service;

import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
public interface SystemConfigService {
    List<Map<String, String>> configs(HttpServletRequest request);
    Map<String, String> update(String key, String value, HttpServletRequest request);
}
