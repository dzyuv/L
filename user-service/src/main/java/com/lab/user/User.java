package com.lab.user;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
@Entity @Table(name="user") public class User {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(unique=true,nullable=false) public String employeeNo;
    @Column(unique=true,nullable=false) public String username;
    @Column(nullable=false) public String passwordHash;
    @Column(nullable=false) public String realName;
    public String email;
    public String phone;
    @Column(nullable=false) public String status="ACTIVE";
    public int failedLoginCount;
    public Instant lockedUntil;
    public Instant lastLoginAt;
    public int tokenVersion;
    @Version public int version;
    public boolean deleted;
    public Instant createdAt=Instant.now();
    public Instant updatedAt=Instant.now();
    @ManyToMany(fetch=FetchType.EAGER)
    @JoinTable(name="user_role", joinColumns=@JoinColumn(name="user_id"), inverseJoinColumns=@JoinColumn(name="role_id"))
    public Set<Role> roles=new LinkedHashSet<>();
}
