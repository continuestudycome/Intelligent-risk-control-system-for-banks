package com.system.controller;

import com.system.common.Result;
import com.system.dto.CustomerProfileSaveDTO;
import com.system.service.CustomerOcrService;
import com.system.service.CustomerProfileService;
import com.system.vo.CustomerProfileVO;
import com.system.vo.OcrExtractVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/customer/profile")
@RequiredArgsConstructor
public class CustomerProfileController {

    private final CustomerProfileService customerProfileService;
    private final CustomerOcrService customerOcrService;

    @GetMapping("/me")
    public Result<CustomerProfileVO> getMyProfile() {
        return Result.success(customerProfileService.getMyProfile());
    }

    @PutMapping("/me")
    public Result<CustomerProfileVO> saveMyProfile(@Valid @RequestBody CustomerProfileSaveDTO dto) {
        return Result.success(customerProfileService.saveMyProfile(dto));
    }

    @PostMapping("/ocr")
    public Result<OcrExtractVO> extractByOcr(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "documentType", required = false) String documentType) {
        return Result.success(customerOcrService.extractProfileFields(file, documentType));
    }
}
