package com.lab.common.api;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtUserFilterTest {
    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    private final JwtKeyProvider keys = new JwtKeyProvider(SECRET);
    private final TokenVersionResolver versions = mock(TokenVersionResolver.class);
    private final JwtUserFilter filter = new JwtUserFilter(keys, versions);

    @Test
    void acceptsTokenWithCurrentVersion() throws Exception {
        when(versions.currentVersion(7L)).thenReturn(2);

        assertThat(filter(token(7L, 2)).getStatus()).isEqualTo(200);
    }

    @Test
    void rejectsTokenWithStaleVersion() throws Exception {
        when(versions.currentVersion(7L)).thenReturn(3);

        assertThat(filter(token(7L, 2)).getStatus()).isEqualTo(401);
    }

    @Test
    void rejectsTokenWithoutVersion() throws Exception {
        assertThat(filter(tokenWithoutVersion(7L)).getStatus()).isEqualTo(401);
    }

    @Test
    void rejectsTokenWhenVersionCannotBeResolved() throws Exception {
        when(versions.currentVersion(7L)).thenReturn(null);

        MockHttpServletResponse response = filter(token(7L, 2));
        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentAsString()).contains("SERVICE_UNAVAILABLE");
    }

    private MockHttpServletResponse filter(String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/resources");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    private String token(Long userId, int version) {
        return Jwts.builder().subject(userId.toString())
                .claim("tokenType", "access").claim("ver", version)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(keys.key()).compact();
    }

    private String tokenWithoutVersion(Long userId) {
        return Jwts.builder().subject(userId.toString())
                .claim("tokenType", "access")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(keys.key()).compact();
    }
}
