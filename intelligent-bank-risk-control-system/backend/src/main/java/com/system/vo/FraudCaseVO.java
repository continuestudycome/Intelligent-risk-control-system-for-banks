package com.system.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FraudCaseVO {
    private Long id;
    private Long transactionId;
    private Long alertId;
    private Long customerId;
    private String fraudType;
    private Integer confirmedResult;
    private Integer labelSource;
    private LocalDateTime createTime;
}
