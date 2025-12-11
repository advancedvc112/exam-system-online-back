package com.exam.online.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("answer_records")
public class AnswerRecordDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long participantId;
    private Long examId;
    private Long questionId;
    private String userAnswer;
    private Integer changeTimes;
    private String correctAnswer;
    private Integer isCorrect;
    private Integer questionScore;
    private BigDecimal trueScore;
    private Integer reviewStatus;
    private Long reviewerId;
    private LocalDateTime reviewTime;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}

