package com.system.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RiskWarningStatsVO {
    /** 待处理欺诈预警（frd_alert.status=PENDING） */
    private long pendingFraudAlerts;
    /** 近 24h 中风险交易笔数 */
    private long mediumRiskTransactions24h;
    /** 近 24h 高危/拦截/确认欺诈笔数 */
    private long criticalRiskTransactions24h;
    /** 近 7 日已拦截笔数 */
    private long interceptedTransactions7d;
    /** 在册黑名单客户数 */
    private long blacklistCustomers;
}
