package com.exam.online.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.exam.online")
@MapperScan("com.exam.online.dal.mapper")
@EnableScheduling
public class ExamSystemOnlineServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ExamSystemOnlineServerApplication.class, args);
    }
}

