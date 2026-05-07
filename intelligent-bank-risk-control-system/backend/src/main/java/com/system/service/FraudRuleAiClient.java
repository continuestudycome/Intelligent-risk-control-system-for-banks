package com.system.service;

import com.system.vo.FraudRuleValidateResultVO;

import java.util.List;
import java.util.Map;

/**
 * 调用 Python 智能服务做规则参数校验与试算。
 */
public interface FraudRuleAiClient {

    FraudRuleValidateResultVO validateBatch(List<Map<String, Object>> rules);

    List<Map<String, Object>> simulate(Map<String, Object> thresholds, List<Map<String, Object>> samples);
}
