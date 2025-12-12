package com.exam.online.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 锁监控服务：定期输出锁的统计信息
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LockMonitorService {
    
    private final DistributedLockService distributedLockService;
    
    /**
     * 每5分钟输出一次锁监控统计信息
     */
    @Scheduled(fixedRate = 300000) // 5分钟 = 300000毫秒
    public void logLockStatistics() {
        DistributedLockService.LockStatistics stats = distributedLockService.getLockStatistics();
        
        log.info("========== [锁监控统计] ==========");
        log.info("进入考试锁统计:");
        log.info("  - 成功次数: {}", stats.getEnterExamSuccess());
        log.info("  - 失败次数: {}", stats.getEnterExamFailure());
        log.info("  - 失败率: {}%", String.format("%.2f", stats.getEnterExamFailureRate()));
        log.info("  - 平均持有时间: {}ms", stats.getEnterExamAvgHoldTime());
        
        log.info("提交考试锁统计:");
        log.info("  - 成功次数: {}", stats.getSubmitExamSuccess());
        log.info("  - 失败次数: {}", stats.getSubmitExamFailure());
        log.info("  - 失败率: {}%", String.format("%.2f", stats.getSubmitExamFailureRate()));
        log.info("  - 平均持有时间: {}ms", stats.getSubmitExamAvgHoldTime());
        log.info("==================================");
        
        // 如果失败率过高，记录警告
        if (stats.getEnterExamFailureRate() > 10.0) {
            log.warn("[锁监控告警] 进入考试锁失败率过高: {}%, 可能存在锁竞争问题", 
                String.format("%.2f", stats.getEnterExamFailureRate()));
        }
        
        if (stats.getSubmitExamFailureRate() > 10.0) {
            log.warn("[锁监控告警] 提交考试锁失败率过高: {}%, 可能存在锁竞争问题", 
                String.format("%.2f", stats.getSubmitExamFailureRate()));
        }
        
        // 如果平均持有时间过长，记录警告
        if (stats.getEnterExamAvgHoldTime() > 5000) {
            log.warn("[锁监控告警] 进入考试锁平均持有时间过长: {}ms, 可能影响性能", 
                stats.getEnterExamAvgHoldTime());
        }
        
        if (stats.getSubmitExamAvgHoldTime() > 3000) {
            log.warn("[锁监控告警] 提交考试锁平均持有时间过长: {}ms, 可能影响性能", 
                stats.getSubmitExamAvgHoldTime());
        }
    }
}

