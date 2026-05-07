package com.system.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class FraudRuleValidateBatchDTO {
    @NotEmpty(message = "规则列表不能为空")
    private List<Item> rules;

    @Data
    public static class Item {
        private String ruleCode;
        private String ruleCondition;
    }
}
