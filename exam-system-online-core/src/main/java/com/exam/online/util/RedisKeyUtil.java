package com.exam.online.util;

public class RedisKeyUtil {
    
    private static final String EXAM_TOKEN_PREFIX = "exam:token:";
    private static final String EXAM_ANSWER_PREFIX = "exam:ans:";
    
    /**
     * 生成考试token的key
     * @param examId 考试ID
     * @param studentId 学生ID
     * @return key
     */
    public static String getExamTokenKey(Long examId, Long studentId) {
        return EXAM_TOKEN_PREFIX + examId + ":" + studentId;
    }
    
    /**
     * 生成答题记录的key
     * @param examId 考试ID
     * @param studentId 学生ID
     * @param sortOrder 题目序号
     * @return key
     */
    public static String getAnswerKey(Long examId, Long studentId, Integer sortOrder) {
        return EXAM_ANSWER_PREFIX + examId + ":" + studentId + ":" + sortOrder;
    }
    
    /**
     * 生成答题记录的模式key（用于批量查询）
     * @param examId 考试ID
     * @param studentId 学生ID
     * @return pattern
     */
    public static String getAnswerPattern(Long examId, Long studentId) {
        return EXAM_ANSWER_PREFIX + examId + ":" + studentId + ":*";
    }
}

