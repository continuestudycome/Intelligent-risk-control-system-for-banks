package com.system.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TokenVO {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private LocalDateTime expireTime;
    private UserInfoVO userInfo;
}
