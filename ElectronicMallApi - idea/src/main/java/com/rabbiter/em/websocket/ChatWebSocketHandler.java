package com.rabbiter.em.websocket;

import cn.hutool.json.JSONUtil;
import com.rabbiter.em.entity.ChatMessage;
import com.rabbiter.em.entity.Ticket;
import com.rabbiter.em.service.ChatMessageService;
import com.rabbiter.em.service.ChatQueueService;
import com.rabbiter.em.service.TicketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天 WebSocket 处理器
 * 处理用户和客服的实时聊天
 */
@Slf4j
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private ChatQueueService chatQueueService;

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private TicketService ticketService;

    // 存储所有活跃的 WebSocket 会话
    // key: sessionId, value: WebSocketSession
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // 存储用户会话映射
    // key: userId, value: sessionId
    private static final Map<Long, String> userSessionMap = new ConcurrentHashMap<>();

    // 存储客服会话映射
    // key: serviceId, value: sessionId
    private static final Map<Long, String> serviceSessionMap = new ConcurrentHashMap<>();

    // 存储聊天室映射
    // key: roomId, value: Set<sessionId>
    private static final Map<String, java.util.Set<String>> chatRoomSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Map<String, Object> attributes = session.getAttributes();
        Long userId = (Long) attributes.get("userId");
        String role = (String) attributes.get("role");
        
        sessions.put(session.getId(), session);
        
        if ("user".equals(role)) {
            userSessionMap.put(userId, session.getId());
            log.info("用户连接 WebSocket：userId={}, sessionId={}", userId, session.getId());
            
            // 用户上线，加入排队队列
            handleUserJoinQueue(userId, session);
        } else if ("service".equals(role)) {
            serviceSessionMap.put(userId, session.getId());
            log.info("客服连接 WebSocket：serviceId={}, sessionId={}", userId, session.getId());
            
            // 客服上线
            chatQueueService.serviceOnline(userId);
            
            // 检查是否有待处理的用户，自动接入
            handleServiceAutoAssign(userId, session);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("收到消息：sessionId={}, payload={}", session.getId(), payload);
        
        try {
            Map<String, Object> msgData = JSONUtil.parseObj(payload);
            String type = (String) msgData.get("type");
            
            Map<String, Object> attributes = session.getAttributes();
            Long senderId = (Long) attributes.get("userId");
            String role = (String) attributes.get("role");
            
            switch (type) {
                case "chat":
                    // 聊天消息
                    handleChatMessage(session, senderId, role, msgData);
                    break;
                case "read":
                    // 标记已读
                    handleReadMessage(session, senderId, role, msgData);
                    break;
                case "end_chat":
                    // 结束聊天
                    handleEndChat(session, senderId, role, msgData);
                    break;
                default:
                    log.warn("未知消息类型：{}", type);
            }
        } catch (Exception e) {
            log.error("处理消息失败", e);
            sendError(session, "消息处理失败：" + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session.getId());
        
        Map<String, Object> attributes = session.getAttributes();
        Long userId = (Long) attributes.get("userId");
        String role = (String) attributes.get("role");
        
        if ("user".equals(role)) {
            userSessionMap.remove(userId);
            log.info("用户断开连接：userId={}", userId);
        } else if ("service".equals(role)) {
            serviceSessionMap.remove(userId);
            chatQueueService.serviceOffline(userId);
            log.info("客服断开连接：serviceId={}", userId);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket 传输错误：sessionId={}", session.getId(), exception);
        if (session.isOpen()) {
            session.close();
        }
    }

    /**
     * 处理用户加入排队队列
     */
    private void handleUserJoinQueue(Long userId, WebSocketSession session) throws IOException {
        // 检查是否有待处理的工单
        Ticket pendingTicket = getPendingTicketForUser(userId);
        
        if (pendingTicket != null) {
            log.info("用户已有工单：ticketId={}, status={}", pendingTicket.getId(), pendingTicket.getStatus());
            
            // 检查工单是否已分配客服
            if (pendingTicket.getAssignedTo() != null && "processing".equals(pendingTicket.getStatus())) {
                // 已分配客服，直接进入聊天
                sendToSession(session, Map.of(
                    "type", "assigned",
                    "ticketId", pendingTicket.getId(),
                    "serviceId", pendingTicket.getAssignedTo(),
                    "roomId", pendingTicket.getChatRoomId(),
                    "message", "客服已接入，开始聊天"
                ));
                log.info("工单已分配客服，直接进入聊天：ticketId={}, serviceId={}", 
                        pendingTicket.getId(), pendingTicket.getAssignedTo());
                return;
            }
            
            // 检查是否在队列中
            Long position = chatQueueService.getQueuePosition(pendingTicket.getId());
            
            if (position == null) {
                // 不在队列中，加入队列
                log.info("工单不在队列中，加入队列：ticketId={}", pendingTicket.getId());
                position = chatQueueService.joinQueue(pendingTicket.getId(), userId);
            }
            
            // 返回排队信息
            Long queueSize = chatQueueService.getQueueSize();
            sendToSession(session, Map.of(
                "type", "queue_info",
                "ticketId", pendingTicket.getId(),
                "position", position,
                "queueSize", queueSize
            ));
            
            // 尝试自动分配给在线客服
            tryAutoAssignTicket(pendingTicket);
            
        } else {
            // 没有工单，创建新工单并加入队列
            log.info("用户没有工单，创建新工单：userId={}", userId);
            Ticket ticket = createTicketForUser(userId, "用户请求在线聊天");
            Long position = chatQueueService.joinQueue(ticket.getId(), userId);
            Long queueSize = chatQueueService.getQueueSize();
            
            sendToSession(session, Map.of(
                "type", "queue_info",
                "ticketId", ticket.getId(),
                "position", position,
                "queueSize", queueSize
            ));
            
            // 尝试自动分配给在线客服
            tryAutoAssignTicket(ticket);
        }
    }

    /**
     * 处理客服自动分配
     */
    private void handleServiceAutoAssign(Long serviceId, WebSocketSession session) throws IOException {
        String ticketIdStr = chatQueueService.getFirstTicketId();
        
        if (ticketIdStr != null) {
            try {
                Long ticketId = Long.valueOf(ticketIdStr);
                Ticket ticket = ticketService.getById(ticketId);
                
                if (ticket != null) {
                    assignTicketToService(ticket, serviceId, session);
                }
            } catch (Exception e) {
                log.error("自动分配工单失败", e);
            }
        }
    }

    /**
     * 尝试自动分配工单给在线客服
     */
    private void tryAutoAssignTicket(Ticket ticket) throws IOException {
        // 检查是否有在线客服
        Long onlineServiceCount = chatQueueService.getOnlineServiceCount();
        if (onlineServiceCount == null || onlineServiceCount == 0) {
            log.info("没有在线客服，工单继续排队：ticketId={}", ticket.getId());
            return;
        }
        
        // 获取第一个在线客服
        String serviceIdStr = chatQueueService.getFirstOnlineServiceId();
        if (serviceIdStr == null) {
            log.info("无法获取在线客服 ID，工单继续排队：ticketId={}", ticket.getId());
            return;
        }
        
        try {
            Long serviceId = Long.valueOf(serviceIdStr);
            String sessionId = serviceSessionMap.get(serviceId);
            if (sessionId != null) {
                WebSocketSession serviceSession = sessions.get(sessionId);
                assignTicketToService(ticket, serviceId, serviceSession);
            }
        } catch (Exception e) {
            log.error("尝试自动分配工单失败", e);
        }
    }

    /**
     * 分配工单给客服
     */
    private void assignTicketToService(Ticket ticket, Long serviceId, WebSocketSession serviceSession) throws IOException {
        Long ticketId = ticket.getId();
        Long userId = ticket.getUserId();
        
        // 分配工单给客服
        ticketService.assignTicket(ticketId, serviceId, serviceId);
        
        // 从队列移除
        chatQueueService.removeFromQueue(ticketId);
        
        // 创建聊天室
        String roomId = chatQueueService.createChatRoom(ticketId, userId, serviceId);
        
        // 更新工单的聊天室 ID
        ticket.setChatRoomId(roomId);
        ticketService.updateById(ticket);
        
        // 通知客服
        if (serviceSession != null && serviceSession.isOpen()) {
            sendToSession(serviceSession, Map.of(
                "type", "new_chat",
                "ticketId", ticketId,
                "userId", userId,
                "roomId", roomId,
                "subject", ticket.getSubject(),
                "description", ticket.getDescription()
            ));
        }
        
        // 通知用户
        sendToUser(userId, Map.of(
            "type", "assigned",
            "ticketId", ticketId,
            "serviceId", serviceId,
            "roomId", roomId,
            "message", "客服已接入，开始聊天"
        ));
        
        log.info("自动分配工单：ticketId={}, serviceId={}, userId={}", ticketId, serviceId, userId);
    }

    /**
     * 处理聊天消息
     */
    private void handleChatMessage(WebSocketSession session, Long senderId, String senderRole, 
                                   Map<String, Object> msgData) throws IOException {
        Long ticketId = Long.valueOf(msgData.get("ticketId").toString());
        String content = (String) msgData.get("content");
        String messageType = (String) msgData.getOrDefault("messageType", "text");
        
        // 获取工单信息
        Ticket ticket = ticketService.getById(ticketId);
        if (ticket == null) {
            sendError(session, "工单不存在");
            return;
        }
        
        // 确定接收者
        Long receiverId = "user".equals(senderRole) ? ticket.getAssignedTo() : ticket.getUserId();
        
        // 保存消息到数据库
        ChatMessage chatMessage = chatMessageService.sendMessage(
            ticketId, senderId, senderRole, receiverId, content, messageType
        );
        
        // 构建消息
        Map<String, Object> chatMsg = Map.of(
            "type", "chat",
            "ticketId", ticketId,
            "messageId", chatMessage.getId(),
            "senderId", senderId,
            "senderRole", senderRole,
            "content", content,
            "messageType", messageType,
            "timestamp", System.currentTimeMillis()
        );
        
        // 发送给接收者
        if ("user".equals(senderRole)) {
            // 用户发送，发送给客服
            sendToService(ticket.getAssignedTo(), chatMsg);
        } else {
            // 客服发送，发送给用户
            sendToUser(ticket.getUserId(), chatMsg);
        }
        
        // 发送确认给发送者
        sendToSession(session, Map.of(
            "type", "chat_sent",
            "messageId", chatMessage.getId(),
            "ticketId", ticketId
        ));
    }

    /**
     * 处理已读消息
     */
    private void handleReadMessage(WebSocketSession session, Long senderId, String senderRole,
                                   Map<String, Object> msgData) {
        Long ticketId = Long.valueOf(msgData.get("ticketId").toString());
        chatMessageService.markAllAsRead(ticketId, senderId);
    }

    /**
     * 处理结束聊天
     */
    private void handleEndChat(WebSocketSession session, Long senderId, String senderRole,
                               Map<String, Object> msgData) throws IOException {
        Long ticketId = Long.valueOf(msgData.get("ticketId").toString());
        
        Ticket ticket = ticketService.getById(ticketId);
        if (ticket == null) {
            sendError(session, "工单不存在");
            return;
        }
        
        // 更新工单状态
        ticket.setStatus("completed");
        ticket.setChatEndedAt(java.time.LocalDateTime.now());
        ticket.setEndedBy(senderRole);
        ticketService.updateById(ticket);
        
        // 关闭聊天室
        String roomId = ticket.getChatRoomId();
        if (roomId != null) {
            chatQueueService.closeChatRoom(roomId);
        }
        
        // 通知对方
        Map<String, Object> endMsg = Map.of(
            "type", "chat_ended",
            "ticketId", ticketId,
            "endedBy", senderRole,
            "timestamp", System.currentTimeMillis()
        );
        
        if ("user".equals(senderRole)) {
            sendToService(ticket.getAssignedTo(), endMsg);
        } else {
            sendToUser(ticket.getUserId(), endMsg);
        }
        
        log.info("结束聊天：ticketId={}, endedBy={}", ticketId, senderRole);
    }

    /**
     * 发送消息到指定会话
     */
    private void sendToSession(WebSocketSession session, Map<String, Object> data) throws IOException {
        if (session != null && session.isOpen()) {
            String json = JSONUtil.toJsonStr(data);
            session.sendMessage(new TextMessage(json));
        }
    }

    /**
     * 发送消息给用户
     */
    private void sendToUser(Long userId, Map<String, Object> data) throws IOException {
        String sessionId = userSessionMap.get(userId);
        if (sessionId != null) {
            WebSocketSession session = sessions.get(sessionId);
            sendToSession(session, data);
        }
    }

    /**
     * 发送消息给客服
     */
    private void sendToService(Long serviceId, Map<String, Object> data) throws IOException {
        String sessionId = serviceSessionMap.get(serviceId);
        if (sessionId != null) {
            WebSocketSession session = sessions.get(sessionId);
            sendToSession(session, data);
        }
    }

    /**
     * 通知用户已分配客服
     */
    private void notifyUserAssigned(Long userId, Map<String, Object> data) throws IOException {
        sendToUser(userId, data);
    }

    /**
     * 发送错误消息
     */
    private void sendError(WebSocketSession session, String message) throws IOException {
        sendToSession(session, Map.of(
            "type", "error",
            "message", message
        ));
    }

    /**
     * 获取用户待处理工单（最新的）
     */
    private Ticket getPendingTicketForUser(Long userId) {
        List<Ticket> tickets = ticketService.lambdaQuery()
                .eq(Ticket::getUserId, userId)
                .in(Ticket::getStatus, "pending", "processing", "assigned")
                .orderByDesc(Ticket::getCreatedAt)
                .list();
        
        if (tickets == null || tickets.isEmpty()) {
            return null;
        }
        
        // 返回最新的工单
        return tickets.get(0);
    }

    /**
     * 为用户创建工单
     */
    private Ticket createTicketForUser(Long userId, String subject) {
        Ticket ticket = new Ticket();
        ticket.setUserId(userId);
        ticket.setCategory("other");
        ticket.setSubject(subject);
        ticket.setDescription("在线咨询");
        ticket.setStatus("pending");
        ticket.setPriority("normal");
        ticket.setCreatedBy(userId);
        
        ticketService.save(ticket);
        return ticket;
    }
}
