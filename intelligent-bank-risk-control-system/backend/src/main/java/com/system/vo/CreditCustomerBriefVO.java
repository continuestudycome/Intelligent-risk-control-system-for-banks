package com.system.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreditCustomerBriefVO {
    private Long customerId;
    private String realName;
    private String phone;
    private String customerNo;
    /** 当前档案中的信用等级（最近一次评估会同步） */
    private String creditLevel;
}
