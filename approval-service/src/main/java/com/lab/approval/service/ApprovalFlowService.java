package com.lab.approval.service;

import com.lab.approval.controller.AdminApprovalFlowController;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

public interface ApprovalFlowService {
    Map<String, Object> list(HttpServletRequest request);
    Map<String, Object> create(AdminApprovalFlowController.FlowRequest body, HttpServletRequest request);
}
