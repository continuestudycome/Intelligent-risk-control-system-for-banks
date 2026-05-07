package com.system.service.impl;

import com.system.exception.ApiException;
import com.system.service.CustomerOcrService;
import com.system.vo.OcrExtractVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomerOcrServiceImpl implements CustomerOcrService {

    @Value("${ai.service.url:http://localhost:8000}")
    private String aiServiceUrl;

    @Override
    public OcrExtractVO extractProfileFields(MultipartFile file, String documentType) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(400, "请先上传证件图片");
        }
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String lower = filename.toLowerCase(Locale.ROOT);

        String docType = (documentType == null || documentType.isBlank()) ? inferDocType(lower) : documentType;
        if (!"ID_CARD".equalsIgnoreCase(docType) && !"BUSINESS_LICENSE".equalsIgnoreCase(docType)) {
            throw new ApiException(400, "documentType 仅支持 ID_CARD 或 BUSINESS_LICENSE");
        }

        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
            body.add("file", fileResource);
            body.add("documentType", docType.toUpperCase(Locale.ROOT));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            RestTemplate directRestTemplate = buildTimeoutRestTemplate();
            ResponseEntity<Map<String, Object>> response = directRestTemplate.exchange(
                    normalizeUrl(aiServiceUrl) + "/ocr/extract",
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<>() {
                    }
            );

            Map<String, Object> data = response.getBody();
            if (data == null) {
                throw new ApiException(500, "OCR服务返回为空");
            }

            return OcrExtractVO.builder()
                    .documentType(stringVal(data.get("documentType"), docType.toUpperCase(Locale.ROOT)))
                    .realName(stringVal(data.get("realName"), null))
                    .idCardNo(stringVal(data.get("idCardNo"), null))
                    .address(stringVal(data.get("address"), null))
                    .phone(stringVal(data.get("phone"), null))
                    .confidenceHint(stringVal(data.get("confidenceHint"), "中"))
                    .rawTextHint(stringVal(data.get("rawTextHint"), "OCR识别成功，请核对后保存"))
                    .build();
        } catch (IOException e) {
            throw new ApiException(500, "读取上传文件失败");
        } catch (ResourceAccessException e) {
            throw new ApiException(500, "OCR服务连接超时，请确认AI服务已启动");
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(500, "调用OCR服务失败: " + e.getMessage());
        }
    }

    private String inferDocType(String filenameLower) {
        if (filenameLower.contains("营业执照") || filenameLower.contains("license")) {
            return "BUSINESS_LICENSE";
        }
        return "ID_CARD";
    }

    private String stringVal(Object value, String defaultVal) {
        if (value == null) {
            return defaultVal;
        }
        String str = String.valueOf(value).trim();
        return str.isEmpty() ? defaultVal : str;
    }

    private String normalizeUrl(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:8000";
        }
        String trimmed = url.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private RestTemplate buildTimeoutRestTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(20000);
        return new RestTemplate(requestFactory);
    }
}
