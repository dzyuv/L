package com.lab.approval.service.impl;

import com.lab.approval.*;
import com.lab.approval.controller.AdminApprovalFlowController;
import com.lab.approval.service.ApprovalFlowService;
import com.lab.common.api.AdminOperationLogger;
import com.lab.common.api.RoleGuard;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ApprovalFlowServiceImpl implements ApprovalFlowService {
    private final ApprovalFlowRepository flows;
    private final ApprovalNodeRepository nodes;
    private final RoleGuard roleGuard;
    private final AdminOperationLogger operationLogger;
    public ApprovalFlowServiceImpl(ApprovalFlowRepository flows, ApprovalNodeRepository nodes, RoleGuard roleGuard,
                                   AdminOperationLogger operationLogger) {
        this.flows=flows; this.nodes=nodes; this.roleGuard=roleGuard; this.operationLogger=operationLogger;
    }
    public Map<String, Object> list(HttpServletRequest request) {
        roleGuard.requireSystemAdmin(request);
        List<Map<String, Object>> items=flows.findAllByOrderByResourceTypeIdAscVersionDesc().stream().map(this::view).toList();
        return Map.of("items",items,"total",items.size());
    }
    @Transactional
    public Map<String, Object> create(AdminApprovalFlowController.FlowRequest body,HttpServletRequest request) {
        roleGuard.requireSystemAdmin(request);
        List<ApprovalFlow> history=flows.findByResourceTypeIdOrderByVersionDesc(body.resourceTypeId());
        history.stream().filter(item->item.enabled).forEach(item->{item.enabled=false;flows.save(item);});
        ApprovalFlow flow=new ApprovalFlow(); flow.resourceTypeId=body.resourceTypeId(); flow.version=history.isEmpty()?1:history.get(0).version+1; flow.createdBy=roleGuard.currentUserId(request); flow=flows.save(flow);
        for(int i=0;i<body.nodes().size();i++) { AdminApprovalFlowController.NodeRequest source=body.nodes().get(i); ApprovalNode node=new ApprovalNode(); node.flowId=flow.id; node.level=source.level(); node.sequenceNo=i+1; node.approverRole=source.approverRole(); node.scopeType=source.scopeType(); node.scopeValue=source.scopeValue(); node.approvalRule=source.approvalRule(); node.quorumCount=source.quorumCount(); node.deadlineMinutes=source.deadlineMinutes(); nodes.save(node); }
        operationLogger.success(request, "APPROVAL_FLOW_PUBLISHED", "RESOURCE_TYPE", body.resourceTypeId(),
                Map.of("flowId", flow.id, "version", flow.version, "nodeCount", body.nodes().size()));
        return view(flow);
    }
    private Map<String,Object> view(ApprovalFlow flow) { Map<String,Object> result=new LinkedHashMap<>(); result.put("id",flow.id); result.put("resourceTypeId",flow.resourceTypeId); result.put("version",flow.version); result.put("enabled",flow.enabled); result.put("createdBy",flow.createdBy); result.put("createdAt",flow.createdAt); result.put("nodes",nodes.findByFlowIdOrderByLevelAscSequenceNoAsc(flow.id)); return result; }
}
