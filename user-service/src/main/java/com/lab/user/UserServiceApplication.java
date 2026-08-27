package com.lab.user;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
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
