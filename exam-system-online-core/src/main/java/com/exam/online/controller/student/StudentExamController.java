package com.exam.online.controller.student;

import com.exam.online.dto.ExamEnterRequest;
import com.exam.online.dto.ExamEnterResponse;
import com.exam.online.dto.SaveAnswerRequest;
import com.exam.online.dto.SubmitExamRequest;
import com.exam.online.dal.dataobject.ExamParticipantDO;
import com.exam.online.service.AnswerBufferService;
import com.exam.online.service.ExamAnswerService;
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
    
    /**
     * 进入考试
     */
    @PostMapping("/{examId}/enter")
    public ExamEnterResponse enterExam(
            @PathVariable("examId") Long examId,
            @Valid @RequestBody ExamEnterRequest request) {
        
        ExamParticipantDO participant = examParticipantService.enterExam(examId, request.getStudentId());
        
        // 从participant中获取token
        String token = participant.getAccessToken();
        
        return new ExamEnterResponse(token, "进入考试成功", participant.getId());
    }
    
    /**
     * 保存答题记录（带3秒缓冲）
     */
    @PostMapping("/{examId}/answers")
    public String saveAnswer(
            @PathVariable("examId") Long examId,
            @Valid @RequestBody SaveAnswerRequest request) {
        
        // 缓冲答题记录，3秒后自动写入Redis和MQ
        answerBufferService.bufferAnswer(
            examId,
            request.getStudentId(),
            request.getSortOrder(),
            request.getAnswer()
        );
        
        return "答题记录已保存";
    }
    
    /**
     * 提交考试
     */
    @PostMapping("/{examId}/submit")
    public String submitExam(
            @PathVariable("examId") Long examId,
            @Valid @RequestBody SubmitExamRequest request) {
        
        // 1. 强制刷新所有缓冲的答题记录
        answerBufferService.flushAll(examId, request.getStudentId());
        
        // 2. 等待一小段时间确保MQ消息发送完成（实际生产环境可以用更好的方式）
        try {
            Thread.sleep(500); // 等待500ms
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 3. 更新提交状态
        examParticipantService.submitExam(examId, request.getStudentId());
        
        return "考试提交成功";
    }
    
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgumentException(IllegalArgumentException ex) {
        return ex.getMessage();
    }
}

