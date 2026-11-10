package com.lab.booking.approval.service;
import com.lab.booking.entity.*;
import com.lab.booking.repository.*;
import com.lab.booking.client.*;
import com.lab.booking.scheduler.*;
import com.lab.booking.config.*;
import com.lab.booking.service.*;
import com.lab.booking.service.impl.*;
import com.lab.booking.controller.*;
import com.lab.booking.approval.entity.*;
import com.lab.booking.approval.repository.*;
import com.lab.booking.approval.service.impl.*;
import com.lab.booking.approval.controller.*;

import com.lab.booking.approval.entity.ApprovalTask;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface ApprovalTaskService {
    List<ApprovalTask> mine(HttpServletRequest request);

    ApprovalTask action(Long taskId, String action, String comment, HttpServletRequest request);
}
