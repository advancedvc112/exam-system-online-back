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
    
    /**
     * 学生进入考试
     */
    @Transactional
    public ExamParticipantDO enterExam(Long examId, Long studentId) {
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
        String tokenKey = com.exam.online.util.RedisKeyUtil.getExamTokenKey(examId, studentId);
        
        long ttlSeconds = 30 * 60; // 默认30分钟
        if (exam.getEndTime() != null) {
            long secondsUntilEnd = java.time.Duration.between(now, exam.getEndTime()).getSeconds();
            if (secondsUntilEnd > 0) {
                ttlSeconds = secondsUntilEnd + 30 * 60; // 考试结束时间 + 30分钟缓冲
            }
        }
        
        // 使用SETNX原子操作，如果key已存在则返回false
        Boolean success = redisService.setIfAbsent(tokenKey, token, ttlSeconds);
        if (Boolean.FALSE.equals(success)) {
            throw new IllegalArgumentException("您已进入考试，不允许重复进入");
        }
        
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
    }
    
    /**
     * 提交考试
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
            return;
        }
        
        participant.setStatus(2); // 已提交
        participant.setSubmitTime(LocalDateTime.now());
        participant.setUpdatedAt(LocalDateTime.now());
        examParticipantMapper.updateById(participant);
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

