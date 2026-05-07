package com.system.service;

import com.system.vo.CustomerRiskProfileVO;

public interface CustomerRiskProfileService {

    /**
     * 聚合客户基础信息、信用概况、近窗交易风险统计、欺诈告警与信贷申请摘要（仅风控）。
     */
    CustomerRiskProfileVO getProfile(Long customerId);
}
