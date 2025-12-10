package com.exam.online.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExamQuestionItemRequest {

    /**
     * 试卷题号（exam_questions 自增id），用于替换已有题目
     */
    private Long id;

    @NotNull(message = "题目ID不能为空")
    private Long questionId;

    @NotNull(message = "题目分数不能为空")
    @Min(value = 1, message = "题目分数必须大于0")
    private Integer questionScore;

    private Integer sortOrder;

    @NotNull(message = "题目组别不能为空")
    private Integer groupId;
}

