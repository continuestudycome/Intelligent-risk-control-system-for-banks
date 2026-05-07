package com.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("frd_alert")
public class FrdAlert {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long transactionId;
    private String transactionNo;
    private Long customerId;
    private String alertLevel;
    private String hitRules;
    private BigDecimal mlScore;
    private String mlModelVersion;
    private String featureSnapshot;
    private String status;
    private Long reviewerId;
    private String reviewComment;
    private LocalDateTime reviewTime;
    private LocalDateTime createTime;
}
