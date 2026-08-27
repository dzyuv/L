package com.lab.approval.service;

import com.lab.approval.ApprovalTask;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

public interface ApprovalTaskService {
    List<ApprovalTask> mine(HttpServletRequest request);
    ApprovalTask action(Long taskId, String action, String comment, HttpServletRequest request);
}
