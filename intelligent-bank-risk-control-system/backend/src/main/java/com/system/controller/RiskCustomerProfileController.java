package com.system.controller;

import com.system.common.Result;
import com.system.service.CustomerRiskProfileService;
import com.system.vo.CustomerRiskProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/risk/profile")
@RequiredArgsConstructor
public class RiskCustomerProfileController {

    private final CustomerRiskProfileService customerRiskProfileService;

    /**
     * 客户风险画像：基础资料、信用、近 90 日交易风险、欺诈告警与信贷申请摘要
     */
    @GetMapping("/customers/{customerId}")
    public Result<CustomerRiskProfileVO> getProfile(@PathVariable("customerId") Long customerId) {
        return Result.success(customerRiskProfileService.getProfile(customerId));
    }
}
