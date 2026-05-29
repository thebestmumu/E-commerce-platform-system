package com.rabbiter.em.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rabbiter.em.entity.AiConversation;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 对话历史 Mapper 接口
 */
@Mapper
public interface AiConversationMapper extends BaseMapper<AiConversation> {
}
