package com.system.controller;

import com.system.common.Result;
import com.system.service.RiskCreditQueryService;
import com.system.vo.CreditCustomerBriefVO;
import com.system.vo.CreditScoreOverviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/risk/credit")
@RequiredArgsConstructor
public class RiskCreditQueryController {

    private final RiskCreditQueryService riskCreditQueryService;

    /**
     * 按姓名/手机号/客户ID 模糊检索；keyword 为空时返回最近登记的一批客户
     */
    @GetMapping("/customers/search")
    public Result<List<CreditCustomerBriefVO>> search(
            @RequestParam(name = "keyword", required = false) String keyword) {
        return Result.success(riskCreditQueryService.searchCustomers(keyword));
    }

    /**
     * 指定客户的当前评分、历史趋势与特征快照（只读，与客户端个人页同源数据）
     */
    @GetMapping("/customers/{customerId}/overview")
    public Result<CreditScoreOverviewVO> overview(@PathVariable("customerId") Long customerId) {
        return Result.success(riskCreditQueryService.getCustomerOverview(customerId));
    }
}
