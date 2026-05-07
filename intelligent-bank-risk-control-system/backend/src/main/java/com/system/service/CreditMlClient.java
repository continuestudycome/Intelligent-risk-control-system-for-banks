package com.system.service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 调用 ai_services 逻辑回归信用评分
 */
public interface CreditMlClient {

    CreditMlResult score(List<Double> normalizedFeatures);

    final class CreditMlResult {
        private final int creditScore;
        private final String riskLevel;
        private final BigDecimal goodProbability;
        private final String modelVersion;
        private final Map<String, Object> metrics;
        private final boolean available;

        public CreditMlResult(
                int creditScore,
                String riskLevel,
                BigDecimal goodProbability,
                String modelVersion,
                Map<String, Object> metrics,
                boolean available) {
            this.creditScore = creditScore;
            this.riskLevel = riskLevel;
            this.goodProbability = goodProbability;
            this.modelVersion = modelVersion;
            this.metrics = metrics != null ? metrics : Collections.emptyMap();
            this.available = available;
        }

        public static CreditMlResult offline() {
            return new CreditMlResult(0, "C", BigDecimal.ZERO, "offline", Collections.emptyMap(), false);
        }

        public int creditScore() {
            return creditScore;
        }

        public String riskLevel() {
            return riskLevel;
        }

        public BigDecimal goodProbability() {
            return goodProbability;
        }

        public String modelVersion() {
            return modelVersion;
        }

        public Map<String, Object> metrics() {
            return metrics;
        }

        public boolean available() {
            return available;
        }
    }
}
