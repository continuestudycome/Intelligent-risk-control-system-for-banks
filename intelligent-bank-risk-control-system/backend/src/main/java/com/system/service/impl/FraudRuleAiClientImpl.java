package com.system.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.exception.ApiException;
import com.system.service.FraudRuleAiClient;
import com.system.vo.FraudRuleValidateResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class FraudRuleAiClientImpl implements FraudRuleAiClient {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.service.url:http://localhost:8000}")
    private String aiServiceUrl;

    @Value("${fraud.ml.timeout-ms:8000}")
    private int timeoutMs;

    @Override
    public FraudRuleValidateResultVO validateBatch(List<Map<String, Object>> rules) {
        try {
            RestTemplate rt = buildRestTemplate();
            String url = normalizeBase(aiServiceUrl) + "/rule/fraud/validate";
            Map<String, Object> body = new HashMap<>();
            body.put("rules", rules);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            String raw = rt.postForObject(url, entity, String.class);
            JsonNode root = objectMapper.readTree(raw);
            boolean ok = root.path("ok").asBoolean(true);
            List<String> errors = readStringList(root.get("errors"));
            List<String> hints = readStringList(root.get("hints"));
            return FraudRuleValidateResultVO.builder()
                    .ok(ok)
                    .errors(errors)
                    .hints(hints)
                    .build();
        } catch (Exception e) {
            log.warn("规则智能校验服务不可用: {}", e.getMessage());
            return FraudRuleValidateResultVO.builder()
                    .ok(true)
                    .errors(List.of())
                    .hints(List.of("无法连接智能服务，已跳过机器校验。请检查 ai_services 是否启动。"))
                    .build();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> simulate(Map<String, Object> thresholds, List<Map<String, Object>> samples) {
        try {
            RestTemplate rt = buildRestTemplate();
            String url = normalizeBase(aiServiceUrl) + "/rule/fraud/simulate";
            Map<String, Object> body = new HashMap<>();
            body.put("thresholds", thresholds);
            body.put("samples", samples);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            String raw = rt.postForObject(url, entity, String.class);
            JsonNode root = objectMapper.readTree(raw);
            JsonNode results = root.get("results");
            if (results == null || !results.isArray()) {
                return List.of();
            }
            List<Map<String, Object>> out = new ArrayList<>();
            for (JsonNode n : results) {
                out.add(objectMapper.convertValue(n, Map.class));
            }
            return out;
        } catch (Exception e) {
            log.warn("规则试算服务不可用: {}", e.getMessage());
            throw new ApiException(502, "智能试算服务暂不可用: " + e.getMessage());
        }
    }

    private static List<String> readStringList(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<String> r = new ArrayList<>();
        for (JsonNode n : node) {
            r.add(n.asText());
        }
        return r;
    }

    private RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(Math.min(timeoutMs, 3000));
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
