package com.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("loan_application")
public class LoanApplication {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String applicationNo;
    private Long customerId;
    private Integer applyType;
    private BigDecimal applyAmount;
    private Integer applyTerm;
    private String applyPurpose;
    private Integer creditScore;
    private BigDecimal suggestedLimit;
    private String currentStatus;
    private Integer finalResult;
    private BigDecimal finalAmount;
    private Integer finalTerm;
    private String remark;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
    private Integer isDeleted;
}
