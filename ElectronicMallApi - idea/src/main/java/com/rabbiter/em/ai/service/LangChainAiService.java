package com.rabbiter.em.ai.service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * LangChain4j AI 服务实现 - 使用 DeepSeek
 * 完全基于 LangChain4j Spring Boot Starter 标准功能
 */
@Service("langChainAiService")
public class LangChainAiService {
    
    private static final Logger log = LoggerFactory.getLogger(LangChainAiService.class);
    
    @Autowired
    private ChatLanguageModel chatModel;  // LangChain4j 自动注入
    
    /**
     * 处理 AI 对话请求
     */
    public String chat(Long userId, String message) {
        log.info("========================================");
        log.info("LangChainAiService 收到请求");
        log.info("用户 ID: {}", userId);
        log.info("消息内容：{}", message != null ? message.substring(0, Math.min(50, message.length())) + "..." : "null");
        log.info("ChatLanguageModel 是否可用：{}", chatModel != null);
        log.info("ChatLanguageModel 类型：{}", chatModel != null ? chatModel.getClass().getSimpleName() : "null");
        log.info("========================================");
        
        try {
            log.info(">>> 调用 DeepSeek API...");
            String response = chatModel.generate(message);
            log.info("<<< DeepSeek API 响应成功");
            log.info("<<< 响应内容：{}", response != null ? response.substring(0, Math.min(100, response.length())) + "..." : "null");
            log.info("========================================");
            
            log.info("LangChainAiService 处理完成");
            return response;
            
        } catch (Exception e) {
            log.error("========================================");
            log.error("DeepSeek API 调用失败");
            log.error("错误类型：{}", e.getClass().getSimpleName());
            log.error("错误信息：{}", e.getMessage());
            log.error("========================================");
            log.error("堆栈跟踪：", e);
            throw e;
        }
    }
}
