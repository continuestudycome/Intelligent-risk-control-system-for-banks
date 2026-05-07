package com.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("loan_approval_record")
public class LoanApprovalRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long applicationId;
    private Integer reviewLevel;
    private Long reviewerId;
    private String reviewerName;
    private String action;
    private String comment;
    private Integer nextLevel;
    private LocalDateTime createTime;
}
