package com.lab.system;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
public class SystemConfigDataInitializer {
    @Bean CommandLineRunner seedSystemConfigs(SystemConfigRepository configs) { return args -> {
        List.of(new String[]{"checkin.window.before_minutes","15","INT","签到提前时间（分钟）"},new String[]{"checkin.window.after_minutes","30","INT","签到延后时间（分钟）"},new String[]{"booking.default_max_duration","120","INT","默认最大预约时长"},new String[]{"booking.slot_minutes","30","INT","默认预约粒度"},new String[]{"violation.max_count","3","INT","最大违约次数"},new String[]{"violation.restriction_days","30","INT","违约限制天数"},new String[]{"approval.timeout_minutes","1440","INT","审批超时时间"}).forEach(data->{ if(configs.findByConfigKey(data[0]).isEmpty()){SystemConfig item=new SystemConfig();item.configKey=data[0];item.configValue=data[1];item.valueType=data[2];item.description=data[3];item.updatedBy=0L;configs.save(item);}});
    }; }
}
