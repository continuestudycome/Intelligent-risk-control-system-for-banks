package com.system.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class LoanAppProfileRowVO {
    private Long id;
    private String applicationNo;
    private String applyTypeName;
    private String currentStatus;
    private String currentStatusName;
    private BigDecimal applyAmount;
    private LocalDateTime createTime;
}
