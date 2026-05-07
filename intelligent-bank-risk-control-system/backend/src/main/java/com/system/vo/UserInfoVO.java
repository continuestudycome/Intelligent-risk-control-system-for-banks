package com.system.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserInfoVO {
    private Long id;
    private String username;
    private String realName;
    private String phone;
    private String email;
    private Integer userType;
    private Integer status;
    private Integer riskLevel;
    private Boolean profileCompleted;
}
