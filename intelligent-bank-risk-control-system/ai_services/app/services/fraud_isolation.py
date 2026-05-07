"""孤立森林反欺诈评分（演示：启动时在合成数据上 fit）。"""

from __future__ import annotations

import numpy as np
from sklearn.ensemble import IsolationForest


class FraudIsolationService:
    MODEL_VERSION = "if-sklearn-1.5"

    def __init__(self) -> None:
        self._model: IsolationForest | None = None

    def fit_default(self) -> None:
        """启动时训练小型孤立森林（演示用合成数据，线上应离线训练后加载模型文件）"""
        rng = np.random.RandomState(42)
        x = rng.randn(600, 6)
        x[:, 0] *= 0.35
        x[:, 4] *= 0.25
        self._model = IsolationForest(
            n_estimators=120, contamination=0.1, random_state=42
        )
        self._model.fit(x)

    def ensure_ready(self) -> None:
        if self._model is None:
            self.fit_default()

    def anomaly_score(self, features: list[float]) -> tuple[float, str]:
        self.ensure_ready()
        assert self._model is not None
        arr = np.array(features, dtype=np.float64).reshape(1, -1)
        decision = float(self._model.decision_function(arr)[0])
        score = float(1.0 / (1.0 + np.exp(decision)))
        return score, self.MODEL_VERSION


fraud_isolation_service = FraudIsolationService()
