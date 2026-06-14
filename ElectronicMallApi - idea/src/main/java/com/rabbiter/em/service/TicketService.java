package com.rabbiter.em.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rabbiter.em.entity.Ticket;
import com.rabbiter.em.entity.TicketHistory;

import java.util.List;

/**
 * 工单服务接口
 */
public interface TicketService extends IService<Ticket> {

    /**
     * 创建工单
     * @param userId 用户 ID
     * @param category 工单分类
     * @param subject 工单主题
     * @param description 问题描述
     * @return 工单号
     */
    String createTicket(Long userId, String category, String subject, String description);

    /**
     * 分配工单给客服
     * @param ticketId 工单 ID
     * @param serviceId 客服 ID
     * @param operatorId 操作人 ID
     */
    void assignTicket(Long ticketId, Long serviceId, Long operatorId);

    /**
     * 解决工单
     * @param ticketId 工单 ID
     * @param operatorId 操作人 ID
     * @param remark 备注
     */
    void resolveTicket(Long ticketId, Long operatorId, String remark);

    /**
     * 关闭工单
     * @param ticketId 工单 ID
     * @param operatorId 操作人 ID
     */
    void closeTicket(Long ticketId, Long operatorId);

    /**
     * 评价工单
     * @param ticketId 工单 ID
     * @param score 满意度评分（1-5）
     * @param comment 评价内容
     */
    void rateTicket(Long ticketId, Integer score, String comment);

    /**
     * 查询用户的工单列表
     * @param userId 用户 ID
     * @param status 状态（可选）
     * @return 工单列表
     */
    List<Ticket> getUserTickets(Long userId, String status);

    /**
     * 查询工单详情
     * @param ticketId 工单 ID
     * @return 工单详情
     */
    Ticket getTicketDetail(Long ticketId);

    /**
     * 查询工单流转历史
     * @param ticketId 工单 ID
     * @return 流转历史列表
     */
    List<TicketHistory> getTicketHistory(Long ticketId);

    /**
     * 添加流转记录
     * @param ticketId 工单 ID
     * @param operatorId 操作人 ID
     * @param operatorRole 操作人角色
     * @param action 操作类型
     * @param remark 备注
     * @param oldValue 旧值
     * @param newValue 新值
     */
    void addHistory(Long ticketId, Long operatorId, String operatorRole, String action, String remark, String oldValue, String newValue);

    /**
     * 开始处理工单
     * @param ticketId 工单 ID
     * @param serviceId 客服 ID
     */
    void startProcessing(Long ticketId, Long serviceId);

    /**
     * 完成工单
     * @param ticketId 工单 ID
     * @param serviceId 客服 ID
     */
    void completeTicket(Long ticketId, Long serviceId);

    /**
     * 回复工单（记录到流转历史中）
     * @param ticketId 工单 ID
     * @param operatorId 操作人 ID（客服或用户）
     * @param operatorRole 操作人角色：service(客服)/customer(用户)
     * @param content 回复内容
     * @param isInternal 是否内部备注（仅客服可见）
     */
    void replyTicket(Long ticketId, Long operatorId, String operatorRole, String content, Boolean isInternal);
}
