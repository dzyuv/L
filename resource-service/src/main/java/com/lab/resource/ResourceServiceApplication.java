package com.lab.resource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication(scanBasePackages={
    "com.lab.resource","com.lab.common"
}
) public class ResourceServiceApplication{
    public static void main(String[]a){
        SpringApplication.run(ResourceServiceApplication.class,a);
    }
}
