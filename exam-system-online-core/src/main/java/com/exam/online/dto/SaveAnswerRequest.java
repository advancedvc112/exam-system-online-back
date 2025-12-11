package com.exam.online.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SaveAnswerRequest {
    @NotNull(message = "学生ID不能为空")
    private Long studentId;
    
    @NotNull(message = "题目序号不能为空")
    private Integer sortOrder;
    
    private String answer;
}

