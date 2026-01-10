package com.uniticket;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@MapperScan("com.uniticket.mapper")
@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
public class UniTicketApplication {

    public static void main(String[] args) {
        SpringApplication.run(UniTicketApplication.class, args);
    }

}
