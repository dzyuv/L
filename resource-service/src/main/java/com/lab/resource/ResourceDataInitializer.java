package com.lab.resource;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Configuration
public class ResourceDataInitializer {
    @Bean
    CommandLineRunner seedDemoResources(ResourceTypeRepository types, ResourceRepository resources, ScheduleRepository schedules) {
        return args -> seed(types, resources, schedules);
    }

    @Transactional
    void seed(ResourceTypeRepository types, ResourceRepository resources, ScheduleRepository schedules) {
        ResourceType type = types.findAll().stream().filter(item -> "实验室".equals(item.name)).findFirst().orElseGet(() -> {
            ResourceType created = new ResourceType();
            created.name = "实验室";
            created.defaultApprovalLevel = 0;
            created.defaultNeedCheckin = false;
            return types.save(created);
        });
        createIfMissing(resources, schedules, type.id, "材料分析实验室", "A201", 12, "材料样品检测与分析");
        createIfMissing(resources, schedules, type.id, "计算机实验室", "B305", 40, "软件开发与课程实践");
        createIfMissing(resources, schedules, type.id, "电子测量实验室", "C108", 20, "电子电路与仪器测量");
    }

    private void createIfMissing(ResourceRepository resources, ScheduleRepository schedules, Long typeId, String name, String location, int capacity, String description) {
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
}
