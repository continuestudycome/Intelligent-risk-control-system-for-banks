package com.system.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.exception.ApiException;
import com.system.service.FraudMlClient;
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
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class FraudMlClientImpl implements FraudMlClient {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.service.url:http://localhost:8000}")
    private String aiServiceUrl;

    @Value("${fraud.ml.timeout-ms:8000}")
    private int timeoutMs;

    @Override
    public MlScoreResult scoreIsolationForest(List<Double> normalizedFeatures) {
        RestTemplate rt = buildRestTemplate();
        String url = normalizeBase(aiServiceUrl) + "/fraud/isolation-score";
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("features", normalizedFeatures);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            String raw = rt.postForObject(url, entity, String.class);
            JsonNode root = objectMapper.readTree(raw);
            double score = root.path("anomaly_score").asDouble(0);
            String ver = root.path("model_version").asText("if-v1");
            BigDecimal bd = BigDecimal.valueOf(score).setScale(4, RoundingMode.HALF_UP);
            return new MlScoreResult(bd, ver, true);
        } catch (Exception e) {
            log.warn("IsolationForest 调用失败，将仅使用规则引擎: {}", e.getMessage());
            return new MlScoreResult(BigDecimal.ZERO, "offline", false);
        }
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
