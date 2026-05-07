package com.system.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class CreditScoreOverviewVO {
    private boolean evaluated;
    private Integer score;
    private String riskLevel;
    private String riskLevelName;
    private String modelVersion;
    private LocalDateTime evaluatedAt;
    private Integer calcDurationMs;
    /** 模型评估指标（来自 AI 服务或库表） */
    private Map<String, Object> metrics;
    /** 特征说明与归一化值 */
    private Map<String, Object> featureSnapshot;
    /** 违约/相对劣后概率展示：1 - good_probability */
    private BigDecimal badProbabilityHint;

    @Data
    @Builder
    public static class HistoryPoint {
        private Long id;
        private Integer score;
        private String riskLevel;
        private String modelVersion;
        private LocalDateTime createTime;
    }

    private List<HistoryPoint> recentTrend;
}
