package com.lab.booking;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
@EnableScheduling @SpringBootApplication(scanBasePackages={
    "com.lab.booking","com.lab.common"
}
) public class BookingServiceApplication{
    public static void main(String[]a){
        SpringApplication.run(BookingServiceApplication.class,a);
    }
}
