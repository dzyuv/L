package com.lab.statistics;
import com.lab.common.persistence.CrudMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
@MapperScan(basePackages="com.lab.statistics", markerInterface=CrudMapper.class)
@SpringBootApplication(scanBasePackages={
    "com.lab.statistics","com.lab.common"
}
) public class StatisticsServiceApplication{
    public static void main(String[]a){
        SpringApplication.run(StatisticsServiceApplication.class,a);
    }
}
