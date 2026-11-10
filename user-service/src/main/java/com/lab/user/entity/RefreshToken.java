package com.lab.user.entity;
import com.lab.user.repository.*;
import com.lab.user.client.*;
import com.lab.user.seed.*;
import com.lab.user.scheduler.*;
import com.lab.user.security.*;
import com.lab.user.config.*;
import com.lab.user.controller.*;
import com.lab.user.service.*;
import com.lab.user.service.impl.*;

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
