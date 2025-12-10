package com.exam.online.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ExamResponse {
    private Long id;
    private String examName;
    private String examDescription;
    private Integer examType;
    private Integer status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer duration;
}

