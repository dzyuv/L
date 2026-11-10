package com.lab.resource.seed;
import com.lab.resource.entity.*;
import com.lab.resource.repository.*;
import com.lab.resource.client.*;
import com.lab.resource.controller.*;
import com.lab.resource.service.*;
import com.lab.resource.service.impl.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Service
public class ResourceSeedService {
    private final ResourceTypeRepository types;
    private final ResourceRepository resources;
    private final ScheduleRepository schedules;
    private final AssetCategoryRepository categories;

    public ResourceSeedService(ResourceTypeRepository types, ResourceRepository resources,
                               ScheduleRepository schedules, AssetCategoryRepository categories) {
        this.types = types;
        this.resources = resources;
        this.schedules = schedules;
        this.categories = categories;
    }

    @Transactional
    public void seedDemoResources() {
        ResourceType type = types.findAll().stream().filter(item -> "实验室".equals(item.name)).findFirst().orElseGet(() -> {
            ResourceType created = new ResourceType();
            created.name = "实验室";
            created.defaultApprovalLevel = 0;
            created.defaultNeedCheckin = false;
            return types.save(created);
        });
        createResourceIfMissing(type.id, "材料分析实验室", "A201", 12, "材料样品检测与分析");
        createResourceIfMissing(type.id, "计算机实验室", "B305", 40, "软件开发与课程实践");
        createResourceIfMissing(type.id, "电子测量实验室", "C108", 20, "电子电路与仪器测量");
    }

    @Transactional
    public void seedDemoAssetCategories() {
        createCategoryIfMissing("计算机", true, true, "台式机、笔记本和工作站，一台设备对应一条资产");
        createCategoryIfMissing("仪器设备", true, true, "实验仪器和贵重设备，一台设备对应一条资产");
        createCategoryIfMissing("普通设备", true, false, "需要独立编号和序列号的普通设备");
    }

    private void createResourceIfMissing(Long typeId, String name, String location, int capacity, String description) {
        Resource resource = resources.findAll().stream().filter(item -> name.equals(item.name)).findFirst().orElseGet(() -> {
            Resource created = new Resource();
            created.typeId = typeId;
            created.name = name;
            created.location = location;
            created.capacity = capacity;
            created.description = description;
            created.needCheckin = false;
            created.maxDurationMinutes = 120;
            return resources.save(created);
        });
        if (schedules.findByResourceIdAndWeekdayAndEnabledTrue(resource.id, 1).isEmpty()) {
            List<ResourceSchedule> weekly = java.util.stream.IntStream.rangeClosed(1, 5).mapToObj(day -> {
                ResourceSchedule schedule = new ResourceSchedule();
                schedule.resourceId = resource.id;
                schedule.weekday = day;
                schedule.openTime = LocalTime.of(9, 0);
                schedule.closeTime = LocalTime.of(17, 0);
                schedule.slotMinutes = 30;
                schedule.maxDurationMinutes = 120;
                return schedule;
            }).toList();
            schedules.saveAll(weekly);
        }
    }

    private void createCategoryIfMissing(String name, boolean serialized, boolean highValue, String description) {
        if (categories.findByNameIgnoreCase(name).isPresent()) return;
        AssetCategory item = new AssetCategory();
        item.name = name;
        item.serialized = serialized;
        item.highValue = highValue;
        item.description = description;
        categories.save(item);
    }
}
