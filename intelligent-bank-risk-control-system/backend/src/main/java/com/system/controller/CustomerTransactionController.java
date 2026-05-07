package com.system.controller;

import com.system.common.Result;
import com.system.dto.TransactionCreateDTO;
import com.system.service.CustomerTransactionService;
import com.system.vo.TransactionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/customer/transaction")
@RequiredArgsConstructor
public class CustomerTransactionController {

    private final CustomerTransactionService customerTransactionService;

    @PostMapping
    public Result<TransactionVO> create(@Valid @RequestBody TransactionCreateDTO dto) {
        TransactionVO vo = customerTransactionService.createTransaction(dto);
        String msg = vo.getRiskMessage() != null && !vo.getRiskMessage().isEmpty()
                ? vo.getRiskMessage()
                : "success";
        return Result.success(vo, msg);
    }

    @GetMapping("/records")
    public Result<List<TransactionVO>> records(
            @RequestParam(value = "transactionType", required = false) Integer transactionType,
            @RequestParam(value = "riskStatus", required = false) String riskStatus,
            @RequestParam(value = "minAmount", required = false) BigDecimal minAmount,
            @RequestParam(value = "maxAmount", required = false) BigDecimal maxAmount,
            @RequestParam(value = "startTime", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(value = "endTime", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime
    ) {
        return Result.success(customerTransactionService.queryMyTransactions(
                transactionType, riskStatus, minAmount, maxAmount, startTime, endTime
        ));
    }

    @GetMapping("/{id}")
    public Result<TransactionVO> detail(@PathVariable("id") Long id) {
        return Result.success(customerTransactionService.getMyTransactionDetail(id));
    }
}
