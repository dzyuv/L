package com.lab.resource.service;
import com.lab.resource.entity.*;
import com.lab.resource.repository.*;
import com.lab.resource.client.*;
import com.lab.resource.seed.*;
import com.lab.resource.controller.*;
import com.lab.resource.service.impl.*;

import com.lab.resource.entity.ResourceClosure;
import com.lab.resource.controller.ClosureController.ResourceClosureRequest;
import jakarta.servlet.http.HttpServletRequest;

public interface ResourceClosureService {
    java.util.List<ResourceClosure> list(Long resourceId, HttpServletRequest servletRequest);
    java.util.Map<String, Object> create(Long resourceId, ResourceClosureRequest request, HttpServletRequest servletRequest);
    java.util.Map<String, Object> cancel(Long id, HttpServletRequest servletRequest);
}
