package com.lab.system;
import com.lab.system.entity.*;
import com.lab.system.repository.*;
import com.lab.system.seed.*;
import com.lab.system.scheduler.*;
import com.lab.system.config.*;
import com.lab.system.controller.*;
import com.lab.system.service.*;
import com.lab.system.service.impl.*;
import com.lab.common.persistence.CrudMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.scheduling.annotation.EnableScheduling;
@EnableScheduling
@MapperScan(basePackages="com.lab.system", markerInterface=CrudMapper.class)
@SpringBootApplication(scanBasePackages={
    "com.lab.system","com.lab.common"
}
) public class SystemServiceApplication{
    public static void main(String[]a){
        SpringApplication.run(SystemServiceApplication.class,a);
    }
}
