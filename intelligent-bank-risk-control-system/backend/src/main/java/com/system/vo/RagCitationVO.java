package com.system.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RagCitationVO {
    private Long id;
    private BigDecimal score;
}
