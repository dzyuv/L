package com.lab.user.service;

import com.lab.user.controller.AuthController;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface UserService {
    Map<String, Object> register(AuthController.Register request);
    Map<String, Object> login(AuthController.Login request);
    Map<String, Object> me(HttpServletRequest request);
}
