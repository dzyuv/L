package com.lab.common.api;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
/** Validates the access token at every business service; identity headers are derived, never trusted. */
public class JwtUserFilter extends OncePerRequestFilter {
    private final JwtKeyProvider keys;

    public JwtUserFilter(JwtKeyProvider keys) {
        this.keys = keys;
    }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.endsWith("/user/login") || path.endsWith("/user/register") || path.endsWith("/user/token/refresh") || path.startsWith("/actuator/");
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            response.sendError(401, "缺少访问令牌");
            return;
        }
        try {
            Claims claims = Jwts.parser().verifyWith(keys.key()).build().parseSignedClaims(header.substring(7)).getPayload();
            request.setAttribute("userId", Long.valueOf(claims.getSubject()));
            request.setAttribute("username", claims.get("username", String.class));
            request.setAttribute("roles", claims.get("roles"));
            chain.doFilter(request, response);
        }
        catch (Exception ex) {
            response.sendError(401, "访问令牌无效或已过期");
        }
    }
}
