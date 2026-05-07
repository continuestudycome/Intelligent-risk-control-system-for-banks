package com.system.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FraudRuleVO {
    private Long id;
    private String ruleCode;
    private String ruleName;
    private Integer ruleType;
    private String ruleCondition;
    private String riskLevel;
    private Integer priority;
    private Integer hitThreshold;
    private Integer status;
    private Integer version;
    private LocalDateTime effectiveTime;
    private LocalDateTime expireTime;
    private String remark;
    private LocalDateTime updateTime;
}
