package com.lab.booking;
import com.lab.common.persistence.CrudMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
@EnableScheduling
@MapperScan(basePackages="com.lab.booking", markerInterface=CrudMapper.class)
@SpringBootApplication(scanBasePackages={
    "com.lab.booking","com.lab.common"
}
) public class BookingServiceApplication{
    public static void main(String[]a){
        SpringApplication.run(BookingServiceApplication.class,a);
    }
}
