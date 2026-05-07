package com.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bot_session")
public class BotSession {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String sessionId;
    private Long userId;
    private Integer userType;
    private String channel;
    private String sessionTopic;
    private Integer status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer satisfaction;
    private LocalDateTime createTime;
}
