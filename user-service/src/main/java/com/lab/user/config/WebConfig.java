package com.lab.user.config;
import com.lab.user.entity.*;
import com.lab.user.repository.*;
import com.lab.user.client.*;
import com.lab.user.seed.*;
import com.lab.user.scheduler.*;
import com.lab.user.security.*;
import com.lab.user.controller.*;
import com.lab.user.service.*;
import com.lab.user.service.impl.*;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
@Configuration public class WebConfig {
    @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
        return http.csrf(c->c.disable()).authorizeHttpRequests(a->a.anyRequest().permitAll()).build();
    }
}
