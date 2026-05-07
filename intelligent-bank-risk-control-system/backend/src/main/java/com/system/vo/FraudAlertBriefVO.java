package com.system.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class FraudAlertBriefVO {
    private Long id;
    private String transactionNo;
    private String alertLevel;
    private BigDecimal mlScore;
    private String status;
    private LocalDateTime createTime;
    /** 命中规则摘要（过长时后端截断） */
    private String hitRulesSummary;
}
