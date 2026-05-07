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
public class LoanApplicationReviewDTO {
    @NotBlank(message = "审批结论不能为空")
    private String action;

    @Size(max = 2000, message = "审批意见过长")
    private String comment;

    /** 通过时核定金额，不传则默认等于申请金额 */
    @DecimalMin(value = "0.01", message = "核定金额必须大于0")
    @Digits(integer = 14, fraction = 2, message = "核定金额格式不正确")
    private BigDecimal finalAmount;

    /** 通过时核定期限(月)，不传则默认等于申请期限 */
    @Min(value = 1, message = "核定期限至少为1个月")
    @Max(value = 360, message = "核定期限不能超过360个月")
    private Integer finalTerm;
}
