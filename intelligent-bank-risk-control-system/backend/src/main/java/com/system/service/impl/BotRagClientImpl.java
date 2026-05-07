package com.system.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.exception.ApiException;
import com.system.service.BotRagClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class BotRagClientImpl implements BotRagClient {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.service.url:http://localhost:8000}")
    private String aiServiceUrl;

    @Value("${bot.rag.timeout-ms:180000}")
    private int timeoutMs;

    @Override
    public JsonNode ragStats() {
        RestTemplate rt = buildRestTemplate();
        String url = normalizeBase(aiServiceUrl) + "/rag/stats";
        try {
            String raw = rt.getForObject(url, String.class);
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            log.debug("RAG stats 不可用: {}", e.getMessage());
            try {
                return objectMapper.readTree("{\"chunk_count\":0,\"knowledge_ids\":[]}");
            } catch (Exception ex) {
                throw new ApiException(502, "RAG 服务异常");
            }
        }
    }

    @Override
    public void ragRebuild(JsonNode requestBody) {
        RestTemplate rt = buildRestTemplate();
        String url = normalizeBase(aiServiceUrl) + "/rag/index/rebuild";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity;
        try {
            entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
        } catch (Exception e) {
            throw new ApiException(500, "构建索引请求失败");
        }
        rt.postForObject(url, entity, String.class);
    }

    @Override
    public void ragUpsert(JsonNode requestBody) {
        RestTemplate rt = buildRestTemplate();
        String url = normalizeBase(aiServiceUrl) + "/rag/index/upsert";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity;
        try {
            entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
        } catch (Exception e) {
            throw new ApiException(500, "构建索引请求失败");
        }
        rt.postForObject(url, entity, String.class);
    }

    @Override
    public void ragDeleteKnowledge(long knowledgeId) {
        RestTemplate rt = buildRestTemplate();
        String url = normalizeBase(aiServiceUrl) + "/rag/index/knowledge/" + knowledgeId;
        rt.delete(url);
    }

    @Override
    public JsonNode ragChat(JsonNode requestBody) {
        RestTemplate rt = buildRestTemplate();
        String url = normalizeBase(aiServiceUrl) + "/chat/rag";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
            String raw = rt.postForObject(url, entity, String.class);
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            log.warn("RAG 服务调用失败: {}", e.getMessage());
            try {
                return objectMapper.readTree(
                        "{\"answer\":\"智能客服引擎暂时不可用，请稍后重试或使用「转人工」功能。\""
                                + ",\"citations\":[],\"mode\":\"error\",\"model\":\"offline\"}");
            } catch (Exception ex) {
                throw new ApiException(502, "RAG 服务异常");
            }
        }
    }

    private RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(Math.min(timeoutMs, 10000));
        f.setReadTimeout(timeoutMs);
        return new RestTemplate(f);
    }

    private static String normalizeBase(String url) {
        if (url == null || url.isBlank()) {
            throw new ApiException(500, "AI 服务地址未配置");
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
