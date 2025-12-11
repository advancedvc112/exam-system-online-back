package com.exam.online.controller.admin.controller.question;

import com.exam.online.dto.QuestionCreateRequest;
import com.exam.online.dto.QuestionResponse;
import com.exam.online.dto.QuestionUpdateRequest;
import com.exam.online.dto.Result;
import com.exam.online.service.QuestionService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
@Validated
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping("/search-by-tag")
    public Result<List<QuestionResponse>> searchByTag(@RequestParam("tag") @NotBlank String tag,
                                              @RequestParam(value = "page", defaultValue = "1") @Min(1) int page,
                                              @RequestParam(value = "size", defaultValue = "20") @Min(1) int size) {
        return Result.success(questionService.listByTag(tag, page, size));
    }

    @PostMapping("/create")
    public Result<QuestionResponse> create(@Valid @RequestBody QuestionCreateRequest request) {
        return Result.success(questionService.createQuestion(request));
    }

    @PutMapping("/update")
    public Result<QuestionResponse> update(@Valid @RequestBody QuestionUpdateRequest request) {
        return Result.success(questionService.updateQuestion(request));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException ex) {
        return Result.failure(ex.getMessage());
    }
}

