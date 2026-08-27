package com.lab.approval;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.context.annotation.*;
@SpringBootApplication(scanBasePackages={
    "com.lab.approval","com.lab.common"
}
) public class ApprovalServiceApplication{
    public static void main(String[]a){
        SpringApplication.run(ApprovalServiceApplication.class,a);
    }
}
