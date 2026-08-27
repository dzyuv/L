package com.lab.resource;
import jakarta.persistence.*;
@Entity public class ResourceType{
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(nullable=false,unique=true) public String name;
    public int defaultApprovalLevel=1;
    public boolean defaultNeedCheckin=true;
    public boolean enabled=true;
    @Version public int version;
    public boolean deleted;
}
