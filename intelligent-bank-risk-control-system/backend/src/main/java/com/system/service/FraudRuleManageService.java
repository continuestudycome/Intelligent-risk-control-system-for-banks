package com.system.service;

import com.system.dto.FraudRuleUpdateDTO;
import com.system.dto.FraudRuleValidateBatchDTO;
import com.system.vo.FraudRuleVO;
import com.system.vo.FraudRuleValidateResultVO;

import java.util.List;
import java.util.Map;

public interface FraudRuleManageService {

    List<FraudRuleVO> listForRisk();

    FraudRuleVO update(Long id, FraudRuleUpdateDTO dto);

    FraudRuleValidateResultVO validateWithAi(FraudRuleValidateBatchDTO dto);

    List<Map<String, Object>> runDefaultSimulation();
}
