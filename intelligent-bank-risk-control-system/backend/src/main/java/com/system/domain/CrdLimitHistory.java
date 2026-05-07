package com.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("crd_limit_history")
public class CrdLimitHistory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long customerId;
    /** 1-上调 2-下调 3-冻结 4-解冻 */
    private Integer adjustType;
    private BigDecimal oldLimit;
    private BigDecimal newLimit;
    private String reason;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime createTime;
}
