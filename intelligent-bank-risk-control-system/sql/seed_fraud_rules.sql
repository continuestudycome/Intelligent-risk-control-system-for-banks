-- 反欺诈参数化规则初始数据（与 application.yml / FraudRiskAssessmentServiceImpl 默认值一致）
-- 首次部署执行一次即可；若已存在相同 rule_code 请使用 INSERT IGNORE 或按需手工调整。

INSERT IGNORE INTO `frd_rule` (
    `rule_code`, `rule_name`, `rule_type`, `rule_condition`, `risk_level`, `priority`, `hit_threshold`, `status`, `version`, `remark`, `is_deleted`
) VALUES
(
    'RULE_AMOUNT_EXTREME',
    '单笔超大额',
    1,
    '{"absoluteHighAmount":85000}',
    'HIGH',
    40,
    1,
    1,
    1,
    '金额达到或超过阈值 → 高危',
    0
),
(
    'RULE_REMOTE_LARGE_TX',
    '异地大额',
    3,
    '{"remoteAmountMin":28000}',
    'HIGH',
    35,
    1,
    1,
    1,
    '与常住省份不同且金额≥阈值 → 高危',
    0
),
(
    'RULE_FREQ_SMALL_PROBE',
    '短时小额试探',
    2,
    '{"probeMaxAmount":500,"probeCountMedium":4}',
    'MEDIUM',
    30,
    1,
    1,
    1,
    '近1小时小额交易笔数≥阈值且当前金额<探测上限 → 中风险',
    0
),
(
    'RULE_ML_ANOMALY_HIGH',
    '孤立森林异常升高危',
    5,
    '{"mlScoreHigh":0.68}',
    'HIGH',
    20,
    1,
    1,
    1,
    '规则未判高危时，异常分≥阈值可将 LOW 升为 HIGH（与 Java mergeWithMl 对齐）',
    0
);
