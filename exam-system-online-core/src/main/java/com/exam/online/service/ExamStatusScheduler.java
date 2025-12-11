package com.exam.online.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.exam.online.dal.dataobject.ExamDO;
import com.exam.online.dal.mapper.ExamMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 定时根据时间窗口自动将考试置为进行中。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExamStatusScheduler {

    private static final int STATUS_NOT_STARTED = 1;
    private static final int STATUS_IN_PROGRESS = 2;

    private final ExamMapper examMapper;

    /**
     * 每隔固定时间扫描，将已到开始时间且未开始的考试置为进行中。
     */
    @Scheduled(
            fixedDelayString = "#{T(org.springframework.boot.convert.DurationStyle).detectAndParse('${exam.status.update-interval:60s}').toMillis()}",
            initialDelayString = "#{T(org.springframework.boot.convert.DurationStyle).detectAndParse('${exam.status.initial-delay:10s}').toMillis()}"
    )
    public void autoStartExams() {
        LocalDateTime now = LocalDateTime.now();

        LambdaUpdateWrapper<ExamDO> updateWrapper = new LambdaUpdateWrapper<ExamDO>()
                .eq(ExamDO::getStatus, STATUS_NOT_STARTED)
                .isNotNull(ExamDO::getStartTime)
                .le(ExamDO::getStartTime, now)
                .gt(ExamDO::getEndTime, now)
                .and(w -> w.isNull(ExamDO::getIsDelete).or().ne(ExamDO::getIsDelete, 1))
                .set(ExamDO::getStatus, STATUS_IN_PROGRESS)
                .set(ExamDO::getUpdateTime, now);

        int updated = examMapper.update(null, updateWrapper);
        if (updated > 0) {
            log.info("自动将 {} 场考试置为进行中", updated);
        }
    }
}

