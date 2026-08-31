package com.lab.resource.service.impl;

import com.lab.common.api.*;
import com.lab.common.exception.BusinessException;
import com.lab.resource.*;
import com.lab.resource.controller.ResourceController;
import com.lab.resource.service.ResourceManagementService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
public class ResourceManagementServiceImpl implements ResourceManagementService {
    private final ResourceRepository resources; private final ResourceTypeRepository types; private final ScheduleRepository schedules;
    private final ClosureRepository closures; private final ResourceManagerRepository managers; private final RoleGuard roleGuard; private final InternalServiceGuard internalServices;
    public ResourceManagementServiceImpl(ResourceRepository resources, ResourceTypeRepository types, ScheduleRepository schedules, ClosureRepository closures, ResourceManagerRepository managers, RoleGuard roleGuard, InternalServiceGuard internalServices) {
        this.resources=resources; this.types=types; this.schedules=schedules; this.closures=closures; this.managers=managers; this.roleGuard=roleGuard; this.internalServices=internalServices;
    }
    public List<Resource> list() { return resources.findAll().stream().filter(item -> !item.deleted && "ACTIVE".equals(item.status)).toList(); }
    public List<?> listPublicTypes() { return types.findAll().stream().filter(item -> !item.deleted && item.enabled).toList(); }
    public List<?> listTypes(HttpServletRequest servletRequest) { roleGuard.requireLabAdmin(servletRequest); return types.findAll().stream().filter(item -> !item.deleted).toList(); }
    public List<?> listSchedules(Long id, HttpServletRequest servletRequest) { roleGuard.requireLabAdmin(servletRequest); resource(id); return schedules.findByResourceIdOrderByWeekdayAscOpenTimeAsc(id); }
    public Resource get(Long id) { return resource(id); }
    public Object createType(ResourceController.TypeRequest request, HttpServletRequest servletRequest) { roleGuard.requireLabAdmin(servletRequest); ResourceType type=new ResourceType(); type.name=request.name(); type.defaultApprovalLevel=request.defaultApprovalLevel(); return types.save(type); }
    public Object updateType(Long id, ResourceController.TypeUpdateRequest request, HttpServletRequest servletRequest) {
        roleGuard.requireLabAdmin(servletRequest);
        ResourceType type=types.findById(id).filter(item -> !item.deleted).orElseThrow(() -> new BusinessException("NOT_FOUND", "Resource type does not exist", HttpStatus.NOT_FOUND));
        types.findAll().stream().filter(item -> !Objects.equals(item.id, id) && !item.deleted && item.name.equalsIgnoreCase(request.name())).findFirst().ifPresent(item -> { throw new BusinessException("TYPE_EXISTS", "Resource type already exists", HttpStatus.CONFLICT); });
        type.name=request.name(); type.defaultApprovalLevel=request.defaultApprovalLevel(); type.enabled=request.enabled();
        return types.save(type);
    }
    public void deleteType(Long id, HttpServletRequest servletRequest) {
        roleGuard.requireLabAdmin(servletRequest);
        ResourceType type=types.findById(id).filter(item -> !item.deleted).orElseThrow(() -> new BusinessException("NOT_FOUND", "Resource type does not exist", HttpStatus.NOT_FOUND));
        if (resources.countByTypeIdAndDeletedFalse(id) > 0) throw new BusinessException("TYPE_IN_USE", "Resource type is used by resources and cannot be deleted", HttpStatus.CONFLICT);
        type.deleted=true; type.enabled=false; types.save(type);
    }
    public Resource create(ResourceController.ResourceRequest request, HttpServletRequest servletRequest) { roleGuard.requireLabAdmin(servletRequest); requireType(request.typeId()); Resource resource=new Resource(); apply(resource,request); return resources.save(resource); }
    public Resource update(Long id, ResourceController.ResourceRequest request, HttpServletRequest servletRequest) { roleGuard.requireLabAdmin(servletRequest); requireType(request.typeId()); Resource resource=resource(id); apply(resource,request); return resources.save(resource); }
    @Transactional
    public List<?> schedule(Long id, List<ResourceController.ScheduleRequest> requests, HttpServletRequest servletRequest) {
        roleGuard.requireLabAdmin(servletRequest); resource(id); schedules.findAll().stream().filter(item -> Objects.equals(item.resourceId, id)).forEach(schedules::delete);
        List<ResourceSchedule> result=new ArrayList<>();
        Set<Integer> weekdays = new HashSet<>();
        for (ResourceController.ScheduleRequest request:requests) {
            if (!request.openTime().isBefore(request.closeTime())) throw new BusinessException("INVALID_SCHEDULE", "Open time must be before close time", HttpStatus.BAD_REQUEST);
            if (!weekdays.add(request.weekday())) throw new BusinessException("INVALID_SCHEDULE", "Duplicate weekday schedule", HttpStatus.BAD_REQUEST);
            long openMinutes = Duration.between(request.openTime(), request.closeTime()).toMinutes();
            if (openMinutes % request.slotMinutes() != 0 || request.maxDurationMinutes() > openMinutes) throw new BusinessException("INVALID_SCHEDULE", "Schedule duration is incompatible with slots", HttpStatus.BAD_REQUEST);
            ResourceSchedule schedule=new ResourceSchedule(); schedule.resourceId=id; schedule.weekday=request.weekday(); schedule.openTime=request.openTime(); schedule.closeTime=request.closeTime(); schedule.slotMinutes=request.slotMinutes(); schedule.maxDurationMinutes=request.maxDurationMinutes(); result.add(schedule);
        }
        return schedules.saveAll(result);
    }
    public Object addManager(Long id, ResourceController.ManagerRequest request, HttpServletRequest servletRequest) {
        roleGuard.requireLabAdmin(servletRequest); resource(id);
        boolean exists=managers.findByResourceIdOrderByIdAsc(id).stream()
                .anyMatch(item->"OWNER".equals(item.managerType)&&Objects.equals(item.userId, request.userId()));
        if (exists) throw new BusinessException("MANAGER_EXISTS", "该用户已经是此资源的负责人", HttpStatus.CONFLICT);
        ResourceManager manager=new ResourceManager(); manager.resourceId=id; manager.userId=request.userId(); manager.managerType="OWNER";
        manager.scopeType="RESOURCE"; manager.scopeValue="";
        return managers.save(manager);
    }
    public List<?> listManagers(Long id, HttpServletRequest servletRequest) {
        roleGuard.requireLabAdmin(servletRequest); resource(id);
        return managers.findByResourceIdOrderByIdAsc(id).stream()
                .filter(item->"OWNER".equals(item.managerType)||"APPROVER".equals(item.managerType)).toList();
    }
    public void removeManager(Long id, Long managerId, HttpServletRequest servletRequest) {
        roleGuard.requireLabAdmin(servletRequest); resource(id);
        ResourceManager manager=managers.findById(managerId).orElseThrow(() -> new BusinessException("NOT_FOUND", "Resource manager does not exist", HttpStatus.NOT_FOUND));
        if (!Objects.equals(manager.resourceId, id)) throw new BusinessException("NOT_FOUND", "Resource manager does not exist", HttpStatus.NOT_FOUND);
        managers.delete(manager);
    }
    public Map<String,Object> calendar(Long id, LocalDate start, LocalDate end) {
        if (start == null || end == null || start.isAfter(end)) throw new BusinessException("INVALID_DATE_RANGE", "Calendar start must not be after end", HttpStatus.BAD_REQUEST);
        Resource resource=resource(id); List<Map<String,Object>> days=new ArrayList<>();
        List<ResourceClosure> activeClosures=closures.findByResourceIdAndStatusNot(id,"CANCELED");
        for(LocalDate date=start;!date.isAfter(end);date=date.plusDays(1)) {
            LocalDateTime dayStart=date.atStartOfDay();
            LocalDateTime dayEnd=date.plusDays(1).atStartOfDay();
            List<Map<String,Object>> dayClosures=activeClosures.stream()
                .filter(item->item.startTime.isBefore(dayEnd)&&item.endTime.isAfter(dayStart))
                .map(item->{
                    Map<String,Object> result=new LinkedHashMap<>();
                    result.put("id",item.id);
                    result.put("startTime",item.startTime);
                    result.put("endTime",item.endTime);
                    result.put("reason",item.reason);
                    result.put("status",item.status);
                    return result;
                }).toList();
            Map<String,Object> day=new LinkedHashMap<>();
            day.put("date",date);
            day.put("open",schedules.findByResourceIdAndWeekdayAndEnabledTrue(id,date.getDayOfWeek().getValue()));
            day.put("closures",dayClosures);
            days.add(day);
        }
        return Map.of("resource",resource,"days",days,"calculatedUntil",Instant.now());
    }
    public ResourceController.BookingRule bookingRule(Long id, LocalDateTime startTime, LocalDateTime endTime, int participants, Long applicantUserId, HttpServletRequest servletRequest) {
        internalServices.require(servletRequest);
        if(!startTime.isBefore(endTime)||!startTime.toLocalDate().equals(endTime.toLocalDate())) throw new BusinessException("INVALID_TIME","Booking interval must be within one day",HttpStatus.BAD_REQUEST);
        if(!startTime.isAfter(LocalDateTime.now())) throw new BusinessException("INVALID_TIME","Booking start time must be in the future",HttpStatus.BAD_REQUEST);
        Resource resource=resource(id); if(resource.deleted||!"ACTIVE".equals(resource.status)) throw new BusinessException("RESOURCE_UNAVAILABLE","Resource is not available",HttpStatus.UNPROCESSABLE_ENTITY);
        ResourceType type=types.findById(resource.typeId).orElseThrow(()->new BusinessException("TYPE_NOT_FOUND","Resource type does not exist",HttpStatus.NOT_FOUND));
        if(type.deleted||!type.enabled) throw new BusinessException("RESOURCE_UNAVAILABLE","Resource type is not available",HttpStatus.UNPROCESSABLE_ENTITY);
        if(participants>resource.capacity) throw new BusinessException("CAPACITY_EXCEEDED","Participants exceed resource capacity",HttpStatus.BAD_REQUEST);
        ResourceSchedule schedule=schedules.findByResourceIdAndWeekdayAndEnabledTrue(id,startTime.getDayOfWeek().getValue()).stream().filter(item->effective(item,startTime.toLocalDate())).filter(item->contains(item,startTime.toLocalTime(),endTime.toLocalTime())).findFirst().orElseThrow(()->new BusinessException("OUTSIDE_OPEN_HOURS","Booking interval is outside open hours",HttpStatus.UNPROCESSABLE_ENTITY));
        long minutes=Duration.between(startTime,endTime).toMinutes();
        if(startTime.getSecond()!=0||startTime.getNano()!=0||endTime.getSecond()!=0||endTime.getNano()!=0||minutes%schedule.slotMinutes!=0||Duration.between(startTime.toLocalDate().atTime(schedule.openTime),startTime).toMinutes()%schedule.slotMinutes!=0||minutes>Math.min(resource.maxDurationMinutes,schedule.maxDurationMinutes)) throw new BusinessException("INVALID_TIME","Booking interval does not match resource rules",HttpStatus.BAD_REQUEST);
        if(closures.findByResourceIdAndStatusNot(id,"CANCELED").stream().anyMatch(item->item.startTime.isBefore(endTime)&&item.endTime.isAfter(startTime))) throw new BusinessException("RESOURCE_CLOSED","Resource is closed during this interval",HttpStatus.UNPROCESSABLE_ENTITY);
        int approvalLevel=resource.approvalLevelOverride!=null?resource.approvalLevelOverride:type.defaultApprovalLevel;
        Long approver=null;
        String approverRole=null;
        if (approvalLevel>0) {
            if (applicantUserId == null) {
                throw new BusinessException("INVALID_ARGUMENT", "Applicant is required to resolve the approver", HttpStatus.BAD_REQUEST);
            }
            List<ResourceManager> owners=managers.findByResourceIdOrderByIdAsc(id).stream()
                    .filter(item->"OWNER".equals(item.managerType)||"APPROVER".equals(item.managerType)).toList();
            if (approvalLevel >= 2) {
                approver=null;
                approverRole=Roles.TEACHER;
            } else {
                approver=owners.stream().map(item->item.userId).filter(userId->!Objects.equals(userId,applicantUserId)).findFirst().orElse(null);
                approverRole=approver==null?Roles.LAB_ADMIN:Roles.TEACHER;
            }
        }
        return new ResourceController.BookingRule(resource.name,type.id,resource.capacity,schedule.slotMinutes,Math.min(resource.maxDurationMinutes,schedule.maxDurationMinutes),resource.needCheckin,approvalLevel,approver,approverRole);
    }
    private Resource resource(Long id) { return resources.findById(id).filter(item -> !item.deleted).orElseThrow(()->new BusinessException("NOT_FOUND","Resource does not exist",HttpStatus.NOT_FOUND)); }
    private void requireType(Long id) { if(types.findById(id).filter(item -> !item.deleted && item.enabled).isEmpty()) throw new BusinessException("TYPE_NOT_FOUND","Resource type does not exist",HttpStatus.NOT_FOUND); }
    private void apply(Resource resource, ResourceController.ResourceRequest request) { resource.typeId=request.typeId(); resource.name=request.name(); resource.location=request.location(); resource.capacity=request.capacity(); resource.description=request.description(); resource.imageUrl=request.imageUrl()==null||request.imageUrl().isBlank()?null:request.imageUrl().trim(); resource.maxDurationMinutes=request.maxDurationMinutes(); resource.needCheckin=request.needCheckin(); resource.approvalLevelOverride=request.approvalLevelOverride(); }
    private boolean effective(ResourceSchedule schedule,LocalDate date) { return (schedule.effectiveFrom==null||!date.isBefore(schedule.effectiveFrom))&&(schedule.effectiveTo==null||!date.isAfter(schedule.effectiveTo)); }
    private boolean contains(ResourceSchedule schedule,LocalTime start,LocalTime end) { return !start.isBefore(schedule.openTime)&&!end.isAfter(schedule.closeTime); }
}
