package com.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("account")
@Schema(description = "用户账户信息")
public class Account {
    @TableId(type = IdType.AUTO)
    @Schema(description = "账户ID", example = "1")
    private Long id;

    @Schema(description = "用户ID", example = "1001")
    private Long userId;

    @Schema(description = "可用余额（元）", example = "100000.00")
    private BigDecimal balance;

    @Schema(description = "冻结金额（元）", example = "0.00")
    private BigDecimal frozen;

    @Version
    @Schema(description = "乐观锁版本号", example = "0")
    private Integer version;
}
