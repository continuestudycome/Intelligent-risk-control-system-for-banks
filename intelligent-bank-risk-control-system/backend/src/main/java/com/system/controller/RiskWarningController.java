package com.system.controller;

import com.system.common.Result;
import com.system.service.RiskWarningService;
import com.system.vo.RiskWarningOverviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/risk/warning")
@RequiredArgsConstructor
public class RiskWarningController {

    private final RiskWarningService riskWarningService;

    /**
     * 风险预警盯盘：指标卡片 + 欺诈预警列表 + 近期非低风险交易
     */
    @GetMapping("/overview")
    public Result<RiskWarningOverviewVO> overview() {
        return Result.success(riskWarningService.getOverview());
    }
}
