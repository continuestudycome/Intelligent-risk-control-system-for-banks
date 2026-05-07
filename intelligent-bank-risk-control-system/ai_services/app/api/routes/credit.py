from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field

from app.services.credit_limit_recommend import recommend_limits
from app.services.credit_score_lr import DEMO_METRICS, MODEL_VERSION, credit_lr_model

router = APIRouter(tags=["credit"])


class LimitRecommendBody(BaseModel):
    """与 backend CreditLimitAiClient 请求体一致。"""

    risk_level: str = Field(default="C", description="最新信用风险等级 A/B/C/D")
    blacklist: bool = False
    txn_count_90d: int = Field(default=0, ge=0, description="近 90 日成功交易笔数")
    asset_amount_wan: float | None = Field(default=None, description="资产，万元；可选")
    min_total: float = Field(default=10_000, gt=0, description="建议总额下限（元）")
    max_total: float = Field(default=2_000_000, gt=0, description="建议总额上限（元）")


@router.post("/credit/score")
async def credit_score(body: dict):
    """
    信用评分：features 为 8 维 [0,1] 归一化特征（收入、资产、授权、资料、交易笔数、交易额、黑名单、历史等级分）。
    返回 300–900 分、风险等级、违约/劣后的对立概率视为 (1-p_good) 仅作展示。
    """
    feats = body.get("features")
    if not feats or not isinstance(feats, list) or len(feats) != 8:
        raise HTTPException(status_code=400, detail="features 必须为长度 8 的数组")
    try:
        values = [float(x) for x in feats]
    except (TypeError, ValueError):
        raise HTTPException(status_code=400, detail="features 元素须为数值") from None
    for v in values:
        if v < -1e-6 or v > 1.0 + 1e-6:
            raise HTTPException(status_code=400, detail="features 元素应在 [0,1] 归一化区间内")

    score, risk, p_good = credit_lr_model.predict(values)
    return {
        "credit_score": score,
        "risk_level": risk,
        "good_probability": round(p_good, 6),
        "model_version": MODEL_VERSION,
        "metrics": DEMO_METRICS,
    }


@router.post("/credit/limit-recommend")
async def credit_limit_recommend(body: LimitRecommendBody):
    """
    授信额度智能推荐：依据风险等级、黑名单、近 90 日交易活跃度、资产（万元）计算建议总额；
    单笔/日限额按等级分档。供 Java 定时批调与初始化授信调用。
    """
    if body.min_total > body.max_total:
        raise HTTPException(status_code=400, detail="min_total 不能大于 max_total")
    return recommend_limits(
        body.risk_level,
        body.blacklist,
        body.txn_count_90d,
        body.asset_amount_wan,
        body.min_total,
        body.max_total,
    )
