package com.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bot_human_transfer")
public class BotHumanTransfer {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String sessionId;
    private String transferReason;
    private Long operatorId;
    private String operatorName;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime handleTime;
    private LocalDateTime closeTime;
}
