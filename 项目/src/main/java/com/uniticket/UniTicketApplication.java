package com.uniticket;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.uniticket.mapper")
@SpringBootApplication
public class UniTicketApplication {

    public static void main(String[] args) {
        SpringApplication.run(UniTicketApplication.class, args);
    }

}
