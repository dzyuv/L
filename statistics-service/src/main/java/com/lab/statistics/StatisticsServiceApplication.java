package com.lab.statistics;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
@SpringBootApplication(scanBasePackages={
    "com.lab.statistics","com.lab.common"
}
) public class StatisticsServiceApplication{
    public static void main(String[]a){
        SpringApplication.run(StatisticsServiceApplication.class,a);
    }
}
