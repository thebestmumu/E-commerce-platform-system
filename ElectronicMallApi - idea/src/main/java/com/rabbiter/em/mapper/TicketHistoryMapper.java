package com.rabbiter.em.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rabbiter.em.entity.TicketHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单流转记录 Mapper 接口
 */
@Mapper
public interface TicketHistoryMapper extends BaseMapper<TicketHistory> {
}
