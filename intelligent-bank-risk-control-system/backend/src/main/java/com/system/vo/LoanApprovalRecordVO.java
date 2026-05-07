package com.system.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LoanApprovalRecordVO {
    private Long id;
    private Integer reviewLevel;
    private String reviewerName;
    private String action;
    private String comment;
    private LocalDateTime createTime;
}
