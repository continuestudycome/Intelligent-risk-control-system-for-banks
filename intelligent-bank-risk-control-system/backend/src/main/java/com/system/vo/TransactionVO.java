package com.system.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionVO {
    private Long id;
    private String transactionNo;
    private Integer transactionType;
    private String transactionTypeName;
    private String fromAccount;
    private String toAccount;
    private BigDecimal amount;
    private String transactionProvince;
    private String transactionCity;
    private String purpose;
    private String riskStatus;
    private String riskStatusName;
    private Integer handleResult;
    private String handleResultName;
    private LocalDateTime transactionTime;
    /** 业务提示：如拦截原因、需二次验证说明（仅部分接口返回） */
    private String riskMessage;
    /** 风控端：客户主键 */
    private Long customerId;
    /** 风控端：客户姓名 */
    private String customerName;
    /** 风控端：客户手机号 */
    private String customerPhone;
}
