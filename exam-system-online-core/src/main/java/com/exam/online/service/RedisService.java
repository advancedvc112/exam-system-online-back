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
    
    /**
     * 执行Lua脚本
     * @param script Lua脚本
     * @param keys 键列表
     * @param args 参数列表
     * @return 执行结果
     */
    @SuppressWarnings("unchecked")
    public <T> T executeScript(String script, java.util.List<String> keys, java.util.List<Object> args) {
        try {
            log.debug("[Redis Lua] 准备执行脚本: keys={}, args={}", keys, args);
            org.springframework.data.redis.core.script.DefaultRedisScript<T> redisScript = 
                new org.springframework.data.redis.core.script.DefaultRedisScript<>();
            redisScript.setScriptText(script);
            redisScript.setResultType((Class<T>) Long.class);
            // 使用正确的execute方法签名：execute(RedisScript<T> script, List<K> keys, Object... args)
            T result = redisTemplate.execute(redisScript, keys, args.toArray());
            log.debug("[Redis Lua] 脚本执行完成: keys={}, result={}", keys, result);
            return result;
        } catch (Exception e) {
            log.error("执行Lua脚本失败: script={}, keys={}, args={}", script, keys, args, e);
            throw new RuntimeException("执行Lua脚本失败", e);
        }
    }
}

