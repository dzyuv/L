package com.lab.user;

import jakarta.persistence.*;

@Entity
@Table(name="permission")
public class Permission {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    public Long id;
    @Column(nullable=false,unique=true,length=100)
    public String code;
    @Column(nullable=false,length=100)
    public String name;
}
