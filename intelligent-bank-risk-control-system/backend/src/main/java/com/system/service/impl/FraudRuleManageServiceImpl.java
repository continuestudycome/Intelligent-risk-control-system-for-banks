package com.system.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.system.domain.FrdRule;
import com.system.domain.SysUser;
import com.system.dto.FraudRuleUpdateDTO;
import com.system.dto.FraudRuleValidateBatchDTO;
import com.system.exception.ApiException;
import com.system.fraud.FraudRuntimeThresholds;
import com.system.mapper.FrdRuleMapper;
import com.system.mapper.UserMapper;
import com.system.service.FraudRuleAiClient;
import com.system.service.FraudRuleManageService;
import com.system.vo.FraudRuleVO;
import com.system.vo.FraudRuleValidateResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FraudRuleManageServiceImpl implements FraudRuleManageService {

    private final FrdRuleMapper frdRuleMapper;
    private final FraudRuleAiClient fraudRuleAiClient;
    private final UserMapper userMapper;

    @Value("${fraud.rule.remote-amount-min:28000}")
    private BigDecimal defaultRemoteMin;

    @Value("${fraud.rule.absolute-high-amount:85000}")
    private BigDecimal defaultAbsoluteHigh;

    @Value("${fraud.rule.probe-max-amount:500}")
    private BigDecimal defaultProbeMax;

    @Value("${fraud.rule.probe-count-medium:4}")
    private int defaultProbeCount;

    @Value("${fraud.ml.score-high:0.68}")
    private double defaultMlHigh;

    @Override
    public List<FraudRuleVO> listForRisk() {
        ensureRiskStaff();
        List<FrdRule> rows = frdRuleMapper.selectList(
                new LambdaQueryWrapper<FrdRule>()
                        .eq(FrdRule::getIsDeleted, 0)
                        .orderByDesc(FrdRule::getPriority)
                        .orderByAsc(FrdRule::getId)
        );
        return rows.stream().map(this::toVo).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FraudRuleVO update(Long id, FraudRuleUpdateDTO dto) {
        ensureRiskStaff();
        FrdRule rule = frdRuleMapper.selectById(id);
        if (rule == null || (rule.getIsDeleted() != null && rule.getIsDeleted() == 1)) {
            throw new ApiException(404, "规则不存在");
        }
        try {
            JSONUtil.parseObj(dto.getRuleCondition());
        } catch (Exception e) {
            throw new ApiException(400, "ruleCondition 须为合法 JSON 对象");
        }
        rule.setRuleCondition(dto.getRuleCondition().trim());
        if (dto.getRiskLevel() != null && !dto.getRiskLevel().isBlank()) {
            rule.setRiskLevel(dto.getRiskLevel().trim().toUpperCase(Locale.ROOT));
        }
        if (dto.getPriority() != null) {
            rule.setPriority(dto.getPriority());
        }
        if (dto.getStatus() != null) {
            rule.setStatus(dto.getStatus());
        }
        if (dto.getRemark() != null) {
            rule.setRemark(dto.getRemark());
        }
        rule.setVersion(rule.getVersion() == null ? 2 : rule.getVersion() + 1);
        frdRuleMapper.updateById(rule);
        return toVo(rule);
    }

    @Override
    public FraudRuleValidateResultVO validateWithAi(FraudRuleValidateBatchDTO dto) {
        ensureRiskStaff();
        List<Map<String, Object>> rules = new ArrayList<>();
        for (FraudRuleValidateBatchDTO.Item it : dto.getRules()) {
            Map<String, Object> m = new HashMap<>();
            m.put("ruleCode", it.getRuleCode());
            m.put("ruleCondition", it.getRuleCondition());
            rules.add(m);
        }
        return fraudRuleAiClient.validateBatch(rules);
    }

    @Override
    public List<Map<String, Object>> runDefaultSimulation() {
        ensureRiskStaff();
        List<FrdRule> rows = frdRuleMapper.selectList(
                new LambdaQueryWrapper<FrdRule>().eq(FrdRule::getIsDeleted, 0)
                        .in(FrdRule::getRuleCode,
                                "RULE_AMOUNT_EXTREME",
                                "RULE_REMOTE_LARGE_TX",
                                "RULE_FREQ_SMALL_PROBE",
                                "RULE_ML_ANOMALY_HIGH")
        );
        FraudRuntimeThresholds t = FraudRuntimeThresholds.fromRows(
                rows,
                defaultAbsoluteHigh,
                defaultRemoteMin,
                defaultProbeMax,
                defaultProbeCount,
                defaultMlHigh
        );
        Map<String, Object> thresholds = new HashMap<>();
        thresholds.put("absoluteHighAmount", t.absoluteHighAmount().doubleValue());
        thresholds.put("remoteAmountMin", t.remoteAmountMin().doubleValue());
        thresholds.put("probeMaxAmount", t.probeMaxAmount().doubleValue());
        thresholds.put("probeCountMedium", t.probeCountMedium());
        thresholds.put("mlScoreHigh", t.mlScoreHigh());
        Map<String, Object> enabled = new HashMap<>();
        enabled.put("RULE_AMOUNT_EXTREME", t.ruleAmountExtremeEnabled());
        enabled.put("RULE_REMOTE_LARGE_TX", t.ruleRemoteLargeTxEnabled());
        enabled.put("RULE_FREQ_SMALL_PROBE", t.ruleFreqProbeEnabled());
        enabled.put("RULE_ML_ANOMALY_HIGH", t.ruleMlAnomalyEnabled());
        thresholds.put("rulesEnabled", enabled);

        List<Map<String, Object>> samples = List.of(
                sample(90000, true, 0, 0.2),
                sample(30000, false, 0, 0.2),
                sample(400, true, 5, 0.2),
                sample(5000, true, 0, 0.85),
                sample(15000, false, 1, 0.55)
        );
        return fraudRuleAiClient.simulate(thresholds, samples);
    }

    private static Map<String, Object> sample(double amount, boolean sameProvince, int probe, double ml) {
        Map<String, Object> m = new HashMap<>();
        m.put("amount", amount);
        m.put("sameProvince", sameProvince);
        m.put("probeCount", probe);
        m.put("mlScore", ml);
        m.put("mlAvailable", true);
        return m;
    }

    private FraudRuleVO toVo(FrdRule r) {
        return FraudRuleVO.builder()
                .id(r.getId())
                .ruleCode(r.getRuleCode())
                .ruleName(r.getRuleName())
                .ruleType(r.getRuleType())
                .ruleCondition(r.getRuleCondition())
                .riskLevel(r.getRiskLevel())
                .priority(r.getPriority())
                .hitThreshold(r.getHitThreshold())
                .status(r.getStatus())
                .version(r.getVersion())
                .effectiveTime(r.getEffectiveTime())
                .expireTime(r.getExpireTime())
                .remark(r.getRemark())
                .updateTime(r.getUpdateTime())
                .build();
    }

    private void ensureRiskStaff() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails ud)) {
            throw new ApiException(401, "未登录或登录已失效");
        }
        SysUser user = userMapper.selectByUsername(ud.getUsername());
        if (user == null || (user.getIsDeleted() != null && user.getIsDeleted() == 1)) {
            throw new ApiException(401, "用户不存在");
        }
        String code = userMapper.selectPrimaryRoleCode(user.getId());
        if (!"RISK_OFFICER".equalsIgnoreCase(code) && !"RISK_MANAGER".equalsIgnoreCase(code)) {
            throw new ApiException(403, "仅风控人员可操作");
        }
    }
}
