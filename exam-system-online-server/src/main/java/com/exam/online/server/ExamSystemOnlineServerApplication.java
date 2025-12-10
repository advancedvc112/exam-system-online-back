package com.exam.online.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.exam.online")
@MapperScan("com.exam.online.dal.mapper")
public class ExamSystemOnlineServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ExamSystemOnlineServerApplication.class, args);
    }
}

