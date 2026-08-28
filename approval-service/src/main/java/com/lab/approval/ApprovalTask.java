package com.lab.approval;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
@TableName("approval_task") public class ApprovalTask{
    @TableId(type=IdType.AUTO) public Long id;
    public Long bookingId;
    public Long applicantUserId;
    public String applicantName;
    public Long resourceId;
    public String resourceName;
    public LocalDateTime startTime;
    public LocalDateTime endTime;
    public int level;
    public String approverRole;
    public Long assignedUserId;
    public String status="PENDING";
    public LocalDateTime deadline;
    public LocalDateTime completedAt;
    public String comment;
    @Version public int version;
    public LocalDateTime createdAt=LocalDateTime.now();
}
