package com.lab.system;
import com.lab.common.persistence.CrudMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
@MapperScan(basePackages="com.lab.system", markerInterface=CrudMapper.class)
@SpringBootApplication(scanBasePackages={
    "com.lab.system","com.lab.common"
}
) public class SystemServiceApplication{
    public static void main(String[]a){
        SpringApplication.run(SystemServiceApplication.class,a);
    }
}
