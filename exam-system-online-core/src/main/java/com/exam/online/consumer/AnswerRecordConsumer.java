package com.exam.online.consumer;

import com.exam.online.dal.dataobject.AnswerRecordDO;
import com.exam.online.dal.dataobject.ExamParticipantDO;
import com.exam.online.dal.dataobject.ExamQuestionDO;
import com.exam.online.dal.mapper.AnswerRecordMapper;
import com.exam.online.dal.mapper.ExamParticipantMapper;
import com.exam.online.dal.mapper.ExamQuestionMapper;
import com.exam.online.dto.AnswerMessage;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@RocketMQMessageListener(
    topic = "${rocketmq.answer.topic:exam-answer-save}",
    consumerGroup = "${rocketmq.answer.consumer-group:exam-answer-consumer-group}"
)
public class AnswerRecordConsumer implements RocketMQListener<AnswerMessage> {
    
    private final AnswerRecordMapper answerRecordMapper;
    private final ExamParticipantMapper examParticipantMapper;
    private final ExamQuestionMapper examQuestionMapper;
    
    @Override
    @Transactional
    public void onMessage(AnswerMessage message) {
        try {
            log.debug("收到答题记录消息: examId={}, studentId={}, sortOrder={}", 
                message.getExamId(), message.getStudentId(), message.getSortOrder());
            
            // 1. 获取或创建participant记录
            ExamParticipantDO participant = examParticipantMapper.selectOne(
                new LambdaQueryWrapper<ExamParticipantDO>()
                    .eq(ExamParticipantDO::getExamId, message.getExamId())
                    .eq(ExamParticipantDO::getUserId, message.getStudentId())
                    .orderByDesc(ExamParticipantDO::getCreatedAt)
                    .last("LIMIT 1")
            );
            
            if (participant == null) {
                log.warn("未找到考试参与记录: examId={}, studentId={}", 
                    message.getExamId(), message.getStudentId());
                return;
            }
            
            // 2. 查询或创建答题记录
            AnswerRecordDO answerRecord = answerRecordMapper.selectOne(
                new LambdaQueryWrapper<AnswerRecordDO>()
                    .eq(AnswerRecordDO::getParticipantId, participant.getId())
                    .eq(AnswerRecordDO::getQuestionId, message.getQuestionId())
            );
            
            LocalDateTime now = LocalDateTime.now();
            
            // 获取题目分数
            ExamQuestionDO examQuestion = examQuestionMapper.selectOne(
                new LambdaQueryWrapper<ExamQuestionDO>()
                    .eq(ExamQuestionDO::getExamId, message.getExamId())
                    .eq(ExamQuestionDO::getQuestionId, message.getQuestionId())
                    .last("LIMIT 1")
            );
            
            Integer questionScore = 0;
            if (examQuestion != null && examQuestion.getQuestionScore() != null) {
                questionScore = examQuestion.getQuestionScore();
            }
            
            if (answerRecord == null) {
                // 创建新记录
                answerRecord = new AnswerRecordDO();
                answerRecord.setParticipantId(participant.getId());
                answerRecord.setExamId(message.getExamId());
                answerRecord.setQuestionId(message.getQuestionId());
                answerRecord.setUserAnswer(message.getAnswer());
                answerRecord.setChangeTimes(1);
                answerRecord.setIsCorrect(0);
                answerRecord.setQuestionScore(questionScore);
                answerRecord.setTrueScore(BigDecimal.ZERO);
                answerRecord.setReviewStatus(0);
                answerRecord.setCreatedTime(now);
                answerRecord.setUpdatedTime(now);
                answerRecordMapper.insert(answerRecord);
            } else {
                // 更新现有记录
                answerRecord.setUserAnswer(message.getAnswer());
                answerRecord.setChangeTimes(
                    (answerRecord.getChangeTimes() == null ? 0 : answerRecord.getChangeTimes()) + 1
                );
                answerRecord.setUpdatedTime(now);
                answerRecordMapper.updateById(answerRecord);
            }
            
            log.debug("答题记录保存成功: participantId={}, questionId={}", 
                participant.getId(), message.getQuestionId());
                
        } catch (Exception e) {
            log.error("处理答题记录消息失败: {}", message, e);
            // RocketMQ会自动重试，超过重试次数会进入死信队列
            throw e;
        }
    }
}

