package com.system.service;

import com.bank.risk.dto.LoginDTO;
import com.bank.risk.dto.RegisterDTO;
import com.bank.risk.vo.TokenVO;

public interface AuthService {

    /**
     * 用户登录
     */
    TokenVO login(LoginDTO loginDTO);

    /**
     * 用户注册
     */
    void register(RegisterDTO registerDTO);

    /**
     * 刷新令牌
     */
    TokenVO refreshToken(String refreshToken);

    /**
     * 用户登出
     */
    void logout(String token);

    /**
     * 获取验证码
     */
    String getCaptcha(String uuid);
}
