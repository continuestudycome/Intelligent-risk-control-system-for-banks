package com.system.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class TransactionRiskSummaryVO {
    /** 统计窗口天数 */
    private int lookbackDays;
    private int totalCount;
    private BigDecimal totalAmount;
    private int lowCount;
    private int mediumCount;
    private int highCount;
    private int interceptedCount;
    private int confirmedFraudCount;
    /** 高风险 / 中风险 / 拦截 / 确认欺诈 最近若干笔 */
    private List<ProfileTxnBriefVO> recentRiskyTransactions;
}
