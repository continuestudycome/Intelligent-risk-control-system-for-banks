package com.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.system.dto.LoginDTO;
import com.system.dto.RegisterDTO;
import com.system.domain.SysRole;
import com.system.domain.SysUser;
import com.system.domain.SysUserRole;
import com.system.domain.CustCustomer;
import com.system.mapper.CustCustomerMapper;
import com.system.mapper.RoleMapper;
import com.system.mapper.UserMapper;
import com.system.mapper.UserRoleMapper;
import com.system.service.AuthService;
import com.system.utils.JwtUtil;
import com.system.exception.ApiException;
import com.system.vo.TokenVO;
import com.system.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final CustCustomerMapper custCustomerMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public TokenVO login(LoginDTO loginDTO) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword())
            );
            org.springframework.security.core.userdetails.User principal =
                (org.springframework.security.core.userdetails.User) authentication.getPrincipal();

            SysUser user = userMapper.selectByUsername(principal.getUsername());
            String roleCode = userMapper.selectPrimaryRoleCode(user.getId());
            int userType = ("RISK_OFFICER".equalsIgnoreCase(roleCode) || "RISK_MANAGER".equalsIgnoreCase(roleCode)) ? 2 : 1;

            String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), userType);
            String refreshToken = jwtUtil.generateRefreshToken(user.getId());

            user.setLastLoginTime(LocalDateTime.now());
            userMapper.updateById(user);

            log.info("用户登录成功: {}", user.getUsername());
            return buildTokenVO(user, accessToken, refreshToken, userType);
        } catch (BadCredentialsException e) {
            throw new ApiException(401, "用户名或密码错误");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterDTO registerDTO) {
        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            throw new ApiException(400, "两次输入的密码不一致");
        }

        SysUser exists = userMapper.selectByUsername(registerDTO.getUsername());
        if (exists != null) {
            throw new ApiException(400, "用户名已存在");
        }

        Long phoneCount = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
            .eq(SysUser::getPhone, registerDTO.getPhone())
            .eq(SysUser::getIsDeleted, 0));
        if (phoneCount != null && phoneCount > 0) {
            throw new ApiException(400, "手机号已被注册");
        }

        SysUser user = new SysUser();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setRealName(registerDTO.getRealName());
        user.setPhone(registerDTO.getPhone());
        user.setEmail(registerDTO.getEmail());
        user.setStatus(1);
        user.setIsDeleted(0);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);

        SysRole role = ensureRole("CUSTOMER", "客户");

        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(role.getId());
        userRoleMapper.insert(userRole);

        log.info("用户注册成功: {}, role=CUSTOMER", registerDTO.getUsername());
    }

    @Override
    public TokenVO refreshToken(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new ApiException(401, "刷新令牌无效，请重新登录");
        }
        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        SysUser user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new ApiException(401, "用户不存在或已禁用");
        }
        String roleCode = userMapper.selectPrimaryRoleCode(userId);
        int userType = ("RISK_OFFICER".equalsIgnoreCase(roleCode) || "RISK_MANAGER".equalsIgnoreCase(roleCode)) ? 2 : 1;
        String newAccessToken = jwtUtil.generateAccessToken(userId, user.getUsername(), userType);
        String newRefreshToken = jwtUtil.generateRefreshToken(userId);
        return buildTokenVO(user, newAccessToken, newRefreshToken, userType);
    }

    @Override
    public void logout(String token) {
        log.info("用户登出: {}", token);
    }

    @Override
    public String getCaptcha(String uuid) {
        return "UklTSw==";
    }

    private TokenVO buildTokenVO(SysUser user, String accessToken, String refreshToken, Integer userType) {
        CustCustomer profile = custCustomerMapper.selectByUserId(user.getId());
        boolean profileCompleted = profile != null && profile.getProfileCompleted() != null && profile.getProfileCompleted() == 1;
        return TokenVO.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(86400L)
            .expireTime(LocalDateTime.now().plusDays(1))
            .userInfo(UserInfoVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(maskPhone(user.getPhone()))
                .email(user.getEmail())
                .userType(userType)
                .status(1)
                .riskLevel(1)
                .profileCompleted(profileCompleted)
                .build())
            .build();
    }

    private SysRole ensureRole(String roleCode, String roleName) {
        SysRole role = roleMapper.selectByRoleCode(roleCode);
        if (role != null) {
            return role;
        }
        SysRole newRole = new SysRole();
        newRole.setRoleCode(roleCode);
        newRole.setRoleName(roleName);
        newRole.setStatus(1);
        newRole.setIsDeleted(0);
        roleMapper.insert(newRole);
        return newRole;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
