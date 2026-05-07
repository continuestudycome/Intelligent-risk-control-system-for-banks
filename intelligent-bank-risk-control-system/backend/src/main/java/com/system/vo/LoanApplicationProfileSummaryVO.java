package com.system.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LoanApplicationProfileSummaryVO {
    private int pendingCount;
    private int approvedCount;
    private int rejectedCount;
    private int otherStatusCount;
    private List<LoanAppProfileRowVO> recentApplications;
}
