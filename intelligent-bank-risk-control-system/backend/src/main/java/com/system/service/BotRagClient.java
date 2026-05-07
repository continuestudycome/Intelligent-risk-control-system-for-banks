package com.system.service;

import com.fasterxml.jackson.databind.JsonNode;

public interface BotRagClient {

    /** GET /rag/stats — chunk_count、knowledge_ids */
    JsonNode ragStats();

    /** POST /rag/index/rebuild — body 含 knowledge_items */
    void ragRebuild(JsonNode requestBody);

    /** POST /rag/index/upsert — 切块写入 Chroma（增量） */
    void ragUpsert(JsonNode requestBody);

    /** DELETE /rag/index/knowledge/{id} — 移除向量 */
    void ragDeleteKnowledge(long knowledgeId);

    /** POST /chat/rag — question、history、category_hint、category_scope */
    JsonNode ragChat(JsonNode requestBody);
}
