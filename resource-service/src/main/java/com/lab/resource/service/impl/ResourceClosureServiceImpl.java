package com.lab.resource.service.impl;

import com.lab.common.api.RoleGuard;
import com.lab.common.exception.BusinessException;
import com.lab.resource.*;
import com.lab.resource.controller.ClosureController.ResourceClosureRequest;
import com.lab.resource.service.ResourceClosureService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.List;

@Service
public class ResourceClosureServiceImpl implements ResourceClosureService {
    private final ResourceRepository resources; private final ClosureRepository closures; private final RoleGuard roleGuard;
    private final BookingClosureClient bookingClosures;
    public ResourceClosureServiceImpl(ResourceRepository resources, ClosureRepository closures, RoleGuard roleGuard,
                                      BookingClosureClient bookingClosures) {
        this.resources=resources; this.closures=closures; this.roleGuard=roleGuard; this.bookingClosures=bookingClosures;
    }
    public List<ResourceClosure> list(Long resourceId, HttpServletRequest servletRequest) { roleGuard.requireLabAdmin(servletRequest); if(!resources.existsById(resourceId)) throw new BusinessException("NOT_FOUND","Resource does not exist",HttpStatus.NOT_FOUND); return closures.findByResourceIdAndStatusNot(resourceId, "CANCELED"); }
    public Map<String, Object> create(Long resourceId, ResourceClosureRequest request, HttpServletRequest servletRequest) {
        roleGuard.requireLabAdmin(servletRequest);
        if(!resources.existsById(resourceId)) throw new BusinessException("NOT_FOUND","Resource does not exist",HttpStatus.NOT_FOUND);
        ResourceClosure closure=new ResourceClosure();
        try {
            closure.resourceId=resourceId; closure.startTime=LocalDateTime.parse(request.startTime()); closure.endTime=LocalDateTime.parse(request.endTime());
        } catch (DateTimeParseException | NullPointerException exception) {
            throw new BusinessException("INVALID_CLOSURE", "Closure times must be valid ISO local date-times", HttpStatus.BAD_REQUEST);
        }
        if(!closure.startTime.isBefore(closure.endTime)) throw new BusinessException("INVALID_CLOSURE","Closure interval is invalid",HttpStatus.BAD_REQUEST);
        closure.reason=Objects.toString(request.reason(),"Maintenance");
        ResourceClosure saved=closures.save(closure);
        int cancelled;
        try {
            cancelled=bookingClosures.cancelOverlapping(resourceId, saved.startTime, saved.endTime, saved.reason);
        } catch (RuntimeException exception) {
            try {
                closures.deleteById(saved.id);
            } catch (RuntimeException suppressed) {
                exception.addSuppressed(suppressed);
            }
            throw exception;
        }
        Map<String, Object> result=new LinkedHashMap<>();
        result.put("closure", saved);
        result.put("cancelledCount", cancelled);
        return result;
    }
    public Map<String, Object> cancel(Long id, HttpServletRequest servletRequest) {
        roleGuard.requireLabAdmin(servletRequest);
        ResourceClosure closure=closures.findById(id).orElseThrow(()->new BusinessException("NOT_FOUND","Closure does not exist",HttpStatus.NOT_FOUND));
        int restored=bookingClosures.restoreOverlapping(closure.resourceId, closure.startTime, closure.endTime);
        closure.status="CANCELED";
        ResourceClosure saved=closures.save(closure);
        Map<String, Object> result=new LinkedHashMap<>();
        result.put("closure", saved);
        result.put("restoredCount", restored);
        return result;
    }
}
