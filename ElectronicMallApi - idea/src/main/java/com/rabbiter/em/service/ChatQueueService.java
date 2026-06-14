package com.rabbiter.em.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 排队队列服务
 * 用于管理用户排队和客服分配
 */
@Slf4j
@Service
public class ChatQueueService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    // Redis key 前缀
    private static final String QUEUE_KEY_PREFIX = "chat:queue:";
    private static final String USER_QUEUE = QUEUE_KEY_PREFIX + "user";  // 用户排队队列
    private static final String SERVICE_SET = "chat:service:online";  // 在线客服集合
    private static final String CHAT_ROOM_PREFIX = "chat:room:";  // 聊天室前缀

    /**
     * 用户加入排队队列
     * @param ticketId 工单 ID
     * @param userId 用户 ID
     * @return 排队位置
     */
    public Long joinQueue(Long ticketId, Long userId) {
        String queueKey = USER_QUEUE;
        // 使用 score 作为排队时间戳
        long timestamp = System.currentTimeMillis();
        redisTemplate.opsForZSet().add(queueKey, ticketId.toString(), timestamp);
        
        // 获取排队位置
        Long position = redisTemplate.opsForZSet().rank(queueKey, ticketId.toString());
        
        log.info("用户加入排队队列：ticketId={}, userId={}, position={}", ticketId, userId, position);
        return position != null ? position + 1 : 1;
    }

    /**
     * 获取用户排队位置
     * @param ticketId 工单 ID
     * @return 排队位置（从 1 开始），如果不在队列中返回 null
     */
    public Long getQueuePosition(Long ticketId) {
        String queueKey = USER_QUEUE;
        Long rank = redisTemplate.opsForZSet().rank(queueKey, ticketId.toString());
        return rank != null ? rank + 1 : null;
    }

    /**
     * 获取队列长度
     * @return 等待人数
     */
    public Long getQueueSize() {
        String queueKey = USER_QUEUE;
        return redisTemplate.opsForZSet().size(queueKey);
    }

    /**
     * 从队列中移除（客服接入后）
     * @param ticketId 工单 ID
     */
    public void removeFromQueue(Long ticketId) {
        String queueKey = USER_QUEUE;
        redisTemplate.opsForZSet().remove(queueKey, ticketId.toString());
        log.info("用户离开排队队列：ticketId={}", ticketId);
    }

    /**
     * 获取队首工单 ID（FIFO）
     * @return 队首工单 ID，如果队列为空返回 null
     */
    public String getFirstTicketId() {
        String queueKey = USER_QUEUE;
        Set<String> result = redisTemplate.opsForZSet().range(queueKey, 0, 0);
        if (result != null && !result.isEmpty()) {
            return result.iterator().next();
        }
        return null;
    }

    /**
     * 客服上线
     * @param serviceId 客服 ID
     */
    public void serviceOnline(Long serviceId) {
        redisTemplate.opsForSet().add(SERVICE_SET, serviceId.toString());
        log.info("客服上线：serviceId={}", serviceId);
    }

    /**
     * 客服下线
     * @param serviceId 客服 ID
     */
    public void serviceOffline(Long serviceId) {
        redisTemplate.opsForSet().remove(SERVICE_SET, serviceId.toString());
        log.info("客服下线：serviceId={}", serviceId);
    }

    /**
     * 获取在线客服数量
     * @return 在线客服数量
     */
    public Long getOnlineServiceCount() {
        return redisTemplate.opsForSet().size(SERVICE_SET);
    }

    /**
     * 获取第一个在线客服 ID
     * @return 在线客服 ID，如果没有在线客服返回 null
     */
    public String getFirstOnlineServiceId() {
        Set<String> members = redisTemplate.opsForSet().members(SERVICE_SET);
        if (members != null && !members.isEmpty()) {
            return members.iterator().next();
        }
        return null;
    }

    /**
     * 创建聊天室
     * @param ticketId 工单 ID
     * @param userId 用户 ID
     * @param serviceId 客服 ID
     * @return 聊天室 ID
     */
    public String createChatRoom(Long ticketId, Long userId, Long serviceId) {
        String roomId = "room:" + ticketId + ":" + System.currentTimeMillis();
        String roomKey = CHAT_ROOM_PREFIX + roomId;
        
        // 存储聊天室信息
        redisTemplate.opsForHash().put(roomKey, "ticketId", ticketId.toString());
        redisTemplate.opsForHash().put(roomKey, "userId", userId.toString());
        redisTemplate.opsForHash().put(roomKey, "serviceId", serviceId.toString());
        redisTemplate.opsForHash().put(roomKey, "createdAt", String.valueOf(System.currentTimeMillis()));
        redisTemplate.opsForHash().put(roomKey, "status", "active");
        
        // 设置过期时间（24 小时）
        redisTemplate.expire(roomKey, 24, TimeUnit.HOURS);
        
        log.info("创建聊天室：roomId={}, ticketId={}, userId={}, serviceId={}", roomId, ticketId, userId, serviceId);
        return roomId;
    }

    /**
     * 获取聊天室信息
     * @param roomId 聊天室 ID
     * @return 聊天室信息
     */
    public java.util.Map<Object, Object> getChatRoom(String roomId) {
        String roomKey = CHAT_ROOM_PREFIX + roomId;
        return redisTemplate.opsForHash().entries(roomKey);
    }

    /**
     * 关闭聊天室
     * @param roomId 聊天室 ID
     */
    public void closeChatRoom(String roomId) {
        String roomKey = CHAT_ROOM_PREFIX + roomId;
        redisTemplate.opsForHash().put(roomKey, "status", "closed");
        redisTemplate.opsForHash().put(roomKey, "closedAt", String.valueOf(System.currentTimeMillis()));
        log.info("关闭聊天室：roomId={}", roomId);
    }

    /**
     * 检查客服是否在线
     * @param serviceId 客服 ID
     * @return true-在线，false-离线
     */
    public boolean isServiceOnline(Long serviceId) {
        return redisTemplate.opsForSet().isMember(SERVICE_SET, serviceId.toString());
    }
}
