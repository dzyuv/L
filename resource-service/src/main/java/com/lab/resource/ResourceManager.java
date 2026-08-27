package com.lab.resource;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(uniqueConstraints=@UniqueConstraint(columnNames={"resourceId","userId","managerType"}))
public class ResourceManager {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    public Long resourceId;
    public Long userId;
    public String managerType="APPROVER";
    public String scopeType="RESOURCE";
    public String scopeValue="";
    public LocalDateTime createdAt=LocalDateTime.now();
}
