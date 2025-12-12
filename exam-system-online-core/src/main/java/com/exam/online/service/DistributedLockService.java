package com.exam.online.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 分布式锁服务（带监控和日志）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedLockService {
    
    private final RedissonClient redissonClient;
    
    // 监控统计：进入考试锁
    private final AtomicLong enterExamLockSuccessCount = new AtomicLong(0);
    private final AtomicLong enterExamLockFailureCount = new AtomicLong(0);
    private final AtomicLong enterExamLockTotalHoldTime = new AtomicLong(0);
    
    // 监控统计：提交考试锁
    private final AtomicLong submitExamLockSuccessCount = new AtomicLong(0);
    private final AtomicLong submitExamLockFailureCount = new AtomicLong(0);
    private final AtomicLong submitExamLockTotalHoldTime = new AtomicLong(0);
    
    /**
     * 获取进入考试的分布式锁
     * @param examId 考试ID
     * @param studentId 学生ID
     * @return 锁对象，如果获取失败返回null
     */
    public RLock getEnterExamLock(Long examId, Long studentId) {
        String lockKey = "exam:enter:lock:" + examId + ":" + studentId;
        RLock lock = redissonClient.getLock(lockKey);
        return lock;
    }
    
    /**
     * 尝试获取进入考试的锁（不等待）
     * @param examId 考试ID
     * @param studentId 学生ID
     * @param timeoutSeconds 锁超时时间（秒）
     * @return 是否获取成功
     */
    public boolean tryLockEnterExam(Long examId, Long studentId, long timeoutSeconds) {
        String lockKey = "exam:enter:lock:" + examId + ":" + studentId;
        long startTime = System.currentTimeMillis();
        RLock lock = getEnterExamLock(examId, studentId);
        
        try {
            // 检查锁是否已被其他线程持有
            boolean isLocked = lock.isLocked();
            if (isLocked) {
                log.info("[锁监控] 进入考试锁已被持有: examId={}, studentId={}, lockKey={}, holderThread={}", 
                    examId, studentId, lockKey, lock.isHeldByCurrentThread() ? "current" : "other");
            }
            
            // 不等待，立即返回；锁超时时间30秒
            boolean acquired = lock.tryLock(0, timeoutSeconds, TimeUnit.SECONDS);
            long acquireTime = System.currentTimeMillis() - startTime;
            
            if (acquired) {
                enterExamLockSuccessCount.incrementAndGet();
                log.info("[锁监控] 获取进入考试锁成功: examId={}, studentId={}, lockKey={}, acquireTime={}ms, timeout={}s, " +
                    "totalSuccess={}, totalFailure={}", 
                    examId, studentId, lockKey, acquireTime, timeoutSeconds,
                    enterExamLockSuccessCount.get(), enterExamLockFailureCount.get());
            } else {
                enterExamLockFailureCount.incrementAndGet();
                double failureRate = calculateFailureRate(enterExamLockSuccessCount.get(), enterExamLockFailureCount.get());
                log.warn("[锁监控] 获取进入考试锁失败: examId={}, studentId={}, lockKey={}, acquireTime={}ms, " +
                    "isLocked={}, totalSuccess={}, totalFailure={}, failureRate={}%", 
                    examId, studentId, lockKey, acquireTime, isLocked,
                    enterExamLockSuccessCount.get(), enterExamLockFailureCount.get(), String.format("%.2f", failureRate));
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            enterExamLockFailureCount.incrementAndGet();
            long acquireTime = System.currentTimeMillis() - startTime;
            log.error("[锁监控] 获取进入考试锁被中断: examId={}, studentId={}, lockKey={}, acquireTime={}ms", 
                examId, studentId, lockKey, acquireTime, e);
            return false;
        } catch (Exception e) {
            enterExamLockFailureCount.incrementAndGet();
            long acquireTime = System.currentTimeMillis() - startTime;
            log.error("[锁监控] 获取进入考试锁异常: examId={}, studentId={}, lockKey={}, acquireTime={}ms", 
                examId, studentId, lockKey, acquireTime, e);
            return false;
        }
    }
    
    /**
     * 释放进入考试的锁（带监控）
     * @param lock 锁对象
     * @param lockStartTime 锁获取时间（用于计算持有时间）
     */
    public void unlockEnterExam(RLock lock, Long lockStartTime) {
        if (lock == null) {
            log.warn("[锁监控] 释放进入考试锁失败: lock为null");
            return;
        }
        
        if (!lock.isHeldByCurrentThread()) {
            log.warn("[锁监控] 释放进入考试锁失败: 当前线程未持有锁, thread={}", Thread.currentThread().getName());
            return;
        }
        
        long holdTime = lockStartTime != null ? System.currentTimeMillis() - lockStartTime : 0;
        
        try {
            lock.unlock();
            enterExamLockTotalHoldTime.addAndGet(holdTime);
            long avgHoldTime = enterExamLockSuccessCount.get() > 0 
                ? enterExamLockTotalHoldTime.get() / enterExamLockSuccessCount.get() 
                : 0;
            
            log.info("[锁监控] 释放进入考试锁成功: holdTime={}ms, avgHoldTime={}ms, totalSuccess={}", 
                holdTime, avgHoldTime, enterExamLockSuccessCount.get());
            
            // 如果持有时间过长，记录警告
            if (holdTime > 5000) {
                log.warn("[锁监控] 进入考试锁持有时间过长: holdTime={}ms, 可能影响性能", holdTime);
            }
        } catch (Exception e) {
            log.error("[锁监控] 释放进入考试锁失败: holdTime={}ms", holdTime, e);
        }
    }
    
    /**
     * 释放进入考试的锁（兼容旧方法，不记录持有时间）
     */
    public void unlockEnterExam(RLock lock) {
        unlockEnterExam(lock, null);
    }
    
    /**
     * 获取提交考试的分布式锁
     * @param examId 考试ID
     * @param studentId 学生ID
     * @return 锁对象
     */
    public RLock getSubmitExamLock(Long examId, Long studentId) {
        String lockKey = "exam:submit:lock:" + examId + ":" + studentId;
        RLock lock = redissonClient.getLock(lockKey);
        return lock;
    }
    
    /**
     * 尝试获取提交考试的锁（不等待）
     * @param examId 考试ID
     * @param studentId 学生ID
     * @param timeoutSeconds 锁超时时间（秒）
     * @return 是否获取成功
     */
    public boolean tryLockSubmitExam(Long examId, Long studentId, long timeoutSeconds) {
        String lockKey = "exam:submit:lock:" + examId + ":" + studentId;
        long startTime = System.currentTimeMillis();
        RLock lock = getSubmitExamLock(examId, studentId);
        
        try {
            // 检查锁是否已被其他线程持有
            boolean isLocked = lock.isLocked();
            if (isLocked) {
                log.info("[锁监控] 提交考试锁已被持有: examId={}, studentId={}, lockKey={}, holderThread={}", 
                    examId, studentId, lockKey, lock.isHeldByCurrentThread() ? "current" : "other");
            }
            
            // 不等待，立即返回；锁超时时间10秒
            boolean acquired = lock.tryLock(0, timeoutSeconds, TimeUnit.SECONDS);
            long acquireTime = System.currentTimeMillis() - startTime;
            
            if (acquired) {
                submitExamLockSuccessCount.incrementAndGet();
                log.info("[锁监控] 获取提交考试锁成功: examId={}, studentId={}, lockKey={}, acquireTime={}ms, timeout={}s, " +
                    "totalSuccess={}, totalFailure={}", 
                    examId, studentId, lockKey, acquireTime, timeoutSeconds,
                    submitExamLockSuccessCount.get(), submitExamLockFailureCount.get());
            } else {
                submitExamLockFailureCount.incrementAndGet();
                double failureRate = calculateFailureRate(submitExamLockSuccessCount.get(), submitExamLockFailureCount.get());
                log.warn("[锁监控] 获取提交考试锁失败: examId={}, studentId={}, lockKey={}, acquireTime={}ms, " +
                    "isLocked={}, totalSuccess={}, totalFailure={}, failureRate={}%", 
                    examId, studentId, lockKey, acquireTime, isLocked,
                    submitExamLockSuccessCount.get(), submitExamLockFailureCount.get(), String.format("%.2f", failureRate));
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            submitExamLockFailureCount.incrementAndGet();
            long acquireTime = System.currentTimeMillis() - startTime;
            log.error("[锁监控] 获取提交考试锁被中断: examId={}, studentId={}, lockKey={}, acquireTime={}ms", 
                examId, studentId, lockKey, acquireTime, e);
            return false;
        } catch (Exception e) {
            submitExamLockFailureCount.incrementAndGet();
            long acquireTime = System.currentTimeMillis() - startTime;
            log.error("[锁监控] 获取提交考试锁异常: examId={}, studentId={}, lockKey={}, acquireTime={}ms", 
                examId, studentId, lockKey, acquireTime, e);
            return false;
        }
    }
    
    /**
     * 释放提交考试的锁（带监控）
     * @param lock 锁对象
     * @param lockStartTime 锁获取时间（用于计算持有时间）
     */
    public void unlockSubmitExam(RLock lock, Long lockStartTime) {
        if (lock == null) {
            log.warn("[锁监控] 释放提交考试锁失败: lock为null");
            return;
        }
        
        if (!lock.isHeldByCurrentThread()) {
            log.warn("[锁监控] 释放提交考试锁失败: 当前线程未持有锁, thread={}", Thread.currentThread().getName());
            return;
        }
        
        long holdTime = lockStartTime != null ? System.currentTimeMillis() - lockStartTime : 0;
        
        try {
            lock.unlock();
            submitExamLockTotalHoldTime.addAndGet(holdTime);
            long avgHoldTime = submitExamLockSuccessCount.get() > 0 
                ? submitExamLockTotalHoldTime.get() / submitExamLockSuccessCount.get() 
                : 0;
            
            log.info("[锁监控] 释放提交考试锁成功: holdTime={}ms, avgHoldTime={}ms, totalSuccess={}", 
                holdTime, avgHoldTime, submitExamLockSuccessCount.get());
            
            // 如果持有时间过长，记录警告
            if (holdTime > 3000) {
                log.warn("[锁监控] 提交考试锁持有时间过长: holdTime={}ms, 可能影响性能", holdTime);
            }
        } catch (Exception e) {
            log.error("[锁监控] 释放提交考试锁失败: holdTime={}ms", holdTime, e);
        }
    }
    
    /**
     * 释放提交考试的锁（兼容旧方法，不记录持有时间）
     */
    public void unlockSubmitExam(RLock lock) {
        unlockSubmitExam(lock, null);
    }
    
    /**
     * 计算失败率
     */
    private double calculateFailureRate(long success, long failure) {
        long total = success + failure;
        if (total == 0) {
            return 0.0;
        }
        return (failure * 100.0) / total;
    }
    
    /**
     * 获取锁监控统计信息（可用于监控系统）
     */
    public LockStatistics getLockStatistics() {
        return new LockStatistics(
            enterExamLockSuccessCount.get(),
            enterExamLockFailureCount.get(),
            enterExamLockSuccessCount.get() > 0 
                ? enterExamLockTotalHoldTime.get() / enterExamLockSuccessCount.get() 
                : 0,
            submitExamLockSuccessCount.get(),
            submitExamLockFailureCount.get(),
            submitExamLockSuccessCount.get() > 0 
                ? submitExamLockTotalHoldTime.get() / submitExamLockSuccessCount.get() 
                : 0
        );
    }
    
    /**
     * 锁监控统计信息
     */
    public static class LockStatistics {
        private final long enterExamSuccess;
        private final long enterExamFailure;
        private final long enterExamAvgHoldTime;
        private final long submitExamSuccess;
        private final long submitExamFailure;
        private final long submitExamAvgHoldTime;
        
        public LockStatistics(long enterExamSuccess, long enterExamFailure, long enterExamAvgHoldTime,
                            long submitExamSuccess, long submitExamFailure, long submitExamAvgHoldTime) {
            this.enterExamSuccess = enterExamSuccess;
            this.enterExamFailure = enterExamFailure;
            this.enterExamAvgHoldTime = enterExamAvgHoldTime;
            this.submitExamSuccess = submitExamSuccess;
            this.submitExamFailure = submitExamFailure;
            this.submitExamAvgHoldTime = submitExamAvgHoldTime;
        }
        
        public long getEnterExamSuccess() { return enterExamSuccess; }
        public long getEnterExamFailure() { return enterExamFailure; }
        public long getEnterExamAvgHoldTime() { return enterExamAvgHoldTime; }
        public long getSubmitExamSuccess() { return submitExamSuccess; }
        public long getSubmitExamFailure() { return submitExamFailure; }
        public long getSubmitExamAvgHoldTime() { return submitExamAvgHoldTime; }
        
        public double getEnterExamFailureRate() {
            long total = enterExamSuccess + enterExamFailure;
            return total > 0 ? (enterExamFailure * 100.0) / total : 0.0;
        }
        
        public double getSubmitExamFailureRate() {
            long total = submitExamSuccess + submitExamFailure;
            return total > 0 ? (submitExamFailure * 100.0) / total : 0.0;
        }
    }
}

