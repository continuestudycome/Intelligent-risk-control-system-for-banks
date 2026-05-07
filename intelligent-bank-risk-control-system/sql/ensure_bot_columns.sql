-- 智能客服最小 DDL：执行后实体字段可与数据库对齐（与 bot_rag_patch.sql 中 ALTER 一致）
-- 在 bank_risk_control 库执行；若提示 Duplicate column 说明已加过，可忽略该行

USE bank_risk_control;

ALTER TABLE `bot_knowledge`
    ADD COLUMN `doc_type` VARCHAR(16) NOT NULL DEFAULT 'FAQ' COMMENT 'FAQ-问答 GUIDE-办理指南 POLICY-政策说明' AFTER `category`;

ALTER TABLE `bot_session`
    ADD COLUMN `session_topic` VARCHAR(64) DEFAULT NULL COMMENT '会话主题/主导业务分类' AFTER `channel`;

ALTER TABLE `bot_message`
    ADD COLUMN `rag_sources` JSON DEFAULT NULL COMMENT 'RAG引用知识条目ID列表' AFTER `confidence`,
    ADD COLUMN `model_name` VARCHAR(64) DEFAULT NULL COMMENT '生成模型' AFTER `rag_sources`;
