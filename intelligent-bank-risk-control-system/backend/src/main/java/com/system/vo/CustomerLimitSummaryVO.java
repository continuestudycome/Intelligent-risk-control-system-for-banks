package com.system.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CustomerLimitSummaryVO {
    private boolean hasLimitAccount;
    private BigDecimal totalLimit;
    private BigDecimal usedLimit;
    private BigDecimal availableLimit;
    private BigDecimal singleLimit;
    private BigDecimal dailyLimit;
    /** 是否存在待复核的上调工单 */
    private boolean pendingIncreaseReview;
    private BigDecimal pendingProposedTotal;
    private String lastAdjustHint;
}
