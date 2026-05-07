-- ============================================================
-- 数据库: bank_risk_control
-- 字符集: utf8mb4
-- 排序规则: utf8mb4_unicode_ci
-- 存储引擎: InnoDB
-- 适用版本: MySQL 8.0+
-- 设计规范: 阿里巴巴 Java 开发手册 + 企业级数据建模规范
-- 约束说明: 本库不设置物理外键，关联关系由业务层与索引保证
-- ============================================================

CREATE DATABASE IF NOT EXISTS `bank_risk_control`
    DEFAULT CHARACTER SET `utf8mb4`
    COLLATE `utf8mb4_unicode_ci`;

USE `bank_risk_control`;

-- ============================================================
-- 1. 用户与权限管理模块 (sys_)
-- ============================================================

CREATE TABLE `sys_user` (
    `id`              BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`        VARCHAR(64)         NOT NULL                COMMENT '登录账号',
    `password`        VARCHAR(128)        NOT NULL                COMMENT '登录密码(BCrypt)',
    `real_name`       VARCHAR(64)         DEFAULT NULL            COMMENT '真实姓名',
    `gender`          TINYINT UNSIGNED    DEFAULT NULL            COMMENT '性别: 0-女 1-男',
    `phone`           VARCHAR(32)         DEFAULT NULL            COMMENT '手机号',
    `email`           VARCHAR(128)        DEFAULT NULL            COMMENT '电子邮箱',
    `avatar`          VARCHAR(512)        DEFAULT NULL            COMMENT '头像URL',
    `status`          TINYINT UNSIGNED    DEFAULT 1               COMMENT '账号状态: 0-禁用 1-启用 2-锁定',
    `last_login_time` DATETIME            DEFAULT NULL            COMMENT '最后登录时间',
    `remark`          VARCHAR(512)        DEFAULT NULL            COMMENT '备注',
    `create_by`       BIGINT UNSIGNED     DEFAULT NULL            COMMENT '创建人ID',
    `create_time`     DATETIME            DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`       BIGINT UNSIGNED     DEFAULT NULL            COMMENT '更新人ID',
    `update_time`     DATETIME            DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      TINYINT UNSIGNED    DEFAULT 0               COMMENT '逻辑删除: 0-正常 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_username` (`username`),
    UNIQUE INDEX `uk_phone` (`phone`),
    INDEX `idx_status` (`status`, `is_deleted`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

CREATE TABLE `sys_role` (
    `id`              BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    `role_code`       VARCHAR(64)         NOT NULL                COMMENT '角色编码: ADMIN/RISK_MANAGER/OPERATOR...',
    `role_name`       VARCHAR(64)         NOT NULL                COMMENT '角色名称',
    `role_desc`       VARCHAR(256)        DEFAULT NULL            COMMENT '角色描述',
    `status`          TINYINT UNSIGNED    DEFAULT 1               COMMENT '状态: 0-禁用 1-启用',
    `remark`          VARCHAR(512)        DEFAULT NULL            COMMENT '备注',
    `create_by`       BIGINT UNSIGNED     DEFAULT NULL            COMMENT '创建人ID',
    `create_time`     DATETIME            DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`       BIGINT UNSIGNED     DEFAULT NULL            COMMENT '更新人ID',
    `update_time`     DATETIME            DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      TINYINT UNSIGNED    DEFAULT 0               COMMENT '逻辑删除标志',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_role_code` (`role_code`),
    INDEX `idx_status` (`status`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统角色映射表';

-- 用户角色关联: 支持多角色
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
    `id`              BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT COMMENT '关联ID',
    `user_id`         BIGINT UNSIGNED     NOT NULL                COMMENT '用户ID',
    `role_id`         BIGINT UNSIGNED     NOT NULL                COMMENT '角色ID',
    `create_time`     DATETIME            DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_user_role` (`user_id`, `role_id`),
    INDEX `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';


-- ============================================================
-- 初始化客户和风控人员角色及账号
-- ============================================================

-- 1. 插入两个角色
INSERT INTO `sys_role` (`role_code`, `role_name`, `role_desc`, `status`) VALUES
('CUSTOMER', '客户', '普通客户角色，可以查看个人信息、申请贷款等', 1),
('RISK_MANAGER', '风控人员', '负责风控管理、审批、风险预警处理', 1);

-- 2. 插入两个用户账号 (密码: 123456, BCrypt加密)
-- 客户账号
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `phone`, `email`, `status`) VALUES
('customer001', '$2b$10$gimzKrQ/9s15OREMjIAGkeF.Mbj6h1MmaX3DsHQsnmZmnxZIScET.', '张三', '13800138001', 'customer001@example.com', 1);

-- 风控人员账号
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `phone`, `email`, `status`) VALUES
('risk_manager001', '$2b$10$gimzKrQ/9s15OREMjIAGkeF.Mbj6h1MmaX3DsHQsnmZmnxZIScET.', '李四', '13800138002', 'risk_manager001@example.com', 1);

-- 3. 关联用户角色
-- 客户角色关联 (假设客户ID为1，角色ID为1)
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES (1, 1);

-- 风控人员角色关联 (假设风控人员ID为2，角色ID为2)
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES (2, 2);

-- 操作日志: 记录关键操作，满足审计要求
CREATE TABLE `sys_operation_log` (
    `id`              BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `user_id`         BIGINT UNSIGNED     DEFAULT NULL            COMMENT '操作用户ID',
    `username`        VARCHAR(64)         DEFAULT NULL            COMMENT '操作用户名',
    `operation_type`  VARCHAR(64)         NOT NULL                COMMENT '操作类型: INSERT/UPDATE/DELETE/LOGIN 等',
    `title`           VARCHAR(128)        DEFAULT NULL            COMMENT '操作模块标题',
    `method`          VARCHAR(512)        DEFAULT NULL            COMMENT '请求方法名(类名.方法名)',
    `request_url`     VARCHAR(512)        DEFAULT NULL            COMMENT '请求URL',
    `request_method`  VARCHAR(16)         DEFAULT NULL            COMMENT 'HTTP方法: GET/POST/PUT/DELETE',
    `request_params`  LONGTEXT            DEFAULT NULL            COMMENT '请求参数(JSON)',
    `response_data`   LONGTEXT            DEFAULT NULL            COMMENT '响应结果(JSON)',
    `ip_address`      VARCHAR(64)         DEFAULT NULL            COMMENT '操作IP',
    `user_agent`      VARCHAR(512)        DEFAULT NULL            COMMENT '浏览器UA',
    `execute_time`    INT UNSIGNED        DEFAULT 0               COMMENT '执行时长(ms)',
    `status`          TINYINT UNSIGNED    DEFAULT 1               COMMENT '操作状态: 0-失败 1-成功',
    `error_msg`       VARCHAR(2048)       DEFAULT NULL            COMMENT '错误信息',
    `create_time`     DATETIME            DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`, `create_time`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_operation_type` (`operation_type`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统操作日志表';

-- ============================================================
-- 2. 客户信息管理模块 (cust_)
-- ============================================================
CREATE TABLE `cust_customer` (
    `id`              BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT COMMENT '客户ID',
    `user_id`         BIGINT UNSIGNED     NOT NULL                COMMENT '关联sys_user.id',
    `customer_no`     VARCHAR(32)         NOT NULL                COMMENT '客户编号(唯一业务号)',
    `customer_type`   TINYINT UNSIGNED    NOT NULL                COMMENT '客户类型: 1-个人 2-企业',
    `real_name`       VARCHAR(128)        NOT NULL                COMMENT '真实姓名/企业全称',
    `id_card_no`      VARCHAR(255)        NOT NULL                COMMENT '身份证号/统一社会信用代码(AES加密)',
    `phone`           VARCHAR(32)         DEFAULT NULL            COMMENT '联系电话',
    `email`           VARCHAR(128)        DEFAULT NULL            COMMENT '电子邮箱',
    `province`        VARCHAR(64)         DEFAULT NULL            COMMENT '所在省份',
    `city`            VARCHAR(64)         DEFAULT NULL            COMMENT '所在城市',
    `address`         VARCHAR(512)        DEFAULT NULL            COMMENT '详细地址',
    `annual_income`   DECIMAL(16,2)       DEFAULT NULL            COMMENT '年收入/年营业额(万元)',
    `asset_amount`    DECIMAL(16,2)       DEFAULT NULL            COMMENT '资产总额(万元)',
    `credit_authorized` TINYINT UNSIGNED  DEFAULT 0               COMMENT '征信授权: 0-未授权 1-已授权',
    `profile_completed` TINYINT UNSIGNED  DEFAULT 0               COMMENT '资料是否完善: 0-否 1-是',
    `credit_level`    CHAR(1)             DEFAULT NULL            COMMENT '信用等级: A/B/C/D',
    `is_blacklist`    TINYINT UNSIGNED    DEFAULT 0               COMMENT '是否黑名单: 0-否 1-是',
    `blacklist_reason` VARCHAR(512)       DEFAULT NULL            COMMENT '黑名单原因',
    `status`          TINYINT UNSIGNED    DEFAULT 1               COMMENT '客户状态: 0-注销 1-正常 2-冻结',
    `remark`          VARCHAR(512)        DEFAULT NULL            COMMENT '备注',
    `create_by`       BIGINT UNSIGNED     DEFAULT NULL            COMMENT '创建人ID',
    `create_time`     DATETIME            DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`       BIGINT UNSIGNED     DEFAULT NULL            COMMENT '更新人ID',
    `update_time`     DATETIME            DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      TINYINT UNSIGNED    DEFAULT 0               COMMENT '逻辑删除标志',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_user_id` (`user_id`),
    UNIQUE INDEX `uk_customer_no` (`customer_no`),
    UNIQUE INDEX `uk_id_card_no` (`id_card_no`),
    INDEX `idx_customer_type` (`customer_type`, `status`, `is_deleted`),
    INDEX `idx_credit_level` (`credit_level`),
    INDEX `idx_is_blacklist` (`is_blacklist`, `status`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户信息主表';

CREATE TABLE `cust_kyc_record` (
    `id`              BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `customer_id`     BIGINT UNSIGNED     NOT NULL                COMMENT '客户ID',
    `kyc_type`        TINYINT UNSIGNED    NOT NULL                COMMENT 'KYC类型: 1-证件核验 2-征信授权 3-资产证明',
    `kyc_status`      TINYINT UNSIGNED    NOT NULL                COMMENT '状态: 0-待处理 1-已通过 2-已拒绝',
    `id_card_image`   VARCHAR(512)        DEFAULT NULL            COMMENT '证件图片URL',
    `ocr_result`      JSON                DEFAULT NULL            COMMENT 'OCR识别结果(JSON)',
    `verify_score`    DECIMAL(5,4)        DEFAULT NULL            COMMENT '核验置信度(0-1)',
    `verify_result`   VARCHAR(512)        DEFAULT NULL            COMMENT '核验结果说明',
    `remark`          VARCHAR(512)        DEFAULT NULL            COMMENT '备注',
    `create_by`       BIGINT UNSIGNED     DEFAULT NULL            COMMENT '创建人ID',
    `create_time`     DATETIME            DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`       BIGINT UNSIGNED     DEFAULT NULL            COMMENT '更新人ID',
    `update_time`     DATETIME            DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      TINYINT UNSIGNED    DEFAULT 0               COMMENT '逻辑删除标志',
    PRIMARY KEY (`id`),
    INDEX `idx_customer_id` (`customer_id`, `kyc_type`, `kyc_status`),
    INDEX `idx_kyc_status` (`kyc_status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户KYC记录表';

-- ============================================================
-- 3. 贷款/信用卡申请与审批流模块 (loan_)
-- ============================================================
CREATE TABLE `loan_application` (
    `id`              BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT COMMENT '申请ID',
    `application_no`  VARCHAR(32)         NOT NULL                COMMENT '申请编号(唯一业务号)',
    `customer_id`     BIGINT UNSIGNED     NOT NULL                COMMENT '客户ID',
    `apply_type`      TINYINT UNSIGNED    NOT NULL                COMMENT '申请类型: 1-信用贷款 2-抵押贷款 3-信用卡',
    `apply_amount`    DECIMAL(16,2)       NOT NULL                COMMENT '申请金额(元)',
    `apply_term`      INT UNSIGNED        DEFAULT NULL            COMMENT '申请期限(月)',
    `apply_purpose`   VARCHAR(256)        DEFAULT NULL            COMMENT '资金用途',
    `credit_score`    INT UNSIGNED        DEFAULT NULL            COMMENT '关联信用评分值',
    `suggested_limit` DECIMAL(16,2)       DEFAULT NULL            COMMENT '系统推荐额度(元)',
    `current_status`  VARCHAR(32)         DEFAULT 'PENDING'       COMMENT '当前状态: PENDING/FIRST_REVIEW/SECOND_REVIEW/FINAL_REVIEW/APPROVED/REJECTED',
    `final_result`    TINYINT UNSIGNED    DEFAULT NULL            COMMENT '最终结果: 0-拒绝 1-通过',
    `final_amount`    DECIMAL(16,2)       DEFAULT NULL            COMMENT '审批通过金额(元)',
    `final_term`      INT UNSIGNED        DEFAULT NULL            COMMENT '审批通过期限(月)',
    `remark`          VARCHAR(512)        DEFAULT NULL            COMMENT '备注',
    `create_by`       BIGINT UNSIGNED     DEFAULT NULL            COMMENT '创建人ID',
    `create_time`     DATETIME            DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`       BIGINT UNSIGNED     DEFAULT NULL            COMMENT '更新人ID',
    `update_time`     DATETIME            DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      TINYINT UNSIGNED    DEFAULT 0               COMMENT '逻辑删除标志',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_application_no` (`application_no`),
    KEY `idx_customer_id` (`customer_id`, `is_deleted`),
    KEY `idx_current_status` (`current_status`, `create_time`),
    KEY `idx_apply_type` (`apply_type`, `current_status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='贷款/信用卡申请表';

CREATE TABLE `loan_approval_record` (
    `id`              BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `application_id`  BIGINT UNSIGNED     NOT NULL                COMMENT '申请ID',
    `review_level`    TINYINT UNSIGNED    NOT NULL                COMMENT '审批层级: 1-初审 2-复审 3-终审',
    `reviewer_id`     BIGINT UNSIGNED     DEFAULT NULL            COMMENT '审批人ID',
    `reviewer_name`   VARCHAR(64)         DEFAULT NULL            COMMENT '审批人姓名',
    `action`          VARCHAR(32)         NOT NULL                COMMENT '操作: PASS/REJECT/RETURN/SUPPLEMENT',
    `comment`         TEXT                DEFAULT NULL            COMMENT '审批意见',
    `next_level`      TINYINT UNSIGNED    DEFAULT NULL            COMMENT '下一处理层级',
    `create_time`     DATETIME            DEFAULT CURRENT_TIMESTAMP COMMENT '审批时间',
    PRIMARY KEY (`id`),
    KEY `idx_application_id` (`application_id`, `review_level`),
    KEY `idx_reviewer_id` (`reviewer_id`, `create_time`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批记录表';

-- ============================================================
-- 4. 简单模拟交易模块 (txn_)
-- ============================================================
CREATE TABLE `acct_account` (
    `id`                BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT COMMENT '账户ID',
    `account_no`        VARCHAR(64)         NOT NULL                COMMENT '账户号',
    `customer_id`       BIGINT UNSIGNED     NOT NULL                COMMENT '客户ID',
    `account_name`      VARCHAR(64)         NOT NULL                COMMENT '账户名称',
    `account_type`      TINYINT UNSIGNED    NOT NULL                COMMENT '账户类型: 1-借记卡 2-信用卡 3-对公账户',
    `balance`           DECIMAL(16,2)       NOT NULL DEFAULT 0.00   COMMENT '可用余额',
    `currency`          VARCHAR(8)          NOT NULL DEFAULT 'CNY'  COMMENT '币种',
    `status`            TINYINT UNSIGNED    NOT NULL DEFAULT 1      COMMENT '状态: 0-禁用 1-可用',
    `create_time`       DATETIME            DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME            DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`        TINYINT UNSIGNED    DEFAULT 0               COMMENT '逻辑删除标志',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_account_no` (`account_no`),
    KEY `idx_customer_id` (`customer_id`, `status`),
    KEY `idx_status` (`status`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户账户表';

CREATE TABLE `txn_transaction` (
    `id`                  BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT COMMENT '交易ID',
    `transaction_no`      VARCHAR(32)         NOT NULL                COMMENT '交易流水号(唯一业务号)',
    `customer_id`         BIGINT UNSIGNED     NOT NULL                COMMENT '付款客户ID',
    `transaction_type`    TINYINT UNSIGNED    NOT NULL                COMMENT '交易类型: 1-转账 2-消费 3-取现 4-还款',
    `from_account`        VARCHAR(64)         DEFAULT NULL            COMMENT '付款账户',
    `to_account`          VARCHAR(64)         DEFAULT NULL            COMMENT '收款账户',
    `amount`              DECIMAL(16,2)       NOT NULL                COMMENT '交易金额(元)',
    `currency`            VARCHAR(8)          DEFAULT 'CNY'           COMMENT '币种: CNY-人民币',
    `transaction_province` VARCHAR(64)        DEFAULT NULL            COMMENT '交易省份',
    `transaction_city`    VARCHAR(64)         DEFAULT NULL            COMMENT '交易城市',
    `transaction_time`    DATETIME            NOT NULL                COMMENT '交易发生时间',
    `remark`              VARCHAR(512)        DEFAULT NULL            COMMENT '交易备注',
    `risk_status`         VARCHAR(32)         DEFAULT 'LOW'           COMMENT '风险状态: LOW/MEDIUM/HIGH/INTERCEPTED/CONFIRMED_FRAUD',
    `handle_result`       TINYINT UNSIGNED    DEFAULT NULL            COMMENT '处置结果: 0-拦截 1-放行 2-二次验证中',
    `create_time`         DATETIME            DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_transaction_no` (`transaction_no`),
    KEY `idx_customer_id` (`customer_id`, `transaction_time`),
    KEY `idx_transaction_type` (`transaction_type`, `create_time`),
    KEY `idx_risk_status` (`risk_status`, `create_time`),
    KEY `idx_transaction_time` (`transaction_time`),
    KEY `idx_amount` (`amount`, `risk_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易流水表';

-- ============================================================
-- 5. 交易实时监控与反欺诈模块 (frd_)
-- ============================================================
CREATE TABLE `frd_rule` (
    `id`              BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT COMMENT '规则ID',
    `rule_code`       VARCHAR(64)         NOT NULL                COMMENT '规则编码(唯一)',
    `rule_name`       VARCHAR(128)        NOT NULL                COMMENT '规则名称',
    `rule_type`       TINYINT UNSIGNED    NOT NULL                COMMENT '规则类型: 1-金额类 2-频次类 3-地域类 4-时间类 5-组合类',
    `rule_condition`  TEXT                NOT NULL                COMMENT '规则条件表达式(SpEL/QLExpress)',
    `risk_level`      VARCHAR(16)         NOT NULL                COMMENT '触达风险等级: HIGH/MEDIUM',
    `priority`        INT                 DEFAULT 0               COMMENT '优先级(数字越大优先级越高)',
    `hit_threshold`   INT                 DEFAULT 1               COMMENT '命中阈值(连续命中次数)',
    `status`          TINYINT UNSIGNED    DEFAULT 1               COMMENT '状态: 0-停用 1-启用',
    `version`         INT UNSIGNED        DEFAULT 1               COMMENT '规则版本号',
    `effective_time`  DATETIME            DEFAULT NULL            COMMENT '生效时间',
    `expire_time`     DATETIME            DEFAULT NULL            COMMENT '失效时间',
    `remark`          VARCHAR(512)        DEFAULT NULL            COMMENT '备注',
    `create_by`       BIGINT UNSIGNED     DEFAULT NULL            COMMENT '创建人ID',
    `create_time`     DATETIME            DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`       BIGINT UNSIGNED     DEFAULT NULL            COMMENT '更新人ID',
    `update_time`     DATETIME            DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      TINYINT UNSIGNED    DEFAULT 0               COMMENT '逻辑删除标志',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rule_code` (`rule_code`),
    KEY `idx_rule_type` (`rule_type`, `status`, `priority`),
    KEY `idx_status` (`status`, `is_deleted`, `priority`),
    KEY `idx_version` (`rule_code`, `version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='反欺诈规则表';

CREATE TABLE `frd_alert` (
    `id`                BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT COMMENT '预警ID',
    `transaction_id`    BIGINT UNSIGNED     NOT NULL                COMMENT '交易ID',
    `transaction_no`    VARCHAR(32)         NOT NULL                COMMENT '交易流水号',
    `customer_id`       BIGINT UNSIGNED     NOT NULL                COMMENT '客户ID',
    `alert_level`       VARCHAR(16)         NOT NULL                COMMENT '预警等级: HIGH/MEDIUM',
    `hit_rules`         VARCHAR(1024)       DEFAULT NULL            COMMENT '命中的规则编码列表(逗号分隔)',
    `ml_score`          DECIMAL(5,4)        DEFAULT NULL            COMMENT '模型风险分数(0.0000-1.0000)',
    `ml_model_version`  VARCHAR(32)         DEFAULT NULL            COMMENT '模型版本',
    `feature_snapshot`  JSON                DEFAULT NULL            COMMENT '交易特征快照(JSON)',
    `status`            VARCHAR(32)         DEFAULT 'PENDING'       COMMENT '处理状态: PENDING/REVIEWING/CONFIRMED/IGNORED',
    `reviewer_id`       BIGINT UNSIGNED     DEFAULT NULL            COMMENT '复核人ID',
    `review_comment`    VARCHAR(1024)       DEFAULT NULL            COMMENT '复核意见',
    `review_time`       DATETIME            DEFAULT NULL            COMMENT '复核时间',
    `create_time`       DATETIME            DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_transaction_id` (`transaction_id`),
    KEY `idx_customer_id` (`customer_id`, `status`),
    KEY `idx_alert_level` (`alert_level`, `status`, `create_time`),
    KEY `idx_status` (`status`, `create_time`),
    KEY `idx_reviewer_id` (`reviewer_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='欺诈预警表';

CREATE TABLE `frd_case` (
    `id`                BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT COMMENT '案例ID',
    `transaction_id`    BIGINT UNSIGNED     NOT NULL                COMMENT '关联交易ID',
    `alert_id`          BIGINT UNSIGNED     DEFAULT NULL            COMMENT '关联预警ID',
    `customer_id`       BIGINT UNSIGNED     NOT NULL                COMMENT '客户ID',
    `fraud_type`        VARCHAR(64)         DEFAULT NULL            COMMENT '欺诈类型: THEFT-盗刷 LAUNDER-洗钱 CASHOUT-套现 IMPERSONATE-伪冒',
    `confirmed_result`  TINYINT UNSIGNED    NOT NULL                COMMENT '确认结果: 0-误报 1-确认欺诈',
    `feature_snapshot`  JSON                DEFAULT NULL            COMMENT '交易特征快照(用于模型训练)',
    `label_source`      TINYINT UNSIGNED    DEFAULT 1               COMMENT '标注来源: 1-人工复核 2-自动确认',
    `create_time`       DATETIME            DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_transaction_id` (`transaction_id`),
    KEY `idx_alert_id` (`alert_id`),
    KEY `idx_customer_id` (`customer_id`, `confirmed_result`),
    KEY `idx_fraud_type` (`fraud_type`, `confirmed_result`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='欺诈案例库表';

-- ============================================================
-- 6. 信用评分与额度管理模块 (crd_)
-- ============================================================
CREATE TABLE `crd_score` (
    `id`                BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT COMMENT '评分ID',
    `customer_id`       BIGINT UNSIGNED     NOT NULL                COMMENT '客户ID',
    `score`             INT UNSIGNED        NOT NULL                COMMENT '信用评分(300-900)',
    `risk_level`        CHAR(1)             NOT NULL                COMMENT '风险等级: A/B/C/D',
    `model_version`     VARCHAR(32)         NOT NULL                COMMENT '评分模型版本',
    `feature_data`      JSON                DEFAULT NULL            COMMENT '评分特征数据快照(JSON)',
    `calc_duration_ms`  INT UNSIGNED        DEFAULT NULL            COMMENT '计算耗时(ms)',
    `create_time`       DATETIME            DEFAULT CURRENT_TIMESTAMP COMMENT '评分时间',
    PRIMARY KEY (`id`),
    KEY `idx_customer_id` (`customer_id`, `create_time`),
    KEY `idx_score` (`score`),
    KEY `idx_risk_level` (`risk_level`, `create_time`),
    KEY `idx_model_version` (`model_version`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='信用评分记录表';

CREATE TABLE `crd_limit` (
    `id`                BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT COMMENT '额度ID',
    `customer_id`       BIGINT UNSIGNED     NOT NULL                COMMENT '客户ID',
    `total_limit`       DECIMAL(16,2)       NOT NULL                COMMENT '授信总额度(元)',
    `used_limit`        DECIMAL(16,2)       DEFAULT 0.00            COMMENT '已用额度(元)',
    `available_limit`   DECIMAL(16,2)       NOT NULL                COMMENT '可用额度(元)',
    `single_limit`      DECIMAL(16,2)       DEFAULT NULL            COMMENT '单笔限额(元)',
    `daily_limit`       DECIMAL(16,2)       DEFAULT NULL            COMMENT '日累计限额(元)',
    `last_adjust_time`  DATETIME            DEFAULT NULL            COMMENT '上次调整时间',
    `remark`            VARCHAR(512)        DEFAULT NULL            COMMENT '备注',
    `create_by`         BIGINT UNSIGNED     DEFAULT NULL            COMMENT '创建人ID',
    `create_time`       DATETIME            DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`         BIGINT UNSIGNED     DEFAULT NULL            COMMENT '更新人ID',
    `update_time`       DATETIME            DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`        TINYINT UNSIGNED    DEFAULT 0               COMMENT '逻辑删除标志',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_customer_id` (`customer_id`),
    KEY `idx_available_limit` (`available_limit`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户信用额度表';

CREATE TABLE `crd_limit_history` (
    `id`            BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT COMMENT '历史ID',
    `customer_id`   BIGINT UNSIGNED     NOT NULL                COMMENT '客户ID',
    `adjust_type`   TINYINT UNSIGNED    NOT NULL                COMMENT '调整类型: 1-上调 2-下调 3-冻结 4-解冻',
    `old_limit`     DECIMAL(16,2)       NOT NULL                COMMENT '调整前额度(元)',
    `new_limit`     DECIMAL(16,2)       NOT NULL                COMMENT '调整后额度(元)',
    `reason`        VARCHAR(512)        DEFAULT NULL            COMMENT '调整原因',
    `operator_id`   BIGINT UNSIGNED     DEFAULT NULL            COMMENT '操作人ID',
    `operator_name` VARCHAR(64)         DEFAULT NULL            COMMENT '操作人姓名',
    `create_time`   DATETIME            DEFAULT CURRENT_TIMESTAMP COMMENT '调整时间',
    PRIMARY KEY (`id`),
    KEY `idx_customer_id` (`customer_id`, `create_time`),
    KEY `idx_adjust_type` (`adjust_type`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='额度调整历史表';

CREATE TABLE `crd_model_version` (
    `id`          BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT COMMENT '模型ID',
    `model_code`  VARCHAR(64)         NOT NULL                COMMENT '模型编码: CREDIT_SCORE/FRAUD_DETECT/OCR',
    `model_name`  VARCHAR(128)        NOT NULL                COMMENT '模型名称',
    `version`     VARCHAR(32)         NOT NULL                COMMENT '版本号',
    `model_path`  VARCHAR(512)        DEFAULT NULL            COMMENT '模型文件存储路径(OSS/本地)',
    `algorithm`   VARCHAR(64)         DEFAULT NULL            COMMENT '算法: LogisticRegression/XGBoost/IsolationForest/ResNet',
    `metrics`     JSON                DEFAULT NULL            COMMENT '模型评估指标(JSON): AUC/F1/Accuracy/Precision/Recall',
    `is_active`   TINYINT UNSIGNED    DEFAULT 0               COMMENT '是否当前启用: 0-否 1-是',
    `train_time`  DATETIME            DEFAULT NULL            COMMENT '训练时间',
    `remark`      VARCHAR(512)        DEFAULT NULL            COMMENT '备注',
    `create_by`   BIGINT UNSIGNED     DEFAULT NULL            COMMENT '创建人ID',
    `create_time` DATETIME            DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   BIGINT UNSIGNED     DEFAULT NULL            COMMENT '更新人ID',
    `update_time` DATETIME            DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_model_version` (`model_code`, `version`),
    KEY `idx_model_code` (`model_code`, `is_active`),
    KEY `idx_is_active` (`is_active`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI模型版本管理表';

-- ============================================================
-- 7. 智能客服模块 (bot_)
-- ============================================================
CREATE TABLE `bot_knowledge` (
    `id`              BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT COMMENT '知识ID',
    `category`        VARCHAR(64)         NOT NULL                COMMENT '知识分类: LOAN-贷款 CREDIT_CARD-信用卡 FRAUD-反欺诈 LIMIT-额度 ACCOUNT-账户 GENERAL-通用',
    `question`        VARCHAR(512)        NOT NULL                COMMENT '标准问题',
    `answer`          TEXT                NOT NULL                COMMENT '标准答案',
    `similar_questions` JSON              DEFAULT NULL            COMMENT '相似问法列表(JSON数组)',
    `keywords`        VARCHAR(512)        DEFAULT NULL            COMMENT '关键词(逗号分隔，用于检索匹配)',
    `hit_count`       INT UNSIGNED        DEFAULT 0               COMMENT '命中次数',
    `status`          TINYINT UNSIGNED    DEFAULT 1               COMMENT '状态: 0-停用 1-启用',
    `create_by`       BIGINT UNSIGNED     DEFAULT NULL            COMMENT '创建人ID',
    `create_time`     DATETIME            DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`       BIGINT UNSIGNED     DEFAULT NULL            COMMENT '更新人ID',
    `update_time`     DATETIME            DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      TINYINT UNSIGNED    DEFAULT 0               COMMENT '逻辑删除标志',
    PRIMARY KEY (`id`),
    KEY `idx_category` (`category`, `status`),
    KEY `idx_keywords` (`keywords`),
    KEY `idx_status` (`status`, `is_deleted`),
    FULLTEXT INDEX `ft_question` (`question`) COMMENT '全文索引用于语义检索'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库表';

CREATE TABLE `bot_session` (
    `id`              BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT COMMENT '会话ID',
    `session_id`      VARCHAR(64)         NOT NULL                COMMENT '会话唯一标识(UUID)',
    `user_id`         BIGINT UNSIGNED     DEFAULT NULL            COMMENT '用户ID(登录用户)',
    `user_type`       TINYINT UNSIGNED    DEFAULT 1               COMMENT '用户类型: 1-访客 2-登录客户 3-内部人员',
    `channel`         VARCHAR(32)         DEFAULT 'WEB'           COMMENT '接入渠道: WEB/APP/MINI_PROGRAM',
    `status`          TINYINT UNSIGNED    DEFAULT 1               COMMENT '会话状态: 0-已结束 1-进行中 2-转人工',
    `start_time`      DATETIME            DEFAULT CURRENT_TIMESTAMP COMMENT '会话开始时间',
    `end_time`        DATETIME            DEFAULT NULL            COMMENT '会话结束时间',
    `satisfaction`    TINYINT UNSIGNED    DEFAULT NULL            COMMENT '满意度: 1-不满意 2-一般 3-满意',
    `create_time`     DATETIME            DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_session_id` (`session_id`),
    KEY `idx_user_id` (`user_id`, `create_time`),
    KEY `idx_status` (`status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客服会话表';

CREATE TABLE `bot_message` (
    `id`              BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `session_id`      VARCHAR(64)         NOT NULL                COMMENT '会话ID',
    `message_type`    TINYINT UNSIGNED    NOT NULL                COMMENT '消息类型: 1-用户提问 2-机器人回复 3-人工回复 4-系统提示',
    `content`         TEXT                NOT NULL                COMMENT '消息内容',
    `matched_knowledge_id` BIGINT UNSIGNED DEFAULT NULL         COMMENT '匹配到的知识ID',
    `confidence`      DECIMAL(5,4)        DEFAULT NULL            COMMENT '匹配置信度(0-1)',
    `is_helpful`      TINYINT UNSIGNED    DEFAULT NULL            COMMENT '是否有用: 0-否 1-是',
    `create_time`     DATETIME            DEFAULT CURRENT_TIMESTAMP COMMENT '消息时间',
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`, `create_time`),
    KEY `idx_message_type` (`message_type`),
    KEY `idx_matched_knowledge` (`matched_knowledge_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话消息表';

CREATE TABLE `bot_human_transfer` (
    `id`              BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT COMMENT '转接ID',
    `session_id`      VARCHAR(64)         NOT NULL                COMMENT '会话ID',
    `transfer_reason` VARCHAR(256)        DEFAULT NULL            COMMENT '转接原因: 无法回答/用户要求/连续匹配失败',
    `operator_id`     BIGINT UNSIGNED     DEFAULT NULL            COMMENT '接入人工客服ID',
    `operator_name`   VARCHAR(64)         DEFAULT NULL            COMMENT '接入人工客服姓名',
    `status`          TINYINT UNSIGNED    DEFAULT 0               COMMENT '状态: 0-待接入 1-处理中 2-已解决 3-已关闭',
    `create_time`     DATETIME            DEFAULT CURRENT_TIMESTAMP COMMENT '转接时间',
    `handle_time`     DATETIME            DEFAULT NULL            COMMENT '接入处理时间',
    `close_time`      DATETIME            DEFAULT NULL            COMMENT '关闭时间',
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_operator_id` (`operator_id`, `status`),
    KEY `idx_status` (`status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='人工转接记录表';

-- ============================================================
-- 9. 登录角色与测试账号初始化（客户 + 风控人员）
-- 默认密码：123456（BCrypt）
-- ============================================================

INSERT INTO `sys_role` (`role_code`, `role_name`, `role_desc`, `status`, `is_deleted`)
VALUES
    ('CUSTOMER', '客户', '系统客户角色', 1, 0),
    ('RISK_OFFICER', '风控人员', '风控审批与监控角色', 1, 0)
ON DUPLICATE KEY UPDATE
    `role_name` = VALUES(`role_name`),
    `role_desc` = VALUES(`role_desc`),
    `status` = VALUES(`status`),
    `is_deleted` = VALUES(`is_deleted`);

INSERT INTO `sys_user` (`username`, `password`, `real_name`, `phone`, `email`, `status`, `is_deleted`)
VALUES
    ('customer_demo', '$2b$10$gimzKrQ/9s15OREMjIAGkeF.Mbj6h1MmaX3DsHQsnmZmnxZIScET.', '客户示例账号', '13800000001', 'customer_demo@bank.com', 1, 0),
    ('risk_demo', '$2b$10$gimzKrQ/9s15OREMjIAGkeF.Mbj6h1MmaX3DsHQsnmZmnxZIScET.', '风控示例账号', '13800000002', 'risk_demo@bank.com', 1, 0)
ON DUPLICATE KEY UPDATE
    `password` = VALUES(`password`),
    `real_name` = VALUES(`real_name`),
    `phone` = VALUES(`phone`),
    `email` = VALUES(`email`),
    `status` = VALUES(`status`),
    `is_deleted` = VALUES(`is_deleted`);

USE bank_risk_control;

UPDATE sys_user
SET password = '$2b$10$gimzKrQ/9s15OREMjIAGkeF.Mbj6h1MmaX3DsHQsnmZmnxZIScET.'
WHERE username IN ('risk_demo', 'customer_demo', 'aaaa', 'bbbb');

INSERT INTO `sys_user_role` (`user_id`, `role_id`)
SELECT u.id, r.id
FROM `sys_user` u
JOIN `sys_role` r ON r.role_code = 'CUSTOMER'
WHERE u.username = 'customer_demo'
ON DUPLICATE KEY UPDATE `role_id` = VALUES(`role_id`);

INSERT INTO `sys_user_role` (`user_id`, `role_id`)
SELECT u.id, r.id
FROM `sys_user` u
JOIN `sys_role` r ON r.role_code = 'RISK_OFFICER'
WHERE u.username = 'risk_demo'
ON DUPLICATE KEY UPDATE `role_id` = VALUES(`role_id`);

-- ============================================================
-- 10. 客户信息模块增量字段（兼容已创建数据库）
-- ============================================================
ALTER TABLE `cust_customer`
    ADD COLUMN IF NOT EXISTS `user_id` BIGINT UNSIGNED NOT NULL COMMENT '关联sys_user.id' AFTER `id`,
    ADD COLUMN IF NOT EXISTS `credit_authorized` TINYINT UNSIGNED DEFAULT 0 COMMENT '征信授权: 0-未授权 1-已授权' AFTER `asset_amount`,
    ADD COLUMN IF NOT EXISTS `profile_completed` TINYINT UNSIGNED DEFAULT 0 COMMENT '资料是否完善: 0-否 1-是' AFTER `credit_authorized`;

ALTER TABLE `cust_customer`
    ADD UNIQUE INDEX `uk_user_id` (`user_id`);

-- ============================================================
-- 11. 账户模块增量表（兼容已创建数据库）
-- ============================================================
CREATE TABLE IF NOT EXISTS `acct_account` (
    `id`                BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT COMMENT '账户ID',
    `account_no`        VARCHAR(64)         NOT NULL                COMMENT '账户号',
    `customer_id`       BIGINT UNSIGNED     NOT NULL                COMMENT '客户ID',
    `account_name`      VARCHAR(64)         NOT NULL                COMMENT '账户名称',
    `account_type`      TINYINT UNSIGNED    NOT NULL                COMMENT '账户类型: 1-借记卡 2-信用卡 3-对公账户',
    `balance`           DECIMAL(16,2)       NOT NULL DEFAULT 0.00   COMMENT '可用余额',
    `currency`          VARCHAR(8)          NOT NULL DEFAULT 'CNY'  COMMENT '币种',
    `status`            TINYINT UNSIGNED    NOT NULL DEFAULT 1      COMMENT '状态: 0-禁用 1-可用',
    `create_time`       DATETIME            DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME            DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`        TINYINT UNSIGNED    DEFAULT 0               COMMENT '逻辑删除标志',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_account_no` (`account_no`),
    KEY `idx_customer_id` (`customer_id`, `status`),
    KEY `idx_status` (`status`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户账户表';

-- 初始化演示账户（可重复执行）
INSERT INTO `acct_account` (`account_no`, `customer_id`, `account_name`, `account_type`, `balance`, `currency`, `status`)
SELECT '6222020000000003', c.id, 'customer_demo借记卡', 1, 200000.00, 'CNY', 1
FROM `cust_customer` c
JOIN `sys_user` u ON u.id = c.user_id
WHERE u.username = 'aaaa'
  AND c.is_deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM `acct_account` a WHERE a.account_no = '6222020000000001'
  );

INSERT INTO `acct_account` (`account_no`, `customer_id`, `account_name`, `account_type`, `balance`, `currency`, `status`)
SELECT '6222020000000004', c.id, 'merchant_demo收款账户', 3, 500000.00, 'CNY', 1
FROM `cust_customer` c
JOIN `sys_user` u ON u.id = c.user_id
WHERE u.username = 'aaaa'
  AND c.is_deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM `acct_account` a WHERE a.account_no = '6222020000000002'
  );