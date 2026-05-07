package com.system.service;

import com.system.vo.CreditScoreOverviewVO;
import com.system.vo.CustomerLimitSummaryVO;

public interface CustomerCreditService {

    CreditScoreOverviewVO getMyOverview();

    /**
     * 风控查询指定客户的评分概况与历史（非客户本人勿调）
     */
    CreditScoreOverviewVO getCustomerOverviewForRisk(Long customerId);

    CreditScoreOverviewVO evaluateMyScore();

    CustomerLimitSummaryVO getMyLimitSummary();
}
