package com.system.controller;

import com.system.common.Result;
import com.system.dto.LimitAdjustReviewDTO;
import com.system.service.CreditLimitManagementService;
import com.system.vo.LimitAdjustPendingVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/risk/limit")
@RequiredArgsConstructor
public class RiskLimitReviewController {

    private final CreditLimitManagementService creditLimitManagementService;

    @GetMapping("/pending-requests")
    public Result<List<LimitAdjustPendingVO>> pendingList() {
        return Result.success(creditLimitManagementService.listPendingIncreaseRequests());
    }

    @PostMapping("/pending-requests/{id}/approve")
    public Result<Void> approve(@PathVariable("id") Long id, @RequestBody(required = false) LimitAdjustReviewDTO dto) {
        creditLimitManagementService.approveIncrease(id, dto != null ? dto.getComment() : null);
        return Result.success();
    }

    @PostMapping("/pending-requests/{id}/reject")
    public Result<Void> reject(@PathVariable("id") Long id, @RequestBody(required = false) LimitAdjustReviewDTO dto) {
        creditLimitManagementService.rejectIncrease(id, dto != null ? dto.getComment() : null);
        return Result.success();
    }

    /** 风控手动触发全量批跑（演示/运维） */
    @PostMapping("/run-adjustment-batch")
    public Result<Void> runBatch() {
        creditLimitManagementService.triggerManualRiskBatch();
        return Result.success();
    }
}
