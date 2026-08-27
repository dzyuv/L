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
import java.util.Objects;

@Service
public class ResourceClosureServiceImpl implements ResourceClosureService {
    private final ResourceRepository resources; private final ClosureRepository closures; private final RoleGuard roleGuard;
    public ResourceClosureServiceImpl(ResourceRepository resources, ClosureRepository closures, RoleGuard roleGuard) { this.resources=resources; this.closures=closures; this.roleGuard=roleGuard; }
    public ResourceClosure create(Long resourceId, ResourceClosureRequest request, HttpServletRequest servletRequest) {
        roleGuard.requireAdmin(servletRequest);
        if(!resources.existsById(resourceId)) throw new BusinessException("NOT_FOUND","Resource does not exist",HttpStatus.NOT_FOUND);
        ResourceClosure closure=new ResourceClosure(); closure.resourceId=resourceId; closure.startTime=LocalDateTime.parse(request.startTime()); closure.endTime=LocalDateTime.parse(request.endTime());
        if(!closure.startTime.isBefore(closure.endTime)) throw new BusinessException("INVALID_CLOSURE","Closure interval is invalid",HttpStatus.BAD_REQUEST);
        closure.reason=Objects.toString(request.reason(),"Maintenance"); return closures.save(closure);
    }
    public ResourceClosure cancel(Long id, HttpServletRequest servletRequest) { roleGuard.requireAdmin(servletRequest); ResourceClosure closure=closures.findById(id).orElseThrow(()->new BusinessException("NOT_FOUND","Closure does not exist",HttpStatus.NOT_FOUND)); closure.status="CANCELED"; return closures.save(closure); }
}
