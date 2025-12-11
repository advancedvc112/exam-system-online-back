package com.exam.online.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 答题缓冲服务：实现3秒合并窗口
 */
@Slf4j
@Service
public class AnswerBufferService {
    
    private static class AnswerCache {
        String answer;
        Long examId;
        Long studentId;
        Integer sortOrder;
    }
    
    private final Map<String, AnswerCache> buffer = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(10);
    
    private final ExamAnswerService examAnswerService;
    
    public AnswerBufferService(ExamAnswerService examAnswerService) {
        this.examAnswerService = examAnswerService;
    }
    
    /**
     * 缓冲答题记录，3秒后自动写入Redis和MQ
     */
    public void bufferAnswer(Long examId, Long studentId, Integer sortOrder, String answer) {
        String key = buildKey(examId, studentId, sortOrder);
        
        // 更新缓存
        AnswerCache cache = new AnswerCache();
        cache.examId = examId;
        cache.studentId = studentId;
        cache.sortOrder = sortOrder;
        cache.answer = answer;
        buffer.put(key, cache);
        
        // 取消旧的定时任务
        ScheduledFuture<?> oldTask = scheduledTasks.remove(key);
        if (oldTask != null) {
            oldTask.cancel(false);
        }
        
        // 创建新的3秒延迟任务
        ScheduledFuture<?> future = executor.schedule(() -> {
            try {
                flushAnswer(key);
            } catch (Exception e) {
                log.error("刷新答题记录失败: key={}", key, e);
            } finally {
                scheduledTasks.remove(key);
            }
        }, 3, TimeUnit.SECONDS);
        
        scheduledTasks.put(key, future);
    }
    
    /**
     * 立即刷新指定key的答题记录
     */
    public void flushAnswer(String key) {
        AnswerCache cache = buffer.remove(key);
        if (cache != null) {
            examAnswerService.saveAnswerToRedisAndMQ(
                cache.examId,
                cache.studentId,
                cache.sortOrder,
                cache.answer
            );
        }
    }
    
    /**
     * 立即刷新所有缓冲的答题记录
     */
    public void flushAll(Long examId, Long studentId) {
        String prefix = examId + ":" + studentId + ":";
        buffer.entrySet().removeIf(entry -> {
            if (entry.getKey().startsWith(prefix)) {
                AnswerCache cache = entry.getValue();
                examAnswerService.saveAnswerToRedisAndMQ(
                    cache.examId,
                    cache.studentId,
                    cache.sortOrder,
                    cache.answer
                );
                return true;
            }
            return false;
        });
        
        // 取消所有相关的定时任务
        scheduledTasks.entrySet().removeIf(entry -> {
            if (entry.getKey().startsWith(prefix)) {
                entry.getValue().cancel(false);
                return true;
            }
            return false;
        });
    }
    
    private String buildKey(Long examId, Long studentId, Integer sortOrder) {
        return examId + ":" + studentId + ":" + sortOrder;
    }
}

