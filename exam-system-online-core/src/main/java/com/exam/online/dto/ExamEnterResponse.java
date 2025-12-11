package com.exam.online.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ExamEnterResponse {
    private String token;
    private String message;
    private Long participantId;
}

