package com.system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BotMessageFeedbackDTO {
    /** true 有用 false 无用 */
    @NotNull
    private Boolean helpful;
}
