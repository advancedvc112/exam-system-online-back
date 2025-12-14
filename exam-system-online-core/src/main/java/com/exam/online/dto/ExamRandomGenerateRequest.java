package com.exam.online.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ExamRandomGenerateRequest {

    @NotBlank(message = "题目标签不能为空")
    private String questionTag;

    /**
     * 题型配置列表，可以为空（为空时启用兜底机制）
     * 每个配置包含：题型、数量、每题分数
     */
    @Valid
    private List<QuestionTypeConfig> typeConfigs;

    /**
     * 题型配置
     */
    @Data
    public static class QuestionTypeConfig {
        /**
         * 题目类型（1：单选 2：多选 3：判断 4：填空 5：简答 6：编程）
         */
        @Min(value = 1, message = "题型必须大于0")
        private Integer category;

        /**
         * 该题型的数量
         */
        @Min(value = 1, message = "题目数量必须大于0")
        private Integer count;

        /**
         * 该题型每题的分数
         */
        @Min(value = 1, message = "题目分数必须大于0")
        private Integer score;

        /**
         * 题目组别
         */
        private Integer groupId;
    }
}

