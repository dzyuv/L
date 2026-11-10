package com.lab.booking.approval.service;

import com.lab.booking.approval.entity.ApprovalTask;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface ApprovalTaskService {
    List<ApprovalTask> mine(HttpServletRequest request);

    ApprovalTask action(Long taskId, String action, String comment, HttpServletRequest request);
}
