package com.exam.online.exception;

/**
 * 限流异常
 */
public class RateLimitException extends RuntimeException {
    
    private final int retryAfter;
    
    public RateLimitException(String message) {
        super(message);
        this.retryAfter = 1;
    }
    
    public RateLimitException(String message, int retryAfter) {
        super(message);
        this.retryAfter = retryAfter;
    }
    
    public int getRetryAfter() {
        return retryAfter;
    }
}

