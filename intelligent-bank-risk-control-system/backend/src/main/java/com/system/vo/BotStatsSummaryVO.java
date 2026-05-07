package com.system.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BotStatsSummaryVO {
    private long totalSessions;
    private long activeSessions;
    private long transferredSessions;
    private long satisfiedCount;
    private long neutralCount;
    private long dissatisfiedCount;
}
