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
public class LoanApplicationCreateDTO {
    @NotNull(message = "申请类型不能为空")
    @Min(value = 1, message = "申请类型不合法")
    @Max(value = 3, message = "申请类型不合法")
    private Integer applyType;

    @NotNull(message = "申请金额不能为空")
    @DecimalMin(value = "1.00", message = "申请金额必须大于0")
    @Digits(integer = 14, fraction = 2, message = "申请金额格式不正确")
    private BigDecimal applyAmount;

    /** 期限(月)；信用卡可不填或填0，由业务层校验 */
    private Integer applyTerm;

    @NotBlank(message = "资金用途不能为空")
    @Size(max = 256, message = "资金用途过长")
    private String applyPurpose;
}
