package com.system.fraud;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.system.domain.FrdRule;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 从 frd_rule 加载阈值；表中没有对应行时使用默认值且启用；status=0 时禁用该条规则。
 */
public record FraudRuntimeThresholds(
        BigDecimal absoluteHighAmount,
        BigDecimal remoteAmountMin,
        BigDecimal probeMaxAmount,
        int probeCountMedium,
        double mlScoreHigh,
        boolean ruleAmountExtremeEnabled,
        boolean ruleRemoteLargeTxEnabled,
        boolean ruleFreqProbeEnabled,
        boolean ruleMlAnomalyEnabled
) {

    public static FraudRuntimeThresholds fromRows(
            List<FrdRule> rows,
            BigDecimal defaultAbsoluteHigh,
            BigDecimal defaultRemoteMin,
            BigDecimal defaultProbeMax,
            int defaultProbeCount,
            double defaultMlHigh
    ) {
        Map<String, FrdRule> byCode = new HashMap<>();
        if (rows != null) {
            for (FrdRule r : rows) {
                if (r.getRuleCode() != null) {
                    byCode.put(r.getRuleCode().trim().toUpperCase(Locale.ROOT), r);
                }
            }
        }

        BigDecimal abs = defaultAbsoluteHigh;
        boolean amtOk = true;
        FrdRule ra = byCode.get("RULE_AMOUNT_EXTREME");
        if (ra != null) {
            amtOk = ra.getStatus() != null && ra.getStatus() == 1;
            if (amtOk) {
                BigDecimal v = readBig(ra.getRuleCondition(), "absoluteHighAmount");
                if (v != null) {
                    abs = v;
                }
            }
        }

        BigDecimal remote = defaultRemoteMin;
        boolean remOk = true;
        FrdRule rr = byCode.get("RULE_REMOTE_LARGE_TX");
        if (rr != null) {
            remOk = rr.getStatus() != null && rr.getStatus() == 1;
            if (remOk) {
                BigDecimal v = readBig(rr.getRuleCondition(), "remoteAmountMin");
                if (v != null) {
                    remote = v;
                }
            }
        }

        BigDecimal probeMax = defaultProbeMax;
        int probeCnt = defaultProbeCount;
        boolean probeOk = true;
        FrdRule rp = byCode.get("RULE_FREQ_SMALL_PROBE");
        if (rp != null) {
            probeOk = rp.getStatus() != null && rp.getStatus() == 1;
            if (probeOk) {
                JSONObject o = parseObj(rp.getRuleCondition());
                if (o != null) {
                    BigDecimal pm = o.getBigDecimal("probeMaxAmount");
                    Integer pc = o.getInt("probeCountMedium");
                    if (pm != null) {
                        probeMax = pm;
                    }
                    if (pc != null) {
                        probeCnt = pc;
                    }
                }
            }
        }

        double ml = defaultMlHigh;
        boolean mlOk = true;
        FrdRule rm = byCode.get("RULE_ML_ANOMALY_HIGH");
        if (rm != null) {
            mlOk = rm.getStatus() != null && rm.getStatus() == 1;
            if (mlOk) {
                JSONObject o = parseObj(rm.getRuleCondition());
                if (o != null && o.getDouble("mlScoreHigh") != null) {
                    ml = o.getDouble("mlScoreHigh");
                }
            }
        }

        return new FraudRuntimeThresholds(
                abs,
                remote,
                probeMax,
                probeCnt,
                ml,
                amtOk,
                remOk,
                probeOk,
                mlOk
        );
    }

    /** ML 规则关闭时，合并逻辑中的阈值提高到不可达，等价于不因孤立森林单独升为高危 */
    public double effectiveMlScoreHigh() {
        return ruleMlAnomalyEnabled ? mlScoreHigh : 2.0;
    }

    private static JSONObject parseObj(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return JSONUtil.parseObj(json);
        } catch (Exception e) {
            return null;
        }
    }

    private static BigDecimal readBig(String json, String key) {
        JSONObject o = parseObj(json);
        return o != null ? o.getBigDecimal(key) : null;
    }
}
