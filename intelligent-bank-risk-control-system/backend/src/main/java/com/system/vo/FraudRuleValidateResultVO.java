package com.system.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class FraudRuleValidateResultVO {
    private boolean ok;
    private List<String> errors;
    private List<String> hints;
}
