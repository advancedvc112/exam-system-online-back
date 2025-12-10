package com.exam.online.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class QuestionResponse {
    private Long id;
    private Integer questionCategory;
    private String questionContent;
    private String questionOptions;
    private String questionAnswer;
    private String questionTags;
}

