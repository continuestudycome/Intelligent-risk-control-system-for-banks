-- 智能客服 v2：知识来源与向量同步时间（在 bank_risk_control 执行）
USE bank_risk_control;

-- 若已执行过 bot_knowledge_answer_mediumtext 可跳过 MODIFY
ALTER TABLE `bot_knowledge`
    MODIFY COLUMN `answer` MEDIUMTEXT NOT NULL COMMENT '标准答案/文档全文';

ALTER TABLE `bot_knowledge`
    ADD COLUMN `source_type` VARCHAR(32) NOT NULL DEFAULT 'MANUAL' COMMENT 'MANUAL-手工录入 FILE-上传文档' AFTER `is_deleted`,
    ADD COLUMN `source_filename` VARCHAR(512) DEFAULT NULL COMMENT '上传原始文件名' AFTER `source_type`,
    ADD COLUMN `vector_indexed_at` DATETIME DEFAULT NULL COMMENT '向量索引最近同步时间' AFTER `source_filename`;
