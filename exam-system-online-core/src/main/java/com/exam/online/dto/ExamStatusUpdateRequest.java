package com.exam.online.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExamStatusUpdateRequest {

    /**
     * 请求者角色：2=教师，3=管理员；其余拒绝
     */
    @NotNull(message = "用户角色不能为空")
    private Integer userRole;

    /**
     * 考试状态（1：未开始 2：进行中 3：已结束 4：已归档）
     */
    @NotNull(message = "考试状态不能为空")
    private Integer status;

    @FutureOrPresent(message = "开始时间不能早于当前时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @Future(message = "结束时间必须大于当前时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}

