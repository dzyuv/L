package com.lab.approval;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="approval_flow", uniqueConstraints=@UniqueConstraint(columnNames={"resourceTypeId", "version"}))
public class ApprovalFlow {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    public Long resourceTypeId;
    public int version;
    public boolean enabled = true;
    public Long createdBy;
    public LocalDateTime createdAt = LocalDateTime.now();
}
