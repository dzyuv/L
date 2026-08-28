package com.lab.approval;
import com.lab.common.persistence.CrudMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.context.annotation.*;
@MapperScan(basePackages="com.lab.approval", markerInterface=CrudMapper.class)
@SpringBootApplication(scanBasePackages={
    "com.lab.approval","com.lab.common"
}
) public class ApprovalServiceApplication{
    public static void main(String[]a){
        SpringApplication.run(ApprovalServiceApplication.class,a);
    }
}
