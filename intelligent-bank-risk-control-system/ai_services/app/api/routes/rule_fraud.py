"""反欺诈规则：参数智能校验与策略试算（供 Java 风控端调用）。"""
from __future__ import annotations

import json

from fastapi import APIRouter

from app.services.fraud_rule_simulate import run_simulation
from app.services.fraud_rule_validate import validate_batch, validate_rule

router = APIRouter(tags=["rule-fraud"])


@router.post("/rule/fraud/validate")
async def fraud_rules_validate(body: dict):
    """
    校验一批规则的 JSON 配置。
    body: { \"rules\": [ { \"ruleCode\", \"ruleCondition\": \"json字符串或对象\" } ] }
    或单条: { \"ruleCode\", \"ruleCondition\" }
    """
    if body.get("rules") is not None:
        return validate_batch(body["rules"])
    code = body.get("ruleCode")
    cond = body.get("ruleCondition")
    if not code:
        return {"ok": False, "errors": ["缺少 ruleCode"], "hints": []}
    cond_str = cond if isinstance(cond, str) else json.dumps(cond, ensure_ascii=False)
    ok, errs, hints = validate_rule(str(code), cond_str)
    return {"ok": ok, "errors": errs, "hints": hints}


@router.post("/rule/fraud/simulate")
async def fraud_rules_simulate(body: dict):
    """
    使用给定阈值对样本交易做风险等级试算。
    body.thresholds: 平面字典，含 absoluteHighAmount, remoteAmountMin, probeMaxAmount, probeCountMedium,
                     mlScoreHigh, rulesEnabled(可选)
    body.samples: [ { amount, sameProvince, probeCount, mlScore?, mlAvailable? } ]
    """
    th = body.get("thresholds") or {}
    samples = body.get("samples") or []
    if not isinstance(th, dict):
        return {"results": [], "error": "thresholds 须为对象"}
    if not isinstance(samples, list):
        return {"results": [], "error": "samples 须为数组"}
    results = run_simulation(th, samples)
    return {"results": results}
