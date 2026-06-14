package com.rabbiter.em.ai.service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 多模型降级策略服务
 * 当主模型连续失败时，自动切换到备用模型
 */
@Slf4j
@Service
public class ModelDegradationService {

    @Autowired
    @Qualifier("chatModel")
    private ChatLanguageModel primaryModel;

    @Autowired(required = false)
    @Qualifier("qianfanChatModel")
    private ChatLanguageModel fallbackModel;

    @Autowired
    private com.rabbiter.em.ai.config.ModelDegradationConfig degradationConfig;

    /**
     * 连续失败计数器
     */
    private final AtomicInteger failureCount = new AtomicInteger(0);

    /**
     * 最后失败时间
     */
    private Instant lastFailureTime;

    /**
     * 是否处于降级状态
     */
    private volatile boolean isFallbackActive = false;

    /**
     * 降级激活时间
     */
    private Instant fallbackActivatedTime;

    @PostConstruct
    public void init() {
        log.info("模型降级策略初始化：");
        log.info("  - 主模型：{}", degradationConfig.getPrimary());
        log.info("  - 备用模型：{}", degradationConfig.getFallback());
        log.info("  - 失败阈值：{}", degradationConfig.getFailureThreshold());
        log.info("  - 冷却时间：{}ms", degradationConfig.getCooldownMs());
    }

    /**
     * 获取当前应该使用的模型
     * @return ChatLanguageModel
     */
    public ChatLanguageModel getCurrentModel() {
        if (!degradationConfig.isEnabled()) {
            return primaryModel;
        }

        // 检查是否应该从降级状态恢复
        if (isFallbackActive && shouldRecover()) {
            log.info("冷却时间已过，尝试恢复主模型");
            isFallbackActive = false;
            failureCount.set(0);
            return primaryModel;
        }

        // 如果处于降级状态，使用备用模型
        if (isFallbackActive) {
            log.debug("当前处于降级状态，使用备用模型");
            return getFallbackModel();
        }

        // 否则使用主模型
        return primaryModel;
    }

    /**
     * 记录模型调用成功
     */
    public void recordSuccess() {
        if (failureCount.get() > 0) {
            log.info("模型调用成功，重置失败计数器");
            failureCount.set(0);
            isFallbackActive = false;
        }
    }

    /**
     * 记录模型调用失败
     */
    public void recordFailure() {
        int count = failureCount.incrementAndGet();
        lastFailureTime = Instant.now();
        
        log.warn("模型调用失败，失败次数：{}/{}", count, degradationConfig.getFailureThreshold());

        // 检查是否达到降级阈值
        if (count >= degradationConfig.getFailureThreshold() && !isFallbackActive) {
            activateFallback();
        }
    }

    /**
     * 激活备用模型
     */
    private void activateFallback() {
        if (fallbackModel == null) {
            log.error("备用模型未配置，无法降级");
            return;
        }

        isFallbackActive = true;
        fallbackActivatedTime = Instant.now();
        log.warn("已激活备用模型：{}", degradationConfig.getFallback());
        log.warn("主模型将在 {}ms 后尝试恢复", degradationConfig.getCooldownMs());
    }

    /**
     * 检查是否应该从降级状态恢复
     */
    private boolean shouldRecover() {
        if (fallbackActivatedTime == null) {
            return true;
        }

        long elapsed = Duration.between(fallbackActivatedTime, Instant.now()).toMillis();
        return elapsed >= degradationConfig.getCooldownMs();
    }

    /**
     * 获取备用模型
     */
    private ChatLanguageModel getFallbackModel() {
        if (fallbackModel == null) {
            log.error("备用模型未配置，回退到主模型");
            return primaryModel;
        }
        return fallbackModel;
    }

    /**
     * 获取当前状态信息
     */
    public String getStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("模型降级状态：\n");
        sb.append("  - 主模型：").append(degradationConfig.getPrimary()).append("\n");
        sb.append("  - 备用模型：").append(degradationConfig.getFallback()).append("\n");
        sb.append("  - 当前状态：").append(isFallbackActive ? "降级中" : "正常").append("\n");
        sb.append("  - 失败计数：").append(failureCount.get()).append("\n");
        if (isFallbackActive && fallbackActivatedTime != null) {
            long elapsed = Duration.between(fallbackActivatedTime, Instant.now()).toMillis();
            long remaining = degradationConfig.getCooldownMs() - elapsed;
            sb.append("  - 冷却剩余：").append(Math.max(0, remaining)).append("ms\n");
        }
        return sb.toString();
    }
}
