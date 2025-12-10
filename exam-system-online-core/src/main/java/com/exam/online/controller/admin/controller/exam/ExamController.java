package com.exam.online.controller.admin.controller.exam;

import com.exam.online.dto.ExamAddQuestionsRequest;
import com.exam.online.dto.ExamCreateRequest;
import com.exam.online.dto.ExamResponse;
import com.exam.online.dto.ExamStatusUpdateRequest;
import com.exam.online.dto.ExamUpdateRequest;
import com.exam.online.service.ExamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exams")
@Validated
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    @PostMapping("/create")
    public ExamResponse create(@Valid @RequestBody ExamCreateRequest request) {
        return examService.createExam(request);
    }

    @PostMapping("/{examId}/questions")
    public String addQuestions(@PathVariable("examId") Long examId,
                               @Valid @RequestBody ExamAddQuestionsRequest request) {
        examService.addQuestions(examId, request);
        return "添加题目成功";
    }

    @PutMapping("/{examId}")
    public String update(@PathVariable("examId") Long examId,
                         @Valid @RequestBody ExamUpdateRequest request) {
        examService.updateExam(examId, request);
        return "更新试卷成功";
    }

    @PutMapping("/{examId}/status")
    public String updateStatus(@PathVariable("examId") Long examId,
                               @Valid @RequestBody ExamStatusUpdateRequest request) {
        examService.updateStatus(examId, request);
        return "更新考试状态成功";
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgumentException(IllegalArgumentException ex) {
        return ex.getMessage();
    }
}

