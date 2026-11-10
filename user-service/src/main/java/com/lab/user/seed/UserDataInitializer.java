package com.lab.user.seed;
import com.lab.user.entity.*;
import com.lab.user.repository.*;
import com.lab.user.client.*;
import com.lab.user.scheduler.*;
import com.lab.user.security.*;
import com.lab.user.config.*;
import com.lab.user.controller.*;
import com.lab.user.service.*;
import com.lab.user.service.impl.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserDataInitializer {
    @Bean
    CommandLineRunner seedUsers(UserSeedService seeds,
                                @Value("${seed.demo-data:false}") boolean seedDemoData) {
        return args -> {
            seeds.seedRequiredRoles();
            if (seedDemoData) seeds.seedDemoUsers();
        };
    }
}
