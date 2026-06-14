package com.rabbiter.em.ai.config;

import com.rabbiter.em.ai.core.RedisChatMemoryStore;
import com.rabbiter.em.ai.mcp.CustomerServiceSkills;
import com.rabbiter.em.ai.mcp.MallToolService;
import com.rabbiter.em.ai.service.MallAiAssistant;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

@Slf4j
@Configuration
public class AiServicesConfig {

    @Resource
    private ChatLanguageModel chatModel;

    @Resource
    private RedisChatMemoryStore chatMemoryStore;

    @Resource
    private MallToolService mallToolService;

    @Resource
    private CustomerServiceSkills customerServiceSkills;

    @Resource
    private OpenTelemetry openTelemetry;

    @Bean
    public MallAiAssistant mallAiAssistant() {
        log.info("正在创建 MallAiAssistant Agent 代理实例...");
        log.info("模型：{}", chatModel.getClass().getSimpleName());
        log.info("工具：MallToolService + CustomerServiceSkills");
        log.info("记忆存储：Redis (异常保护 + 自动降级)");
        if (openTelemetry != null) {
            log.info("OpenTelemetry 追踪：已启用（Langfuse）");
        } else {
            log.info("OpenTelemetry 追踪：未启用");
        }

        ChatMemoryProvider memoryProvider = memoryId -> MessageWindowChatMemory.builder()
                .maxMessages(10)
                .chatMemoryStore(chatMemoryStore)
                .build();

        AiServices<MallAiAssistant> builder = AiServices.builder(MallAiAssistant.class)
                .chatLanguageModel(chatModel)
                .chatMemoryProvider(memoryProvider)
                .tools(mallToolService, customerServiceSkills);

        MallAiAssistant assistant = builder.build();

        log.info("MallAiAssistant Agent 代理实例创建完成 (Redis 持久化记忆)");
        return assistant;
    }
}