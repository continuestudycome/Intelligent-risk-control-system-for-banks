package com.system.controller;

import com.bank.risk.common.Result;
import com.bank.risk.dto.LoginDTO;
import com.bank.risk.dto.RegisterDTO;
import com.bank.risk.service.AuthService;
import com.bank.risk.vo.TokenVO;
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
    public Result<TokenVO> refreshToken(@RequestBody Map<String, String> params) {
        String refreshToken = params.get("refreshToken");
        if (refreshToken == null || refreshToken.isEmpty()) {
            return Result.error(400, "刷新令牌不能为空");
        }
        return Result.success(authService.refreshToken(refreshToken));
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
            @RequestParam(required = false) String uuid) {
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
