package com.exam.online.aspect;

import com.exam.online.annotation.UserRateLimit;
import com.exam.online.exception.RateLimitException;
import com.exam.online.service.UserRateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * 限流切面：使用AOP在方法执行前进行限流检查
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {
    
    private final UserRateLimitService userRateLimitService;
    
    @Around("@annotation(userRateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, UserRateLimit userRateLimit) throws Throwable {
        // 提取用户ID
        Long userId = extractUserId(joinPoint, userRateLimit.userIdParam());
        
        if (userId == null) {
            log.warn("[限流切面] 无法提取用户ID，跳过限流检查: method={}", 
                joinPoint.getSignature().getName());
            return joinPoint.proceed();
        }
        
        // 进行限流检查
        try {
            userRateLimitService.checkUserRateLimit(userId, userRateLimit.interfaceType());
        } catch (RateLimitException e) {
            // 限流异常直接抛出，由全局异常处理器处理
            throw e;
        }
        
        // 限流检查通过，继续执行
        return joinPoint.proceed();
    }
    
    /**
     * 从方法参数中提取用户ID
     */
    private Long extractUserId(ProceedingJoinPoint joinPoint, String paramName) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Parameter[] parameters = method.getParameters();
            Object[] args = joinPoint.getArgs();
            
            for (int i = 0; i < parameters.length; i++) {
                Parameter parameter = parameters[i];
                if (paramName.equals(parameter.getName())) {
                    Object arg = args[i];
                    if (arg instanceof Long) {
                        return (Long) arg;
                    } else if (arg != null) {
                        // 尝试从对象中获取studentId字段
                        try {
                            java.lang.reflect.Field field = arg.getClass().getDeclaredField("studentId");
                            field.setAccessible(true);
                            Object value = field.get(arg);
                            if (value instanceof Long) {
                                return (Long) value;
                            }
                        } catch (Exception e) {
                            // 忽略
                        }
                    }
                }
            }
            
            // 如果没找到指定参数名，尝试查找包含studentId的对象
            for (Object arg : args) {
                if (arg != null) {
                    try {
                        java.lang.reflect.Field field = arg.getClass().getDeclaredField("studentId");
                        field.setAccessible(true);
                        Object value = field.get(arg);
                        if (value instanceof Long) {
                            return (Long) value;
                        }
                    } catch (Exception e) {
                        // 忽略
                    }
                }
            }
            
            return null;
        } catch (Exception e) {
            log.warn("[限流切面] 提取用户ID失败: method={}", 
                joinPoint.getSignature().getName(), e);
            return null;
        }
    }
}

