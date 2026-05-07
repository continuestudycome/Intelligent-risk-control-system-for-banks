"""
规则试算：与 Java FraudRiskAssessmentServiceImpl + mergeWithMl 逻辑对齐（演示用）。
"""
from __future__ import annotations

from typing import Any


def _decide_rule_level(
    amount: float,
    same_province: bool,
    probe_count: int,
    th: dict[str, Any],
) -> tuple[str, list[str]]:
    """返回 (rule_level, hit_rules)，LEVEL 为 LOW/MEDIUM/HIGH"""
    hit: list[str] = []
    absolute_high = float(th.get("absoluteHighAmount", 85000))
    remote_min = float(th.get("remoteAmountMin", 28000))
    probe_max = float(th.get("probeMaxAmount", 500))
    probe_need = int(th.get("probeCountMedium", 4))

    enabled = th.get("rulesEnabled") or {}
    amount_on = enabled.get("RULE_AMOUNT_EXTREME", True)
    remote_on = enabled.get("RULE_REMOTE_LARGE_TX", True)
    probe_on = enabled.get("RULE_FREQ_SMALL_PROBE", True)

    level = "LOW"

    if amount_on and amount >= absolute_high:
        hit.append("RULE_AMOUNT_EXTREME")
        level = "HIGH"

    if remote_on and (not same_province) and amount >= remote_min:
        hit.append("RULE_REMOTE_LARGE_TX")
        level = "HIGH"

    if probe_on and probe_count >= probe_need and amount < probe_max:
        hit.append("RULE_FREQ_SMALL_PROBE")
        if level == "LOW":
            level = "MEDIUM"

    return level, hit


def merge_with_ml(
    rule_level: str,
    ml_score: float | None,
    ml_available: bool,
    ml_score_high: float,
    ml_rule_enabled: bool,
) -> str:
    if not ml_rule_enabled:
        ml_score_high = 2.0
    if not ml_available or ml_score is None:
        return rule_level
    if rule_level == "HIGH":
        return "HIGH"
    if ml_score >= ml_score_high:
        return "HIGH"
    if rule_level == "MEDIUM":
        return "MEDIUM"
    return "LOW"


def simulate_sample(sample: dict[str, Any], th: dict[str, Any]) -> dict[str, Any]:
    amount = float(sample.get("amount", 0))
    same_province = bool(sample.get("sameProvince", True))
    probe_count = int(sample.get("probeCount", 0))
    ml_score = sample.get("mlScore")
    ml_available = bool(sample.get("mlAvailable", True))

    ml_high = float(th.get("mlScoreHigh", 0.68))
    enabled = th.get("rulesEnabled") or {}
    ml_on = enabled.get("RULE_ML_ANOMALY_HIGH", True)

    rl, hits = _decide_rule_level(amount, same_province, probe_count, th)
    ms = float(ml_score) if ml_score is not None else None
    final = merge_with_ml(rl, ms, ml_available, ml_high, ml_on)
    return {
        "ruleLevel": rl,
        "hitRules": hits,
        "finalLevel": final,
        "mlUsed": ml_available and ml_on and ms is not None,
    }


def run_simulation(thresholds: dict[str, Any], samples: list[dict[str, Any]]) -> list[dict[str, Any]]:
    out: list[dict[str, Any]] = []
    for i, s in enumerate(samples):
        r = simulate_sample(s, thresholds)
        out.append({"index": i, "input": s, **r})
    return out
