package com.lab.user;

import jakarta.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name="role")
public class Role {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    public Long id;
    @Column(nullable=false,unique=true,length=30)
    public String code;
    @Column(nullable=false,length=50)
    public String name;
    @Column(nullable=false,length=20)
    public String status="ACTIVE";
    @ManyToMany(fetch=FetchType.EAGER)
    @JoinTable(name="role_permission", joinColumns=@JoinColumn(name="role_id"), inverseJoinColumns=@JoinColumn(name="permission_id"))
    public Set<Permission> permissions=new LinkedHashSet<>();
}
