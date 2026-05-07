package com.system.credit;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 与交易限额 {@code CustomerTransactionServiceImpl} 中信用等级策略保持同量级，便于演示一致。
 * 授信动态调整的额度计算优先由 ai_services {@code credit_limit_recommend} 执行；此类仅在 AI 不可用时兜底。
 */
public final class CreditLimitRuleHelper {

    private CreditLimitRuleHelper() {
    }

    public static BigDecimal baseTotalLimitByRisk(String riskLevel) {
        if (riskLevel == null || riskLevel.isBlank()) {
            return new BigDecimal("100000.00");
        }
        return switch (riskLevel.trim().toUpperCase()) {
            case "A" -> new BigDecimal("500000.00");
            case "B" -> new BigDecimal("300000.00");
            case "C" -> new BigDecimal("100000.00");
            default -> new BigDecimal("30000.00");
        };
    }

    public static BigDecimal singleLimitByRisk(String riskLevel) {
        if (riskLevel == null || riskLevel.isBlank()) {
            return new BigDecimal("10000.00");
        }
        return switch (riskLevel.trim().toUpperCase()) {
            case "A" -> new BigDecimal("50000.00");
            case "B" -> new BigDecimal("30000.00");
            case "C" -> new BigDecimal("10000.00");
            default -> new BigDecimal("3000.00");
        };
    }

    public static BigDecimal dailyLimitByRisk(String riskLevel) {
        if (riskLevel == null || riskLevel.isBlank()) {
            return new BigDecimal("50000.00");
        }
        return switch (riskLevel.trim().toUpperCase()) {
            case "A" -> new BigDecimal("200000.00");
            case "B" -> new BigDecimal("100000.00");
            case "C" -> new BigDecimal("50000.00");
            default -> new BigDecimal("10000.00");
        };
    }

    /**
     * 结合信用等级底盘与行为系数计算建议授信总额（元），并限制在 [minTotal, maxTotal]。
     */
    public static BigDecimal computeRecommendedTotal(
            String riskLevel,
            boolean blacklist,
            long txnCount90d,
            BigDecimal assetAmountWan,
            BigDecimal minTotal,
            BigDecimal maxTotal
    ) {
        BigDecimal base = baseTotalLimitByRisk(riskLevel);
        double behavior = 1.0;
        if (blacklist) {
            behavior *= 0.25;
        }
        if (txnCount90d >= 40) {
            behavior *= 1.08;
        } else if (txnCount90d <= 2) {
            behavior *= 0.92;
        }
        if (assetAmountWan != null && assetAmountWan.doubleValue() > 500) {
            behavior *= 1.05;
        }
        BigDecimal rec = base.multiply(BigDecimal.valueOf(behavior)).setScale(2, RoundingMode.HALF_UP);
        rec = rec.max(minTotal).min(maxTotal);
        return rec;
    }
}
