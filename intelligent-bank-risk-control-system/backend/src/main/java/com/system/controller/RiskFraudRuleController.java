package com.system.controller;

import com.system.common.Result;
import com.system.dto.FraudRuleUpdateDTO;
import com.system.dto.FraudRuleValidateBatchDTO;
import com.system.service.FraudRuleManageService;
import com.system.vo.FraudRuleVO;
import com.system.vo.FraudRuleValidateResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/risk/fraud-rules")
@RequiredArgsConstructor
public class RiskFraudRuleController {

    private final FraudRuleManageService fraudRuleManageService;

    @GetMapping
    public Result<List<FraudRuleVO>> list() {
        return Result.success(fraudRuleManageService.listForRisk());
    }

    @PutMapping("/{id}")
    public Result<FraudRuleVO> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody FraudRuleUpdateDTO dto
    ) {
        return Result.success(fraudRuleManageService.update(id, dto));
    }

    /** 调用 Python 智能服务批量校验规则 JSON（阈值区间、字段一致性等） */
    @PostMapping("/validate-ai")
    public Result<FraudRuleValidateResultVO> validateAi(@Valid @RequestBody FraudRuleValidateBatchDTO dto) {
        return Result.success(fraudRuleManageService.validateWithAi(dto));
    }

    /** 按当前库中阈值对内置样本做风险等级试算（Python） */
    @PostMapping("/simulate-default")
    public Result<List<Map<String, Object>>> simulateDefault() {
        return Result.success(fraudRuleManageService.runDefaultSimulation());
    }
}
