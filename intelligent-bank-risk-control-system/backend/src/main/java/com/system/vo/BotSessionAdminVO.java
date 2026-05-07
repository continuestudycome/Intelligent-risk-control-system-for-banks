package com.system.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BotSessionAdminVO {
    private Long id;
    private String sessionId;
    private Long userId;
    private String channel;
    private String sessionTopic;
    private Integer status;
    private Integer satisfaction;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createTime;
    private Long messageCount;
}
