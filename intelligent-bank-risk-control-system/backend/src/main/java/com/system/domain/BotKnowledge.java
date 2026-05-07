package com.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bot_knowledge")
public class BotKnowledge {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String category;
    private String docType;
    private String question;
    private String answer;
    /** JSON 数组字符串 */
    private String similarQuestions;
    private String keywords;
    private Integer hitCount;
    private Integer status;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
    private Integer isDeleted;
    /** MANUAL / FILE */
    private String sourceType;
    private String sourceFilename;
    private LocalDateTime vectorIndexedAt;
}
