package com.lab.resource.seed;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "seed.demo-data", havingValue = "true")
public class ResourceDataInitializer {
    @Bean
    CommandLineRunner seedDemoResources(ResourceSeedService seeds) {
        return args -> seeds.seedDemoResources();
    }
}
