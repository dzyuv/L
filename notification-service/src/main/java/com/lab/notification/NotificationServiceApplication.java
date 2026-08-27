package com.lab.notification;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
@SpringBootApplication(scanBasePackages={
    "com.lab.notification","com.lab.common"
}
) public class NotificationServiceApplication{
    public static void main(String[]a){
        SpringApplication.run(NotificationServiceApplication.class,a);
    }
}
