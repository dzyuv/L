package com.lab.resource.seed;
import com.lab.resource.entity.*;
import com.lab.resource.repository.*;
import com.lab.resource.client.*;
import com.lab.resource.controller.*;
import com.lab.resource.service.*;
import com.lab.resource.service.impl.*;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "seed.demo-data", havingValue = "true")
public class AssetDataInitializer {
    @Bean
    CommandLineRunner seedDemoAssetCategories(ResourceSeedService seeds) {
        return args -> seeds.seedDemoAssetCategories();
    }
}
