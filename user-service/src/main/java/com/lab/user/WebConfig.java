package com.lab.user;
import com.lab.common.api.*;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
@Configuration public class WebConfig {
    @Bean RequestIdFilter requestIdFilter(){
        return new RequestIdFilter();
    }
    @Bean JwtUserFilter jwtUserFilter(JwtKeyProvider keys){
        return new JwtUserFilter(keys);
    }
    @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
        return http.csrf(c->c.disable()).authorizeHttpRequests(a->a.anyRequest().permitAll()).build();
    }
}
