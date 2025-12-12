package com.exam.online.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.online.dal.dataobject.ExamDO;
import com.exam.online.dal.dataobject.ExamParticipantDO;
import com.exam.online.dal.dataobject.SystemUserDO;
import com.exam.online.dal.mapper.ExamMapper;
import com.exam.online.dal.mapper.ExamParticipantMapper;
import com.exam.online.dal.mapper.SystemUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExamParticipantService {
    
    private final ExamParticipantMapper examParticipantMapper;
    private final ExamMapper examMapper;
    private final SystemUserMapper systemUserMapper;
    private final RedisService redisService;
    private final DistributedLockService distributedLockService;
    
    /**
     * 学生进入考试（使用分布式锁优化）
     */
    @Transactional
    public ExamParticipantDO enterExam(Long examId, Long studentId) {
        // 快速检查：如果Redis已有token，直接拒绝重入（无需加锁）
        String tokenKeyFast = com.exam.online.util.RedisKeyUtil.getExamTokenKey(examId, studentId);
        String existingTokenFast = redisService.get(tokenKeyFast);
        if (existingTokenFast != null) {
            log.warn("重复进入考试被拒绝（快速检查）: examId={}, studentId={}, key={}", examId, studentId, tokenKeyFast);
            throw new IllegalArgumentException("您已进入考试，不允许重复进入");
        }

        // 进入锁前先校验身份，非学生直接拒绝，减少锁竞争
        SystemUserDO preCheckUser = systemUserMapper.selectById(studentId);
        if (preCheckUser == null || preCheckUser.getIsDeleted() != null && preCheckUser.getIsDeleted() == 1) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (preCheckUser.getUserRole() == null || preCheckUser.getUserRole() != 1) {
            throw new IllegalArgumentException("只有学生可以进入考试");
        }

        // 获取分布式锁
        boolean lockAcquired = distributedLockService.tryLockEnterExam(examId, studentId, 30);
        
        if (!lockAcquired) {
            // 获取锁失败，说明有其他请求正在处理，等待后查询现有token
            log.warn("获取进入考试锁失败，尝试查询现有token: examId={}, studentId={}", examId, studentId);
            try {
                Thread.sleep(100); // 等待100ms
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // 查询Redis中是否已有token
            String tokenKey = com.exam.online.util.RedisKeyUtil.getExamTokenKey(examId, studentId);
            String existingToken = redisService.get(tokenKey);
            if (existingToken != null) {
                // 查询参与记录并返回
                ExamParticipantDO participant = examParticipantMapper.selectOne(
                    new LambdaQueryWrapper<ExamParticipantDO>()
                        .eq(ExamParticipantDO::getExamId, examId)
                        .eq(ExamParticipantDO::getUserId, studentId)
                        .orderByDesc(ExamParticipantDO::getCreatedAt)
                        .last("LIMIT 1")
                );
                if (participant != null) {
                    log.info("从现有记录返回token: examId={}, studentId={}", examId, studentId);
                    return participant;
                }
            }
            
            throw new IllegalArgumentException("系统繁忙，请稍后重试");
        }
        
        // 获取锁成功，执行进入考试逻辑
        org.redisson.api.RLock lock = distributedLockService.getEnterExamLock(examId, studentId);
        long lockStartTime = System.currentTimeMillis();
        try {
            // 加锁后再次检查，防止并发窗口重复进入
            String tokenKey = com.exam.online.util.RedisKeyUtil.getExamTokenKey(examId, studentId);
            String existingToken = redisService.get(tokenKey);
            if (existingToken != null) {
                log.warn("重复进入考试被拒绝（锁内检查）: examId={}, studentId={}, key={}", examId, studentId, tokenKey);
                throw new IllegalArgumentException("您已进入考试，不允许重复进入");
            }
            
            // 1. 校验学生身份
            SystemUserDO user = systemUserMapper.selectById(studentId);
            if (user == null || user.getIsDeleted() != null && user.getIsDeleted() == 1) {
                throw new IllegalArgumentException("用户不存在");
            }
            if (user.getUserRole() == null || user.getUserRole() != 1) {
                throw new IllegalArgumentException("只有学生可以进入考试");
            }
            
            // 2. 校验考试状态
            ExamDO exam = examMapper.selectById(examId);
            if (exam == null || (exam.getIsDelete() != null && exam.getIsDelete() == 1)) {
                throw new IllegalArgumentException("考试不存在或已被删除");
            }
            
            LocalDateTime now = LocalDateTime.now();
            if (exam.getStartTime() != null && now.isBefore(exam.getStartTime())) {
                throw new IllegalArgumentException("考试尚未开始");
            }
            if (exam.getEndTime() != null && now.isAfter(exam.getEndTime())) {
                throw new IllegalArgumentException("考试已结束");
            }
            
            // 3. 生成token并存入Redis（使用SETNX防重复进入）
            String token = UUID.randomUUID().toString().replace("-", "");
            
            long ttlSeconds = 30 * 60; // 默认30分钟
            if (exam.getEndTime() != null) {
                long secondsUntilEnd = java.time.Duration.between(now, exam.getEndTime()).getSeconds();
                if (secondsUntilEnd > 0) {
                    ttlSeconds = secondsUntilEnd + 30 * 60; // 考试结束时间 + 30分钟缓冲
                }
            }

            log.info("准备写入考试token到Redis: examId={}, studentId={}, key={}, ttlSeconds={}, token={}",
                    examId, studentId, tokenKey, ttlSeconds, token);
            
            // 使用SETNX原子操作，如果key已存在则返回false
            Boolean success = redisService.setIfAbsent(tokenKey, token, ttlSeconds);
            if (Boolean.FALSE.equals(success)) {
                log.warn("重复进入考试被拒绝: examId={}, studentId={}, key={}", examId, studentId, tokenKey);
                throw new IllegalArgumentException("您已进入考试，不允许重复进入");
            }
            log.info("考试token写入Redis成功: examId={}, studentId={}, key={}", examId, studentId, tokenKey);
            
            // 5. 创建或更新参与记录
            ExamParticipantDO participant = examParticipantMapper.selectOne(
                new LambdaQueryWrapper<ExamParticipantDO>()
                    .eq(ExamParticipantDO::getExamId, examId)
                    .eq(ExamParticipantDO::getUserId, studentId)
                    .orderByDesc(ExamParticipantDO::getCreatedAt)
                    .last("LIMIT 1")
            );
            
            if (participant == null) {
                participant = new ExamParticipantDO();
                participant.setExamId(examId);
                participant.setUserId(studentId);
                participant.setAttempt(1);
                participant.setJoinTime(now);
                participant.setStartTime(now);
                participant.setStatus(1); // 进行中
                participant.setAccessToken(token);
                participant.setIsConnected(1);
                participant.setCreatedAt(now);
                participant.setUpdatedAt(now);
                examParticipantMapper.insert(participant);
            } else {
                // 如果已存在，更新token和状态
                participant.setAccessToken(token);
                participant.setJoinTime(now);
                participant.setStartTime(now);
                participant.setStatus(1);
                participant.setIsConnected(1);
                participant.setUpdatedAt(now);
                examParticipantMapper.updateById(participant);
            }
            
            return participant;
        } finally {
            // 释放分布式锁（记录持有时间）
            distributedLockService.unlockEnterExam(lock, lockStartTime);
        }
    }
    
    /**
     * 提交考试（内部方法，不加锁，由Controller层保证锁）
     */
    @Transactional
    public void submitExam(Long examId, Long studentId) {
        ExamParticipantDO participant = examParticipantMapper.selectOne(
            new LambdaQueryWrapper<ExamParticipantDO>()
                .eq(ExamParticipantDO::getExamId, examId)
                .eq(ExamParticipantDO::getUserId, studentId)
                .orderByDesc(ExamParticipantDO::getCreatedAt)
                .last("LIMIT 1")
        );
        
        if (participant == null) {
            throw new IllegalArgumentException("未找到考试参与记录");
        }
        
        if (participant.getStatus() != null && participant.getStatus() == 2) {
            // 已提交，幂等处理
            log.info("考试已提交，幂等返回: examId={}, studentId={}", examId, studentId);
            return;
        }
        
        participant.setStatus(2); // 已提交
        participant.setSubmitTime(LocalDateTime.now());
        participant.setUpdatedAt(LocalDateTime.now());
        examParticipantMapper.updateById(participant);
        
        log.info("考试提交成功: examId={}, studentId={}", examId, studentId);
    }
    
    /**
     * 根据examId和studentId获取参与记录
     */
    public ExamParticipantDO getParticipant(Long examId, Long studentId) {
        return examParticipantMapper.selectOne(
            new LambdaQueryWrapper<ExamParticipantDO>()
                .eq(ExamParticipantDO::getExamId, examId)
                .eq(ExamParticipantDO::getUserId, studentId)
                .orderByDesc(ExamParticipantDO::getCreatedAt)
                .last("LIMIT 1")
        );
    }
}

