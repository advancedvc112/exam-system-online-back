package com.exam.online.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class QuestionUpdateRequest {

    @NotNull(message = "题目ID不能为空")
    private Long id;

    private Integer questionCategory;

    private String questionContent;

    private String questionOptions;

    private String questionAnswer;

    @Size(max = 500, message = "题目标签长度不能超过500")
    private String questionTags;
}

