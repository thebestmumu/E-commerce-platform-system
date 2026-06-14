package com.rabbiter.em.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rabbiter.em.entity.Ticket;
import com.rabbiter.em.entity.TicketHistory;
import com.rabbiter.em.exception.ServiceException;
import com.rabbiter.em.mapper.TicketMapper;
import com.rabbiter.em.mapper.TicketHistoryMapper;
import com.rabbiter.em.service.TicketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

/**
 * 工单服务实现类
 */
@Slf4j
@Service
public class TicketServiceImpl extends ServiceImpl<TicketMapper, Ticket> implements TicketService {

    @Resource
    private TicketHistoryMapper ticketHistoryMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createTicket(Long userId, String category, String subject, String description) {
        log.info("创建工单 - userId: {}, category: {}, subject: {}", userId, category, subject);

        // 检查用户是否有待处理的同类工单（防止重复创建）
        if (userId != null && userId > 0) {
            List<Ticket> existingTickets = lambdaQuery()
                    .eq(Ticket::getUserId, userId)
                    .eq(Ticket::getCategory, category)
                    .eq(Ticket::getStatus, "pending")
                    .orderByDesc(Ticket::getCreatedAt)
                    .list();
            
            if (!existingTickets.isEmpty()) {
                Ticket existingTicket = existingTickets.get(0);
                log.info("用户已有待处理工单，不再重复创建 - ticketNo: {}", existingTicket.getTicketNo());
                // 返回已有工单号，不创建新工单
                return existingTicket.getTicketNo();
            }
        }

        // 生成工单号：TKT-YYYYMMDD-XXXX
        String dateStr = LocalDateTime.now().format(DATE_FORMATTER);
        String randomSuffix = String.format("%04d", new Random().nextInt(10000));
        String ticketNo = "TKT-" + dateStr + "-" + randomSuffix;

        // 创建工单
        Ticket ticket = new Ticket();
        ticket.setTicketNo(ticketNo);
        ticket.setUserId(userId);
        ticket.setCategory(category);
        ticket.setSubject(subject);
        ticket.setDescription(description);
        ticket.setStatus("pending");
        ticket.setPriority("normal");
        ticket.setCreatedBy(userId);

        save(ticket);

        // 添加流转记录
        addHistory(ticket.getId(), userId, "customer", "create", "工单创建成功", null, "pending");

        log.info("工单创建成功 - ticketNo: {}", ticketNo);
        return ticketNo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignTicket(Long ticketId, Long serviceId, Long operatorId) {
        log.info("分配工单 - ticketId: {}, serviceId: {}", ticketId, serviceId);

        Ticket ticket = getById(ticketId);
        if (ticket == null) {
            throw new ServiceException("404", "工单不存在");
        }

        String oldStatus = ticket.getStatus();
        ticket.setAssignedTo(serviceId);
        ticket.setStatus("processing");
        updateById(ticket);

        // 添加流转记录
        addHistory(ticketId, operatorId, "system", "assign", "分配给客服处理", oldStatus, "processing");

        log.info("工单分配成功 - ticketId: {}, assignedTo: {}", ticketId, serviceId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resolveTicket(Long ticketId, Long operatorId, String remark) {
        log.info("解决工单 - ticketId: {}, remark: {}", ticketId, remark);

        Ticket ticket = getById(ticketId);
        if (ticket == null) {
            throw new ServiceException("404", "工单不存在");
        }

        String oldStatus = ticket.getStatus();
        ticket.setStatus("resolved");
        ticket.setResolvedAt(LocalDateTime.now());
        updateById(ticket);

        // 添加流转记录
        addHistory(ticketId, operatorId, "service", "resolve", remark, oldStatus, "resolved");

        log.info("工单解决成功 - ticketId: {}", ticketId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeTicket(Long ticketId, Long operatorId) {
        log.info("关闭工单 - ticketId: {}", ticketId);

        Ticket ticket = getById(ticketId);
        if (ticket == null) {
            throw new ServiceException("404", "工单不存在");
        }

        String oldStatus = ticket.getStatus();
        ticket.setStatus("closed");
        ticket.setClosedAt(LocalDateTime.now());
        updateById(ticket);

        // 添加流转记录
        addHistory(ticketId, operatorId, operatorId.equals(ticket.getUserId()) ? "customer" : "service", 
                   "close", "工单已关闭", oldStatus, "closed");

        log.info("工单关闭成功 - ticketId: {}", ticketId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rateTicket(Long ticketId, Integer score, String comment) {
        log.info("评价工单 - ticketId: {}, score: {}", ticketId, score);

        if (score < 1 || score > 5) {
            throw new ServiceException("400", "满意度评分必须在 1-5 之间");
        }

        Ticket ticket = getById(ticketId);
        if (ticket == null) {
            throw new ServiceException("404", "工单不存在");
        }

        ticket.setSatisfactionScore(score);
        ticket.setSatisfactionComment(comment);
        updateById(ticket);

        log.info("工单评价成功 - ticketId: {}, score: {}", ticketId, score);
    }

    @Override
    public List<Ticket> getUserTickets(Long userId, String status) {
        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<>();
        // 如果指定了 userId，则查询该用户的工单
        if (userId != null) {
            wrapper.eq(Ticket::getUserId, userId);
        }
        // 如果指定了 status，则按状态筛选
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Ticket::getStatus, status);
        }
        wrapper.orderByDesc(Ticket::getCreatedAt);
        return list(wrapper);
    }

    @Override
    public Ticket getTicketDetail(Long ticketId) {
        return getById(ticketId);
    }

    @Override
    public List<TicketHistory> getTicketHistory(Long ticketId) {
        LambdaQueryWrapper<TicketHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TicketHistory::getTicketId, ticketId);
        wrapper.orderByAsc(TicketHistory::getCreatedAt);
        return ticketHistoryMapper.selectList(wrapper);
    }

    @Override
    public void addHistory(Long ticketId, Long operatorId, String operatorRole, String action, 
                          String remark, String oldValue, String newValue) {
        TicketHistory history = new TicketHistory();
        history.setTicketId(ticketId);
        history.setOperatorId(operatorId);
        history.setOperatorRole(operatorRole);
        history.setAction(action);
        history.setRemark(remark);
        history.setOldValue(oldValue);
        history.setNewValue(newValue);
        ticketHistoryMapper.insert(history);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startProcessing(Long ticketId, Long serviceId) {
        log.info("开始处理工单 - ticketId: {}, serviceId: {}", ticketId, serviceId);

        Ticket ticket = getById(ticketId);
        if (ticket == null) {
            throw new ServiceException("404", "工单不存在");
        }

        String oldStatus = ticket.getStatus();
        ticket.setStatus("processing");
        updateById(ticket);

        // 添加流转记录
        addHistory(ticketId, serviceId, "service", "start_processing", "客服开始处理", oldStatus, "processing");

        log.info("工单开始处理 - ticketId: {}", ticketId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeTicket(Long ticketId, Long serviceId) {
        log.info("完成工单 - ticketId: {}, serviceId: {}", ticketId, serviceId);

        Ticket ticket = getById(ticketId);
        if (ticket == null) {
            throw new ServiceException("404", "工单不存在");
        }

        String oldStatus = ticket.getStatus();
        ticket.setStatus("completed");
        ticket.setResolvedAt(LocalDateTime.now());
        updateById(ticket);

        // 添加流转记录
        addHistory(ticketId, serviceId, "service", "complete", "工单已完成", oldStatus, "completed");

        log.info("工单完成 - ticketId: {}", ticketId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replyTicket(Long ticketId, Long operatorId, String operatorRole, String content, Boolean isInternal) {
        log.info("回复工单 - ticketId: {}, operatorRole: {}, isInternal: {}", ticketId, operatorRole, isInternal);

        Ticket ticket = getById(ticketId);
        if (ticket == null) {
            throw new ServiceException("404", "工单不存在");
        }

        // 构建回复内容标记，区分内部备注和公开回复
        String replyContent = (isInternal != null && isInternal) ? "[内部备注] " + content : content;

        // 添加流转记录（使用 reply 操作类型）
        addHistory(ticketId, operatorId, operatorRole, "reply", replyContent, null, null);

        // 如果是客服回复且工单处于待处理状态，自动转为处理中
        if ("service".equals(operatorRole) && "pending".equals(ticket.getStatus())) {
            ticket.setStatus("processing");
            updateById(ticket);
            addHistory(ticketId, operatorId, "system", "auto_assign", "客服回复后自动转为处理中", "pending", "processing");
        }

        log.info("工单回复成功 - ticketId: {}", ticketId);
    }
}
