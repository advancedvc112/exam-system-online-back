package com.exam.online.actuator;

import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 监控模块启动类 - Spring Boot Admin Server
 * 
 * 访问地址：
 * - Admin UI: http://localhost:8081
 * - 健康检查: http://localhost:8081/actuator/health
 * - 所有指标: http://localhost:8081/actuator/metrics
 */
@EnableAdminServer
@SpringBootApplication(
    scanBasePackages = {
        "com.exam.online.actuator",
        "com.exam.online.service",
        "com.exam.online.config"
    }
)
@ComponentScan(
    basePackages = {
        "com.exam.online.actuator",
        "com.exam.online.service",
        "com.exam.online.config"
    },
    // 排除不需要的服务类（监控模块只需要限流和锁相关的服务）
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = {
                // 依赖 RocketMQ 的服务
                com.exam.online.service.ExamAnswerService.class,
                com.exam.online.service.AnswerBufferService.class,
                com.exam.online.consumer.AnswerRecordConsumer.class,
                // 依赖数据库 Mapper 的服务
                com.exam.online.service.AuthService.class,
                com.exam.online.service.ExamParticipantService.class,
                com.exam.online.service.ExamService.class,
                com.exam.online.service.QuestionService.class,
                com.exam.online.service.ExamStatusScheduler.class
            }
        )
    }
)
@EnableScheduling
public class ActuatorApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ActuatorApplication.class, args);
    }
}

