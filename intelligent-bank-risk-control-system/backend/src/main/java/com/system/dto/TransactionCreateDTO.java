package com.system.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionCreateDTO {
    @NotNull(message = "交易类型不能为空")
    @Min(value = 1, message = "交易类型不合法")
    @Max(value = 4, message = "交易类型不合法")
    private Integer transactionType;

    @NotBlank(message = "付款账户不能为空")
    @Size(max = 64, message = "付款账户长度不能超过64")
    private String fromAccount;

    @NotBlank(message = "收款账户不能为空")
    @Size(max = 64, message = "收款账户长度不能超过64")
    private String toAccount;

    @NotNull(message = "交易金额不能为空")
    @DecimalMin(value = "0.01", message = "交易金额必须大于0")
    @Digits(integer = 14, fraction = 2, message = "交易金额格式不正确")
    private BigDecimal amount;

    @NotBlank(message = "交易省份不能为空")
    @Size(max = 64, message = "交易省份长度不能超过64")
    private String transactionProvince;

    @NotBlank(message = "交易城市不能为空")
    @Size(max = 64, message = "交易城市长度不能超过64")
    private String transactionCity;

    @NotBlank(message = "交易用途不能为空")
    @Size(max = 512, message = "交易用途长度不能超过512")
    private String purpose;
}
