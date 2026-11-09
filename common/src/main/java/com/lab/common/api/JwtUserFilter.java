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
    private final TokenVersionResolver tokenVersions;

    public JwtUserFilter(JwtKeyProvider keys, TokenVersionResolver tokenVersions) {
        this.keys = keys;
        this.tokenVersions = tokenVersions;
    }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.endsWith("/user/login") || path.endsWith("/user/register") || path.endsWith("/user/token/refresh") || path.endsWith("/user/logout")
                || path.contains("/api/v1/internal/") || path.startsWith("/actuator/");
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            unauthorized(request, response, "Authentication required");
            return;
        }
        try {
            Claims claims = Jwts.parser().verifyWith(keys.key()).build().parseSignedClaims(header.substring(7)).getPayload();
            if(!"access".equals(claims.get("tokenType",String.class))){
                unauthorized(request, response, "Authentication required");
                return;
            }
            Long userId = Long.valueOf(claims.getSubject());
            Object rawVersion = claims.get("ver");
            if (!(rawVersion instanceof Number number)) {
                unauthorized(request, response, "Session expired; please sign in again");
                return;
            }
            Integer currentVersion = tokenVersions.currentVersion(userId);
            if (currentVersion == null) {
                serviceUnavailable(request, response, "Unable to verify the session; please retry later");
                return;
            }
            if (currentVersion.intValue() != number.intValue()) {
                unauthorized(request, response, "Session expired; please sign in again");
                return;
            }
            request.setAttribute("userId", userId);
            request.setAttribute("username", claims.get("username", String.class));
            request.setAttribute("realName", claims.get("realName", String.class));
            request.setAttribute("roles", claims.get("roles"));
        }
        catch (Exception ex) {
            unauthorized(request, response, "Session expired; please sign in again");
            return;
        }
        chain.doFilter(request, response);
    }

    private void unauthorized(HttpServletRequest request, HttpServletResponse response, String message) throws IOException {
        write(request, response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED", message);
    }

    private void serviceUnavailable(HttpServletRequest request, HttpServletResponse response, String message) throws IOException {
        write(request, response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", message);
    }

    private void write(HttpServletRequest request, HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        String requestId = Objects.toString(request.getAttribute(RequestIdFilter.HEADER), request.getHeader(RequestIdFilter.HEADER));
        if (requestId == null) requestId = "";
        response.getWriter().write("{\"code\":\"" + escape(code) + "\",\"message\":\"" + escape(message)
                + "\",\"data\":null,\"requestId\":\"" + escape(requestId) + "\"}");
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
