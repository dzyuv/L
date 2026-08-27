package com.lab.statistics.service;

import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
public interface StatisticsService { Map<String, Object> usage(HttpServletRequest request); }
