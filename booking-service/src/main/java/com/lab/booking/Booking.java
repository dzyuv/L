package com.lab.booking;
import com.baomidou.mybatisplus.annotation.*;
import java.time.*;
@TableName("booking")
public class Booking{
    @TableId(type=IdType.AUTO) public Long id;
    public String bookingNo;
    public Long userId;
    public Long resourceId;
    public String applicantNameSnapshot;
    public String resourceNameSnapshot;
    public LocalDateTime startTime;
    public LocalDateTime endTime;
    public int slotMinutesSnapshot;
    public String purpose;
    public int participants;
    public String status;
    public int approvalLevelSnapshot;
    public LocalDateTime approvalDeadline;
    public boolean needCheckinSnapshot;
    public String clientRequestId;
    public LocalDateTime checkinAt;
    public LocalDateTime completedAt;
    public LocalDateTime canceledAt;
    public String cancelReason;
    public String rejectReason;
    @Version public int version;
    public LocalDateTime createdAt=LocalDateTime.now();
    public LocalDateTime updatedAt=LocalDateTime.now();
}
