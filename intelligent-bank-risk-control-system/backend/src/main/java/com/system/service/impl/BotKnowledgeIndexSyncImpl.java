package com.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.system.domain.BotKnowledge;
import com.system.mapper.BotKnowledgeMapper;
import com.system.service.BotKnowledgeIndexSync;
import com.system.service.BotRagClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotKnowledgeIndexSyncImpl implements BotKnowledgeIndexSync {

    private final BotKnowledgeMapper botKnowledgeMapper;
    private final BotRagClient botRagClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void ensureIndexedWithDb() {
        List<BotKnowledge> all = loadAllActive();
        List<Long> expected = all.stream().map(BotKnowledge::getId).sorted().toList();
        JsonNode stats = botRagClient.ragStats();
        if (stats == null || stats.isMissingNode()) {
            rebuildPayload(all);
            return;
        }
        JsonNode remoteArr = stats.path("knowledge_ids");
        if (!remoteArr.isArray()) {
            rebuildPayload(all);
            return;
        }
        List<Long> remote = new ArrayList<>();
        for (JsonNode n : remoteArr) {
            remote.add(n.asLong(0));
        }
        remote.sort(Long::compareTo);
        if (!expected.equals(remote)) {
            rebuildPayload(all);
        }
    }

    @Override
    public void rebuildFullIndexFromDb() {
        rebuildPayload(loadAllActive());
    }

    @Override
    public void upsertKnowledge(BotKnowledge k) {
        if (k == null || k.getId() == null) {
            return;
        }
        ObjectNode body = objectMapper.createObjectNode();
        ArrayNode arr = objectMapper.createArrayNode();
        arr.add(toKnowledgeJson(k));
        body.set("knowledge_items", arr);
        try {
            botRagClient.ragUpsert(body);
        } catch (Exception e) {
            log.warn("RAG 向量 upsert 失败: {}", e.getMessage());
            return;
        }
        try {
            botKnowledgeMapper.update(
                    null,
                    new LambdaUpdateWrapper<BotKnowledge>()
                            .set(BotKnowledge::getVectorIndexedAt, LocalDateTime.now())
                            .eq(BotKnowledge::getId, k.getId()));
        } catch (Exception e) {
            log.debug("未写入 vector_indexed_at（列未迁移时可忽略）: {}", e.getMessage());
        }
    }

    @Override
    public void removeKnowledgeFromIndex(Long id) {
        if (id == null) {
            return;
        }
        try {
            botRagClient.ragDeleteKnowledge(id);
        } catch (Exception e) {
            log.warn("RAG 向量删除失败: {}", e.getMessage());
        }
    }

    private List<BotKnowledge> loadAllActive() {
        return botKnowledgeMapper.selectList(
                new LambdaQueryWrapper<BotKnowledge>()
                        .eq(BotKnowledge::getStatus, 1)
                        .eq(BotKnowledge::getIsDeleted, 0));
    }

    private void rebuildPayload(List<BotKnowledge> all) {
        ArrayNode arr = objectMapper.createArrayNode();
        for (BotKnowledge k : all) {
            arr.add(toKnowledgeJson(k));
        }
        ObjectNode body = objectMapper.createObjectNode();
        body.set("knowledge_items", arr);
        try {
            botRagClient.ragRebuild(body);
        } catch (Exception e) {
            log.warn("RAG 索引重建失败: {}", e.getMessage());
        }
    }

    private ObjectNode toKnowledgeJson(BotKnowledge k) {
        ObjectNode n = objectMapper.createObjectNode();
        n.put("id", k.getId());
        n.put("category", k.getCategory() != null ? k.getCategory() : "");
        n.put("docType", k.getDocType() != null ? k.getDocType() : "FAQ");
        n.put("question", k.getQuestion());
        n.put("answer", k.getAnswer());
        if (k.getKeywords() != null) {
            n.put("keywords", k.getKeywords());
        }
        if (k.getSimilarQuestions() != null && !k.getSimilarQuestions().isBlank()) {
            try {
                n.set("similarQuestions", objectMapper.readTree(k.getSimilarQuestions()));
            } catch (Exception e) {
                n.put("similarQuestions", k.getSimilarQuestions());
            }
        }
        return n;
    }
}
