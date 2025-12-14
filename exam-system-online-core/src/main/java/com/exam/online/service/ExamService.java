package com.exam.online.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.online.dal.dataobject.ExamDO;
import com.exam.online.dal.dataobject.ExamQuestionDO;
import com.exam.online.dal.dataobject.QuestionBankDO;
import com.exam.online.dal.mapper.ExamMapper;
import com.exam.online.dal.mapper.ExamQuestionMapper;
import com.exam.online.dal.mapper.QuestionBankMapper;
import com.exam.online.dto.ExamAddQuestionsRequest;
import com.exam.online.dto.ExamCreateRequest;
import com.exam.online.dto.ExamResponse;
import com.exam.online.dto.ExamQuestionItemRequest;
import com.exam.online.dto.ExamRandomGenerateRequest;
import com.exam.online.dto.ExamRandomGenerateResponse;
import com.exam.online.dto.ExamStatusUpdateRequest;
import com.exam.online.dto.ExamUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamMapper examMapper;
    private final ExamQuestionMapper examQuestionMapper;
    private final QuestionBankMapper questionBankMapper;
    private static final long MAX_PAGE_SIZE = 100L;

    public List<ExamResponse> listAll(int page, int pageSize) {
        int safePage = Math.max(page, 1);
        long safeSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);

        LambdaQueryWrapper<ExamDO> wrapper = new LambdaQueryWrapper<ExamDO>()
                .eq(ExamDO::getIsDelete, 0)
                .orderByAsc(ExamDO::getId);

        Page<ExamDO> pageResult = examMapper.selectPage(new Page<>(safePage, safeSize), wrapper);

        return pageResult.getRecords().stream()
                .map(e -> new ExamResponse(
                        e.getId(),
                        e.getExamName(),
                        e.getExamDescription(),
                        e.getExamType(),
                        e.getStatus(),
                        e.getStartTime(),
                        e.getEndTime(),
                        e.getDuration()
                ))
                .toList();
    }

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

    @Transactional
    public ExamRandomGenerateResponse randomGenerateQuestions(Long examId, ExamRandomGenerateRequest request) {
        if (examId == null || examId <= 0) {
            throw new IllegalArgumentException("考试ID不合法");
        }
        if (!StringUtils.hasText(request.getQuestionTag())) {
            throw new IllegalArgumentException("题目标签不能为空");
        }

        ExamDO exam = examMapper.selectById(examId);
        if (exam == null || (exam.getIsDelete() != null && exam.getIsDelete() == 1)) {
            throw new IllegalArgumentException("考试不存在或已被删除");
        }

        // 查询该考试已添加的题目ID，避免重复添加
        List<ExamQuestionDO> existingQuestions = examQuestionMapper.selectList(
                new LambdaQueryWrapper<ExamQuestionDO>()
                        .eq(ExamQuestionDO::getExamId, examId)
                        .select(ExamQuestionDO::getQuestionId)
        );
        Set<Long> existingQuestionIds = existingQuestions.stream()
                .map(ExamQuestionDO::getQuestionId)
                .collect(Collectors.toSet());

        // 根据标签查询所有符合条件的题目
        LambdaQueryWrapper<QuestionBankDO> questionWrapper = new LambdaQueryWrapper<QuestionBankDO>()
                .eq(QuestionBankDO::getIsDeleted, 0)
                .like(QuestionBankDO::getQuestionTags, request.getQuestionTag());
        
        List<QuestionBankDO> allQuestions = questionBankMapper.selectList(questionWrapper);
        
        // 过滤掉已添加的题目
        List<QuestionBankDO> availableQuestions = allQuestions.stream()
                .filter(q -> !existingQuestionIds.contains(q.getId()))
                .collect(Collectors.toList());

        if (availableQuestions.isEmpty()) {
            throw new IllegalArgumentException("没有可用的题目，可能所有符合条件的题目都已添加到该考试中");
        }

        // 按题型分组
        var questionsByCategory = availableQuestions.stream()
                .collect(Collectors.groupingBy(QuestionBankDO::getQuestionCategory));

        // 确定题型配置
        List<ExamRandomGenerateRequest.QuestionTypeConfig> typeConfigs;
        if (request.getTypeConfigs() == null || request.getTypeConfigs().isEmpty()) {
            // 启用兜底机制
            typeConfigs = generateDefaultTypeConfigs(availableQuestions.size(), questionsByCategory);
        } else {
            typeConfigs = request.getTypeConfigs();
        }

        // 验证题型配置并抽取题目
        List<ExamQuestionItemRequest> items = new ArrayList<>();
        List<ExamRandomGenerateResponse.QuestionItem> questionItems = new ArrayList<>();
        int sortSeed = existingQuestions.isEmpty() ? 1 : 
                examQuestionMapper.selectCount(
                        new LambdaQueryWrapper<ExamQuestionDO>()
                                .eq(ExamQuestionDO::getExamId, examId)
                ).intValue() + 1;

        int currentSortOrder = sortSeed;
        int totalScore = 0;
        
        for (ExamRandomGenerateRequest.QuestionTypeConfig config : typeConfigs) {
            Integer category = config.getCategory();
            Integer count = config.getCount();
            Integer score = config.getScore();
            Integer groupId = config.getGroupId() != null ? config.getGroupId() : category; // 默认使用题型作为组别

            // 获取该题型的可用题目
            List<QuestionBankDO> categoryQuestions = questionsByCategory.getOrDefault(category, Collections.emptyList());
            
            if (categoryQuestions.isEmpty()) {
                throw new IllegalArgumentException(
                        String.format("题型%d没有可用的题目", category));
            }

            if (categoryQuestions.size() < count) {
                throw new IllegalArgumentException(
                        String.format("题型%d可用题目数量不足：需要%d道，但只有%d道可用", 
                                category, count, categoryQuestions.size()));
            }

            // 随机打乱并抽取指定数量的题目
            Collections.shuffle(categoryQuestions);
            List<QuestionBankDO> selectedQuestions = categoryQuestions.subList(0, count);

            // 添加到items和questionItems
            for (QuestionBankDO question : selectedQuestions) {
                ExamQuestionItemRequest item = new ExamQuestionItemRequest();
                item.setQuestionId(question.getId());
                item.setQuestionScore(score);
                item.setGroupId(groupId);
                item.setSortOrder(currentSortOrder);
                items.add(item);
                
                // 构建响应项
                ExamRandomGenerateResponse.QuestionItem questionItem = new ExamRandomGenerateResponse.QuestionItem();
                questionItem.setQuestionId(question.getId());
                questionItem.setQuestionCategory(question.getQuestionCategory());
                questionItem.setQuestionContent(question.getQuestionContent());
                questionItem.setQuestionScore(score);
                questionItem.setSortOrder(currentSortOrder);
                questionItem.setGroupId(groupId);
                questionItems.add(questionItem);
                
                totalScore += score;
                currentSortOrder++;
            }

            // 从可用题目中移除已选中的题目，避免重复
            categoryQuestions.removeAll(selectedQuestions);
        }

        if (items.isEmpty()) {
            throw new IllegalArgumentException("未能抽取到任何题目");
        }

        // 使用现有的addQuestions逻辑添加题目
        ExamAddQuestionsRequest addRequest = new ExamAddQuestionsRequest();
        addRequest.setItems(items);
        addQuestions(examId, addRequest);
        
        // 构建并返回响应
        ExamRandomGenerateResponse response = new ExamRandomGenerateResponse();
        response.setQuestions(questionItems);
        response.setTotalCount(questionItems.size());
        response.setTotalScore(totalScore);
        
        return response;
    }

    /**
     * 生成默认题型配置（兜底机制）
     * 比例：选择（单选+多选）：判断：填空：简答：编程 = 5：3：2：2：1
     * 
     * @param totalAvailableCount 可用题目总数
     * @param questionsByCategory 按题型分组的题目
     * @return 题型配置列表
     */
    private List<ExamRandomGenerateRequest.QuestionTypeConfig> generateDefaultTypeConfigs(
            int totalAvailableCount, 
            java.util.Map<Integer, List<QuestionBankDO>> questionsByCategory) {
        
        List<ExamRandomGenerateRequest.QuestionTypeConfig> configs = new ArrayList<>();
        
        // 计算各题型的可用数量
        int singleChoiceCount = questionsByCategory.getOrDefault(1, Collections.emptyList()).size();
        int multipleChoiceCount = questionsByCategory.getOrDefault(2, Collections.emptyList()).size();
        int judgeCount = questionsByCategory.getOrDefault(3, Collections.emptyList()).size();
        int fillBlankCount = questionsByCategory.getOrDefault(4, Collections.emptyList()).size();
        int shortAnswerCount = questionsByCategory.getOrDefault(5, Collections.emptyList()).size();
        int programmingCount = questionsByCategory.getOrDefault(6, Collections.emptyList()).size();

        // 总比例：选择5 + 判断3 + 填空2 + 简答2 + 编程1 = 13
        // 根据可用题目数量和比例，计算各题型的理想数量
        // 但需要确保不超过实际可用数量
        
        // 计算各题型的理想数量（按比例）
        double ratioChoice = 5.0 / 13;      // 选择（单选+多选）
        double ratioJudge = 3.0 / 13;       // 判断
        double ratioFillBlank = 2.0 / 13;   // 填空
        double ratioShortAnswer = 2.0 / 13; // 简答
        double ratioProgramming = 1.0 / 13; // 编程

        // 根据可用题目总数，按比例计算各题型的目标数量
        // 但需要确保不超过实际可用数量，并尽量保持比例
        int targetChoice = (int) Math.round(totalAvailableCount * ratioChoice);
        int targetJudge = (int) Math.round(totalAvailableCount * ratioJudge);
        int targetFillBlank = (int) Math.round(totalAvailableCount * ratioFillBlank);
        int targetShortAnswer = (int) Math.round(totalAvailableCount * ratioShortAnswer);
        int targetProgramming = (int) Math.round(totalAvailableCount * ratioProgramming);

        // 确保不超过实际可用数量
        targetChoice = Math.min(targetChoice, singleChoiceCount + multipleChoiceCount);
        targetJudge = Math.min(targetJudge, judgeCount);
        targetFillBlank = Math.min(targetFillBlank, fillBlankCount);
        targetShortAnswer = Math.min(targetShortAnswer, shortAnswerCount);
        targetProgramming = Math.min(targetProgramming, programmingCount);

        // 分配单选和多选（优先单选，如果单选不够再用多选补足）
        int singleChoiceTarget = Math.min(targetChoice, singleChoiceCount);
        int remainingChoice = targetChoice - singleChoiceTarget;
        int multipleChoiceTarget = Math.min(remainingChoice, multipleChoiceCount);
        
        // 如果还有剩余的选择题配额，但单选和多选都用完了，尝试从其他题型调整
        // 这里简化处理：如果选择不够，就按实际可用数量来
        if (singleChoiceTarget + multipleChoiceTarget < targetChoice) {
            // 选择不够，按实际可用数量
            singleChoiceTarget = singleChoiceCount;
            multipleChoiceTarget = multipleChoiceCount;
        }

        // 创建配置（默认分数：选择2分，判断1分，填空2分，简答5分，编程10分）
        if (singleChoiceTarget > 0 && singleChoiceCount > 0) {
            ExamRandomGenerateRequest.QuestionTypeConfig config = new ExamRandomGenerateRequest.QuestionTypeConfig();
            config.setCategory(1);
            config.setCount(singleChoiceTarget);
            config.setScore(2);
            config.setGroupId(1);
            configs.add(config);
        }

        if (multipleChoiceTarget > 0 && multipleChoiceCount > 0) {
            ExamRandomGenerateRequest.QuestionTypeConfig config = new ExamRandomGenerateRequest.QuestionTypeConfig();
            config.setCategory(2);
            config.setCount(multipleChoiceTarget);
            config.setScore(2);
            config.setGroupId(1);
            configs.add(config);
        }

        if (targetJudge > 0 && judgeCount > 0) {
            ExamRandomGenerateRequest.QuestionTypeConfig config = new ExamRandomGenerateRequest.QuestionTypeConfig();
            config.setCategory(3);
            config.setCount(targetJudge);
            config.setScore(1);
            config.setGroupId(2);
            configs.add(config);
        }

        if (targetFillBlank > 0 && fillBlankCount > 0) {
            ExamRandomGenerateRequest.QuestionTypeConfig config = new ExamRandomGenerateRequest.QuestionTypeConfig();
            config.setCategory(4);
            config.setCount(targetFillBlank);
            config.setScore(2);
            config.setGroupId(3);
            configs.add(config);
        }

        if (targetShortAnswer > 0 && shortAnswerCount > 0) {
            ExamRandomGenerateRequest.QuestionTypeConfig config = new ExamRandomGenerateRequest.QuestionTypeConfig();
            config.setCategory(5);
            config.setCount(targetShortAnswer);
            config.setScore(5);
            config.setGroupId(4);
            configs.add(config);
        }

        if (targetProgramming > 0 && programmingCount > 0) {
            ExamRandomGenerateRequest.QuestionTypeConfig config = new ExamRandomGenerateRequest.QuestionTypeConfig();
            config.setCategory(6);
            config.setCount(targetProgramming);
            config.setScore(10);
            config.setGroupId(5);
            configs.add(config);
        }

        // 如果没有任何配置，说明所有题型都没有可用题目
        if (configs.isEmpty()) {
            throw new IllegalArgumentException("没有可用的题目进行智能组卷，请检查题目标签和题目类型");
        }

        return configs;
    }

    private void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && !end.isAfter(start)) {
            throw new IllegalArgumentException("结束时间必须晚于开始时间");
        }
    }
}

