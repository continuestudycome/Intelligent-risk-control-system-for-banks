package com.system.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LimitAdjustReviewDTO {
    @Size(max = 2000)
    private String comment;
}
