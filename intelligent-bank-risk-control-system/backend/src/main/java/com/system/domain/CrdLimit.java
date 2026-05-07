package com.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("crd_limit")
public class CrdLimit {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long customerId;
    private BigDecimal totalLimit;
    private BigDecimal usedLimit;
    private BigDecimal availableLimit;
    private BigDecimal singleLimit;
    private BigDecimal dailyLimit;
    private LocalDateTime lastAdjustTime;
    private String remark;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
    private Integer isDeleted;
}
