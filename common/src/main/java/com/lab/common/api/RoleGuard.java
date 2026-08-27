package com.lab.common.api;

import com.lab.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import java.util.Collection;
import java.util.Set;

/** Authorization helper for endpoints that require an administrative role. */
@Component
public class RoleGuard {
    private static final Set<String> ADMIN_ROLES=Set.of("LAB_ADMIN","SYSTEM_ADMIN");

    public void requireAdmin(HttpServletRequest request){
        Object roles=request.getAttribute("roles");
        if(roles instanceof Collection<?> collection && collection.stream().map(String::valueOf).anyMatch(ADMIN_ROLES::contains)){
            return;
        }
        throw new BusinessException("FORBIDDEN","Administrative role is required",HttpStatus.FORBIDDEN);
    }
}
