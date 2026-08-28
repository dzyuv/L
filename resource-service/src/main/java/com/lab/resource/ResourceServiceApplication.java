package com.lab.resource;
import com.lab.common.persistence.CrudMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@MapperScan(basePackages="com.lab.resource", markerInterface=CrudMapper.class)
@SpringBootApplication(scanBasePackages={
    "com.lab.resource","com.lab.common"
}
) public class ResourceServiceApplication{
    public static void main(String[]a){
        SpringApplication.run(ResourceServiceApplication.class,a);
    }
}
