package com.system.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.exception.ApiException;
import com.system.service.CreditMlClient;
import com.system.service.CreditMlClient.CreditMlResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class CreditMlClientImpl implements CreditMlClient {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.service.url:http://localhost:8000}")
    private String aiServiceUrl;

    @Value("${credit.ml.timeout-ms:8000}")
    private int timeoutMs;

    @Override
    public CreditMlResult score(List<Double> normalizedFeatures) {
        RestTemplate rt = buildRestTemplate();
        String url = normalizeBase(aiServiceUrl) + "/credit/score";
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("features", normalizedFeatures);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            String raw = rt.postForObject(url, entity, String.class);
            JsonNode root = objectMapper.readTree(raw);
            int score = root.path("credit_score").asInt(0);
            String risk = root.path("risk_level").asText("C");
            BigDecimal p = BigDecimal.valueOf(root.path("good_probability").asDouble(0))
                    .setScale(6, RoundingMode.HALF_UP);
            String ver = root.path("model_version").asText("credit-v1");
            Map<String, Object> metrics = parseMetrics(root.path("metrics"));
            return new CreditMlResult(score, risk, p, ver, metrics, true);
        } catch (Exception e) {
            log.warn("信用评分 AI 调用失败，将使用本地兜底模型: {}", e.getMessage());
            return CreditMlResult.offline();
        }
    }

    private Map<String, Object> parseMetrics(JsonNode node) {
        Map<String, Object> m = new HashMap<>();
        if (node == null || !node.isObject()) {
            return m;
        }
        Iterator<String> it = node.fieldNames();
        while (it.hasNext()) {
            String k = it.next();
            JsonNode v = node.get(k);
            if (v.isNumber()) {
                m.put(k, v.doubleValue());
            } else if (v.isTextual()) {
                m.put(k, v.asText());
            } else {
                m.put(k, v.toString());
            }
        }
        return m;
    }

    private RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(Math.min(timeoutMs, 5000));
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
