package com.system.service;

import java.math.BigDecimal;

/**
 * 调用 ai_services 授信额度推荐（Python）；不可用时由 {@link com.system.credit.CreditLimitRuleHelper} 本地兜底。
 */
public interface CreditLimitAiClient {

    /**
     * @param assetAmountWan 客户资产（万元），可为 null
     */
    LimitRecommendResult recommend(
            String riskLevel,
            boolean blacklist,
            long txnCount90d,
            BigDecimal assetAmountWan,
            BigDecimal minTotal,
            BigDecimal maxTotal);

    final class LimitRecommendResult {
        private final BigDecimal recommendedTotal;
        private final BigDecimal singleLimit;
        private final BigDecimal dailyLimit;
        private final String modelVersion;
        private final boolean available;

        public LimitRecommendResult(
                BigDecimal recommendedTotal,
                BigDecimal singleLimit,
                BigDecimal dailyLimit,
                String modelVersion,
                boolean available) {
            this.recommendedTotal = recommendedTotal;
            this.singleLimit = singleLimit;
            this.dailyLimit = dailyLimit;
            this.modelVersion = modelVersion != null ? modelVersion : "unknown";
            this.available = available;
        }

        public static LimitRecommendResult offline() {
            return new LimitRecommendResult(null, null, null, "offline", false);
        }

        public BigDecimal recommendedTotal() {
            return recommendedTotal;
        }

        public BigDecimal singleLimit() {
            return singleLimit;
        }

        public BigDecimal dailyLimit() {
            return dailyLimit;
        }

        public String modelVersion() {
            return modelVersion;
        }

        public boolean available() {
            return available;
        }
    }
}
