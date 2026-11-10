package com.lab.user.service;
import com.lab.user.entity.*;
import com.lab.user.repository.*;
import com.lab.user.client.*;
import com.lab.user.seed.*;
import com.lab.user.scheduler.*;
import com.lab.user.security.*;
import com.lab.user.config.*;
import com.lab.user.controller.*;
import com.lab.user.service.impl.*;

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
