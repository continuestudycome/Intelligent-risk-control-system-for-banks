package com.system.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.exception.ApiException;
import com.system.service.CreditLimitAiClient;
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
import java.util.Map;

@Slf4j
@Service
public class CreditLimitAiClientImpl implements CreditLimitAiClient {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.service.url:http://localhost:8000}")
    private String aiServiceUrl;

    @Value("${credit.limit.timeout-ms:8000}")
    private int timeoutMs;

    @Override
    public LimitRecommendResult recommend(
            String riskLevel,
            boolean blacklist,
            long txnCount90d,
            BigDecimal assetAmountWan,
            BigDecimal minTotal,
            BigDecimal maxTotal) {
        RestTemplate rt = buildRestTemplate();
        String url = normalizeBase(aiServiceUrl) + "/credit/limit-recommend";
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("risk_level", riskLevel != null ? riskLevel : "C");
            body.put("blacklist", blacklist);
            body.put("txn_count_90d", txnCount90d);
            if (assetAmountWan != null) {
                body.put("asset_amount_wan", assetAmountWan.doubleValue());
            } else {
                body.put("asset_amount_wan", null);
            }
            body.put("min_total", minTotal != null ? minTotal.doubleValue() : 10000d);
            body.put("max_total", maxTotal != null ? maxTotal.doubleValue() : 2_000_000d);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            String raw = rt.postForObject(url, entity, String.class);
            JsonNode root = objectMapper.readTree(raw);
            BigDecimal total = BigDecimal.valueOf(root.path("recommended_total").asDouble(0))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal single = BigDecimal.valueOf(root.path("single_limit").asDouble(0))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal daily = BigDecimal.valueOf(root.path("daily_limit").asDouble(0))
                    .setScale(2, RoundingMode.HALF_UP);
            String ver = root.path("model_version").asText("limit-rule-v1");
            return new LimitRecommendResult(total, single, daily, ver, true);
        } catch (Exception e) {
            log.warn("授信额度 AI 调用失败，将使用本地规则兜底: {}", e.getMessage());
            return LimitRecommendResult.offline();
        }
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
