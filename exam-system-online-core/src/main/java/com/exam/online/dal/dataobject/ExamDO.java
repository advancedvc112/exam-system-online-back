package com.exam.online.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("exams")
public class ExamDO {
    @TableId
    private Long id;
    private String examName;
    private String examDescription;
    private Integer examType;
    private Integer status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer duration;
    private Integer totalScore;
    private Integer questionCount;
    private Long creatorId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDelete;
}

