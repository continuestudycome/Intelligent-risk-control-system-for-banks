package com.system.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("bot_message")
public class BotMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String sessionId;
    /** 1用户 2机器人 3人工 4系统 */
    private Integer messageType;
    private String content;
    private Long matchedKnowledgeId;
    private BigDecimal confidence;
    /** JSON 数组：引用知识 ID（未执行 bot_rag_patch 时可无列，不参与 SQL 映射） */
    @TableField(exist = false)
    private String ragSources;
    @TableField(exist = false)
    private String modelName;
    private Integer isHelpful;
    private LocalDateTime createTime;
}
