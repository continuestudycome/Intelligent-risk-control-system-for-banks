package com.system.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CustomerProfileSaveDTO {
    @NotNull(message = "客户类型不能为空")
    @Min(value = 1, message = "客户类型非法")
    @Max(value = 2, message = "客户类型非法")
    private Integer customerType;

    @NotBlank(message = "姓名/企业名称不能为空")
    @Size(max = 128, message = "姓名/企业名称长度不能超过128")
    private String realName;

    @NotBlank(message = "证件号不能为空")
    @Size(max = 64, message = "证件号长度不能超过64")
    private String idCardNo;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过128")
    private String email;

    @NotBlank(message = "省份不能为空")
    @Size(max = 64, message = "省份长度不能超过64")
    private String province;

    @NotBlank(message = "城市不能为空")
    @Size(max = 64, message = "城市长度不能超过64")
    private String city;

    @NotBlank(message = "地址不能为空")
    @Size(max = 512, message = "地址长度不能超过512")
    private String address;

    @NotNull(message = "年收入不能为空")
    @DecimalMin(value = "0.00", message = "年收入不能小于0")
    @Digits(integer = 14, fraction = 2, message = "年收入格式不正确")
    private BigDecimal annualIncome;

    @NotNull(message = "资产总额不能为空")
    @DecimalMin(value = "0.00", message = "资产总额不能小于0")
    @Digits(integer = 14, fraction = 2, message = "资产总额格式不正确")
    private BigDecimal assetAmount;

    @NotNull(message = "请确认征信授权")
    private Boolean creditAuthorized;
}
