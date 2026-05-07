package com.system.controller;

import com.system.common.Result;
import com.system.dto.FraudAlertReviewDTO;
import com.system.service.FraudAlertManageService;
import com.system.vo.FraudAlertVO;
import com.system.vo.FraudCaseVO;
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
@RequestMapping("/risk/fraud")
@RequiredArgsConstructor
public class RiskFraudAlertController {

    private final FraudAlertManageService fraudAlertManageService;

    @GetMapping("/alerts")
    public Result<List<FraudAlertVO>> listAlerts(
            @RequestParam(value = "status", required = false) String status
    ) {
        return Result.success(fraudAlertManageService.listAlerts(status));
    }

    @PostMapping("/alerts/{id}/review")
    public Result<FraudAlertVO> review(
            @PathVariable("id") Long id,
            @Valid @RequestBody FraudAlertReviewDTO dto
    ) {
        return Result.success(fraudAlertManageService.review(id, dto));
    }

    @GetMapping("/cases")
    public Result<List<FraudCaseVO>> listCases() {
        return Result.success(fraudAlertManageService.listConfirmedCases());
    }
}
