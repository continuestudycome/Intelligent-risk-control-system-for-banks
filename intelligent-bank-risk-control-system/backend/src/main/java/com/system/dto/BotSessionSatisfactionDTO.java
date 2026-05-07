package com.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BotSessionSatisfactionDTO {
    /** 1不满意 2一般 3满意 */
    @NotNull
    @Min(1)
    @Max(3)
    private Integer satisfaction;
}
