package com.system.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ProfileTxnBriefVO {
    private Long id;
    private String transactionNo;
    private Integer transactionType;
    private String transactionTypeName;
    private BigDecimal amount;
    private String riskStatus;
    private String riskStatusName;
    private LocalDateTime transactionTime;
}
