package com.system.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class LoanApplicationVO {
    private Long id;
    private String applicationNo;
    private Integer applyType;
    private String applyTypeName;
    private BigDecimal applyAmount;
    private Integer applyTerm;
    private String applyPurpose;
    private String currentStatus;
    private String currentStatusName;
    private Integer finalResult;
    private String finalResultName;
    private BigDecimal finalAmount;
    private Integer finalTerm;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<LoanApprovalRecordVO> approvalRecords;
}
