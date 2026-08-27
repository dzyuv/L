package com.lab.resource;
import jakarta.persistence.*;
@Entity @Table(name="resource",uniqueConstraints=@UniqueConstraint(columnNames={
    "typeId","name"
}
)) public class Resource{
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    public Long typeId;
    @Column(nullable=false) public String name;
    public String location;
    public int capacity;
    public String status="ACTIVE";
    public String description;
    public Long ownerUserId;
    public String imageUrl;
    public boolean needCheckin=true;
    public int maxDurationMinutes=120;
    public int slotMinutes=30;
    public Boolean approvalRequiredOverride;
    public Integer approvalLevelOverride;
    @Version public int version;
    public boolean deleted;
}
