package com.system.service;

import com.system.vo.OcrExtractVO;
import org.springframework.web.multipart.MultipartFile;

public interface CustomerOcrService {
    OcrExtractVO extractProfileFields(MultipartFile file, String documentType);
}
