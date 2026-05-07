package com.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("frd_case")
public class FrdCase {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long transactionId;
    private Long alertId;
    private Long customerId;
    private String fraudType;
    private Integer confirmedResult;
    private String featureSnapshot;
    private Integer labelSource;
    private LocalDateTime createTime;
}
