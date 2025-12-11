package com.exam.online.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnswerMessage {
    private Long examId;
    private Long studentId;
    private Integer sortOrder;
    private Long questionId;
    private String answer;
    private LocalDateTime timestamp;
}

