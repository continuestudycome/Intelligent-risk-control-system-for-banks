package com.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FraudRuleUpdateDTO {
    @NotBlank(message = "规则参数 JSON 不能为空")
    private String ruleCondition;
    private String riskLevel;
    private Integer priority;
    /** 0 停用 1 启用 */
    private Integer status;
    private String remark;
}
