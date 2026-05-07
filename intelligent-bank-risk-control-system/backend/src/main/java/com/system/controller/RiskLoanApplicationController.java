package com.system.controller;

import com.system.common.Result;
import com.system.dto.LoanApplicationReviewDTO;
import com.system.service.LoanApplicationService;
import com.system.vo.LoanApplicationVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/risk/loan-application")
@RequiredArgsConstructor
public class RiskLoanApplicationController {

    private final LoanApplicationService loanApplicationService;

    @GetMapping
    public Result<List<LoanApplicationVO>> list(
            @RequestParam(value = "currentStatus", required = false) String currentStatus,
            @RequestParam(value = "applyType", required = false) Integer applyType
    ) {
        return Result.success(loanApplicationService.listForRisk(currentStatus, applyType));
    }

    @GetMapping("/{id}")
    public Result<LoanApplicationVO> detail(@PathVariable("id") Long id) {
        return Result.success(loanApplicationService.getRiskApplicationDetail(id));
    }

    @PostMapping("/{id}/review")
    public Result<LoanApplicationVO> review(
            @PathVariable("id") Long id,
            @Valid @RequestBody LoanApplicationReviewDTO dto
    ) {
        return Result.success(loanApplicationService.review(id, dto));
    }
}
