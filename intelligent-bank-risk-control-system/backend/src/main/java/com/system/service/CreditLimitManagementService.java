package com.system.service;

import com.system.vo.CustomerLimitSummaryVO;
import com.system.vo.LimitAdjustPendingVO;

import java.util.List;

public interface CreditLimitManagementService {

    /** 信用评分落库后：若无授信账户则按当前评分初始化 */
    void ensureLimitAccountAfterScoreEvaluation(Long customerId);

    /** 定时任务：遍历客户，下调自动执行，上调生成工单 */
    void runPeriodicAdjustment();

    /** 风控手动触发全量批跑（含权限校验） */
    void triggerManualRiskBatch();

    CustomerLimitSummaryVO getCustomerLimitSummary(Long customerId);

    List<LimitAdjustPendingVO> listPendingIncreaseRequests();

    void approveIncrease(Long requestId, String comment);

    void rejectIncrease(Long requestId, String comment);
}
