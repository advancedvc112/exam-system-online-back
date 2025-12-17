package com.exam.online.controller.admin.controller.exam;

import com.exam.online.dto.ExamAddQuestionsRequest;
import com.exam.online.dto.ExamCreateRequest;
import com.exam.online.dto.ExamResponse;
import com.exam.online.dto.ExamRandomGenerateRequest;
import com.exam.online.dto.ExamRandomGenerateResponse;
import com.exam.online.dto.ExamStatusUpdateRequest;
import com.exam.online.dto.ExamUpdateRequest;
import com.exam.online.dto.Result;
import com.exam.online.service.ExamService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/exams")
@Validated
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    /**
     * 分页查询全部考试
     */
    @GetMapping("/list")
    public Result<java.util.List<ExamResponse>> listAll(@RequestParam(value = "page", defaultValue = "1") @Min(1) int page,
                                                        @RequestParam(value = "size", defaultValue = "20") @Min(1) int size) {
        log.info("分页查询考试列表 page={}, size={}", page, size);
        return Result.success(examService.listAll(page, size));
    }

    /**
     * 根据考试ID查询考试详情
     */
    @GetMapping("/{examId}")
    public Result<ExamResponse> getById(@PathVariable("examId") Long examId) {
        log.info("查询考试详情，examId={}", examId);
        return Result.success(examService.getById(examId));
    }

    /**
     * 创建试卷
     */
    @PostMapping("/create")
    public Result<ExamResponse> create(@Valid @RequestBody ExamCreateRequest request) {
        log.info("创建考试，name={}, type={}, creatorId={}", request.getExamName(), request.getExamType(), request.getCreatorId());
        return Result.success(examService.createExam(request));
    }

    /**
     * 试卷添加题目
     */
    @PostMapping("/{examId}/questions")
    public Result<Void> addQuestions(@PathVariable("examId") Long examId,
                               @Valid @RequestBody ExamAddQuestionsRequest request) {
        log.info("试卷添加题目，examId={}, items={}", examId, request.getItems() == null ? 0 : request.getItems().size());
        examService.addQuestions(examId, request);
        return Result.success("添加题目成功");
    }

    /**
     * 根据考试ID查询试卷题目内容
     */
    @GetMapping("/{examId}/questions/detail")
    public Result<ExamRandomGenerateResponse> getExamQuestions(@PathVariable("examId") Long examId) {
        log.info("查询试卷题目详情，examId={}", examId);
        return Result.success(examService.getExamQuestions(examId));
    }

    /**
     * 随机智能组卷
     * 支持多题型配置，如果typeConfigs为null或空，则启用兜底机制按比例自动组卷
     */
    @PostMapping("/{examId}/questions/random-generate")
    public Result<ExamRandomGenerateResponse> randomGenerateQuestions(@PathVariable("examId") Long examId,
                                                                      @Valid @RequestBody ExamRandomGenerateRequest request) {
        log.info("随机智能组卷，examId={}, questionTag={}, typeConfigs={}", 
                examId, request.getQuestionTag(), 
                request.getTypeConfigs() == null ? "null(启用兜底)" : request.getTypeConfigs().size() + "个配置");
        ExamRandomGenerateResponse response = examService.randomGenerateQuestions(examId, request);
        return Result.success("智能组卷成功", response);
    }

    /**
     * 更新试卷信息
     */
    @PutMapping("/{examId}")
    public Result<Void> update(@PathVariable("examId") Long examId,
                         @Valid @RequestBody ExamUpdateRequest request) {
        log.info("更新考试，examId={}", examId);
        examService.updateExam(examId, request);
        return Result.success("更新试卷成功");
    }

    /**
     * 更新试卷状态
     */
    @PutMapping("/{examId}/status")
    public Result<Void> updateStatus(@PathVariable("examId") Long examId,
                               @Valid @RequestBody ExamStatusUpdateRequest request) {
        log.info("更新考试状态，examId={}, status={}", examId, request.getStatus());
        examService.updateStatus(examId, request);
        return Result.success("更新考试状态成功");
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException ex) {
        return Result.failure(ex.getMessage());
    }
}

