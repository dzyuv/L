package com.lab.common.api;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Objects;
/** Validates the access token at every business service; identity headers are derived, never trusted. */
public class JwtUserFilter extends OncePerRequestFilter {
    private final JwtKeyProvider keys;

    public JwtUserFilter(JwtKeyProvider keys) {
        this.keys = keys;
    }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.endsWith("/user/login") || path.endsWith("/user/register") || path.endsWith("/user/token/refresh")
                || path.contains("/api/v1/internal/") || path.startsWith("/actuator/");
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            unauthorized(request, response, "请先登录后再操作");
            return;
        }
        try {
            Claims claims = Jwts.parser().verifyWith(keys.key()).build().parseSignedClaims(header.substring(7)).getPayload();
            if(!"access".equals(claims.get("tokenType",String.class))){
                unauthorized(request, response, "请先登录后再操作");
                return;
            }
            request.setAttribute("userId", Long.valueOf(claims.getSubject()));
            request.setAttribute("username", claims.get("username", String.class));
            request.setAttribute("realName", claims.get("realName", String.class));
            request.setAttribute("roles", claims.get("roles"));
            chain.doFilter(request, response);
        }
        catch (Exception ex) {
            unauthorized(request, response, "登录已失效，请重新登录");
        }
    }

    private void unauthorized(HttpServletRequest request, HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        String requestId = Objects.toString(request.getAttribute(RequestIdFilter.HEADER), request.getHeader(RequestIdFilter.HEADER));
        if (requestId == null) requestId = "";
        response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"" + escape(message)
                + "\",\"data\":null,\"requestId\":\"" + escape(requestId) + "\"}");
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
