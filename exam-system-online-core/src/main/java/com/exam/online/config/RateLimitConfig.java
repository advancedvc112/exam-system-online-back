package com.exam.online.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 限流配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitConfig {
    
    /**
     * 是否启用限流
     */
    private boolean enabled = true;
    
    /**
     * 默认限流算法
     */
    private String defaultAlgorithm = "token-bucket";
    
    /**
     * 用户级别限流规则
     */
    private UserLevel userLevel = new UserLevel();
    
    @Data
    public static class UserLevel {
        /**
         * 是否启用用户级别限流
         */
        private boolean enabled = true;
        
        /**
         * 限流算法
         */
        private String algorithm = "token-bucket";
        
        /**
         * 限流规则
         */
        private Map<String, Rule> rules = new HashMap<>();
    }
    
    @Data
    public static class Rule {
        /**
         * 是否启用
         */
        private boolean enabled = true;
        
        /**
         * QPS（每秒请求数）
         */
        private int qps = 10;
        
        /**
         * 桶容量（令牌桶算法）
         */
        private int capacity = 10;
        
        /**
         * 补充速率（令牌桶算法）
         */
        private int refillRate = 10;
        
        /**
         * 时间间隔（秒）
         */
        private int interval = 1;
    }
}

