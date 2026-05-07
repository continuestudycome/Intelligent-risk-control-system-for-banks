-- 授信额度上调人工复核工单（与 crd_limit / crd_limit_history 配合）
USE `bank_risk_control`;

CREATE TABLE IF NOT EXISTS `crd_limit_adjust_request` (
    `id`                    BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT COMMENT '工单ID',
    `customer_id`           BIGINT UNSIGNED     NOT NULL                COMMENT '客户ID',
    `current_total_limit`   DECIMAL(16,2)       NOT NULL                COMMENT '申请时授信总额(元)',
    `proposed_total_limit`  DECIMAL(16,2)       NOT NULL                COMMENT '建议上调至(元)',
    `trigger_score`       INT                 DEFAULT NULL            COMMENT '触发时信用评分',
    `trigger_risk_level`    CHAR(1)             DEFAULT NULL            COMMENT '触发时风险等级',
    `reason`                VARCHAR(512)        DEFAULT NULL            COMMENT '系统说明',
    `status`                VARCHAR(32)         NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
    `reviewer_id`           BIGINT UNSIGNED     DEFAULT NULL            COMMENT '复核人用户ID',
    `review_comment`        VARCHAR(512)       DEFAULT NULL            COMMENT '复核意见',
    `create_time`           DATETIME            DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `review_time`           DATETIME            DEFAULT NULL            COMMENT '复核时间',
    PRIMARY KEY (`id`),
    KEY `idx_customer_status` (`customer_id`, `status`),
    KEY `idx_status_time` (`status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='授信上调复核工单';
