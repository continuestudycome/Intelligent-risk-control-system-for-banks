package com.system.service;

import com.system.vo.TransactionVO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 风控端：全行交易流水查询 */
public interface RiskTransactionQueryService {

    List<TransactionVO> listTransactions(
            Long customerId,
            String customerKeyword,
            Integer transactionType,
            String riskStatus,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            LocalDateTime startTime,
            LocalDateTime endTime);

    TransactionVO getTransactionDetail(Long id);

    /**
     * 近期非低风险交易流水（用于风险预警盯盘），按时间倒序截断。
     */
    List<TransactionVO> listRecentRiskEvents(LocalDateTime since, int limit);
}
