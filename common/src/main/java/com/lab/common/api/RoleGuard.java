package com.lab.common.api;

import com.lab.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/** Authorization helper for endpoints that require an administrative role. */
@Component
public class RoleGuard {
    public void requireSystemAdmin(HttpServletRequest request){
        requireAny(request, "SYSTEM_ADMIN");
    }

    public void requireLabAdmin(HttpServletRequest request){
        requireAny(request, "LAB_ADMIN");
    }

    public void requireAny(HttpServletRequest request,String... acceptedRoles){
        Set<String> current=roles(request);
        for(String role:acceptedRoles) if(current.contains(role)) return;
        throw new BusinessException("FORBIDDEN","Required role is missing",HttpStatus.FORBIDDEN);
    }

    public boolean hasRole(HttpServletRequest request,String role){
        return roles(request).contains(role);
    }

    public Long currentUserId(HttpServletRequest request){
        if(request.getAttribute("userId") instanceof Long id) return id;
        throw new BusinessException("UNAUTHORIZED","Login required",HttpStatus.UNAUTHORIZED);
    }

    public Set<String> roles(HttpServletRequest request){
        Object value=request.getAttribute("roles");
        Set<String> result=new LinkedHashSet<>();
        if(value instanceof Collection<?> collection) collection.forEach(item->result.add(String.valueOf(item)));
        return result;
    }
}
