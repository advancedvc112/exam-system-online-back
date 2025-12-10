package com.exam.online.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExamCreateRequest {

    @NotBlank(message = "考试名称不能为空")
    private String examName;

    private String examDescription;

    @NotNull(message = "考试类型不能为空")
    private Integer examType;

    @NotNull(message = "考试状态不能为空")
    private Integer status;

    @NotNull(message = "开始时间不能为空")
    @FutureOrPresent(message = "开始时间不能早于当前时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    @Future(message = "结束时间必须大于当前时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @NotNull(message = "考试时长不能为空")
    @Min(value = 1, message = "考试时长必须大于0")
    private Integer duration;

    @NotNull(message = "创建者不能为空")
    private Long creatorId;
}

