package com.exam.online.controller.admin;

import com.exam.online.dto.Result;
import com.exam.online.exception.RateLimitException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * 处理限流异常
     */
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    @ExceptionHandler(RateLimitException.class)
    public Result<Void> handleRateLimitException(RateLimitException ex) {
        log.warn("[限流异常] 请求被限流: message={}, retryAfter={}", ex.getMessage(), ex.getRetryAfter());
        return Result.failure(429, ex.getMessage());
    }
}

