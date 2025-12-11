package com.exam.online.service;

import com.exam.online.dal.dataobject.ExamQuestionDO;
import com.exam.online.dal.mapper.ExamQuestionMapper;
import com.exam.online.dto.AnswerMessage;
import com.exam.online.util.RedisKeyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExamAnswerService {
    
    private final RedisService redisService;
    private final RocketMQTemplate rocketMQTemplate;
    private final ExamQuestionMapper examQuestionMapper;
    
    @Value("${rocketmq.answer.topic:exam-answer-save}")
    private String answerTopic;
    
    /**
     * 保存答题记录到Redis和MQ
     */
    public void saveAnswerToRedisAndMQ(Long examId, Long studentId, Integer sortOrder, String answer) {
        // 1. 保存到Redis，TTL=60秒，每次更新自动刷新
        String answerKey = RedisKeyUtil.getAnswerKey(examId, studentId, sortOrder);
        redisService.setWithMilliseconds(answerKey, answer, 60000); // 60秒
        
        // 2. 查询questionId
        ExamQuestionDO examQuestion = examQuestionMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ExamQuestionDO>()
                .eq(ExamQuestionDO::getExamId, examId)
                .eq(ExamQuestionDO::getSortOrder, sortOrder)
                .last("LIMIT 1")
        );
        
        if (examQuestion == null) {
            log.warn("未找到题目: examId={}, sortOrder={}", examId, sortOrder);
            return;
        }
        
        // 3. 发送到RocketMQ
        AnswerMessage message = new AnswerMessage();
        message.setExamId(examId);
        message.setStudentId(studentId);
        message.setSortOrder(sortOrder);
        message.setQuestionId(examQuestion.getQuestionId());
        message.setAnswer(answer);
        message.setTimestamp(LocalDateTime.now());
        
        try {
            rocketMQTemplate.convertAndSend(answerTopic, message);
            log.debug("答题记录已发送到MQ: examId={}, studentId={}, sortOrder={}", examId, studentId, sortOrder);
        } catch (Exception e) {
            log.error("发送答题记录到MQ失败: examId={}, studentId={}, sortOrder={}", examId, studentId, sortOrder, e);
            // 这里可以选择重试或者记录到失败队列
        }
    }
    
    /**
     * 从Redis获取所有答题记录
     */
    public java.util.Map<Integer, String> getAllAnswers(Long examId, Long studentId) {
        String pattern = RedisKeyUtil.getAnswerPattern(examId, studentId);
        java.util.Set<String> keys = redisService.keys(pattern);
        
        java.util.Map<Integer, String> answers = new java.util.HashMap<>();
        for (String key : keys) {
            // 解析sortOrder
            String[] parts = key.split(":");
            if (parts.length >= 4) {
                try {
                    Integer sortOrder = Integer.parseInt(parts[3]);
                    String answer = redisService.get(key);
                    if (answer != null) {
                        answers.put(sortOrder, answer);
                    }
                } catch (NumberFormatException e) {
                    log.warn("解析sortOrder失败: key={}", key);
                }
            }
        }
        return answers;
    }
}

