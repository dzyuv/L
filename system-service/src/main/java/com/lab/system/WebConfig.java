package com.lab.system;
import com.lab.common.api.*;
import org.springframework.context.annotation.*;
@Configuration public class WebConfig{
    @Bean RequestIdFilter requestIdFilter(){
        return new RequestIdFilter();
    }
    @Bean JwtUserFilter jwtUserFilter(JwtKeyProvider keys){
        return new JwtUserFilter(keys);
    }
}
