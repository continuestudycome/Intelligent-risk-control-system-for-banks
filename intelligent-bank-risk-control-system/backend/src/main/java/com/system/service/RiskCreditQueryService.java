package com.system.service;

import com.system.vo.CreditCustomerBriefVO;
import com.system.vo.CreditScoreOverviewVO;

import java.util.List;

/** 风控侧：按客户查询信用评分概况与检索客户 */
public interface RiskCreditQueryService {

    List<CreditCustomerBriefVO> searchCustomers(String keyword);

    CreditScoreOverviewVO getCustomerOverview(Long customerId);
}
