package com.lab.system.service;
import com.lab.system.entity.*;
import com.lab.system.repository.*;
import com.lab.system.seed.*;
import com.lab.system.scheduler.*;
import com.lab.system.config.*;
import com.lab.system.controller.*;
import com.lab.system.service.impl.*;

import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
public interface SystemConfigService {
    List<Map<String, String>> configs(HttpServletRequest request);
    Map<String, String> update(String key, String value, HttpServletRequest request);
    Map<String, String> allValues();
    Map<String, Integer> runtime();
}
