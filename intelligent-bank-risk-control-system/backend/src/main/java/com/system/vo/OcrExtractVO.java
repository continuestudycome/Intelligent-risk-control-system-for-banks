package com.system.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OcrExtractVO {
    private String documentType;
    private String realName;
    private String idCardNo;
    private String address;
    private String phone;
    private String confidenceHint;
    private String rawTextHint;
}
