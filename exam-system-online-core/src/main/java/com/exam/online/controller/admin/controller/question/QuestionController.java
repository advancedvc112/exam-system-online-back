package com.exam.online.controller.admin.controller.question;

import com.exam.online.dto.QuestionCreateRequest;
import com.exam.online.dto.QuestionResponse;
import com.exam.online.dto.QuestionUpdateRequest;
import com.exam.online.dto.Result;
import com.exam.online.service.QuestionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/questions")
@Validated
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    /**
     * 按标签搜索题目（分页）
     */
    @GetMapping("/search-by-tag")
    public Result<List<QuestionResponse>> searchByTag(@RequestParam("tag") @NotBlank String tag,
                                              @RequestParam(value = "page", defaultValue = "1") @Min(1) int page,
                                              @RequestParam(value = "size", defaultValue = "20") @Min(1) int size) {
        log.info("按标签搜索题目 tag={}, page={}, size={}", tag, page, size);
        return Result.success(questionService.listByTag(tag, page, size));
    }

    /**
     * 分页查询全部题目
     */
    @GetMapping("/list")
    public Result<List<QuestionResponse>> listAll(@RequestParam(value = "page", defaultValue = "1") @Min(1) int page,
                                                  @RequestParam(value = "size", defaultValue = "20") @Min(1) int size) {
        log.info("分页查询全部题目 page={}, size={}", page, size);
        return Result.success(questionService.listAll(page, size));
    }

    /**
     * 创建题目
     */
    @PostMapping("/create")
    public Result<QuestionResponse> create(@Valid @RequestBody QuestionCreateRequest request) {
        log.info("创建题目，category={}, tags={}", request.getQuestionCategory(), request.getQuestionTags());
        return Result.success(questionService.createQuestion(request));
    }

    /**
     * 更新题目
     */
    @PutMapping("/update")
    public Result<QuestionResponse> update(@Valid @RequestBody QuestionUpdateRequest request) {
        log.info("更新题目 id={}", request.getId());
        return Result.success(questionService.updateQuestion(request));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException ex) {
        return Result.failure(ex.getMessage());
    }
}

