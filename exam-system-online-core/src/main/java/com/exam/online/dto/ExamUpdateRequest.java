package com.exam.online.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ExamUpdateRequest {

    private String examName;

    private String examDescription;

    private Integer examType;

    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @FutureOrPresent(message = "开始时间不能早于当前时间")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Future(message = "结束时间必须大于当前时间")
    private LocalDateTime endTime;

    @Min(value = 1, message = "考试时长必须大于0")
    private Integer duration;

    /**
     * 可选：同时替换试卷题目列表
     */
    @Valid
    private List<ExamQuestionItemRequest> items;
}

