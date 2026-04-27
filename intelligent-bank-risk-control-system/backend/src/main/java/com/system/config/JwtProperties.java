package com.system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "risk.jwt")
public class JwtProperties {

    private String secret = "your-256-bit-secret-key-here-must-be-at-least-32-characters-long";
    private Long accessTokenExpiration = 86400000L;   // 24小时
    private Long refreshTokenExpiration = 604800000L; // 7天
}
