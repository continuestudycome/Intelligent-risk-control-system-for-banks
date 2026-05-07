package com.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("cust_customer")
public class CustCustomer {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String customerNo;
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
    private Integer creditAuthorized;
    private Integer profileCompleted;
    private String creditLevel;
    private Integer isBlacklist;
    private String blacklistReason;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
}
