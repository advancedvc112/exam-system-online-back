package com.exam.online.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("questions_bank")
public class QuestionBankDO {
    @TableId
    private Long id;
    private Integer questionCategory;
    private String questionContent;
    private String questionOptions;
    private String questionAnswer;
    private String questionTags;
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

