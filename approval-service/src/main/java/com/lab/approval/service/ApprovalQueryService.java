package com.lab.approval.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

public interface ApprovalQueryService {
    Map<String, Object> pending(HttpServletRequest request);
}
