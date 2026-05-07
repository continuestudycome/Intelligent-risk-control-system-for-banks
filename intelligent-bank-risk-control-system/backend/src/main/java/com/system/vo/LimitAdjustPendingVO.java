package com.system.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class LimitAdjustPendingVO {
    private Long id;
    private Long customerId;
    private String customerName;
    private BigDecimal currentTotalLimit;
    private BigDecimal proposedTotalLimit;
    private Integer triggerScore;
    private String triggerRiskLevel;
    private String reason;
    private String status;
    private LocalDateTime createTime;
}
