package com.lab.user.service;

import com.lab.user.controller.AuthController;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface UserService {
    Map<String, Object> register(AuthController.Register request);
    Map<String, Object> login(AuthController.Login request);
    Map<String,Object> refresh(String refreshToken);
    Map<String,Object> logout(String refreshToken,HttpServletRequest request);
    Map<String, Object> me(HttpServletRequest request);
}
