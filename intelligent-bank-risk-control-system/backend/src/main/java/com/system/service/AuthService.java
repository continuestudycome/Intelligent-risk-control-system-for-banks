package com.system.service;

import com.system.dto.LoginDTO;
import com.system.dto.RegisterDTO;
import com.system.vo.TokenVO;

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
