package com.lab.resource.service;

import com.lab.resource.ResourceClosure;
import com.lab.resource.controller.ClosureController.ResourceClosureRequest;
import jakarta.servlet.http.HttpServletRequest;

public interface ResourceClosureService {
    java.util.List<ResourceClosure> list(Long resourceId, HttpServletRequest servletRequest);
    ResourceClosure create(Long resourceId, ResourceClosureRequest request, HttpServletRequest servletRequest);
    ResourceClosure cancel(Long id, HttpServletRequest servletRequest);
}
