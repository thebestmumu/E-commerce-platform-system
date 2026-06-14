package com.rabbiter.em.ai.core;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis 对话记忆存储 - 保存和加载多轮对话历史
 * 
 * ===== 作用 =====
 * AI 本身是无状态的，每次调用都是"失忆"的。
 * 这个类负责把对话历史保存到 Redis，下次调用时再加载回来，实现"记忆"功能。
 * 
 * ===== 前端类比 =====
 * 就像 localStorage 保存聊天记录：
 * - 用户发消息 → 保存到 localStorage
 * - 刷新页面 → 从 localStorage 读取历史
 * - 只是这里用的是 Redis（服务端）而不是 localStorage（浏览器端）
 * 
 * ===== 为什么用 Redis？ =====
 * - 速度快（内存数据库）
 * - 支持分布式部署（多个后端实例共享数据）
 * - 支持自动过期（TTL 设置 30 分钟）
 */
@Component
public class RedisChatMemoryStore implements ChatMemoryStore {

    private static final Logger log = LoggerFactory.getLogger(RedisChatMemoryStore.class);

    // Redis key 前缀，类似 localStorage 的 key 命名规范
    // 例如：userId=123 → key="ai:memory:123"
    private static final String MEMORY_KEY_PREFIX = "ai:memory:";
    
    // 对话记忆保留 30 分钟，超时自动删除（节省内存）
    private static final long MEMORY_TTL_MINUTES = 30;

    /** Redis 操作模板（Spring 封装的 Redis 客户端） */
    @Resource
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 获取对话历史
     * 
     * @param memoryId 记忆 ID（通常是 userId）
     * @return 历史消息列表
     * 
     * 类比：localStorage.getItem('ai:memory:' + userId)
     */
    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String key = MEMORY_KEY_PREFIX + memoryId;
        try {
            // 从 Redis 读取 JSON 字符串
            String json = redisTemplate.opsForValue().get(key);
            if (json == null || json.isEmpty()) {
                return new ArrayList<>();  // 没有历史记录，返回空列表
            }
            // 把 JSON 反序列化为 ChatMessage 对象列表
            return ChatMessageDeserializer.messagesFromJson(json);
        } catch (Exception e) {
            log.error("从Redis读取对话记忆失败, key: {}", key, e);
            return new ArrayList<>();
        }
    }

    /**
     * 更新对话历史
     * 
     * @param memoryId 记忆 ID（通常是 userId）
     * @param messages 完整的消息列表（包含历史 + 新消息）
     * 
     * 类比：localStorage.setItem('ai:memory:' + userId, JSON.stringify(messages))
     */
    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String key = MEMORY_KEY_PREFIX + memoryId;
        try {
            // 把消息列表序列化为 JSON 字符串
            String json = ChatMessageSerializer.messagesToJson(messages);
            // 保存到 Redis，设置 30 分钟过期时间
            redisTemplate.opsForValue().set(key, json, MEMORY_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("写入对话记忆到Redis失败, key: {}", key, e);
        }
    }

    /**
     * 删除对话历史（清空记忆）
     * 
     * 类比：localStorage.removeItem('ai:memory:' + userId)
     */
    @Override
    public void deleteMessages(Object memoryId) {
        String key = MEMORY_KEY_PREFIX + memoryId;
        redisTemplate.delete(key);
        log.debug("已清除对话记忆, key: {}", key);
    }
}