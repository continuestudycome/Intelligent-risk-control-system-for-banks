"""
授信额度智能推荐：与 Java 侧原 CreditLimitRuleHelper 规则一致，统一在 Python 实现。
结合风险等级底盘、黑名单、近 90 日交易笔数、资产（万元）计算建议总额；单笔/日限额按等级分档。
"""

from __future__ import annotations

from decimal import Decimal, ROUND_HALF_UP

LIMIT_MODEL_VERSION = "limit-rule-v1"


def _base_total_limit_by_risk(risk_level: str | None) -> Decimal:
    if not risk_level or not str(risk_level).strip():
        return Decimal("100000.00")
    r = str(risk_level).strip().upper()
    if r == "A":
        return Decimal("500000.00")
    if r == "B":
        return Decimal("300000.00")
    if r == "C":
        return Decimal("100000.00")
    return Decimal("30000.00")


def _single_limit_by_risk(risk_level: str | None) -> Decimal:
    if not risk_level or not str(risk_level).strip():
        return Decimal("10000.00")
    r = str(risk_level).strip().upper()
    if r == "A":
        return Decimal("50000.00")
    if r == "B":
        return Decimal("30000.00")
    if r == "C":
        return Decimal("10000.00")
    return Decimal("3000.00")


def _daily_limit_by_risk(risk_level: str | None) -> Decimal:
    if not risk_level or not str(risk_level).strip():
        return Decimal("50000.00")
    r = str(risk_level).strip().upper()
    if r == "A":
        return Decimal("200000.00")
    if r == "B":
        return Decimal("100000.00")
    if r == "C":
        return Decimal("50000.00")
    return Decimal("10000.00")


def compute_recommended_total(
    risk_level: str | None,
    blacklist: bool,
    txn_count_90d: int,
    asset_amount_wan: Decimal | None,
    min_total: Decimal,
    max_total: Decimal,
) -> Decimal:
    """建议授信总额（元），并限制在 [min_total, max_total]。"""
    base = _base_total_limit_by_risk(risk_level)
    behavior = 1.0
    if blacklist:
        behavior *= 0.25
    if txn_count_90d >= 40:
        behavior *= 1.08
    elif txn_count_90d <= 2:
        behavior *= 0.92
    if asset_amount_wan is not None and float(asset_amount_wan) > 500:
        behavior *= 1.05
    rec = (base * Decimal(str(behavior))).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
    if rec < min_total:
        rec = min_total
    if rec > max_total:
        rec = max_total
    return rec


def recommend_limits(
    risk_level: str | None,
    blacklist: bool,
    txn_count_90d: int,
    asset_amount_wan: float | None,
    min_total: float,
    max_total: float,
) -> dict:
    """
    返回建议总额、单笔限额、日累计限额（元），以及模型版本。
    asset_amount_wan 为客户资产，单位：万元。
    """
    asset_dec: Decimal | None
    if asset_amount_wan is None:
        asset_dec = None
    else:
        asset_dec = Decimal(str(asset_amount_wan))
    min_d = Decimal(str(min_total))
    max_d = Decimal(str(max_total))
    total = compute_recommended_total(
        risk_level, blacklist, txn_count_90d, asset_dec, min_d, max_d
    )
    single = _single_limit_by_risk(risk_level)
    daily = _daily_limit_by_risk(risk_level)
    return {
        "recommended_total": float(total),
        "single_limit": float(single),
        "daily_limit": float(daily),
        "model_version": LIMIT_MODEL_VERSION,
    }
