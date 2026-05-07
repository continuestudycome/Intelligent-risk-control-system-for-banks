package com.system.service;

import com.system.dto.FraudAlertReviewDTO;
import com.system.vo.FraudAlertVO;
import com.system.vo.FraudCaseVO;

import java.util.List;

public interface FraudAlertManageService {

    List<FraudAlertVO> listAlerts(String status);

    FraudAlertVO review(Long alertId, FraudAlertReviewDTO dto);

    List<FraudCaseVO> listConfirmedCases();
}
