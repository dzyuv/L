package com.lab.resource.service;

import com.lab.resource.ResourceClosure;
import com.lab.resource.controller.ClosureController.ResourceClosureRequest;
import jakarta.servlet.http.HttpServletRequest;

public interface ResourceClosureService {
    ResourceClosure create(Long resourceId, ResourceClosureRequest request, HttpServletRequest servletRequest);
    ResourceClosure cancel(Long id, HttpServletRequest servletRequest);
}
