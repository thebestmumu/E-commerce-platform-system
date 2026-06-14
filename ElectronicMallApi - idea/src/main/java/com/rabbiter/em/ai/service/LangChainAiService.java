package com.rabbiter.em.ai.service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * LangChain4j AI 服务实现 - 使用 DeepSeek
 * 完全基于 LangChain4j Spring Boot Starter 标准功能
 * 支持智能模型选择（简单问题用文心一言，复杂问题用 DeepSeek）
 */
@Service("langChainAiService")
public class LangChainAiService {
    
    private static final Logger log = LoggerFactory.getLogger(LangChainAiService.class);
    
    @Autowired
    private ChatLanguageModel chatModel;  // LangChain4j 自动注入（DeepSeek）
    
    @Autowired
    private SmartModelService smartModelService;
    
    /**
     * 处理 AI 对话请求（带智能模型选择）
     */
    public String chat(Long userId, String message) {
        log.info("========================================");
        log.info("LangChainAiService 收到请求");
        log.info("用户 ID: {}", userId);
        log.info("消息内容：{}", message != null ? message.substring(0, Math.min(50, message.length())) + "..." : "null");
        
        try {
            // 根据问题复杂度智能选择模型
            ChatLanguageModel currentModel = smartModelService.selectModel(message);
            log.info("使用模型：{} (复杂度：{})", 
                currentModel.getClass().getSimpleName(),
                isSimpleQuestion(message) ? "简单" : "复杂");
            
            log.info(">>> 调用 AI API...");
            String response = currentModel.generate(message);
            log.info("<<< AI API 响应成功");
            
            log.info("<<< 响应内容：{}", response != null ? response.substring(0, Math.min(100, response.length())) + "..." : "null");
            log.info("========================================");
            
            log.info("LangChainAiService 处理完成");
            return response;
            
        } catch (Exception e) {
            log.error("========================================");
            log.error("AI API 调用失败");
            log.error("错误类型：{}", e.getClass().getSimpleName());
            log.error("错误信息：{}", e.getMessage());
            log.error("========================================");
            log.error("堆栈跟踪：", e);
            throw e;
        }
    }
    
    /**
     * 判断是否是简单问题（用于日志记录）
     */
    private boolean isSimpleQuestion(String message) {
        if (message == null || message.length() <= 10) {
            return true;
        }
        // 简单判断：不包含复杂关键词
        String[] complexKeywords = {"为什么", "怎么", "如何", "详细", "解释", "分析"};
        for (String keyword : complexKeywords) {
            if (message.contains(keyword)) {
                return false;
            }
        }
        return message.length() <= 20;
    }
}
