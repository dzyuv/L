package com.lab.resource.service;

import com.lab.resource.Resource;
import com.lab.resource.controller.ResourceController;
import jakarta.servlet.http.HttpServletRequest;
import java.time.*;
import java.util.*;

public interface ResourceManagementService {
    List<Resource> list();
    List<?> listTypes(HttpServletRequest servletRequest);
    List<?> listSchedules(Long id, HttpServletRequest servletRequest);
    Resource get(Long id);
    Object createType(ResourceController.TypeRequest request, HttpServletRequest servletRequest);
    Resource create(ResourceController.ResourceRequest request, HttpServletRequest servletRequest);
    Resource update(Long id, ResourceController.ResourceRequest request, HttpServletRequest servletRequest);
    List<?> schedule(Long id, List<ResourceController.ScheduleRequest> requests, HttpServletRequest servletRequest);
    Object addManager(Long id, ResourceController.ManagerRequest request, HttpServletRequest servletRequest);
    List<?> listManagers(Long id, HttpServletRequest servletRequest);
    void removeManager(Long id, Long managerId, HttpServletRequest servletRequest);
    Map<String, Object> calendar(Long id, LocalDate start, LocalDate end);
    ResourceController.BookingRule bookingRule(Long id, LocalDateTime startTime, LocalDateTime endTime, int participants, HttpServletRequest servletRequest);
}
