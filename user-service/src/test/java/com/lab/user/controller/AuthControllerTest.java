package com.lab.user.controller;

import com.lab.common.api.ApiResponse;
import com.lab.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthControllerTest {
    private final UserService users = mock(UserService.class);
    private final AuthController controller = new AuthController(users, 30, false, "Lax");

    @Test
    void loginSetsHttpOnlyRefreshCookieAndHidesTokenFromBody() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accessToken", "access");
        result.put("refreshToken", "refresh");
        result.put("expiresIn", 7200);
        when(users.login(any())).thenReturn(result);
        MockHttpServletResponse response = new MockHttpServletResponse();

        ApiResponse<?> api = controller.login(new AuthController.Login("u", "p"), response, new MockHttpServletRequest());

        assertThat(response.getHeader("Set-Cookie"))
                .contains("refreshToken=refresh")
                .contains("HttpOnly")
                .contains("Path=/api/v1/user");
        assertThat(((Map<?, ?>) api.data()).containsKey("refreshToken")).isFalse();
    }

    @Test
    void refreshPrefersCookieToken() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accessToken", "access");
        result.put("refreshToken", "new-refresh");
        when(users.refresh(eq("cookie-refresh"))).thenReturn(result);
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.refresh("cookie-refresh", new AuthController.Refresh("body-refresh"), response, new MockHttpServletRequest());

        assertThat(response.getHeader("Set-Cookie")).contains("refreshToken=new-refresh");
    }

    @Test
    void logoutClearsRefreshCookie() {
        when(users.logout(eq("refresh"), any())).thenReturn(Map.of("loggedOut", true));
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.logout("refresh", null, response, new MockHttpServletRequest());

        assertThat(response.getHeader("Set-Cookie")).contains("Max-Age=0");
    }
}
