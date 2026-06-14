package com.rabbiter.em.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rabbiter.em.entity.Ticket;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单 Mapper 接口
 */
@Mapper
public interface TicketMapper extends BaseMapper<Ticket> {
}
