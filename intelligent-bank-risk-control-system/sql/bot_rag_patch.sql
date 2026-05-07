-- 智能客服 RAG 扩展（在已有 bot_* 表上执行）
USE bank_risk_control;

-- 文档类型：FAQ / GUIDE / POLICY
ALTER TABLE `bot_knowledge`
    ADD COLUMN `doc_type` VARCHAR(16) NOT NULL DEFAULT 'FAQ' COMMENT 'FAQ-问答 GUIDE-办理指南 POLICY-政策说明' AFTER `category`;

ALTER TABLE `bot_session`
    ADD COLUMN `session_topic` VARCHAR(64) DEFAULT NULL COMMENT '会话主题/主导业务分类' AFTER `channel`;

ALTER TABLE `bot_message`
    ADD COLUMN `rag_sources` JSON DEFAULT NULL COMMENT 'RAG引用知识条目ID列表' AFTER `confidence`,
    ADD COLUMN `model_name` VARCHAR(64) DEFAULT NULL COMMENT '生成模型' AFTER `rag_sources`;

-- 常用分类代码（业务域）：LOAN/CREDIT_CARD/CREDIT/TRANSACTION/ACCOUNT/FRAUD/LIMIT/GENERAL

-- 示例知识（可重复执行时先删后插，或用手工维护）
INSERT INTO `bot_knowledge` (`category`, `doc_type`, `question`, `answer`, `similar_questions`, `keywords`, `status`, `is_deleted`)
VALUES
(
  'LOAN', 'FAQ', '如何申请贷款？',
  '1）登录后进入「贷款/信用卡申请」；2）选择贷款产品并填写金额、期限、用途；3）提交后系统与人工审批，您可在「申请进度」查看状态。需准备有效身份与收入证明（以页面提示为准）。',
  '["怎么借钱","贷款流程","申请个人贷款"]',
  '贷款,申请,流程', 1, 0
),
(
  'TRANSACTION', 'FAQ', '交易被拦截了怎么办？',
  '若交易因风控被拦截，资金不会划出。请①确认收款方与金额无误；②在设备上完成二次验证；③仍失败可检查是否异地大额或频繁小额试探触发了规则。您可联系智能客服或转人工，我们将协助核实。',
  '["钱转不出去","为什么拦截","交易失败"]',
  '拦截,交易,风险', 1, 0
),
(
  'ACCOUNT', 'GUIDE', '如何保护账户安全？',
  '请开启登录通知、勿向他人透露验证码、避免在公共网络进行大额操作；发现异常请立即修改密码并联系银行官方渠道。',
  '["盗刷","安全"]', '安全,账户', 1, 0
),
(
  'CREDIT', 'POLICY', '信用额度如何调整？',
  '系统会结合信用评分、用信与风险情况动态管理授信。您可在个人中心查看额度与调额记录；若需上调且符合政策，可发起申请并由风控复核。',
  '["额度多少","提额","降额"]', '额度,信用', 1, 0
);
