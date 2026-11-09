package com.lab.user;

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
