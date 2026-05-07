package com.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FraudAlertReviewDTO {
    /** CONFIRM_FRAUD：确认欺诈并入库案例；FALSE_POSITIVE：误报关闭 */
    @NotBlank(message = "复核结论不能为空")
    private String decision;

    @Size(max = 2000, message = "意见过长")
    private String comment;

    /** 欺诈类型：THEFT/LAUNDER/CASHOUT/IMPERSONATE（确认欺诈时可选） */
    @Size(max = 64)
    private String fraudType;
}
