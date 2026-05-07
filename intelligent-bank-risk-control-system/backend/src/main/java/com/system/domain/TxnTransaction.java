package com.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("txn_transaction")
public class TxnTransaction {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String transactionNo;
    private Long customerId;
    private Integer transactionType;
    private String fromAccount;
    private String toAccount;
    private BigDecimal amount;
    private String currency;
    private String transactionProvince;
    private String transactionCity;
    private LocalDateTime transactionTime;
    private String remark;
    private String riskStatus;
    private Integer handleResult;
    private LocalDateTime createTime;
}
