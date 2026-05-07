package com.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CustomerBotChatDTO {
    @NotBlank(message = "问题不能为空")
    private String content;
    /** 若指定，则优先只检索该业务分类下的知识；为空则全库检索 */
    private String scopeCategory;
}
