package com.exam.online.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用户级别限流注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UserRateLimit {
    
    /**
     * 接口类型（enter-exam, save-answer, submit-exam）
     */
    String interfaceType();
    
    /**
     * 用户ID参数名（默认为studentId）
     */
    String userIdParam() default "studentId";
}

