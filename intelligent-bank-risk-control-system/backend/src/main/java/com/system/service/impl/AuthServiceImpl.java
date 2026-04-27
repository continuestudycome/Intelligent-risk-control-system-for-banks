package com.system.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.util.IdUtil;
import com.system.dto.LoginDTO;
import com.system.dto.RegisterDTO;
import com.system.domain.User;
import com.system.exception.BusinessException;
import com.system.mapper.UserMapper;
import com.system.service.AuthService;
import com.system.utils.JwtUtil;
import com.system.vo.TokenVO;
import com.system.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CAPTCHA_KEY_PREFIX = "captcha:";
    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";
    private static final String REFRESH_TOKEN_PREFIX = "refresh:token:";

    @Override
    public TokenVO login(LoginDTO loginDTO) {
        // 1. 验证验证码（如果启用）
        // validateCaptcha(loginDTO.getCaptcha(), loginDTO.getUsername());

        // 2. 认证
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginDTO.getUsername(),
                            loginDTO.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            log.warn("登录失败，用户名或密码错误: {}", loginDTO.getUsername());
            throw new BusinessException(401, "用户名或密码错误");
        }

        // 3. 获取用户信息
        org.springframework.security.core.userdetails.User securityUser =
                (org.springframework.security.core.userdetails.User) authentication.getPrincipal();

        User user = userMapper.selectByUsername(securityUser.getUsername());
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }

        // 4. 生成Token
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getUserType());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        // 5. 缓存刷新令牌
        String refreshKey = REFRESH_TOKEN_PREFIX + user.getId();
        redisTemplate.opsForValue().set(refreshKey, refreshToken, 7, TimeUnit.DAYS);

        // 6. 记录登录日志
        recordLoginLog(user.getId(), user.getUsername(), "LOGIN_SUCCESS");

        log.info("用户登录成功: {}", user.getUsername());

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
                        .userType(user.getUserType())
                        .status(user.getStatus())
                        .riskLevel(user.getRiskLevel())
                        .build())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterDTO registerDTO) {
        // 1. 校验密码一致性
        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            throw new BusinessException(400, "两次输入的密码不一致");
        }

        // 2. 检查用户名是否已存在
        if (userMapper.selectByUsername(registerDTO.getUsername()) != null) {
            throw new BusinessException(400, "用户名已存在");
        }

        // 3. 检查手机号是否已存在
        if (userMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getPhone, registerDTO.getPhone())
        ) > 0) {
            throw new BusinessException(400, "手机号已被注册");
        }

        // 4. 创建用户
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setRealName(registerDTO.getRealName());
        user.setPhone(registerDTO.getPhone());
        user.setEmail(registerDTO.getEmail());
        user.setIdCard(registerDTO.getIdCard());
        user.setUserType(registerDTO.getUserType());
        user.setStatus(1);
        user.setRiskLevel(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setDeleted(0);

        userMapper.insert(user);

        log.info("用户注册成功: {}", registerDTO.getUsername());
    }

    @Override
    public TokenVO refreshToken(String refreshToken) {
        // 1. 验证刷新令牌
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new BusinessException(401, "刷新令牌已过期，请重新登录");
        }

        Long userId = jwtUtil.getUserIdFromToken(refreshToken);

        // 2. 检查Redis中是否存在
        String refreshKey = REFRESH_TOKEN_PREFIX + userId;
        String storedToken = (String) redisTemplate.opsForValue().get(refreshKey);

        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new BusinessException(401, "刷新令牌无效，请重新登录");
        }

        // 3. 获取用户信息
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == 0) {
            throw new BusinessException(401, "用户不存在或已被禁用");
        }

        // 4. 生成新的Token
        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getUserType());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getId());

        // 5. 更新刷新令牌
        redisTemplate.opsForValue().set(refreshKey, newRefreshToken, 7, TimeUnit.DAYS);

        return TokenVO.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(86400L)
                .expireTime(LocalDateTime.now().plusDays(1))
                .userInfo(UserInfoVO.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .realName(user.getRealName())
                        .phone(maskPhone(user.getPhone()))
                        .userType(user.getUserType())
                        .status(user.getStatus())
                        .riskLevel(user.getRiskLevel())
                        .build())
                .build();
    }

    @Override
    public void logout(String token) {
        if (token != null && jwtUtil.validateToken(token)) {
            // 将Token加入黑名单
            long expiration = jwtUtil.getExpirationTime(token);
            if (expiration > 0) {
                String blacklistKey = TOKEN_BLACKLIST_PREFIX + token;
                redisTemplate.opsForValue().set(blacklistKey, "1", expiration, TimeUnit.MILLISECONDS);
            }

            Long userId = jwtUtil.getUserIdFromToken(token);
            // 删除刷新令牌
            redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);

            log.info("用户登出: userId={}", userId);
        }
    }

    @Override
    public String getCaptcha(String uuid) {
        if (uuid == null || uuid.isEmpty()) {
            uuid = IdUtil.simpleUUID();
        }

        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 20);
        String code = captcha.getCode();

        // 缓存验证码，5分钟有效
        redisTemplate.opsForValue().set(CAPTCHA_KEY_PREFIX + uuid, code, 5, TimeUnit.MINUTES);

        return captcha.getImageBase64();
    }

    /**
     * 验证验证码
     */
    private void validateCaptcha(String captcha, String uuid) {
        if (captcha == null || uuid == null) {
            throw new BusinessException(400, "验证码不能为空");
        }

        String key = CAPTCHA_KEY_PREFIX + uuid;
        String storedCode = (String) redisTemplate.opsForValue().get(key);

        if (storedCode == null) {
            throw new BusinessException(400, "验证码已过期");
        }

        if (!storedCode.equalsIgnoreCase(captcha)) {
            throw new BusinessException(400, "验证码错误");
        }

        // 验证成功后删除
        redisTemplate.delete(key);
    }

    /**
     * 记录登录日志
     */
    private void recordLoginLog(Long userId, String username, String action) {
        // 实际项目中可以保存到数据库或发送到消息队列
        log.info("登录日志 - userId: {}, username: {}, action: {}, time: {}",
                userId, username, action, LocalDateTime.now());
    }

    /**
     * 手机号脱敏
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
