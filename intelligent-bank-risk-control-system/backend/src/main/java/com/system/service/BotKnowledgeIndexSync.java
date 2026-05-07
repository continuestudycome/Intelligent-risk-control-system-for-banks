package com.system.service;

import com.system.domain.BotKnowledge;

/**
 * 将数据库中的知识条目同步到 Python 侧 Chroma 向量索引。
 */
public interface BotKnowledgeIndexSync {

    /** 若索引中的知识 id 与当前库不一致，则全量重建。 */
    void ensureIndexedWithDb();

    /** 全量重建索引（管理端 CRUD 后调用）。 */
    void rebuildFullIndexFromDb();

    /** 单条知识切块嵌入写入 Chroma（增量）。 */
    void upsertKnowledge(BotKnowledge k);

    /** 从向量库移除指定知识 id。 */
    void removeKnowledgeFromIndex(Long id);
}
