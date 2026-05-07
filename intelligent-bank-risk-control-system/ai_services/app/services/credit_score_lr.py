"""
个人信用评分（演示）：逻辑回归 + 8 维归一化特征，输出 300–900 分与 A/B/C/D。
训练数据为合成样本；生产环境应离线训练并加载持久化模型。
"""

from __future__ import annotations

import numpy as np
from sklearn.linear_model import LogisticRegression


MODEL_VERSION = "credit-lr-sklearn-1.0"
# 演示用固定指标（随模型部署写入库 crd_model_version.metrics）
DEMO_METRICS = {
    "auc": 0.847,
    "f1_score": 0.812,
    "accuracy": 0.835,
    "precision": 0.806,
    "recall": 0.818,
}


def risk_from_score(score: int) -> str:
    if score >= 720:
        return "A"
    if score >= 620:
        return "B"
    if score >= 520:
        return "C"
    return "D"


class CreditScoreLRModel:
    def __init__(self) -> None:
        self._clf: LogisticRegression | None = None

    def fit_default(self) -> None:
        rng = np.random.RandomState(42)
        n = 1200
        x = rng.rand(n, 8)
        w = np.array([0.22, 0.18, 0.14, 0.12, 0.11, 0.09, 0.08, 0.06])
        logits = (x * w).sum(axis=1) + rng.normal(0, 0.06, n)
        y = (logits > 0.42).astype(int)
        self._clf = LogisticRegression(max_iter=400, random_state=42)
        self._clf.fit(x, y)

    def ensure_fit(self) -> None:
        if self._clf is None:
            self.fit_default()

    def predict(self, features: list[float]) -> tuple[int, str, float]:
        self.ensure_fit()
        assert self._clf is not None
        arr = np.array(features, dtype=np.float64).reshape(1, -1)
        proba = self._clf.predict_proba(arr)[0]
        # 假设正类 1 = 信用较好
        p_good = float(proba[1] if len(proba) > 1 else proba[0])
        raw = 300.0 + p_good * 600.0
        score = int(round(raw))
        score = max(300, min(900, score))
        return score, risk_from_score(score), p_good


credit_lr_model = CreditScoreLRModel()
