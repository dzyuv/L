package com.lab.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;

@TableName("refresh_token")
public class RefreshToken {
    @TableId(type=IdType.AUTO) public Long id;
    public String tokenHash;
    public Long userId;
    public int tokenVersion;
    public Instant expiresAt;
    public Instant revokedAt;
    public Instant createdAt=Instant.now();
}
