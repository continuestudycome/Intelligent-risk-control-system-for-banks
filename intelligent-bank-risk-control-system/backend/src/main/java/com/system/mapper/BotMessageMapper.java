package com.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.system.domain.BotMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BotMessageMapper extends BaseMapper<BotMessage> {
}
