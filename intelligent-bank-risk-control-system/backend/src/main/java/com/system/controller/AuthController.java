package com.system.controller;

import com.system.common.Result;
import com.system.dto.LoginDTO;
import com.system.dto.RefreshTokenDTO;
import com.system.dto.RegisterDTO;
import com.system.service.AuthService;
import com.system.vo.TokenVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<TokenVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        TokenVO tokenVO = authService.login(loginDTO);
        return Result.success(tokenVO);
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody RegisterDTO registerDTO) {
        authService.register(registerDTO);
        return Result.success(null);
    }

    /**
     * 刷新令牌
     */
    @PostMapping("/refresh")
    public Result<TokenVO> refreshToken(@Valid @RequestBody RefreshTokenDTO refreshTokenDTO) {
        return Result.success(authService.refreshToken(refreshTokenDTO.getRefreshToken()));
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public Result<?> logout(HttpServletRequest request) {
        String token = extractToken(request);
        authService.logout(token);
        return Result.success(null);
    }

    /**
     * 获取验证码
     */
    @GetMapping("/captcha")
    public Result<Map<String, String>> getCaptcha(
            @RequestParam(name = "uuid", required = false) String uuid) {
        String base64Image = authService.getCaptcha(uuid);
        String captchaUuid = uuid != null ? uuid : java.util.UUID.randomUUID().toString().replace("-", "");

        return Result.success(Map.of(
                "uuid", captchaUuid,
                "image", "data:image/png;base64," + base64Image
        ));
    }

    /**
     * 从请求头中提取Token
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
