package com.rabbiter.em.ai.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.qianfan.QianfanChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 百度文心一言（Qianfan）配置
 * 作为简单问题的轻量级模型
 * 
 * 注意：此配置为可选，如果没有配置 API Key，将只使用 DeepSeek 模型
 * 
 * 认证方式说明：
 * - 方式 1：只使用 API Key（推荐）- 适用于已开通千帆服务的账号
 * - 方式 2：使用 API Key + Secret Key - 适用于 OAuth2.0 认证
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "qianfan", name = "enabled", havingValue = "true", matchIfMissing = false)
public class QianfanConfig {

    @Value("${qianfan.api-key}")
    private String apiKey;

    @Value("${qianfan.secret-key:}")
    private String secretKey;  // 可选，如果为空则使用 API Key 方式

    @Value("${qianfan.chat-model.model-name:ernie-bot-4}")
    private String modelName;

    @Value("${qianfan.chat-model.temperature:0.7}")
    private Double temperature;

    @Value("${qianfan.chat-model.max-tokens:2000}")
    private Integer maxTokens;

    @Value("${qianfan.chat-model.timeout:30s}")
    private String timeout;

    @Bean("qianfanChatModel")
    public ChatLanguageModel qianfanChatModel() {
        log.info("========================================");
        log.info("初始化文心一言模型：{}", modelName);
        log.info("认证方式：{}", secretKey.isEmpty() ? "API Key 方式" : "API Key + Secret Key");
        log.info("API Key: {}****{}", 
            apiKey.substring(0, Math.min(4, apiKey.length())),
            apiKey.substring(Math.max(0, apiKey.length() - 4))
        );
        log.info("========================================");
        
        try {
            // 根据是否有 Secret Key 选择认证方式
            if (secretKey != null && !secretKey.isEmpty()) {
                // 方式 2：使用 API Key + Secret Key（OAuth2.0）
                log.info("使用 OAuth2.0 认证方式（API Key + Secret Key）");
                return QianfanChatModel.builder()
                        .apiKey(apiKey)
                        .secretKey(secretKey)
                        .modelName(modelName)
                        .temperature(temperature)
                        .maxOutputTokens(maxTokens)
                        .build();
            } else {
                // 方式 1：只使用 API Key（推荐，更简单）
                log.info("使用 API Key 认证方式（推荐）");
                return QianfanChatModel.builder()
                        .apiKey(apiKey)
                        .modelName(modelName)
                        .temperature(temperature)
                        .maxOutputTokens(maxTokens)
                        .build();
            }
        } catch (Exception e) {
            log.error("文心一言模型初始化失败：{}", e.getMessage());
            log.error("请检查 API Key 是否正确，以及是否已开通千帆服务");
            throw e;
        }
    }
}
