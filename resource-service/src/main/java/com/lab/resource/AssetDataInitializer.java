package com.lab.resource;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AssetDataInitializer {
    @Bean
    CommandLineRunner seedAssetCategories(AssetCategoryRepository categories) {
        return args -> {
            create(categories, "计算机", true, true, "台式机、笔记本和工作站，一台设备对应一条资产");
            create(categories, "仪器设备", true, true, "实验仪器和贵重设备，一台设备对应一条资产");
            create(categories, "普通设备", true, false, "需要独立编号和序列号的普通设备");
        };
    }

    private void create(AssetCategoryRepository categories, String name, boolean serialized, boolean highValue, String description) {
        if (categories.findByNameIgnoreCase(name).isPresent()) return;
        AssetCategory item = new AssetCategory(); item.name = name; item.serialized = serialized; item.highValue = highValue; item.description = description; categories.save(item);
    }
}
