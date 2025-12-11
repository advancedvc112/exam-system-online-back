package com.exam.online.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.online.dal.dataobject.QuestionBankDO;
import com.exam.online.dal.mapper.QuestionBankMapper;
import com.exam.online.dto.QuestionCreateRequest;
import com.exam.online.dto.QuestionResponse;
import com.exam.online.dto.QuestionUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionBankMapper questionBankMapper;
    private static final long MAX_PAGE_SIZE = 100L;

    public List<QuestionResponse> listByTag(String tag, int page, int pageSize) {
        if (!StringUtils.hasText(tag)) {
            throw new IllegalArgumentException("tag不能为空");
        }
        int safePage = Math.max(page, 1);
        long safeSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);

        LambdaQueryWrapper<QuestionBankDO> wrapper = new LambdaQueryWrapper<QuestionBankDO>()
                .eq(QuestionBankDO::getIsDeleted, 0)
                .like(QuestionBankDO::getQuestionTags, tag)
                .orderByDesc(QuestionBankDO::getId);

        Page<QuestionBankDO> pageResult = questionBankMapper.selectPage(new Page<>(safePage, safeSize), wrapper);

        return pageResult.getRecords().stream()
                .map(q -> new QuestionResponse(
                        q.getId(),
                        q.getQuestionCategory(),
                        q.getQuestionContent(),
                        sanitizeOptionsForResponse(q.getQuestionCategory(), q.getQuestionOptions()),
                        q.getQuestionAnswer(),
                        q.getQuestionTags()))
                .toList();
    }

    public QuestionResponse createQuestion(QuestionCreateRequest request) {
        QuestionBankDO entity = new QuestionBankDO();
        entity.setQuestionCategory(request.getQuestionCategory());
        entity.setQuestionContent(request.getQuestionContent());
        entity.setQuestionOptions(normalizeOptions(request.getQuestionCategory(), request.getQuestionOptions()));
        entity.setQuestionAnswer(request.getQuestionAnswer());
        entity.setQuestionTags(request.getQuestionTags());
        entity.setIsDeleted(0);

        questionBankMapper.insert(entity);

        return new QuestionResponse(
                entity.getId(),
                entity.getQuestionCategory(),
                entity.getQuestionContent(),
                sanitizeOptionsForResponse(entity.getQuestionCategory(), entity.getQuestionOptions()),
                entity.getQuestionAnswer(),
                entity.getQuestionTags()
        );
    }

    public QuestionResponse updateQuestion(QuestionUpdateRequest request) {
        QuestionBankDO exists = questionBankMapper.selectById(request.getId());
        if (exists == null || (exists.getIsDeleted() != null && exists.getIsDeleted() == 1)) {
            throw new IllegalArgumentException("题目不存在或已删除");
        }

        LambdaUpdateWrapper<QuestionBankDO> wrapper = new LambdaUpdateWrapper<QuestionBankDO>()
                .eq(QuestionBankDO::getId, request.getId());

        boolean hasUpdate = false;
        if (request.getQuestionCategory() != null) {
            wrapper.set(QuestionBankDO::getQuestionCategory, request.getQuestionCategory());
            hasUpdate = true;
        }
        if (request.getQuestionContent() != null) {
            wrapper.set(QuestionBankDO::getQuestionContent, request.getQuestionContent());
            hasUpdate = true;
        }
        // options 仅在单选/多选存在，其它类型需置空
        Integer effectiveCategory = request.getQuestionCategory() != null
                ? request.getQuestionCategory()
                : exists.getQuestionCategory();
        boolean updateOptions = request.getQuestionOptions() != null || request.getQuestionCategory() != null;
        if (updateOptions) {
            String options = normalizeOptions(effectiveCategory, request.getQuestionOptions());
            wrapper.set(QuestionBankDO::getQuestionOptions, options);
            hasUpdate = true;
        }
        if (request.getQuestionAnswer() != null) {
            wrapper.set(QuestionBankDO::getQuestionAnswer, request.getQuestionAnswer());
            hasUpdate = true;
        }
        if (request.getQuestionTags() != null) {
            wrapper.set(QuestionBankDO::getQuestionTags, request.getQuestionTags());
            hasUpdate = true;
        }

        if (!hasUpdate) {
            throw new IllegalArgumentException("未提供需要更新的字段");
        }

        questionBankMapper.update(null, wrapper);

        QuestionBankDO updated = questionBankMapper.selectById(request.getId());
        return new QuestionResponse(
                updated.getId(),
                updated.getQuestionCategory(),
                updated.getQuestionContent(),
                sanitizeOptionsForResponse(updated.getQuestionCategory(), updated.getQuestionOptions()),
                updated.getQuestionAnswer(),
                updated.getQuestionTags()
        );
    }

    private boolean categoryHasOptions(Integer category) {
        return category != null && (category == 1 || category == 2);
    }

    private String normalizeOptions(Integer category, String rawOptions) {
        return categoryHasOptions(category) ? rawOptions : null;
    }

    private String sanitizeOptionsForResponse(Integer category, String options) {
        return categoryHasOptions(category) ? options : null;
    }
}

