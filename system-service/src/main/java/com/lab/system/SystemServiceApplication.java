package com.lab.system;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
@SpringBootApplication(scanBasePackages={
    "com.lab.system","com.lab.common"
}
) public class SystemServiceApplication{
    public static void main(String[]a){
        SpringApplication.run(SystemServiceApplication.class,a);
    }
}
