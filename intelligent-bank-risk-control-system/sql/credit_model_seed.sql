-- 信用评分模型元数据（与 ai_services credit_score_lr.MODEL_VERSION / DEMO_METRICS 对齐）
USE `bank_risk_control`;

INSERT INTO `crd_model_version` (
    `model_code`,
    `model_name`,
    `version`,
    `algorithm`,
    `metrics`,
    `is_active`,
    `train_time`,
    `remark`
) VALUES (
    'CREDIT_SCORE',
    '个人信用评分(逻辑回归)',
    'credit-lr-sklearn-1.0',
    'LogisticRegression',
    '{"auc":0.847,"f1_score":0.812,"accuracy":0.835,"precision":0.806,"recall":0.818}',
    1,
    NOW(),
    '智能服务启动时用合成样本训练，演示环境专用'
)
ON DUPLICATE KEY UPDATE
    `model_name` = VALUES(`model_name`),
    `algorithm` = VALUES(`algorithm`),
    `metrics` = VALUES(`metrics`),
    `is_active` = VALUES(`is_active`),
    `train_time` = VALUES(`train_time`),
    `remark` = VALUES(`remark`);
