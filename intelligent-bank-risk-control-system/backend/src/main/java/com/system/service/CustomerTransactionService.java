package com.system.service;

import com.system.dto.TransactionCreateDTO;
import com.system.vo.TransactionVO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface CustomerTransactionService {
    TransactionVO createTransaction(TransactionCreateDTO dto);

    List<TransactionVO> queryMyTransactions(
            Integer transactionType,
            String riskStatus,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    TransactionVO getMyTransactionDetail(Long id);
}
