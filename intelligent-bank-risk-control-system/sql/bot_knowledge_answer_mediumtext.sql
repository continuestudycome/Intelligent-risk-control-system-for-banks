-- 支持较长文档正文（上传 PDF/Word 等）
ALTER TABLE `bot_knowledge`
    MODIFY COLUMN `answer` MEDIUMTEXT NOT NULL COMMENT '标准答案/文档正文';
