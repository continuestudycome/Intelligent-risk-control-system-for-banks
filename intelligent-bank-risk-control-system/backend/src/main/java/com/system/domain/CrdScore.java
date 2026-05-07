package com.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("crd_score")
public class CrdScore {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long customerId;
    private Integer score;
    private String riskLevel;
    private String modelVersion;
    /** JSON：归一化特征、模型指标快照等 */
    private String featureData;
    private Integer calcDurationMs;
    private LocalDateTime createTime;
}
