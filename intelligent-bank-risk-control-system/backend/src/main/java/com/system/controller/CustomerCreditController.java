package com.system.controller;

import com.system.common.Result;
import com.system.service.CustomerCreditService;
import com.system.vo.CreditScoreOverviewVO;
import com.system.vo.CustomerLimitSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customer/credit")
@RequiredArgsConstructor
public class CustomerCreditController {

    private final CustomerCreditService customerCreditService;

    /**
     * 当前评分概况与近期评分走势（用于趋势分析）
     */
    @GetMapping("/overview")
    public Result<CreditScoreOverviewVO> overview() {
        return Result.success(customerCreditService.getMyOverview());
    }

    /**
     * 触发重新评估（调用 ai_services 逻辑回归，失败则线性兜底）
     */
    @PostMapping("/evaluate")
    public Result<CreditScoreOverviewVO> evaluate() {
        CreditScoreOverviewVO vo = customerCreditService.evaluateMyScore();
        return Result.success(vo, "评估完成");
    }

    /**
     * 当前客户授信额度摘要（含是否有待复核的上调工单）
     */
    @GetMapping("/limit")
    public Result<CustomerLimitSummaryVO> limitSummary() {
        return Result.success(customerCreditService.getMyLimitSummary());
    }
}
