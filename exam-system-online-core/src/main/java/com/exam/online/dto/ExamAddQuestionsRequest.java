package com.exam.online.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ExamAddQuestionsRequest {

    @NotEmpty(message = "题目列表不能为空")
    @Valid
    private List<ExamQuestionItemRequest> items;
}

