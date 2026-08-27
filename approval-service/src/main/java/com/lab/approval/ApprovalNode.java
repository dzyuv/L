package com.lab.approval;

import jakarta.persistence.*;

@Entity
@Table(name="approval_node", uniqueConstraints=@UniqueConstraint(columnNames={"flowId", "level", "sequenceNo"}))
public class ApprovalNode {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
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
