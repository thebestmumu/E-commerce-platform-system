package com.rabbiter.em.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客服工单实体类
 */
@Data
@TableName("ticket")
public class Ticket {

    /**
     * 主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 工单号（格式：TKT-YYYYMMDD-XXXX）
     */
    private String ticketNo;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 关联订单 ID（NULL 表示非订单问题）
     */
    private Long orderId;

    /**
     * 工单分类：technical(技术)/billing(账单)/product(商品)/complaint(投诉)/other(其他)
     */
    private String category;

    /**
     * 工单主题
     */
    private String subject;

    /**
     * 问题详细描述
     */
    private String description;

    /**
     * 状态：pending(待处理)/processing(处理中)/resolved(已解决)/closed(已关闭)
     */
    private String status;

    /**
     * 优先级：low(低)/normal(普通)/high(高)/urgent(紧急)
     */
    private String priority;

    /**
     * 分配给的客服 ID（NULL 表示未分配）
     */
    private Long assignedTo;

    /**
     * 创建人 ID
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 解决时间
     */
    private LocalDateTime resolvedAt;

    /**
     * 关闭时间
     */
    private LocalDateTime closedAt;

    /**
     * 满意度评分：1-5 星
     */
    private Integer satisfactionScore;

    /**
     * 满意度评价内容
     */
    private String satisfactionComment;

    /**
     * 排队位置
     */
    private Integer queuePosition;

    /**
     * 聊天室 ID
     */
    private String chatRoomId;

    /**
     * 聊天开始时间
     */
    private LocalDateTime chatStartedAt;

    /**
     * 聊天结束时间
     */
    private LocalDateTime chatEndedAt;

    /**
     * 结束方：user/service
     */
    private String endedBy;
}
