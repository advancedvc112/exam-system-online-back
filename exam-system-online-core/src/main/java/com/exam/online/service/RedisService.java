package com.exam.online.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 设置key-value，带过期时间（秒）
     */
    public void set(String key, String value, long timeoutSeconds) {
        try {
            redisTemplate.opsForValue().set(key, value, timeoutSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Redis设置失败: key={}", key, e);
            throw new RuntimeException("Redis操作失败", e);
        }
    }
    
    /**
     * 获取value
     */
    public String get(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            log.error("Redis获取失败: key={}", key, e);
            return null;
        }
    }
    
    /**
     * 设置key-value，带过期时间（毫秒），用于自动刷新TTL
     */
    public void setWithMilliseconds(String key, String value, long timeoutMillis) {
        try {
            redisTemplate.opsForValue().set(key, value, timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("Redis设置失败: key={}", key, e);
            throw new RuntimeException("Redis操作失败", e);
        }
    }
    
    /**
     * 使用SETNX原子操作设置key（如果不存在）
     * @return true表示设置成功，false表示key已存在
     */
    public Boolean setIfAbsent(String key, String value, long timeoutSeconds) {
        try {
            return redisTemplate.opsForValue().setIfAbsent(key, value, timeoutSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Redis SETNX失败: key={}", key, e);
            throw new RuntimeException("Redis操作失败", e);
        }
    }
    
    /**
     * 根据模式获取所有key
     */
    public Set<String> keys(String pattern) {
        try {
            return redisTemplate.keys(pattern);
        } catch (Exception e) {
            log.error("Redis获取keys失败: pattern={}", pattern, e);
            return Set.of();
        }
    }
    
    /**
     * 删除key
     */
    public void delete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("Redis删除失败: key={}", key, e);
        }
    }
    
    /**
     * 批量删除key
     */
    public void delete(Set<String> keys) {
        if (keys != null && !keys.isEmpty()) {
            try {
                redisTemplate.delete(keys);
            } catch (Exception e) {
                log.error("Redis批量删除失败", e);
            }
        }
    }
}

