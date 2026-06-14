package com.rabbiter.em.dto;

import lombok.Data;

/**
 * 客服工单统计 DTO
 */
@Data
public class TicketStatsDTO {

    /**
     * 待处理工单数
     */
    private Integer pendingCount;

    /**
     * 处理中工单数
     */
    private Integer processingCount;

    /**
     * 已解决工单数
     */
    private Integer resolvedCount;

    /**
     * 总工单数
     */
    private Integer totalCount;

    /**
     * 平均响应时间（分钟）
     */
    private Double avgResponseTime;

    /**
     * 平均解决时间（小时）
     */
    private Double avgResolutionTime;

    /**
     * 满意度评分（1-5）
     */
    private Double avgSatisfactionScore;
}
