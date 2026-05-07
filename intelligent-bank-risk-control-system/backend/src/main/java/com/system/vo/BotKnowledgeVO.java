package com.system.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BotKnowledgeVO {
    private Long id;
    private String category;
    private String docType;
    private String question;
    private String answer;
    private String similarQuestions;
    private String keywords;
    private Integer hitCount;
    private Integer status;
    private LocalDateTime updateTime;
    private String sourceType;
    private String sourceFilename;
    private LocalDateTime vectorIndexedAt;
}
