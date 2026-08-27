package com.lab.user;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="refresh_token")
public class RefreshToken {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(nullable=false,unique=true,length=128) public String tokenHash;
    @Column(nullable=false) public Long userId;
    public int tokenVersion;
    @Column(nullable=false) public Instant expiresAt;
    public Instant revokedAt;
    public Instant createdAt=Instant.now();
}
