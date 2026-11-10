package com.lab.user.security;
import com.lab.user.entity.User;
import com.lab.user.repository.UserRepository;

import com.lab.common.api.TokenVersionResolver;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/** Uses the local user table instead of calling user-service over HTTP. */
@Component
@Primary
public class LocalTokenVersionResolver implements TokenVersionResolver {
    private final UserRepository users;

    public LocalTokenVersionResolver(UserRepository users) {
        this.users = users;
    }

    @Override
    public Integer currentVersion(Long userId) {
        User user = users.selectById(userId);
        if (user == null || user.deleted || !"ACTIVE".equals(user.status)) return -1;
        return user.tokenVersion;
    }
}
