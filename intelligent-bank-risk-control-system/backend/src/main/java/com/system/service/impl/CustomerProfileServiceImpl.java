package com.system.service.impl;

import com.system.domain.CustCustomer;
import com.system.domain.SysUser;
import com.system.dto.CustomerProfileSaveDTO;
import com.system.exception.ApiException;
import com.system.mapper.CustCustomerMapper;
import com.system.mapper.UserMapper;
import com.system.service.CustomerProfileService;
import com.system.vo.CustomerProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CustomerProfileServiceImpl implements CustomerProfileService {

    private final UserMapper userMapper;
    private final CustCustomerMapper custCustomerMapper;

    @Override
    public CustomerProfileVO getMyProfile() {
        SysUser user = getCurrentUser();
        ensureCustomer(user.getId());
        CustCustomer profile = custCustomerMapper.selectByUserId(user.getId());
        if (profile == null) {
            return CustomerProfileVO.builder()
                    .customerType(1)
                    .realName(user.getRealName())
                    .phone(user.getPhone())
                    .email(user.getEmail())
                    .creditAuthorized(false)
                    .profileCompleted(false)
                    .build();
        }
        CustomerProfileVO vo = toVO(profile);
        // 兼容历史脏数据：cust_customer 已有记录但字段为空时，使用注册信息回填展示
        if (isBlank(vo.getRealName())) {
            vo.setRealName(user.getRealName());
        }
        if (isBlank(vo.getPhone())) {
            vo.setPhone(user.getPhone());
        }
        if (isBlank(vo.getEmail())) {
            vo.setEmail(user.getEmail());
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CustomerProfileVO saveMyProfile(CustomerProfileSaveDTO dto) {
        SysUser user = getCurrentUser();
        ensureCustomer(user.getId());

        if (Boolean.FALSE.equals(dto.getCreditAuthorized())) {
            throw new ApiException(400, "请先完成征信授权后再保存");
        }

        CustCustomer profile = custCustomerMapper.selectByUserId(user.getId());
        if (profile == null) {
            profile = new CustCustomer();
            profile.setUserId(user.getId());
            profile.setCustomerNo("CUST" + user.getId() + System.currentTimeMillis() % 100000);
            profile.setIsDeleted(0);
            profile.setStatus(1);
            profile.setIsBlacklist(0);
            profile.setCreateTime(LocalDateTime.now());
        }

        profile.setCustomerType(dto.getCustomerType());
        profile.setRealName(dto.getRealName());
        profile.setIdCardNo(dto.getIdCardNo());
        profile.setPhone(dto.getPhone());
        profile.setEmail(dto.getEmail());
        profile.setProvince(dto.getProvince());
        profile.setCity(dto.getCity());
        profile.setAddress(dto.getAddress());
        profile.setAnnualIncome(dto.getAnnualIncome());
        profile.setAssetAmount(dto.getAssetAmount());
        profile.setCreditAuthorized(Boolean.TRUE.equals(dto.getCreditAuthorized()) ? 1 : 0);
        profile.setProfileCompleted(1);
        profile.setUpdateTime(LocalDateTime.now());

        if (profile.getId() == null) {
            custCustomerMapper.insert(profile);
        } else {
            custCustomerMapper.updateById(profile);
        }

        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        userMapper.updateById(user);

        return toVO(profile);
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
            throw new ApiException(403, "仅客户账号可维护个人中心信息");
        }
    }

    private CustomerProfileVO toVO(CustCustomer profile) {
        return CustomerProfileVO.builder()
                .customerType(profile.getCustomerType())
                .realName(profile.getRealName())
                .idCardNo(profile.getIdCardNo())
                .phone(profile.getPhone())
                .email(profile.getEmail())
                .province(profile.getProvince())
                .city(profile.getCity())
                .address(profile.getAddress())
                .annualIncome(profile.getAnnualIncome())
                .assetAmount(profile.getAssetAmount())
                .creditAuthorized(profile.getCreditAuthorized() != null && profile.getCreditAuthorized() == 1)
                .profileCompleted(profile.getProfileCompleted() != null && profile.getProfileCompleted() == 1)
                .build();
    }

    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
}
