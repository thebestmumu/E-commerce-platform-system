package com.rabbiter.em.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rabbiter.em.ai.service.AiAssistantService;
import com.rabbiter.em.ai.entity.ChatRequest;
import com.rabbiter.em.ai.entity.ChatResponse;
import com.rabbiter.em.entity.AiConversation;
import com.rabbiter.em.mapper.AiConversationMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;

/**
 * AI 对话历史 Service
 */
@Service
public class AiConversationService extends ServiceImpl<AiConversationMapper, AiConversation> {

    private static final Logger log = LoggerFactory.getLogger(AiConversationService.class);
    
    @Resource
    private AiConversationMapper aiConversationMapper;
    
    @Resource
    private AiAssistantService aiAssistantService;
    
    // 创建线程池用于异步生成标题
    private final ExecutorService titleGenerationExecutor = Executors.newSingleThreadExecutor();

    /**
     * 获取用户的对话列表
     */
    public List<AiConversation> getUserConversations(Long userId) {
        LambdaQueryWrapper<AiConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiConversation::getUserId, userId)
               .eq(AiConversation::getIsDeleted, 0)
               .orderByDesc(AiConversation::getLastMessageTime);
        return aiConversationMapper.selectList(wrapper);
    }

    /**
     * 创建新对话
     */
    @Transactional(rollbackFor = Exception.class)
    public AiConversation createConversation(Long userId, String title, String type) {
        AiConversation conversation = new AiConversation();
        conversation.setUserId(userId);
        conversation.setTitle(title != null && !title.isEmpty() ? title : "新对话");
        conversation.setType(type != null && !type.isEmpty() ? type : "chat");
        conversation.setMessages("[]");
        conversation.setLastMessageTime(new Date());
        conversation.setIsDeleted(0);
        
        aiConversationMapper.insert(conversation);
        return conversation;
    }

    /**
     * 获取对话详情
     */
    public AiConversation getConversationById(Long id) {
        return aiConversationMapper.selectById(id);
    }

    /**
     * 更新对话消息
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateConversationMessages(Long id, List<Map<String, Object>> messages) {
        AiConversation conversation = aiConversationMapper.selectById(id);
        if (conversation == null) {
            return false;
        }

        // 将消息列表转换为 JSON 字符串
        String messagesJson = JSONUtil.toJsonStr(messages);
        conversation.setMessages(messagesJson);
        conversation.setLastMessageTime(new Date());

        return aiConversationMapper.updateById(conversation) > 0;
    }

    /**
     * 添加消息到对话
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean addMessage(Long id, String role, String content, String action, Object actionData) {
        AiConversation conversation = aiConversationMapper.selectById(id);
        if (conversation == null) {
            return false;
        }

        // 解析现有消息
        List<Map<String, Object>> messages;
        if (StrUtil.isBlank(conversation.getMessages())) {
            messages = new ArrayList<>();
        } else {
            // 使用 JSONArray 解析
            cn.hutool.json.JSONArray jsonArray = JSONUtil.parseArray(conversation.getMessages());
            messages = new ArrayList<>();
            for (int i = 0; i < jsonArray.size(); i++) {
                cn.hutool.json.JSONObject jsonObject = jsonArray.getJSONObject(i);
                Map<String, Object> map = new HashMap<>();
                for (String key : jsonObject.keySet()) {
                    map.put(key, jsonObject.get(key));
                }
                messages.add(map);
            }
        }

        // 添加新消息
        Map<String, Object> message = new HashMap<>();
        message.put("role", role);
        message.put("content", content);
        if (action != null) {
            message.put("action", action);
        }
        if (actionData != null) {
            message.put("actionData", actionData);
        }
        messages.add(message);

        // 更新消息列表
        String messagesJson = JSONUtil.toJsonStr(messages);
        conversation.setMessages(messagesJson);
        conversation.setLastMessageTime(new Date());

        // 如果是用户的第一条消息，异步调用 AI 生成标题
        if ("user".equals(role) && isUserFirstMessage(messages)) {
            final Long conversationId = id;
            final String userMessage = content;
            
            // 异步生成标题，不阻塞主流程
            titleGenerationExecutor.submit(() -> {
                try {
                    String aiTitle = generateTitleWithAI(userMessage);
                    log.info("AI 生成的标题：{}", aiTitle);
                    
                    // 更新数据库标题
                    updateConversationTitle(conversationId, aiTitle);
                    log.info("对话标题已更新为：{}", aiTitle);
                } catch (Exception e) {
                    log.error("AI 生成标题失败，使用默认标题：{}", e.getMessage());
                    // 如果 AI 生成失败，使用简单规则生成
                    String simpleTitle = generateTitleFromMessage(userMessage);
                    updateConversationTitle(conversationId, simpleTitle);
                }
            });
        }

        return aiConversationMapper.updateById(conversation) > 0;
    }

    /**
     * 判断是否是用户的第一条消息
     */
    private boolean isUserFirstMessage(List<Map<String, Object>> messages) {
        // 统计用户消息数量（不包括刚添加的这条）
        int userMessageCount = 0;
        for (Map<String, Object> msg : messages) {
            if ("user".equals(msg.get("role"))) {
                userMessageCount++;
            }
        }
        // 如果只有 1 条用户消息（就是刚添加的这条），说明是第一条
        return userMessageCount == 1;
    }

    /**
     * 从用户消息生成标题（简单规则）
     */
    private String generateTitleFromMessage(String content) {
        if (content == null || content.isEmpty()) {
            return "新对话";
        }

        // 简单规则：截取前 20 个字符作为标题
        if (content.length() <= 20) {
            return content.trim();
        } else {
            return content.substring(0, 20).trim() + "...";
        }
    }
    
    /**
     * 使用 AI 生成标题
     */
    private String generateTitleWithAI(String userMessage) {
        try {
            // 构造提示词，使用小皮助手的系统提示词，更明确地强调身份
            String systemPrompt = "【系统指令】你是小皮助手，是一个电商购物助手。你现在的任务是帮用户总结对话标题，不是聊天或回答问题。\n\n" +
                                "【你的身份】小皮助手 - 专业的电商购物助手\n" +
                                "【你的任务】从用户的第一条消息中提取关键词，生成一个简短的标题\n" +
                                "【重要要求】\n" +
                                "1. 只返回标题，不要返回任何其他内容\n" +
                                "2. 不要自我介绍\n" +
                                "3. 不要说'我是小皮助手'\n" +
                                "4. 不要回答问题\n" +
                                "5. 标题不超过 10 个字\n" +
                                "6. 只返回标题文字本身\n\n";
            
            // 让 AI 总结用户消息生成简短标题
            String userPrompt = "请总结以下用户消息，生成一个简短标题：\n" + userMessage;
            
            log.info("调用 AI 生成标题，用户消息：{}", userMessage);
            
            // 创建聊天请求（使用完整的 prompt）
            ChatRequest request = new ChatRequest();
            request.setMessage(systemPrompt + userPrompt);
            
            // 调用 AI 服务
            ChatResponse response = aiAssistantService.chat(request);
            
            if (response != null && response.getMessage() != null) {
                String title = response.getMessage().trim();
                
                // 清理多余内容
                title = title.replace("\"", "").replace("'", "")
                            .replace("标题：", "").replace("总结：", "")
                            .replace("小皮助手", "")
                            .trim();
                
                // 如果标题太长，截断
                if (title.length() > 20) {
                    title = title.substring(0, 20) + "...";
                }
                
                log.info("AI 生成的标题：{}", title);
                return title;
            }
        } catch (Exception e) {
            log.error("AI 生成标题异常：{}", e.getMessage(), e);
        }
        
        // 失败时返回默认值
        return "新对话";
    }

    /**
     * 更新对话标题
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateConversationTitle(Long id, String title) {
        AiConversation conversation = aiConversationMapper.selectById(id);
        if (conversation == null) {
            return false;
        }

        conversation.setTitle(title);
        return aiConversationMapper.updateById(conversation) > 0;
    }

    /**
     * 删除对话（软删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteConversation(Long id) {
        AiConversation conversation = aiConversationMapper.selectById(id);
        if (conversation == null) {
            return false;
        }

        conversation.setIsDeleted(1);
        return aiConversationMapper.updateById(conversation) > 0;
    }

    /**
     * 批量删除对话
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteConversations(List<Long> ids, Long userId) {
        for (Long id : ids) {
            AiConversation conversation = aiConversationMapper.selectById(id);
            if (conversation != null && conversation.getUserId().equals(userId)) {
                conversation.setIsDeleted(1);
                aiConversationMapper.updateById(conversation);
            }
        }
        return true;
    }

    /**
     * 清空用户的所有对话
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean clearUserConversations(Long userId) {
        LambdaQueryWrapper<AiConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiConversation::getUserId, userId)
               .eq(AiConversation::getIsDeleted, 0);
        
        List<AiConversation> conversations = aiConversationMapper.selectList(wrapper);
        for (AiConversation conversation : conversations) {
            conversation.setIsDeleted(1);
            aiConversationMapper.updateById(conversation);
        }
        return true;
    }
    
    /**
     * 销毁方法，关闭线程池
     */
    @PreDestroy
    public void destroy() {
        if (titleGenerationExecutor != null) {
            titleGenerationExecutor.shutdown();
            try {
                if (!titleGenerationExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    titleGenerationExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                titleGenerationExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
