package com.exam.online.controller.admin.controller.student;

import com.exam.online.annotation.UserRateLimit;
import com.exam.online.dto.ExamEnterRequest;
import com.exam.online.dto.ExamEnterResponse;
import com.exam.online.dto.Result;
import com.exam.online.dto.SaveAnswerRequest;
import com.exam.online.dto.SubmitExamRequest;
import com.exam.online.dal.dataobject.ExamParticipantDO;
import com.exam.online.service.AnswerBufferService;
import com.exam.online.service.DistributedLockService;
import com.exam.online.service.ExamParticipantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/student/exams")
@RequiredArgsConstructor
public class StudentExamController {
    
    private final ExamParticipantService examParticipantService;
    private final AnswerBufferService answerBufferService;
    private final DistributedLockService distributedLockService;
    
    /**
     * 进入考试（用户级别限流：每个学生每秒最多2次）
     */
    @UserRateLimit(interfaceType = "enter-exam")
    @PostMapping("/{examId}/enter")
    public Result<ExamEnterResponse> enterExam(
            @PathVariable("examId") Long examId,
            @Valid @RequestBody ExamEnterRequest request) {
        
        log.info("学生进入考试请求 examId={}, studentId={}", examId, request.getStudentId());
        ExamParticipantDO participant = examParticipantService.enterExam(examId, request.getStudentId());
        
        // 从participant中获取token
        String token = participant.getAccessToken();
        log.info("学生进入考试成功，返回token: examId={}, studentId={}, participantId={}, token={}",
                examId, request.getStudentId(), participant.getId(), token);
        
        return Result.success(new ExamEnterResponse(token, "进入考试成功", participant.getId()));
    }
    
    /**
     * 保存答题记录（带3秒缓冲，用户级别限流：每个学生每秒最多10次）
     */
    @UserRateLimit(interfaceType = "save-answer")
    @PostMapping("/{examId}/answers")
    public Result<Void> saveAnswer(
            @PathVariable("examId") Long examId,
            @Valid @RequestBody SaveAnswerRequest request) {
        
        log.info("保存答题请求 examId={}, studentId={}, sortOrder={}", examId, request.getStudentId(), request.getSortOrder());
        // 缓冲答题记录，3秒后自动写入Redis和MQ
        answerBufferService.bufferAnswer(
            examId,
            request.getStudentId(),
            request.getSortOrder(),
            request.getAnswer()
        );
        
        return Result.success("答题记录已保存");
    }
    
    /**
     * 提交考试（使用分布式锁优化，用户级别限流：每个学生每秒最多1次）
     */
    @UserRateLimit(interfaceType = "submit-exam")
    @PostMapping("/{examId}/submit")
    public Result<Void> submitExam(
            @PathVariable("examId") Long examId,
            @Valid @RequestBody SubmitExamRequest request) {
        
        log.info("提交考试请求 examId={}, studentId={}", examId, request.getStudentId());
        
        // 获取分布式锁
        boolean lockAcquired = distributedLockService.tryLockSubmitExam(examId, request.getStudentId(), 10);
        
        if (!lockAcquired) {
            // 获取锁失败，说明有其他请求正在提交
            log.warn("获取提交考试锁失败: examId={}, studentId={}", examId, request.getStudentId());
            // 查询当前状态
            com.exam.online.dal.dataobject.ExamParticipantDO participant = 
                examParticipantService.getParticipant(examId, request.getStudentId());
            if (participant != null && participant.getStatus() != null && participant.getStatus() == 2) {
                return Result.success("考试已提交");
            } else {
                return Result.failure("提交中，请勿重复提交");
            }
        }
        
        // 获取锁成功，执行提交逻辑
        org.redisson.api.RLock lock = distributedLockService.getSubmitExamLock(examId, request.getStudentId());
        long lockStartTime = System.currentTimeMillis();
        try {
            // 1. 检查是否已提交（幂等检查）
            com.exam.online.dal.dataobject.ExamParticipantDO participant = 
                examParticipantService.getParticipant(examId, request.getStudentId());
            if (participant != null && participant.getStatus() != null && participant.getStatus() == 2) {
                log.info("考试已提交，幂等返回: examId={}, studentId={}", examId, request.getStudentId());
                return Result.success("考试已提交");
            }
            
            // 2. 强制刷新所有缓冲的答题记录
            answerBufferService.flushAll(examId, request.getStudentId());
            log.info("已刷新所有缓冲答题记录: examId={}, studentId={}", examId, request.getStudentId());
            
            // 3. 等待一小段时间确保MQ消息发送完成（实际生产环境可以用更好的方式）
            try {
                Thread.sleep(500); // 等待500ms
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // 4. 更新提交状态
            examParticipantService.submitExam(examId, request.getStudentId());
            
            log.info("考试提交成功: examId={}, studentId={}", examId, request.getStudentId());
            return Result.success("考试提交成功");
        } finally {
            // 释放分布式锁（记录持有时间）
            distributedLockService.unlockSubmitExam(lock, lockStartTime);
        }
    }
    
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException ex) {
        return Result.failure(ex.getMessage());
    }
}

