package com.rabbiter.em.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rabbiter.em.entity.ChatMessage;
import com.rabbiter.em.mapper.ChatMessageMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天消息服务
 */
@Slf4j
@Service
public class ChatMessageService extends ServiceImpl<ChatMessageMapper, ChatMessage> {

    @Autowired
    private ChatQueueService chatQueueService;

    /**
     * 发送消息
     */
    @Transactional(rollbackFor = Exception.class)
    public ChatMessage sendMessage(Long ticketId, Long senderId, String senderRole, 
                                   Long receiverId, String content, String messageType) {
        ChatMessage message = new ChatMessage();
        message.setTicketId(ticketId);
        message.setSenderId(senderId);
        message.setSenderRole(senderRole);
        message.setReceiverId(receiverId);
        message.setContent(content);
        message.setMessageType(messageType != null ? messageType : "text");
        message.setIsRead(false);
        message.setCreatedAt(LocalDateTime.now());
        
        save(message);
        
        log.info("发送消息：ticketId={}, senderId={}, senderRole={}, receiverId={}, content={}", 
                ticketId, senderId, senderRole, receiverId, content);
        
        return message;
    }

    /**
     * 获取工单的所有聊天记录
     */
    public List<ChatMessage> getChatHistory(Long ticketId) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getTicketId, ticketId)
               .orderByAsc(ChatMessage::getCreatedAt);
        return list(wrapper);
    }

    /**
     * 标记消息为已读
     */
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long messageId) {
        ChatMessage message = getById(messageId);
        if (message != null && !message.getIsRead()) {
            message.setIsRead(true);
            message.setReadAt(LocalDateTime.now());
            updateById(message);
            log.info("消息已读：messageId={}", messageId);
        }
    }

    /**
     * 标记工单的所有消息为已读
     */
    @Transactional(rollbackFor = Exception.class)
    public void markAllAsRead(Long ticketId, Long receiverId) {
        List<ChatMessage> unreadMessages = lambdaQuery()
                .eq(ChatMessage::getTicketId, ticketId)
                .eq(ChatMessage::getReceiverId, receiverId)
                .eq(ChatMessage::getIsRead, false)
                .list();
        
        for (ChatMessage message : unreadMessages) {
            message.setIsRead(true);
            message.setReadAt(LocalDateTime.now());
        }
        updateBatchById(unreadMessages);
        
        log.info("批量标记已读：ticketId={}, receiverId={}, count={}", ticketId, receiverId, unreadMessages.size());
    }

    /**
     * 获取未读消息数量
     */
    public Long getUnreadCount(Long ticketId, Long receiverId) {
        return lambdaQuery()
                .eq(ChatMessage::getTicketId, ticketId)
                .eq(ChatMessage::getReceiverId, receiverId)
                .eq(ChatMessage::getIsRead, false)
                .count();
    }
}
