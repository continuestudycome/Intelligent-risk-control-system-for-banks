package com.system.service;

import com.system.dto.LoanApplicationCreateDTO;
import com.system.dto.LoanApplicationReviewDTO;
import com.system.vo.LoanApplicationVO;

import java.util.List;

public interface LoanApplicationService {

    LoanApplicationVO createApplication(LoanApplicationCreateDTO dto);

    List<LoanApplicationVO> listMyApplications();

    LoanApplicationVO getMyApplicationDetail(Long id);

    List<LoanApplicationVO> listForRisk(String currentStatus, Integer applyType);

    LoanApplicationVO getRiskApplicationDetail(Long id);

    LoanApplicationVO review(Long applicationId, LoanApplicationReviewDTO dto);
}
