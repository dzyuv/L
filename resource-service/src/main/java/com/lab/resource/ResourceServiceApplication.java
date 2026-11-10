package com.lab.resource;
import com.lab.resource.entity.*;
import com.lab.resource.repository.*;
import com.lab.resource.client.*;
import com.lab.resource.seed.*;
import com.lab.resource.controller.*;
import com.lab.resource.service.*;
import com.lab.resource.service.impl.*;
import com.lab.common.persistence.CrudMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
@EnableFeignClients
@MapperScan(basePackages="com.lab.resource", markerInterface=CrudMapper.class)
@SpringBootApplication(scanBasePackages={
    "com.lab.resource","com.lab.common"
}
) public class ResourceServiceApplication{
    public static void main(String[]a){
        SpringApplication.run(ResourceServiceApplication.class,a);
    }
}
