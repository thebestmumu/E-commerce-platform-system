package com.rabbiter.em.ai.core;

import com.rabbiter.em.ai.entity.ChatRequest;
import com.rabbiter.em.ai.entity.ChatResponse;

/**
 * AI 意图识别引擎接口
 * 采用适配器模式，支持多种实现方式
 */
public interface AiIntentEngine {
    
    /**
     * 识别用户意图
     * @param request 聊天请求
     * @return 意图识别结果
     */
    AiIntentResult recognizeIntent(ChatRequest request);
    
    /**
     * 批量意图识别
     * @param requests 请求列表
     * @return 意图识别结果列表
     */
    
    /**
     * 获取支持的意图列表
     * @return 意图列表
     */
    AiIntent[] getSupportedIntents();
    
    /**
     * 是否支持某个意图
     * @param intent 意图
     * @return 是否支持
     */
    boolean supportsIntent(AiIntent intent);
}
