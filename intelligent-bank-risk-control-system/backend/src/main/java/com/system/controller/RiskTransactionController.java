package com.system.controller;

import com.system.common.Result;
import com.system.service.RiskTransactionQueryService;
import com.system.vo.TransactionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/risk/transaction")
@RequiredArgsConstructor
public class RiskTransactionController {

    private final RiskTransactionQueryService riskTransactionQueryService;

    /**
     * 全行交易流水（默认最多 500 条，条件筛选）。customerId 优先于 customerKeyword。
     */
    @GetMapping("/records")
    public Result<List<TransactionVO>> records(
            @RequestParam(name = "customerId", required = false) Long customerId,
            @RequestParam(name = "customerKeyword", required = false) String customerKeyword,
            @RequestParam(name = "transactionType", required = false) Integer transactionType,
            @RequestParam(name = "riskStatus", required = false) String riskStatus,
            @RequestParam(name = "minAmount", required = false) BigDecimal minAmount,
            @RequestParam(name = "maxAmount", required = false) BigDecimal maxAmount,
            @RequestParam(name = "startTime", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(name = "endTime", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return Result.success(riskTransactionQueryService.listTransactions(
                customerId,
                customerKeyword,
                transactionType,
                riskStatus,
                minAmount,
                maxAmount,
                startTime,
                endTime));
    }

    @GetMapping("/{id}")
    public Result<TransactionVO> detail(@PathVariable("id") Long id) {
        return Result.success(riskTransactionQueryService.getTransactionDetail(id));
    }
}
