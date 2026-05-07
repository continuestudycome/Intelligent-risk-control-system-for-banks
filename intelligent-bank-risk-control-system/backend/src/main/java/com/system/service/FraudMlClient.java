package com.system.service;

import java.math.BigDecimal;
import java.util.List;

public interface FraudMlClient {

    record MlScoreResult(BigDecimal anomalyScore, String modelVersion, boolean available) {
    }

    /**
     * 调用 AI 服务孤立森林异常评分，features 已归一化到 [0,1] 区间
     */
    MlScoreResult scoreIsolationForest(List<Double> normalizedFeatures);
}
