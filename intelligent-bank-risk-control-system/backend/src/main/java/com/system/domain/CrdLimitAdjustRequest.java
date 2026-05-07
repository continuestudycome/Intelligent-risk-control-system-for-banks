package com.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("crd_limit_adjust_request")
public class CrdLimitAdjustRequest {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long customerId;
    private BigDecimal currentTotalLimit;
    private BigDecimal proposedTotalLimit;
    private Integer triggerScore;
    private String triggerRiskLevel;
    private String reason;
    private String status;
    private Long reviewerId;
    private String reviewComment;
    private LocalDateTime createTime;
    private LocalDateTime reviewTime;
}
