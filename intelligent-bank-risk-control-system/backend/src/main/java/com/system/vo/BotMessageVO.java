package com.system.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class BotMessageVO {
    private Long id;
    private Integer messageType;
    private String content;
    private Long matchedKnowledgeId;
    private BigDecimal confidence;
    private Integer isHelpful;
    private LocalDateTime createTime;
}
