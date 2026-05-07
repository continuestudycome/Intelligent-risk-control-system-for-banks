package com.system.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RiskWarningOverviewVO {
    private RiskWarningStatsVO stats;
    private List<FraudAlertVO> fraudAlerts;
    private List<TransactionVO> riskTransactions;
}
