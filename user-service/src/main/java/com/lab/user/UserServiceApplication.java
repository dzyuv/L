package com.lab.user;
import com.lab.user.entity.*;
import com.lab.user.repository.*;
import com.lab.user.client.*;
import com.lab.user.seed.*;
import com.lab.user.scheduler.*;
import com.lab.user.security.*;
import com.lab.user.config.*;
import com.lab.user.controller.*;
import com.lab.user.service.*;
import com.lab.user.service.impl.*;
import com.lab.common.persistence.CrudMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
@EnableScheduling
@EnableFeignClients
@MapperScan(basePackages="com.lab.user", markerInterface=CrudMapper.class)
@SpringBootApplication(scanBasePackages={
    "com.lab.user","com.lab.common"
}
) public class UserServiceApplication {
    public static void main(String[] a){
        SpringApplication.run(UserServiceApplication.class,a);
    }
    @Bean PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
}
