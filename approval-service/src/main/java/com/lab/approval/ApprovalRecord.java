package com.lab.approval;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "approval_record")
public class ApprovalRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public Long taskId;
    public Long bookingId;
    public Long approverId;
    public String result;
    public String comment;
    public String requestId;
    public LocalDateTime createdAt = LocalDateTime.now();
}
