package com.system.service;

import com.system.domain.CustCustomer;
import com.system.dto.TransactionCreateDTO;
import com.system.fraud.FraudAssessmentResult;

public interface FraudRiskAssessmentService {

    FraudAssessmentResult assess(CustCustomer customer, TransactionCreateDTO dto);
}
