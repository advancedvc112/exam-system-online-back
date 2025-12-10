package com.exam.online.service;

import com.exam.online.dal.dataobject.ExamDO;
import com.exam.online.dal.dataobject.ExamQuestionDO;
import com.exam.online.dal.mapper.ExamMapper;
import com.exam.online.dal.mapper.ExamQuestionMapper;
import com.exam.online.dto.ExamAddQuestionsRequest;
import com.exam.online.dto.ExamCreateRequest;
import com.exam.online.dto.ExamResponse;
import com.exam.online.dto.ExamQuestionItemRequest;
import com.exam.online.dto.ExamStatusUpdateRequest;
import com.exam.online.dto.ExamUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamMapper examMapper;
    private final ExamQuestionMapper examQuestionMapper;

    public ExamResponse createExam(ExamCreateRequest request) {
        validateTimeRange(request.getStartTime(), request.getEndTime());

        ExamDO entity = new ExamDO();
        entity.setExamName(request.getExamName());
        entity.setExamDescription(request.getExamDescription());
        entity.setExamType(request.getExamType());
        entity.setStatus(request.getStatus());
        entity.setStartTime(request.getStartTime());
        entity.setEndTime(request.getEndTime());
        entity.setDuration(request.getDuration());
        entity.setCreatorId(request.getCreatorId());

        // 默认值留待后续完善组卷和统计
        entity.setTotalScore(0);
        entity.setQuestionCount(0);
        entity.setIsDelete(0);

        examMapper.insert(entity);

        return new ExamResponse(
                entity.getId(),
                entity.getExamName(),
                entity.getExamDescription(),
                entity.getExamType(),
                entity.getStatus(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getDuration()
        );
    }

    @Transactional
    public void updateExam(Long examId, ExamUpdateRequest request) {
        if (examId == null || examId <= 0) {
            throw new IllegalArgumentException("考试ID不合法");
        }
        ExamDO exam = examMapper.selectById(examId);
        if (exam == null || (exam.getIsDelete() != null && exam.getIsDelete() == 1)) {
            throw new IllegalArgumentException("考试不存在或已被删除");
        }

        // 更新考试基础信息
        boolean hasExamUpdate = false;
        if (request.getExamName() != null) {
            exam.setExamName(request.getExamName());
            hasExamUpdate = true;
        }
        if (request.getExamDescription() != null) {
            exam.setExamDescription(request.getExamDescription());
            hasExamUpdate = true;
        }
        if (request.getExamType() != null) {
            exam.setExamType(request.getExamType());
            hasExamUpdate = true;
        }
        if (request.getStatus() != null) {
            exam.setStatus(request.getStatus());
            hasExamUpdate = true;
        }
        if (request.getStartTime() != null) {
            exam.setStartTime(request.getStartTime());
            hasExamUpdate = true;
        }
        if (request.getEndTime() != null) {
            exam.setEndTime(request.getEndTime());
            hasExamUpdate = true;
        }
        if (request.getDuration() != null) {
            exam.setDuration(request.getDuration());
            hasExamUpdate = true;
        }
        if (exam.getStartTime() != null && exam.getEndTime() != null) {
            validateTimeRange(exam.getStartTime(), exam.getEndTime());
        }

        // 若提供题目列表，则整体替换
        List<ExamQuestionItemRequest> items = request.getItems();
        if (items != null) {
            if (items.isEmpty()) {
                throw new IllegalArgumentException("题目列表不能为空");
            }

            // 校验请求中的 sort_order 唯一性，题目ID 唯一性
            Set<Integer> sortOrders = new HashSet<>();
            Set<Long> questionIds = new HashSet<>();
            for (ExamQuestionItemRequest item : items) {
                if (item.getSortOrder() == null) {
                    throw new IllegalArgumentException("sort_order 不能为空（用于定位待替换的题目）");
                }
                if (!sortOrders.add(item.getSortOrder())) {
                    throw new IllegalArgumentException("sort_order 重复：" + item.getSortOrder());
                }
                if (!questionIds.add(item.getQuestionId())) {
                    throw new IllegalArgumentException("题目ID重复：" + item.getQuestionId());
                }
            }

            // 查出现有试卷题目映射，按 sort_order 定位
            List<ExamQuestionDO> existing = examQuestionMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ExamQuestionDO>()
                            .eq(ExamQuestionDO::getExamId, examId)
            );
            var existingMap = existing.stream()
                    .collect(Collectors.toMap(ExamQuestionDO::getSortOrder, q -> q));

            for (ExamQuestionItemRequest item : items) {
                ExamQuestionDO relation = existingMap.get(item.getSortOrder());
                if (relation == null) {
                    throw new IllegalArgumentException("未找到 sort_order 对应的题目：" + item.getSortOrder());
                }
                relation.setQuestionId(item.getQuestionId());
                relation.setQuestionScore(item.getQuestionScore());
                relation.setGroupId(item.getGroupId());
                // sort_order 作为定位字段，此处保持原值
                examQuestionMapper.updateById(relation);
            }

            // 重新计算总分（题量不变）
            List<ExamQuestionDO> afterUpdate = examQuestionMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ExamQuestionDO>()
                            .eq(ExamQuestionDO::getExamId, examId)
            );
            int totalScore = afterUpdate.stream()
                    .mapToInt(ExamQuestionDO::getQuestionScore)
                    .sum();
            exam.setQuestionCount(afterUpdate.size());
            exam.setTotalScore(totalScore);
            hasExamUpdate = true; // 总分变动
        }

        if (!hasExamUpdate) {
            throw new IllegalArgumentException("未提供需要更新的内容");
        }

        examMapper.updateById(exam);
    }

    @Transactional
    public void updateStatus(Long examId, ExamStatusUpdateRequest request) {
        if (examId == null || examId <= 0) {
            throw new IllegalArgumentException("考试ID不合法");
        }
        if (request.getUserRole() == null || (request.getUserRole() != 2 && request.getUserRole() != 3)) {
            throw new IllegalArgumentException("仅教师或管理员可修改考试状态和时间");
        }

        ExamDO exam = examMapper.selectById(examId);
        if (exam == null || (exam.getIsDelete() != null && exam.getIsDelete() == 1)) {
            throw new IllegalArgumentException("考试不存在或已被删除");
        }

        if (request.getStatus() != null) {
            exam.setStatus(request.getStatus());
        }
        if (request.getStartTime() != null) {
            exam.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            exam.setEndTime(request.getEndTime());
        }
        if (exam.getStartTime() != null && exam.getEndTime() != null) {
            validateTimeRange(exam.getStartTime(), exam.getEndTime());
        }

        examMapper.updateById(exam);
    }

    @Transactional
    public void addQuestions(Long examId, ExamAddQuestionsRequest request) {
        if (examId == null || examId <= 0) {
            throw new IllegalArgumentException("考试ID不合法");
        }
        List<ExamQuestionItemRequest> items = request.getItems();
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("题目列表不能为空");
        }

        ExamDO exam = examMapper.selectById(examId);
        if (exam == null || (exam.getIsDelete() != null && exam.getIsDelete() == 1)) {
            throw new IllegalArgumentException("考试不存在或已被删除");
        }

        // 检查请求内重复题目
        Set<Long> inputIds = new HashSet<>();
        for (ExamQuestionItemRequest item : items) {
            if (!inputIds.add(item.getQuestionId())) {
                throw new IllegalArgumentException("题目ID重复：" + item.getQuestionId());
            }
        }

        // 检查数据库中已存在的关联，避免违反唯一约束
        long existsCount = examQuestionMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ExamQuestionDO>()
                        .eq(ExamQuestionDO::getExamId, examId)
                        .in(ExamQuestionDO::getQuestionId, inputIds)
        );
        if (existsCount > 0) {
            throw new IllegalArgumentException("存在已添加的题目，请勿重复添加");
        }

        int sortSeed = 1;
        if (!items.isEmpty()) {
            Integer maxSort = examQuestionMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ExamQuestionDO>()
                            .eq(ExamQuestionDO::getExamId, examId)
            ).intValue();
            sortSeed = maxSort + 1;
        }

        int totalAddedScore = 0;
        int idx = 0;
        for (ExamQuestionItemRequest item : items) {
            ExamQuestionDO relation = new ExamQuestionDO();
            relation.setExamId(examId);
            relation.setQuestionId(item.getQuestionId());
            relation.setQuestionScore(item.getQuestionScore());
            relation.setGroupId(item.getGroupId());
            int sortOrder = item.getSortOrder() != null ? item.getSortOrder() : (sortSeed + idx);
            relation.setSortOrder(sortOrder);
            examQuestionMapper.insert(relation);
            totalAddedScore += item.getQuestionScore();
            idx++;
        }

        // 更新考试总分与题量
        int existingCount = exam.getQuestionCount() == null ? 0 : exam.getQuestionCount();
        int existingScore = exam.getTotalScore() == null ? 0 : exam.getTotalScore();
        exam.setQuestionCount(existingCount + items.size());
        exam.setTotalScore(existingScore + totalAddedScore);
        examMapper.updateById(exam);
    }

    private void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && !end.isAfter(start)) {
            throw new IllegalArgumentException("结束时间必须晚于开始时间");
        }
    }
}

