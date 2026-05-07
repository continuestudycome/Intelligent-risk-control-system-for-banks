package com.system.service.impl;

import com.system.domain.AcctAccount;
import com.system.domain.CustCustomer;
import com.system.domain.SysUser;
import com.system.exception.ApiException;
import com.system.mapper.AcctAccountMapper;
import com.system.mapper.CustCustomerMapper;
import com.system.mapper.UserMapper;
import com.system.service.CustomerAccountService;
import com.system.vo.CustomerAccountVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerAccountServiceImpl implements CustomerAccountService {

    private final UserMapper userMapper;
    private final CustCustomerMapper custCustomerMapper;
    private final AcctAccountMapper acctAccountMapper;

    @Override
    public List<CustomerAccountVO> listMyAccounts() {
        SysUser user = getCurrentUser();
        ensureCustomer(user.getId());
        CustCustomer customer = getCustomerOrThrow(user.getId());
        return acctAccountMapper.selectByCustomerId(customer.getId()).stream()
                .map(this::toVO)
                .toList();
    }

    private CustomerAccountVO toVO(AcctAccount account) {
        return CustomerAccountVO.builder()
                .id(account.getId())
                .accountNo(account.getAccountNo())
                .accountName(account.getAccountName())
                .accountType(account.getAccountType())
                .accountTypeName(typeName(account.getAccountType()))
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .status(account.getStatus())
                .build();
    }

    private String typeName(Integer type) {
        if (type == null) return "未知";
        return switch (type) {
            case 1 -> "借记卡";
            case 2 -> "信用卡";
            case 3 -> "对公账户";
            default -> "未知";
        };
    }

    private SysUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User principal)) {
            throw new ApiException(401, "未登录或登录已失效");
        }
        SysUser user = userMapper.selectByUsername(principal.getUsername());
        if (user == null || (user.getIsDeleted() != null && user.getIsDeleted() == 1)) {
            throw new ApiException(401, "用户不存在");
        }
        return user;
    }

    private void ensureCustomer(Long userId) {
        String roleCode = userMapper.selectPrimaryRoleCode(userId);
        if (!"CUSTOMER".equalsIgnoreCase(roleCode)) {
            throw new ApiException(403, "仅客户可访问账户");
        }
    }

    private CustCustomer getCustomerOrThrow(Long userId) {
        CustCustomer customer = custCustomerMapper.selectByUserId(userId);
        if (customer == null || customer.getIsDeleted() != null && customer.getIsDeleted() == 1) {
            throw new ApiException(400, "客户档案不存在，请先完善个人中心信息");
        }
        return customer;
    }
}
