package com.exam.online.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("exam_participants")
public class ExamParticipantDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long examId;
    private Long userId;
    private Integer attempt;
    private LocalDateTime joinTime;
    private LocalDateTime startTime;
    private LocalDateTime submitTime;
    private Integer status;
    private String accessToken;
    private String connectionId;
    private Integer isConnected;
    private LocalDateTime disconnectTime;
    private Integer reconnectCount;
    private Integer durationUsed;
    private String ipAddress;
    private Integer cheatSuspect;
    private String cheatEvidence;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

