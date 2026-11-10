package com.lab.booking;
import com.lab.booking.entity.*;
import com.lab.booking.repository.*;
import com.lab.booking.client.*;
import com.lab.booking.scheduler.*;
import com.lab.booking.config.*;
import com.lab.booking.service.*;
import com.lab.booking.service.impl.*;
import com.lab.booking.controller.*;
import com.lab.booking.approval.entity.*;
import com.lab.booking.approval.repository.*;
import com.lab.booking.approval.service.*;
import com.lab.booking.approval.service.impl.*;
import com.lab.booking.approval.controller.*;
import com.lab.common.persistence.CrudMapper;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "5m")
@EnableFeignClients
@MapperScan(basePackages="com.lab.booking", markerInterface=CrudMapper.class)
@SpringBootApplication(scanBasePackages={
    "com.lab.booking","com.lab.common"
}
) public class BookingServiceApplication{
    public static void main(String[]a){
        SpringApplication.run(BookingServiceApplication.class,a);
    }
}
