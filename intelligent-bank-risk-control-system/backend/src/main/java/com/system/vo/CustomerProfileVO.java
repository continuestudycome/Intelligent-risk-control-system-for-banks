package com.system.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CustomerProfileVO {
    private Integer customerType;
    private String realName;
    private String idCardNo;
    private String phone;
    private String email;
    private String province;
    private String city;
    private String address;
    private BigDecimal annualIncome;
    private BigDecimal assetAmount;
    private Boolean creditAuthorized;
    private Boolean profileCompleted;
}
