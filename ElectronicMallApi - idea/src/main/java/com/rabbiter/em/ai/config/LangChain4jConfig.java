package com.rabbiter.em.ai.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    
    @Bean
    public ChatLanguageModel chatModel() {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();
    }
}
