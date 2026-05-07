package com.system.service.impl;

import com.system.domain.CustCustomer;
import com.system.domain.SysUser;
import com.system.exception.ApiException;
import com.system.mapper.CustCustomerMapper;
import com.system.mapper.UserMapper;
import com.system.service.CustomerCreditService;
import com.system.service.RiskCreditQueryService;
import com.system.vo.CreditCustomerBriefVO;
import com.system.vo.CreditScoreOverviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RiskCreditQueryServiceImpl implements RiskCreditQueryService {

    private final CustCustomerMapper custCustomerMapper;
    private final UserMapper userMapper;
    private final CustomerCreditService customerCreditService;

    @Override
    public List<CreditCustomerBriefVO> searchCustomers(String keyword) {
        ensureRiskStaff();
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        Long idExact = null;
        if (kw != null) {
            try {
                idExact = Long.parseLong(kw);
            } catch (NumberFormatException ignored) {
                // 非纯数字则不按 id 精确匹配
            }
        }
        int pageSize = kw == null ? 30 : 50;
        List<CustCustomer> rows;
        if (kw == null) {
            rows = custCustomerMapper.listCreditBriefRecent(pageSize);
        } else if (idExact != null) {
            rows = custCustomerMapper.listCreditBriefByKeywordWithId(kw, idExact, pageSize);
        } else {
            rows = custCustomerMapper.listCreditBriefByKeywordNoId(kw, pageSize);
        }
        return rows.stream()
                .map(c -> CreditCustomerBriefVO.builder()
                        .customerId(c.getId())
                        .realName(c.getRealName())
                        .phone(c.getPhone())
                        .customerNo(c.getCustomerNo())
                        .creditLevel(c.getCreditLevel())
                        .build())
                .toList();
    }

    @Override
    public CreditScoreOverviewVO getCustomerOverview(Long customerId) {
        return customerCreditService.getCustomerOverviewForRisk(customerId);
    }

    private void ensureRiskStaff() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails ud)) {
            throw new ApiException(401, "未登录或登录已失效");
        }
        SysUser user = userMapper.selectByUsername(ud.getUsername());
        if (user == null || (user.getIsDeleted() != null && user.getIsDeleted() == 1)) {
            throw new ApiException(401, "用户不存在");
        }
        String code = userMapper.selectPrimaryRoleCode(user.getId());
        if (!"RISK_OFFICER".equalsIgnoreCase(code) && !"RISK_MANAGER".equalsIgnoreCase(code)) {
            throw new ApiException(403, "仅风控人员可操作");
        }
    }
}
