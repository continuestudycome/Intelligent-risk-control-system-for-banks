package com.system.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CustomerAccountVO {
    private Long id;
    private String accountNo;
    private String accountName;
    private Integer accountType;
    private String accountTypeName;
    private BigDecimal balance;
    private String currency;
    private Integer status;
}
