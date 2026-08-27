package com.lab.approval.controller;

import com.lab.approval.service.ApprovalFlowService;
import com.lab.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/admin/approval-flows")
public class AdminApprovalFlowController {
    private final ApprovalFlowService service;
    public AdminApprovalFlowController(ApprovalFlowService service){this.service=service;}
    public record NodeRequest(@Min(1) @Max(2) int level,@NotBlank String approverRole,@NotBlank String scopeType,String scopeValue,@NotBlank String approvalRule,Integer quorumCount,@Min(1) int deadlineMinutes){}
    public record FlowRequest(@NotNull Long resourceTypeId,@NotEmpty List<@Valid NodeRequest> nodes){}
    @GetMapping public ApiResponse<?> list(HttpServletRequest request){return ok(service.list(request),request);}
    @PostMapping public ApiResponse<?> create(@Valid @RequestBody FlowRequest body,HttpServletRequest request){return ok(service.create(body,request),request);}
    private <T> ApiResponse<T> ok(T data,HttpServletRequest request){return ApiResponse.success(data,Objects.toString(request.getAttribute("X-Request-Id"),""));}
}
