"""
反欺诈规则参数校验（与 Java 侧 FraudRiskAssessmentServiceImpl 使用的字段对齐）。
"""
from __future__ import annotations

import json
from typing import Any


def _parse_condition(raw: str) -> dict[str, Any]:
    if not raw or not str(raw).strip():
        raise ValueError("rule_condition 不能为空")
    data = json.loads(raw)
    if not isinstance(data, dict):
        raise ValueError("rule_condition 须为 JSON 对象")
    return data


def validate_rule(rule_code: str, rule_condition: str) -> tuple[bool, list[str], list[str]]:
    """
    返回 (ok, errors, hints)。
    errors 阻断保存；hints 为优化建议。
    """
    errors: list[str] = []
    hints: list[str] = []
    try:
        cfg = _parse_condition(rule_condition)
    except (json.JSONDecodeError, ValueError) as e:
        return False, [str(e)], []

    code = (rule_code or "").strip().upper()

    if code == "RULE_AMOUNT_EXTREME":
        v = cfg.get("absoluteHighAmount")
        if v is None:
            errors.append("缺少字段 absoluteHighAmount")
        else:
            try:
                x = float(v)
            except (TypeError, ValueError):
                errors.append("absoluteHighAmount 须为数字")
            else:
                if x <= 0:
                    errors.append("absoluteHighAmount 须为正数")
                elif x < 1000:
                    hints.append("单笔超大额阈值过低，可能误伤大量正常交易")
                elif x > 5_000_000:
                    hints.append("阈值极高，可能难以触发单笔超大额规则")

    elif code == "RULE_REMOTE_LARGE_TX":
        v = cfg.get("remoteAmountMin")
        if v is None:
            errors.append("缺少字段 remoteAmountMin")
        else:
            try:
                x = float(v)
            except (TypeError, ValueError):
                errors.append("remoteAmountMin 须为数字")
            else:
                if x <= 0:
                    errors.append("remoteAmountMin 须为正数")
                if x > 500_000:
                    hints.append("异地大额门槛过高，可能很少触发")

    elif code == "RULE_FREQ_SMALL_PROBE":
        pa = cfg.get("probeMaxAmount")
        pc = cfg.get("probeCountMedium")
        if pa is None:
            errors.append("缺少字段 probeMaxAmount")
        if pc is None:
            errors.append("缺少字段 probeCountMedium")
        if pa is not None:
            try:
                paf = float(pa)
                if paf <= 0:
                    errors.append("probeMaxAmount 须为正数")
            except (TypeError, ValueError):
                errors.append("probeMaxAmount 须为数字")
        if pc is not None:
            try:
                pci = int(pc)
                if pci < 1:
                    errors.append("probeCountMedium 须 >= 1")
                elif pci > 50:
                    hints.append("试探次数阈值过高，规则可能长期不触发")
            except (TypeError, ValueError):
                errors.append("probeCountMedium 须为整数")

    elif code == "RULE_ML_ANOMALY_HIGH":
        v = cfg.get("mlScoreHigh")
        if v is None:
            errors.append("缺少字段 mlScoreHigh")
        else:
            try:
                x = float(v)
                if not (0 < x <= 1):
                    errors.append("mlScoreHigh 建议在区间 (0,1]")
            except (TypeError, ValueError):
                errors.append("mlScoreHigh 须为数字")
            else:
                if x < 0.5:
                    hints.append("孤立森林升级阈值较低，可能增加误报")
                if x > 0.95:
                    hints.append("阈值很高，模型几乎不会单独把交易升为高危")

    else:
        hints.append(f"未识别的规则编码 {code}，仅校验 JSON 格式")

    ok = len(errors) == 0
    return ok, errors, hints


def validate_batch(rules: list[dict[str, Any]]) -> dict[str, Any]:
    """rules: [{ \"ruleCode\", \"ruleCondition\" }, ...]"""
    all_errors: list[str] = []
    all_hints: list[str] = []
    abs_high: float | None = None
    remote_min: float | None = None

    for r in rules:
        code = str(r.get("ruleCode", "")).strip().upper()
        cond = r.get("ruleCondition")
        if cond is None:
            all_errors.append(f"{code}: 缺少 ruleCondition")
            continue
        cond_str = cond if isinstance(cond, str) else json.dumps(cond, ensure_ascii=False)
        ok, errs, hints = validate_rule(code, cond_str)
        all_errors.extend(errs)
        all_hints.extend(hints)
        try:
            cfg = json.loads(cond_str) if isinstance(cond_str, str) else cond_str
        except json.JSONDecodeError:
            continue
        if isinstance(cfg, dict):
            if code == "RULE_AMOUNT_EXTREME" and "absoluteHighAmount" in cfg:
                try:
                    abs_high = float(cfg["absoluteHighAmount"])
                except (TypeError, ValueError):
                    pass
            if code == "RULE_REMOTE_LARGE_TX" and "remoteAmountMin" in cfg:
                try:
                    remote_min = float(cfg["remoteAmountMin"])
                except (TypeError, ValueError):
                    pass

    if abs_high is not None and remote_min is not None and remote_min >= abs_high:
        all_errors.append("一致性: remoteAmountMin 应小于 absoluteHighAmount，否则异地大额与超大额规则边界重叠混乱")

    return {
        "ok": len(all_errors) == 0,
        "errors": all_errors,
        "hints": all_hints,
    }
