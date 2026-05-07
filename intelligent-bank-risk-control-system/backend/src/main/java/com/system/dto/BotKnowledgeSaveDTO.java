package com.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BotKnowledgeSaveDTO {
    @NotBlank
    private String category;
    @NotBlank
    private String docType;
    @NotBlank
    private String question;
    @NotBlank
    private String answer;
    /** JSON 数组字符串，如 ["问法1","问法2"] */
    private String similarQuestions;
    private String keywords;
    private Integer status;
    /** MANUAL / FILE */
    private String sourceType;
    private String sourceFilename;
}
