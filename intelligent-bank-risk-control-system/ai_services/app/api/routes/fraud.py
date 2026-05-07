from fastapi import APIRouter, HTTPException

from app.services.fraud_isolation import fraud_isolation_service

router = APIRouter(tags=["fraud"])


@router.post("/fraud/isolation-score")
async def fraud_isolation_score(body: dict):
    """孤立森林异常评分：features 为 6 维归一化特征，返回 0~1 异常分数（越高越异常）"""
    feats = body.get("features")
    if not feats or not isinstance(feats, list) or len(feats) != 6:
        raise HTTPException(status_code=400, detail="features 必须为长度 6 的数组")
    try:
        values = [float(x) for x in feats]
    except (TypeError, ValueError):
        raise HTTPException(status_code=400, detail="features 元素须为数值") from None

    score, version = fraud_isolation_service.anomaly_score(values)
    return {"anomaly_score": score, "model_version": version}
