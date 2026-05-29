package com.rabbiter.em.ai.service;

import com.rabbiter.em.ai.entity.ChatRequest;
import com.rabbiter.em.ai.entity.ChatResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 服务接口
 */
public interface AiService {
    
    /**
     * 聊天对话
     * @param request 聊天请求
     * @return 聊天响应
     */
    ChatResponse chat(ChatRequest request);
    
    /**
     * 流式聊天对话（使用 PrintWriter，原生 Servlet）
     * @param request 聊天请求
     * @param writer 响应写入器
     */
    void streamChatWithWriter(ChatRequest request, java.io.PrintWriter writer);
    
    /**
     * 获取 AI 响应
     * @param message 用户消息
     * @param history 历史消息
     * @return AI 响应
     */
    String getAiResponse(String message, ChatRequest.Message... history);
    
    /**
     * 识别用户意图
     * @param message 用户消息
     * @return 意图
     */
    String recognizeIntent(String message);
    
    /**
     * 提取实体
     * @param message 用户消息
     * @param intent 意图
     * @return 实体
     */
    Object extractEntities(String message, String intent);
    
    /**
     * 获取个性化推荐
     * @param userId 用户ID
     * @return 推荐结果
     */
    Object getPersonalizedRecommendation(Long userId);
}
