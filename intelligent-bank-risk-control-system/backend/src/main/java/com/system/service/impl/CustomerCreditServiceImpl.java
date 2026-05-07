package com.system.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.system.domain.CrdScore;
import com.system.domain.CustCustomer;
import com.system.domain.SysUser;
import com.system.exception.ApiException;
import com.system.mapper.CrdScoreMapper;
import com.system.mapper.CustCustomerMapper;
import com.system.mapper.TxnTransactionMapper;
import com.system.mapper.UserMapper;
import com.system.service.CreditLimitManagementService;
import com.system.service.CreditMlClient;
import com.system.service.CreditMlClient.CreditMlResult;
import com.system.service.CustomerCreditService;
import com.system.vo.CreditScoreOverviewVO;
import com.system.vo.CustomerLimitSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomerCreditServiceImpl implements CustomerCreditService {

    private static final int TXN_LOOKBACK_DAYS = 90;
    private static final String[] FEATURE_NAMES = {
            "年收入(归一化)", "资产(归一化)", "征信已授权", "资料已完善",
            "近90天成功交易笔数(归一化)", "近90天成功交易额(归一化)", "黑名单", "历史信用等级(归一化)"
    };

    private final UserMapper userMapper;
    private final CustCustomerMapper custCustomerMapper;
    private final CrdScoreMapper crdScoreMapper;
    private final TxnTransactionMapper txnTransactionMapper;
    private final CreditMlClient creditMlClient;
    private final CreditLimitManagementService creditLimitManagementService;

    @Override
    public CreditScoreOverviewVO getMyOverview() {
        CustCustomer customer = currentCustomer();
        return buildOverview(customer.getId());
    }

    @Override
    public CreditScoreOverviewVO getCustomerOverviewForRisk(Long customerId) {
        ensureRiskStaff();
        CustCustomer c = custCustomerMapper.selectById(customerId);
        if (c == null || (c.getIsDeleted() != null && c.getIsDeleted() == 1)) {
            throw new ApiException(404, "客户不存在");
        }
        return buildOverview(customerId);
    }

    private CreditScoreOverviewVO buildOverview(Long customerId) {
        List<CrdScore> trend = crdScoreMapper.selectList(
                new LambdaQueryWrapper<CrdScore>()
                        .eq(CrdScore::getCustomerId, customerId)
                        .orderByDesc(CrdScore::getCreateTime)
                        .last("LIMIT 15")
        );
        List<CreditScoreOverviewVO.HistoryPoint> points = new ArrayList<>();
        for (CrdScore s : trend) {
            points.add(CreditScoreOverviewVO.HistoryPoint.builder()
                    .id(s.getId())
                    .score(s.getScore())
                    .riskLevel(s.getRiskLevel())
                    .modelVersion(s.getModelVersion())
                    .createTime(s.getCreateTime())
                    .build());
        }

        CrdScore latest = crdScoreMapper.selectOne(
                new LambdaQueryWrapper<CrdScore>()
                        .eq(CrdScore::getCustomerId, customerId)
                        .orderByDesc(CrdScore::getCreateTime)
                        .last("LIMIT 1")
        );
        if (latest == null) {
            return CreditScoreOverviewVO.builder()
                    .evaluated(false)
                    .recentTrend(points)
                    .build();
        }
        ParsedSnapshot ps = parseSnapshot(latest.getFeatureData());
        return CreditScoreOverviewVO.builder()
                .evaluated(true)
                .score(latest.getScore())
                .riskLevel(latest.getRiskLevel())
                .riskLevelName(riskName(latest.getRiskLevel()))
                .modelVersion(latest.getModelVersion())
                .evaluatedAt(latest.getCreateTime())
                .calcDurationMs(latest.getCalcDurationMs())
                .metrics(ps.metrics())
                .featureSnapshot(ps.snapshot())
                .badProbabilityHint(ps.badHint())
                .recentTrend(points)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreditScoreOverviewVO evaluateMyScore() {
        long t0 = System.currentTimeMillis();
        CustCustomer customer = currentCustomer();

        Long cnt = txnTransactionMapper.countSuccessfulSinceDays(customer.getId(), TXN_LOOKBACK_DAYS);
        long txnCount = cnt == null ? 0L : cnt;
        BigDecimal sum = txnTransactionMapper.sumSuccessfulAmountSinceDays(customer.getId(), TXN_LOOKBACK_DAYS);
        if (sum == null) {
            sum = BigDecimal.ZERO;
        }

        List<Double> features = buildNormalizedFeatures(customer, txnCount, sum);
        Map<String, Object> featureSnapshot = buildFeatureSnapshot(customer, features, txnCount, sum);

        CreditMlResult remote = creditMlClient.score(features);
        CreditMlResult used = remote.available() ? remote : localFallback(features);
        Map<String, Object> metrics = remote.available()
                ? new LinkedHashMap<>(remote.metrics())
                : new LinkedHashMap<>(Map.of("note", "AI 服务不可用，已使用本地线性兜底"));

        int score = used.creditScore();
        String risk = used.riskLevel();
        String modelVer = used.modelVersion();

        if (customer.getIsBlacklist() != null && customer.getIsBlacklist() == 1) {
            score = Math.min(score, 380);
            risk = "D";
        }

        long ms = System.currentTimeMillis() - t0;
        if (ms > Integer.MAX_VALUE) {
            ms = Integer.MAX_VALUE;
        }

        featureSnapshot.put("goodProbability", used.goodProbability());
        featureSnapshot.put("mlMetrics", metrics);
        featureSnapshot.put("source", remote.available() ? "ai_services" : "fallback");
        if (used.goodProbability() != null) {
            BigDecimal one = BigDecimal.ONE;
            featureSnapshot.put("badProbabilityHint", one.subtract(used.goodProbability()).max(BigDecimal.ZERO)
                    .setScale(4, RoundingMode.HALF_UP));
        }

        CrdScore row = new CrdScore();
        row.setCustomerId(customer.getId());
        row.setScore(score);
        row.setRiskLevel(risk);
        row.setModelVersion(modelVer);
        row.setFeatureData(JSONUtil.toJsonStr(featureSnapshot));
        row.setCalcDurationMs((int) ms);
        row.setCreateTime(LocalDateTime.now());
        crdScoreMapper.insert(row);

        customer.setCreditLevel(risk);
        custCustomerMapper.updateById(customer);

        creditLimitManagementService.ensureLimitAccountAfterScoreEvaluation(customer.getId());

        return getMyOverview();
    }

    @Override
    public CustomerLimitSummaryVO getMyLimitSummary() {
        CustCustomer customer = currentCustomer();
        return creditLimitManagementService.getCustomerLimitSummary(customer.getId());
    }

    private CreditMlResult localFallback(List<Double> f) {
        double[] w = {0.18, 0.16, 0.12, 0.10, 0.10, 0.10, 0.14, 0.10};
        double acc = 0;
        for (int i = 0; i < f.size() && i < w.length; i++) {
            acc += w[i] * f.get(i);
        }
        int score = (int) Math.round(300 + acc * 600);
        score = Math.max(300, Math.min(900, score));
        String risk = riskFromScore(score);
        BigDecimal p = BigDecimal.valueOf((score - 300) / 600.0).setScale(4, RoundingMode.HALF_UP);
        return new CreditMlResult(score, risk, p, "fallback-linear-v1", Map.of(), false);
    }

    private static String riskFromScore(int score) {
        if (score >= 720) {
            return "A";
        }
        if (score >= 620) {
            return "B";
        }
        if (score >= 520) {
            return "C";
        }
        return "D";
    }

    private List<Double> buildNormalizedFeatures(CustCustomer c, long txnCount, BigDecimal sumWan) {
        double f0 = normIncomeWan(c.getAnnualIncome());
        double f1 = normAssetWan(c.getAssetAmount());
        double f2 = c.getCreditAuthorized() != null && c.getCreditAuthorized() == 1 ? 1.0 : 0.0;
        double f3 = c.getProfileCompleted() != null && c.getProfileCompleted() == 1 ? 1.0 : 0.0;
        double f4 = Math.min(txnCount / 100.0, 1.0);
        double sum = sumWan == null ? 0.0 : sumWan.doubleValue();
        double f5 = Math.min(sum / 5_000_000.0, 1.0);
        double f6 = c.getIsBlacklist() != null && c.getIsBlacklist() == 1 ? 1.0 : 0.0;
        double f7 = priorLevelNorm(c.getCreditLevel());
        return List.of(f0, f1, f2, f3, f4, f5, f6, f7);
    }

    private Map<String, Object> buildFeatureSnapshot(CustCustomer c, List<Double> f, long txnCount, BigDecimal sum) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("annualIncomeWan", c.getAnnualIncome());
        m.put("assetWan", c.getAssetAmount());
        m.put("txnSuccessCount90d", txnCount);
        m.put("txnSuccessAmount90d", sum);
        m.put("featureNames", List.of(FEATURE_NAMES));
        m.put("normalizedFeatures", f);
        return m;
    }

    private double normIncomeWan(BigDecimal wan) {
        if (wan == null) {
            return 0.0;
        }
        double v = wan.doubleValue();
        return Math.min(Math.log10(v * 10_000 + 1.0) / 7.0, 1.0);
    }

    private double normAssetWan(BigDecimal wan) {
        if (wan == null) {
            return 0.0;
        }
        double v = wan.doubleValue();
        return Math.min(Math.log10(v * 100_000 + 1.0) / 8.0, 1.0);
    }

    private double priorLevelNorm(String level) {
        if (level == null || level.isBlank()) {
            return 0.5;
        }
        return switch (Character.toUpperCase(level.trim().charAt(0))) {
            case 'A' -> 1.0;
            case 'B' -> 0.72;
            case 'C' -> 0.48;
            case 'D' -> 0.22;
            default -> 0.5;
        };
    }

    private record ParsedSnapshot(Map<String, Object> metrics, Map<String, Object> snapshot, BigDecimal badHint) {
        static ParsedSnapshot empty() {
            return new ParsedSnapshot(Map.of(), Map.of(), null);
        }
    }

    private ParsedSnapshot parseSnapshot(String json) {
        if (json == null || json.isBlank()) {
            return ParsedSnapshot.empty();
        }
        try {
            cn.hutool.json.JSONObject obj = JSONUtil.parseObj(json);
            Map<String, Object> snap = new LinkedHashMap<>(obj);
            cn.hutool.json.JSONObject ml = obj.getJSONObject("mlMetrics");
            Map<String, Object> mlMetrics = ml != null ? new LinkedHashMap<>(ml) : Map.of();
            BigDecimal bad = null;
            Object gh = snap.get("badProbabilityHint");
            if (gh instanceof Number n) {
                bad = BigDecimal.valueOf(n.doubleValue());
            }
            return new ParsedSnapshot(mlMetrics, snap, bad);
        } catch (Exception e) {
            return ParsedSnapshot.empty();
        }
    }

    private static String riskName(String r) {
        if (r == null) {
            return "未知";
        }
        return switch (r.toUpperCase()) {
            case "A" -> "信用极好";
            case "B" -> "良好";
            case "C" -> "一般";
            case "D" -> "关注";
            default -> r;
        };
    }

    private CustCustomer currentCustomer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User principal)) {
            throw new ApiException(401, "未登录或登录已失效");
        }
        SysUser user = userMapper.selectByUsername(principal.getUsername());
        if (user == null || (user.getIsDeleted() != null && user.getIsDeleted() == 1)) {
            throw new ApiException(401, "用户不存在");
        }
        String roleCode = userMapper.selectPrimaryRoleCode(user.getId());
        if (!"CUSTOMER".equalsIgnoreCase(roleCode)) {
            throw new ApiException(403, "仅客户可查看信用评分");
        }
        CustCustomer c = custCustomerMapper.selectByUserId(user.getId());
        if (c == null || (c.getIsDeleted() != null && c.getIsDeleted() == 1)) {
            throw new ApiException(400, "客户档案不存在");
        }
        return c;
    }

    private void ensureRiskStaff() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User principal)) {
            throw new ApiException(401, "未登录或登录已失效");
        }
        SysUser user = userMapper.selectByUsername(principal.getUsername());
        if (user == null || (user.getIsDeleted() != null && user.getIsDeleted() == 1)) {
            throw new ApiException(401, "用户不存在");
        }
        String code = userMapper.selectPrimaryRoleCode(user.getId());
        if (!"RISK_OFFICER".equalsIgnoreCase(code) && !"RISK_MANAGER".equalsIgnoreCase(code)) {
            throw new ApiException(403, "仅风控人员可操作");
        }
    }
}
