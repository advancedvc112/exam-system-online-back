package com.exam.online.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExamEnterRequest {
    @NotNull(message = "学生ID不能为空")
    private Long studentId;
}

