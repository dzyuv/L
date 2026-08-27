package com.lab.resource.controller;

import com.lab.resource.Resource;
import com.lab.resource.service.ResourceManagementService;
import com.lab.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1")
public class ResourceController {
    private final ResourceManagementService service;
    public ResourceController(ResourceManagementService service) { this.service = service; }
    public record TypeRequest(@NotBlank @Size(max = 100) String name, @Min(0) int defaultApprovalLevel) {}
    public record ResourceRequest(@NotNull Long typeId, @NotBlank String name, @NotBlank String location, @Min(1) int capacity, String description, @Min(1) int maxDurationMinutes, boolean needCheckin) {}
    public record ScheduleRequest(@Min(1) @Max(7) int weekday, @NotNull LocalTime openTime, @NotNull LocalTime closeTime, @Min(5) int slotMinutes, @Min(1) int maxDurationMinutes) {}
    public record ManagerRequest(@NotNull Long userId, @NotBlank @Size(max = 30) String managerType) {}
    public record BookingRule(String resourceName, int capacity, int slotMinutes, int maxDurationMinutes, boolean needCheckin, int approvalLevel, Long approverUserId) {}
    @GetMapping("/resources") public ApiResponse<?> list(HttpServletRequest request) { return ok(service.list(), request); }
    @GetMapping("/admin/resource-types") public ApiResponse<?> listTypes(HttpServletRequest request) { return ok(service.listTypes(request), request); }
    @GetMapping("/admin/resources/{id}/schedules") public ApiResponse<?> listSchedules(@PathVariable("id") Long id, HttpServletRequest request) { return ok(service.listSchedules(id, request), request); }
    @GetMapping("/resources/{id}") public ApiResponse<?> get(@PathVariable("id") Long id, HttpServletRequest request) { return ok(service.get(id), request); }
    @PostMapping("/admin/resource-types") public ApiResponse<?> createType(@Valid @RequestBody TypeRequest body, HttpServletRequest request) { return ok(service.createType(body, request), request); }
    @PostMapping("/admin/resources") public ApiResponse<?> create(@Valid @RequestBody ResourceRequest body, HttpServletRequest request) { return ok(service.create(body, request), request); }
    @PutMapping("/admin/resources/{id}") public ApiResponse<?> update(@PathVariable("id") Long id, @Valid @RequestBody ResourceRequest body, HttpServletRequest request) { return ok(service.update(id, body, request), request); }
    @PutMapping("/admin/resources/{id}/schedules") public ApiResponse<?> schedule(@PathVariable("id") Long id, @Valid @RequestBody List<ScheduleRequest> body, HttpServletRequest request) { return ok(service.schedule(id, body, request), request); }
    @PostMapping("/admin/resources/{id}/managers") public ApiResponse<?> addManager(@PathVariable("id") Long id, @Valid @RequestBody ManagerRequest body, HttpServletRequest request) { return ok(service.addManager(id, body, request), request); }
    @GetMapping("/resources/{id}/calendar") public ApiResponse<?> calendar(@PathVariable("id") Long id, @RequestParam("start") LocalDate start, @RequestParam("end") LocalDate end, HttpServletRequest request) { return ok(service.calendar(id, start, end), request); }
    @GetMapping("/internal/resources/{id}/booking-rule") public ApiResponse<BookingRule> bookingRule(@PathVariable("id") Long id, @RequestParam("startTime") LocalDateTime startTime, @RequestParam("endTime") LocalDateTime endTime, @RequestParam("participants") @Min(1) int participants, HttpServletRequest request) { return ok(service.bookingRule(id, startTime, endTime, participants, request), request); }
    private <T> ApiResponse<T> ok(T data, HttpServletRequest request) { return ApiResponse.success(data, Objects.toString(request.getAttribute("X-Request-Id"), "")); }
}
