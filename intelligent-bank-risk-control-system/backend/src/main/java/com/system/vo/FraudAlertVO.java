package com.system.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class FraudAlertVO {
    private Long id;
    private Long transactionId;
    private String transactionNo;
    private Long customerId;
    private String alertLevel;
    private String hitRules;
    private BigDecimal mlScore;
    private String mlModelVersion;
    private String featureSnapshot;
    private String status;
    private Long reviewerId;
    private String reviewComment;
    private LocalDateTime reviewTime;
    private LocalDateTime createTime;
}
