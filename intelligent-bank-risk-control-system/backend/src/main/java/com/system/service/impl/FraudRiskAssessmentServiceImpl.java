package com.system.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.system.domain.CustCustomer;
import com.system.domain.FrdRule;
import com.system.dto.TransactionCreateDTO;
import com.system.fraud.FraudAssessmentResult;
import com.system.fraud.FraudRiskLevel;
import com.system.fraud.FraudRuntimeThresholds;
import com.system.mapper.FrdRuleMapper;
import com.system.mapper.TxnTransactionMapper;
import com.system.service.FraudMlClient;
import com.system.service.FraudRiskAssessmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FraudRiskAssessmentServiceImpl implements FraudRiskAssessmentService {

    private final TxnTransactionMapper txnTransactionMapper;
    private final FraudMlClient fraudMlClient;
    private final FrdRuleMapper frdRuleMapper;

    @Value("${fraud.ml.score-high:0.68}")
    private double mlScoreHigh;

    @Value("${fraud.rule.remote-amount-min:28000}")
    private BigDecimal remoteAmountMin;

    @Value("${fraud.rule.absolute-high-amount:85000}")
    private BigDecimal absoluteHighAmount;

    @Value("${fraud.rule.probe-max-amount:500}")
    private BigDecimal probeMaxAmount;

    @Value("${fraud.rule.probe-count-medium:4}")
    private int probeCountMedium;

    @Override
    public FraudAssessmentResult assess(CustCustomer customer, TransactionCreateDTO dto) {
        FraudRuntimeThresholds thr = loadRuntimeThresholds();
        BigDecimal amount = dto.getAmount();
        Long probeCnt = txnTransactionMapper.countSmallAmountTransactions(
                customer.getId(), thr.probeMaxAmount(), 1);
        long probe = probeCnt == null ? 0L : probeCnt;

        boolean sameProvince = isSameProvince(customer.getProvince(), dto.getTransactionProvince());
        int hour = LocalDateTime.now().getHour();

        List<Double> features = buildNormalizedFeatures(amount, dto.getTransactionType(), sameProvince, hour, probe);

        FraudMlClient.MlScoreResult ml = fraudMlClient.scoreIsolationForest(features);

        List<String> hitRules = new ArrayList<>();
        FraudRiskLevel ruleLevel = FraudRiskLevel.LOW;

        if (thr.ruleAmountExtremeEnabled() && amount.compareTo(thr.absoluteHighAmount()) >= 0) {
            hitRules.add("RULE_AMOUNT_EXTREME");
            ruleLevel = FraudRiskLevel.HIGH;
        }
        if (thr.ruleRemoteLargeTxEnabled() && !sameProvince && amount.compareTo(thr.remoteAmountMin()) >= 0) {
            hitRules.add("RULE_REMOTE_LARGE_TX");
            ruleLevel = FraudRiskLevel.HIGH;
        }
        if (thr.ruleFreqProbeEnabled()
                && probe >= thr.probeCountMedium()
                && amount.compareTo(thr.probeMaxAmount()) < 0) {
            hitRules.add("RULE_FREQ_SMALL_PROBE");
            if (ruleLevel == FraudRiskLevel.LOW) {
                ruleLevel = FraudRiskLevel.MEDIUM;
            }
        }

        FraudRiskLevel merged = mergeWithMl(
                ruleLevel,
                ml.anomalyScore(),
                ml.available(),
                thr.effectiveMlScoreHigh());

        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("amount", amount);
        snap.put("transactionType", dto.getTransactionType());
        snap.put("sameProvinceAsProfile", sameProvince);
        snap.put("hour", hour);
        snap.put("probeSmallCount1h", probe);
        snap.put("runtimeThresholds", Map.of(
                "absoluteHighAmount", thr.absoluteHighAmount(),
                "remoteAmountMin", thr.remoteAmountMin(),
                "probeMaxAmount", thr.probeMaxAmount(),
                "probeCountMedium", thr.probeCountMedium(),
                "mlScoreHigh", thr.effectiveMlScoreHigh()
        ));
        snap.put("normalizedFeatures", features);
        snap.put("hitRules", hitRules);
        snap.put("mlAnomalyScore", ml.anomalyScore());
        snap.put("mlAvailable", ml.available());
        snap.put("mlScoreHighUsed", thr.effectiveMlScoreHigh());
        snap.put("finalLevel", merged.name());

        String json = JSONUtil.toJsonStr(snap);

        return new FraudAssessmentResult(
                merged,
                ml.anomalyScore(),
                ml.modelVersion(),
                hitRules,
                json
        );
    }

    /**
     * 规则优先；孤立森林仅在「规则已为中风险」时强化档位，或分数极高时直接高危。
     * 避免演示模型把规则为 LOW 的正常小额批量打成 MEDIUM（此前 ML≥medium 阈值即升中风险）。
     */
    private FraudRiskLevel mergeWithMl(
            FraudRiskLevel ruleLevel,
            BigDecimal mlScore,
            boolean mlAvailable,
            double mlScoreHighThreshold
    ) {
        if (!mlAvailable || mlScore == null) {
            return ruleLevel;
        }
        double s = mlScore.doubleValue();
        if (ruleLevel == FraudRiskLevel.HIGH) {
            return FraudRiskLevel.HIGH;
        }
        if (s >= mlScoreHighThreshold) {
            return FraudRiskLevel.HIGH;
        }
        if (ruleLevel == FraudRiskLevel.MEDIUM) {
            return FraudRiskLevel.MEDIUM;
        }
        return FraudRiskLevel.LOW;
    }

    private FraudRuntimeThresholds loadRuntimeThresholds() {
        List<FrdRule> rows = frdRuleMapper.selectList(
                new LambdaQueryWrapper<FrdRule>()
                        .eq(FrdRule::getIsDeleted, 0)
                        .in(FrdRule::getRuleCode,
                                "RULE_AMOUNT_EXTREME",
                                "RULE_REMOTE_LARGE_TX",
                                "RULE_FREQ_SMALL_PROBE",
                                "RULE_ML_ANOMALY_HIGH")
        );
        return FraudRuntimeThresholds.fromRows(
                rows,
                absoluteHighAmount,
                remoteAmountMin,
                probeMaxAmount,
                probeCountMedium,
                mlScoreHigh
        );
    }

    private List<Double> buildNormalizedFeatures(
            BigDecimal amount,
            Integer transactionType,
            boolean sameProvince,
            int hour,
            long probeCount
    ) {
        double amt = amount.doubleValue();
        double f0 = Math.min(Math.log10(amt + 1.0) / 6.0, 1.0);
        double f1 = (transactionType == null ? 1 : transactionType) / 4.0;
        double f2 = sameProvince ? 1.0 : 0.0;
        double f3 = hour / 23.0;
        double f4 = Math.min(probeCount / 8.0, 1.0);
        double f5 = Math.min(amt / 200_000.0, 1.0);
        return List.of(f0, f1, f2, f3, f4, f5);
    }

    private boolean isSameProvince(String profileProvince, String txProvince) {
        if (profileProvince == null || profileProvince.isBlank()
                || txProvince == null || txProvince.isBlank()) {
            return true;
        }
        String a = profileProvince.trim().toLowerCase(Locale.ROOT);
        String b = txProvince.trim().toLowerCase(Locale.ROOT);
        return a.equals(b);
    }
}
