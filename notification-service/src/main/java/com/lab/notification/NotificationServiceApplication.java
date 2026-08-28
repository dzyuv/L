package com.lab.notification;
import com.lab.common.persistence.CrudMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
@MapperScan(basePackages="com.lab.notification", markerInterface=CrudMapper.class)
@SpringBootApplication(scanBasePackages={
    "com.lab.notification","com.lab.common"
}
) public class NotificationServiceApplication{
    public static void main(String[]a){
        SpringApplication.run(NotificationServiceApplication.class,a);
    }
}
