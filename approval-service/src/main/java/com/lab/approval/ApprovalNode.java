package com.lab.approval;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("approval_node")
public class ApprovalNode {
    @TableId(type=IdType.AUTO) public Long id;
    public Long flowId;
    public int level;
    public int sequenceNo = 1;
    public String approverRole;
    public String scopeType = "RESOURCE";
    public String scopeValue = "";
    public String approvalRule = "ANY_ONE";
    public Integer quorumCount;
    public int deadlineMinutes = 1440;
}
