package com.system.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CustomerBotReplyVO {
    private Long assistantMessageId;
    private String answer;
    private List<RagCitationVO> citations;
    private String mode;
    private String model;
}
