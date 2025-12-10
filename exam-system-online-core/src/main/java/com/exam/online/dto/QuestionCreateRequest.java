package com.exam.online.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class QuestionCreateRequest {

    @NotNull(message = "题型不能为空")
    private Integer questionCategory;

    @NotBlank(message = "题目内容不能为空")
    private String questionContent;

    private String questionOptions;

    @NotBlank(message = "题目答案不能为空")
    private String questionAnswer;

    @NotBlank(message = "题目标签不能为空")
    @Size(max = 500, message = "题目标签长度不能超过500")
    private String questionTags;
}

