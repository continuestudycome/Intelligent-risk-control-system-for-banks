package com.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("acct_account")
public class AcctAccount {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String accountNo;
    private Long customerId;
    private String accountName;
    private Integer accountType;
    private BigDecimal balance;
    private String currency;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
}
