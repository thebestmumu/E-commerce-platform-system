package com.rabbiter.em.ai.core;

import cn.hutool.core.date.DateUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * AI 对话上下文管理器
 * 基于 Redis 自动管理对话历史，实现上下文感知
 */
@Component
public class AiContextManager {
    
    private static final Logger log = LoggerFactory.getLogger(AiContextManager.class);
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Redis Key 前缀
     */
    private static final String CONTEXT_KEY_PREFIX = "ai:context:";
    
    /**
     * 上下文过期时间（分钟）
     */
    private static final long CONTEXT_TTL = 30;
    
    /**
     * 最大上下文长度
     */
    private static final int MAX_CONTEXT_SIZE = 10;
    
    /**
     * 添加对话到上下文
     * @param userId 用户 ID
     * @param role 角色（user/assistant）
     * @param content 内容
     */
    public void addMessage(Long userId, String role, String content) {
        String key = getContextKey(userId);
        
        try {
            // 获取现有上下文
            List<ContextMessage> messages = getMessages(userId);
            
            // 添加新消息
            ContextMessage message = new ContextMessage(role, content, System.currentTimeMillis());
            messages.add(message);
            
            // 限制上下文长度
            if (messages.size() > MAX_CONTEXT_SIZE) {
                messages = messages.subList(messages.size() - MAX_CONTEXT_SIZE, messages.size());
            }
            
            // 保存到 Redis
            String json = objectMapper.writeValueAsString(messages);
            redisTemplate.opsForValue().set(key, json, CONTEXT_TTL, TimeUnit.MINUTES);
            
            log.debug("添加消息到上下文，用户：{}, 角色：{}, 内容：{}", userId, role, content);
        } catch (Exception e) {
            log.error("添加消息到上下文失败：{}", e.getMessage(), e);
        }
    }
    
    /**
     * 获取用户的对话历史
     * @param userId 用户 ID
     * @return 对话历史列表
     */
    public List<ContextMessage> getMessages(Long userId) {
        String key = getContextKey(userId);
        
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return new ArrayList<>();
            }
            
            if (value instanceof String) {
                return objectMapper.readValue((String) value, new TypeReference<List<ContextMessage>>() {});
            }
            
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("获取上下文失败：{}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 获取最近的 N 条消息
     * @param userId 用户 ID
     * @param limit 数量限制
     * @return 消息列表
     */
    public List<ContextMessage> getRecentMessages(Long userId, int limit) {
        List<ContextMessage> allMessages = getMessages(userId);
        if (allMessages.size() <= limit) {
            return allMessages;
        }
        return allMessages.subList(allMessages.size() - limit, allMessages.size());
    }
    
    /**
     * 清空用户上下文
     * @param userId 用户 ID
     */
    public void clearContext(Long userId) {
        String key = getContextKey(userId);
        redisTemplate.delete(key);
        log.info("清空用户上下文，用户：{}", userId);
    }
    
    /**
     * 获取上下文键
     */
    private String getContextKey(Long userId) {
        return CONTEXT_KEY_PREFIX + userId;
    }
    
    /**
     * 获取最后一条用户消息
     */
    public String getLastUserMessage(Long userId) {
        List<ContextMessage> messages = getMessages(userId);
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("user".equals(messages.get(i).getRole())) {
                return messages.get(i).getContent();
            }
        }
        return null;
    }
    
    /**
     * 对话上下文消息
     */
    public static class ContextMessage {
        private String role;
        private String content;
        private Long timestamp;
        
        public ContextMessage() {}
        
        public ContextMessage(String role, String content, Long timestamp) {
            this.role = role;
            this.content = content;
            this.timestamp = timestamp;
        }
        
        public String getRole() {
            return role;
        }
        
        public void setRole(String role) {
            this.role = role;
        }
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
        
        public Long getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(Long timestamp) {
            this.timestamp = timestamp;
        }
    }
}
