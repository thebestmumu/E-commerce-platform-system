package com.rabbiter.em.controller;

import cn.hutool.jwt.JWTUtil;
import com.rabbiter.em.entity.ChatMessage;
import com.rabbiter.em.entity.Ticket;
import com.rabbiter.em.service.ChatMessageService;
import com.rabbiter.em.service.ChatQueueService;
import com.rabbiter.em.service.TicketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 聊天 API 接口
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatQueueService chatQueueService;

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private TicketService ticketService;

    /**
     * 获取排队信息
     */
    @GetMapping("/queue")
    public Map<String, Object> getQueueInfo(@RequestHeader("token") String token) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 解析 token 获取用户 ID
            cn.hutool.jwt.JWT jwtToken = JWTUtil.parseToken(token);
            cn.hutool.jwt.JWTPayload payload = jwtToken.getPayload();
            Object audObj = payload.getClaim("aud");
            Long userId = Long.valueOf(audObj.toString());
            
            // 获取用户待处理工单
            Ticket pendingTicket = ticketService.lambdaQuery()
                    .eq(Ticket::getUserId, userId)
                    .in(Ticket::getStatus, "pending", "processing")
                    .orderByDesc(Ticket::getCreatedAt)
                    .one();
            
            if (pendingTicket == null) {
                result.put("code", "200");
                result.put("message", "没有待处理的工单");
                result.put("inQueue", false);
                return result;
            }
            
            // 获取排队位置
            Long position = chatQueueService.getQueuePosition(pendingTicket.getId());
            Long queueSize = chatQueueService.getQueueSize();
            
            result.put("code", "200");
            result.put("inQueue", position != null);
            result.put("ticketId", pendingTicket.getId());
            result.put("position", position != null ? position : 0);
            result.put("queueSize", queueSize != null ? queueSize : 0);
            result.put("status", pendingTicket.getStatus());
            
            if (pendingTicket.getAssignedTo() != null) {
                result.put("serviceId", pendingTicket.getAssignedTo());
            }
            
        } catch (Exception e) {
            log.error("获取排队信息失败", e);
            result.put("code", "500");
            result.put("message", "获取排队信息失败：" + e.getMessage());
        }
        
        return result;
    }

    /**
     * 获取聊天历史
     */
    @GetMapping("/history/{ticketId}")
    public Map<String, Object> getChatHistory(@PathVariable Long ticketId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<ChatMessage> messages = chatMessageService.getChatHistory(ticketId);
            
            result.put("code", "200");
            result.put("data", messages);
            
        } catch (Exception e) {
            log.error("获取聊天历史失败", e);
            result.put("code", "500");
            result.put("message", "获取聊天历史失败：" + e.getMessage());
        }
        
        return result;
    }

    /**
     * 获取在线客服数量
     */
    @GetMapping("/service/online")
    public Map<String, Object> getOnlineServiceCount() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Long count = chatQueueService.getOnlineServiceCount();
            
            result.put("code", "200");
            result.put("onlineCount", count != null ? count : 0);
            
        } catch (Exception e) {
            log.error("获取在线客服数量失败", e);
            result.put("code", "500");
            result.put("message", "获取在线客服数量失败：" + e.getMessage());
        }
        
        return result;
    }

    /**
     * 结束聊天（用户主动）
     */
    @PostMapping("/end/{ticketId}")
    public Map<String, Object> endChat(@PathVariable Long ticketId,
                                       @RequestHeader("token") String token) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 解析 token 获取用户 ID
            cn.hutool.jwt.JWT jwtToken = JWTUtil.parseToken(token);
            cn.hutool.jwt.JWTPayload payload = jwtToken.getPayload();
            Object audObj = payload.getClaim("aud");
            Long userId = Long.valueOf(audObj.toString());
            
            Ticket ticket = ticketService.getById(ticketId);
            if (ticket == null) {
                result.put("code", "404");
                result.put("message", "工单不存在");
                return result;
            }
            
            // 更新工单状态
            ticket.setStatus("completed");
            ticket.setChatEndedAt(java.time.LocalDateTime.now());
            ticket.setEndedBy("user");
            ticketService.updateById(ticket);
            
            // 关闭聊天室
            String roomId = ticket.getChatRoomId();
            if (roomId != null) {
                chatQueueService.closeChatRoom(roomId);
            }
            
            result.put("code", "200");
            result.put("message", "聊天已结束");
            
        } catch (Exception e) {
            log.error("结束聊天失败", e);
            result.put("code", "500");
            result.put("message", "结束聊天失败：" + e.getMessage());
        }
        
        return result;
    }

    /**
     * 查询用户的工单（用于检查是否已有工单）
     */
    @GetMapping("/ticket/user/{userId}")
    public Map<String, Object> getUserTicket(@PathVariable Long userId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 获取用户待处理的工单
            Ticket pendingTicket = ticketService.lambdaQuery()
                    .eq(Ticket::getUserId, userId)
                    .in(Ticket::getStatus, "pending", "processing", "assigned")
                    .orderByDesc(Ticket::getCreatedAt)
                    .one();
            
            if (pendingTicket != null) {
                result.put("code", "200");
                result.put("data", pendingTicket);
            } else {
                result.put("code", "200");
                result.put("data", null);
                result.put("message", "没有待处理的工单");
            }
            
        } catch (Exception e) {
            log.error("查询用户工单失败", e);
            result.put("code", "500");
            result.put("message", "查询工单失败：" + e.getMessage());
        }
        
        return result;
    }

    /**
     * 创建工单
     */
    @PostMapping("/ticket")
    public Map<String, Object> createTicket(@RequestBody Map<String, String> request,
                                            @RequestHeader("token") String token) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 解析 token 获取用户 ID
            cn.hutool.jwt.JWT jwtToken = JWTUtil.parseToken(token);
            cn.hutool.jwt.JWTPayload payload = jwtToken.getPayload();
            Object audObj = payload.getClaim("aud");
            Long userId = Long.valueOf(audObj.toString());
            
            String category = request.get("category");
            String subject = request.get("subject");
            String description = request.get("description");
            
            if (category == null || subject == null || description == null) {
                result.put("code", "400");
                result.put("message", "缺少必要参数");
                return result;
            }
            
            String ticketNo = ticketService.createTicket(userId, category, subject, description);
            
            // 获取刚创建的工单
            Ticket ticket = ticketService.lambdaQuery()
                    .eq(Ticket::getTicketNo, ticketNo)
                    .one();
            
            result.put("code", "200");
            result.put("data", ticket);
            result.put("message", "工单创建成功");
            
        } catch (Exception e) {
            log.error("创建工单失败", e);
            result.put("code", "500");
            result.put("message", "创建工单失败：" + e.getMessage());
        }
        
        return result;
    }
}
