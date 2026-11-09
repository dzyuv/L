package com.lab.user;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserDataInitializer {
    @Bean
    CommandLineRunner seedRequiredRoles(UserSeedService seeds) {
        return args -> seeds.seedRequiredRoles();
    }

    @Bean
    @ConditionalOnProperty(name = "seed.demo-data", havingValue = "true")
    CommandLineRunner seedDemoUsers(UserSeedService seeds) {
        return args -> seeds.seedDemoUsers();
    }
}
