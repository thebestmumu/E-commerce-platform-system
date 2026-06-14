package com.rabbiter.em.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工单流转记录实体类
 */
@Data
@TableName("ticket_history")
public class TicketHistory {

    /**
     * 主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 工单 ID
     */
    private Long ticketId;

    /**
     * 操作人 ID（用户或客服）
     */
    private Long operatorId;

    /**
     * 操作人角色：customer(用户)/service(客服)/system(系统)
     */
    private String operatorRole;

    /**
     * 操作类型：create(创建)/assign(分配)/transfer(转交)/reply(回复)/resolve(解决)/close(关闭)/reopen(重新打开)
     */
    private String action;

    /**
     * 操作备注
     */
    private String remark;

    /**
     * 旧值（如原状态）
     */
    private String oldValue;

    /**
     * 新值（如新状态）
     */
    private String newValue;

    /**
     * 操作时间
     */
    private LocalDateTime createdAt;
}
