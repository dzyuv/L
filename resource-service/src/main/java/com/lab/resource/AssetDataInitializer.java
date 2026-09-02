package com.lab.resource;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class AssetDataInitializer {
    @Bean
    CommandLineRunner seedAssetCategories(AssetCategoryRepository categories, JdbcTemplate jdbc) {
        return args -> {
            jdbc.execute("""
                    CREATE TABLE IF NOT EXISTS asset_purchase (
                      id BIGINT NOT NULL AUTO_INCREMENT,
                      purchaser_id BIGINT NULL,
                      purchaser_name VARCHAR(255) NULL,
                      purchased_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                      source VARCHAR(30) NOT NULL DEFAULT 'PURCHASE',
                      category_id BIGINT NULL,
                      name VARCHAR(255) NOT NULL,
                      brand VARCHAR(255) NULL,
                      model VARCHAR(255) NULL,
                      quantity INT NOT NULL,
                      resource_id BIGINT NULL,
                      location VARCHAR(255) NULL,
                      asset_nos VARCHAR(2000) NULL,
                      PRIMARY KEY (id),
                      KEY idx_purchase_time (purchased_at)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """);
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
