package com.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("frd_rule")
public class FrdRule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String ruleCode;
    private String ruleName;
    private Integer ruleType;
    /** JSON 参数字符串，与反欺诈引擎字段对齐 */
    private String ruleCondition;
    private String riskLevel;
    private Integer priority;
    private Integer hitThreshold;
    private Integer status;
    private Integer version;
    private LocalDateTime effectiveTime;
    private LocalDateTime expireTime;
    private String remark;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
    private Integer isDeleted;
}
