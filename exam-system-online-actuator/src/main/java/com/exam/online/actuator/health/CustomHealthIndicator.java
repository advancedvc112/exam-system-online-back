package com.exam.online.actuator.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.concurrent.TimeUnit;

/**
 * 自定义健康检查指示器
 * 检查 Redis、MySQL 等组件的健康状态
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomHealthIndicator implements HealthIndicator {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final DataSource dataSource;
    
    @Override
    public Health health() {
        Health.Builder builder = new Health.Builder();
        
        // 检查 Redis 连接
        Health redisHealth = checkRedisDetailed();
        builder.withDetail("redis", redisHealth);
        
        // 检查 MySQL 连接
        Health mysqlHealth = checkMySQLDetailed();
        builder.withDetail("mysql", mysqlHealth);
        
        // 整体健康状态
        if (redisHealth.getStatus().equals(org.springframework.boot.actuate.health.Status.UP) 
            && mysqlHealth.getStatus().equals(org.springframework.boot.actuate.health.Status.UP)) {
            builder.up();
        } else {
            builder.down();
        }
        
        return builder.build();
    }
    
    /**
     * 检查 Redis 连接（详细信息）
     */
    private Health checkRedisDetailed() {
        try {
            long startTime = System.currentTimeMillis();
            String testKey = "health:check:" + System.currentTimeMillis();
            redisTemplate.opsForValue().set(testKey, "ok", 5, TimeUnit.SECONDS);
            String value = (String) redisTemplate.opsForValue().get(testKey);
            redisTemplate.delete(testKey);
            long responseTime = System.currentTimeMillis() - startTime;
            
            if ("ok".equals(value)) {
                return Health.up()
                    .withDetail("status", "连接正常")
                    .withDetail("responseTime", responseTime + "ms")
                    .withDetail("operation", "读写测试成功")
                    .build();
            } else {
                return Health.down()
                    .withDetail("status", "连接异常")
                    .withDetail("error", "返回值不匹配")
                    .build();
            }
        } catch (Exception e) {
            log.error("Redis 健康检查失败", e);
            return Health.down()
                .withDetail("status", "连接异常")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
    
    /**
     * 检查 MySQL 连接（详细信息）
     */
    private Health checkMySQLDetailed() {
        try (Connection connection = dataSource.getConnection()) {
            long startTime = System.currentTimeMillis();
            boolean isValid = connection.isValid(3); // 3秒超时
            long responseTime = System.currentTimeMillis() - startTime;
            
            if (isValid) {
                String catalog = connection.getCatalog();
                String url = connection.getMetaData().getURL();
                String driverName = connection.getMetaData().getDriverName();
                
                return Health.up()
                    .withDetail("status", "连接正常")
                    .withDetail("responseTime", responseTime + "ms")
                    .withDetail("database", catalog)
                    .withDetail("url", url)
                    .withDetail("driver", driverName)
                    .build();
            } else {
                return Health.down()
                    .withDetail("status", "连接异常")
                    .withDetail("error", "连接验证失败")
                    .build();
            }
        } catch (Exception e) {
            log.error("MySQL 健康检查失败", e);
            return Health.down()
                .withDetail("status", "连接异常")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}

