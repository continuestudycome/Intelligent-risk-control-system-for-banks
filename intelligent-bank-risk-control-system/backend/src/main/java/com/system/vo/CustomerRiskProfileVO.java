package com.system.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 风控侧「客户风险画像」聚合视图：基础资料 + 信用 + 交易风险 + 欺诈告警 + 信贷申请摘要。
 */
@Data
@Builder
public class CustomerRiskProfileVO {
    private CustomerRiskBasicVO basic;
    /** 规则与模型生成的画像标签，便于快速扫读 */
    private List<String> portraitTags;
    private CreditScoreOverviewVO credit;
    private TransactionRiskSummaryVO transactions;
    private List<FraudAlertBriefVO> fraudAlerts;
    private LoanApplicationProfileSummaryVO loans;
}
