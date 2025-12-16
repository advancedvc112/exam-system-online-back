package com.exam.online.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamRandomGenerateResponse {
    
    /**
     * 组卷的题目列表
     */
    private List<QuestionItem> questions;
    
    /**
     * 总题目数
     */
    private Integer totalCount;
    
    /**
     * 总分数
     */
    private Integer totalScore;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionItem {
        /**
         * 题目ID
         */
        private Long questionId;
        
        /**
         * 题型（1：单选 2：多选 3：判断 4：填空 5：简答 6：编程）
         */
        private Integer questionCategory;
        
        /**
         * 题目内容
         */
        private String questionContent;
        
        /**
         * 题目分数
         */
        private Integer questionScore;
        
        /**
         * 题目排序
         */
        private Integer sortOrder;
        
        /**
         * 题目组别
         */
        private Integer groupId;
    }
}

