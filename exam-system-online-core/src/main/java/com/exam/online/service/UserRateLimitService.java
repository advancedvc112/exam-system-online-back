package com.exam.online.service;

import com.exam.online.config.RateLimitConfig;
import com.exam.online.exception.RateLimitException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 用户级别限流服务（使用Redis + Lua脚本实现令牌桶算法）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserRateLimitService {
    
    private final RedisService redisService;
    private final RateLimitConfig rateLimitConfig;
    
    // 监控统计
    private final AtomicLong rateLimitSuccessCount = new AtomicLong(0);
    private final AtomicLong rateLimitFailureCount = new AtomicLong(0);
    
    /**
     * 令牌桶算法的Lua脚本
     */
    private static final String TOKEN_BUCKET_SCRIPT =
        "local key = KEYS[1]\n" +
        "local capacity = tonumber(ARGV[1])\n" +
        "local refillRate = tonumber(ARGV[2])\n" +
        "local interval = tonumber(ARGV[3])\n" +
        "local now = tonumber(ARGV[4])\n" +
        "local expireTime = tonumber(ARGV[5])\n" +
        "\n" +
        "-- 参数有效性检查\n" +
        "if not capacity or not refillRate or not interval or not now or not expireTime then\n" +
        "    return -1\n" +
        "end\n" +
        "\n" +
        "local tokens = redis.call('hget', key, 'tokens')\n" +
        "local lastUpdate = redis.call('hget', key, 'lastUpdate')\n" +
        "\n" +
        "if not tokens or tokens == false then\n" +
        "    tokens = capacity\n" +
        "    lastUpdate = now\n" +
        "else\n" +
        "    tokens = tonumber(tokens)\n" +
        "    lastUpdate = tonumber(lastUpdate)\n" +
        "    if not tokens or not lastUpdate then\n" +
        "        tokens = capacity\n" +
        "        lastUpdate = now\n" +
        "    else\n" +
        "        local elapsed = (now - lastUpdate) / 1000\n" +
        "        local refill = math.floor(elapsed * refillRate / interval)\n" +
        "        tokens = math.min(capacity, math.max(0, tokens + refill))\n" +
        "    end\n" +
        "end\n" +
        "\n" +
        "local expireSeconds = math.floor(expireTime)\n" +
        "if expireSeconds <= 0 then\n" +
        "    expireSeconds = 60\n" +
        "end\n" +
        "\n" +
        "if tokens and tokens > 0 then\n" +
        "    tokens = tokens - 1\n" +
        "    redis.call('hmset', key, 'tokens', tostring(tokens), 'lastUpdate', tostring(now))\n" +
        "    redis.call('expire', key, expireSeconds)\n" +
        "    return 1\n" +
        "else\n" +
        "    redis.call('hmset', key, 'tokens', tostring(tokens), 'lastUpdate', tostring(now))\n" +
        "    redis.call('expire', key, expireSeconds)\n" +
        "    return 0\n" +
        "end";
    
    /**
     * 检查用户级别限流
     * @param userId 用户ID
     * @param interfaceType 接口类型（enter-exam, save-answer, submit-exam）
     * @throws RateLimitException 如果被限流则抛出异常
     */
    public void checkUserRateLimit(Long userId, String interfaceType) {
        if (!rateLimitConfig.isEnabled() || !rateLimitConfig.getUserLevel().isEnabled()) {
            return;
        }
        
        RateLimitConfig.Rule rule = rateLimitConfig.getUserLevel().getRules().get(interfaceType);
        if (rule == null || !rule.isEnabled()) {
            return;
        }
        
        String key = buildRateLimitKey(userId, interfaceType);
        long now = System.currentTimeMillis();
        
        try {
            List<String> keys = Arrays.asList(key);
            List<Object> args = Arrays.asList(
                String.valueOf(rule.getCapacity()),
                String.valueOf(rule.getRefillRate()),
                String.valueOf(rule.getInterval()),
                String.valueOf(now),
                "60" // 过期时间60秒
            );
            
            Long result = redisService.executeScript(TOKEN_BUCKET_SCRIPT, keys, args);
            
            if (result == null || result == 0) {
                // 被限流
                rateLimitFailureCount.incrementAndGet();
                double failureRate = calculateFailureRate();
                log.warn("[限流监控] 用户级别限流触发: userId={}, interfaceType={}, key={}, " +
                    "qps={}, capacity={}, totalSuccess={}, totalFailure={}, failureRate={}%",
                    userId, interfaceType, key, rule.getQps(), rule.getCapacity(),
                    rateLimitSuccessCount.get(), rateLimitFailureCount.get(), 
                    String.format("%.2f", failureRate));
                throw new RateLimitException("请求过于频繁，请稍后重试", rule.getInterval());
            } else {
                // 允许通过
                rateLimitSuccessCount.incrementAndGet();
                log.debug("[限流监控] 用户级别限流检查通过: userId={}, interfaceType={}, key={}",
                    userId, interfaceType, key);
            }
        } catch (RateLimitException e) {
            throw e;
        } catch (Exception e) {
            // Redis故障时，降级策略：允许通过，记录告警
            log.error("[限流监控] 限流检查异常，降级允许通过: userId={}, interfaceType={}, key={}",
                userId, interfaceType, key, e);
            // 不抛出异常，允许请求通过（fail-open策略）
        }
    }
    
    /**
     * 构建限流key
     */
    private String buildRateLimitKey(Long userId, String interfaceType) {
        return "rate_limit:" + interfaceType + ":user:" + userId;
    }
    
    /**
     * 计算失败率
     */
    private double calculateFailureRate() {
        long total = rateLimitSuccessCount.get() + rateLimitFailureCount.get();
        if (total == 0) {
            return 0.0;
        }
        return (rateLimitFailureCount.get() * 100.0) / total;
    }
    
    /**
     * 获取限流统计信息
     */
    public RateLimitStatistics getStatistics() {
        return new RateLimitStatistics(
            rateLimitSuccessCount.get(),
            rateLimitFailureCount.get(),
            calculateFailureRate()
        );
    }
    
    /**
     * 限流统计信息
     */
    public static class RateLimitStatistics {
        private final long successCount;
        private final long failureCount;
        private final double failureRate;
        
        public RateLimitStatistics(long successCount, long failureCount, double failureRate) {
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.failureRate = failureRate;
        }
        
        public long getSuccessCount() { return successCount; }
        public long getFailureCount() { return failureCount; }
        public double getFailureRate() { return failureRate; }
    }
}

