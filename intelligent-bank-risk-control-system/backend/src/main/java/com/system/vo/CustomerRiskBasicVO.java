package com.system.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CustomerRiskBasicVO {
    private Long customerId;
    private String customerNo;
    private String realName;
    /** 脱敏手机号 */
    private String phoneMasked;
    /** 脱敏证件号 */
    private String idCardMasked;
    private String province;
    private String city;
    private String creditLevel;
    private Integer isBlacklist;
    private String blacklistReason;
    private BigDecimal annualIncome;
    private BigDecimal assetAmount;
    private Integer profileCompleted;
    private LocalDateTime registerTime;
}
