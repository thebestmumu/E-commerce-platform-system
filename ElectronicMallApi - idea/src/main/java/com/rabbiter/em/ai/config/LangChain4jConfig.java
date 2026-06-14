package com.rabbiter.em.ai.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "langchain4j", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LangChain4jConfig {
    
    @Value("${langchain4j.open-ai.base-url:https://api.deepseek.com/v1}")
    private String baseUrl;
    
    @Value("${langchain4j.open-ai.api-key}")
    private String apiKey;
    
    @Value("${langchain4j.open-ai.chat-model.model-name:deepseek-chat}")
    private String modelName;
    
    @Value("${langchain4j.open-ai.chat-model.temperature:0.7}")
    private Double temperature;
    
    @Value("${langchain4j.open-ai.chat-model.max-tokens:2000}")
    private Integer maxTokens;
    
    // 注入 Langfuse 追踪监听器（如果启用了 Langfuse）
    @Autowired(required = false)
    private List<ChatModelListener> listeners;
    
    @Bean
    public ChatLanguageModel chatModel() {
        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(temperature)
                .maxTokens(maxTokens);
        
        // 如果配置了监听器，注册到 ChatModel
        if (listeners != null && !listeners.isEmpty()) {
            builder.listeners(listeners);
            log.info("Langfuse 追踪监听器已注册到 ChatLanguageModel");
        } else {
            log.warn("Langfuse 追踪监听器未配置，将不会追踪 AI 调用");
        }
        
        return builder.build();
    }
}
