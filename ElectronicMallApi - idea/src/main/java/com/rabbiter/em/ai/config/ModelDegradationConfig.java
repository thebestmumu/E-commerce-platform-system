package com.rabbiter.em.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 多模型降级策略配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.model-degradation")
public class ModelDegradationConfig {

    /**
     * 是否启用降级策略
     */
    private boolean enabled = true;

    /**
     * 主模型提供商
     */
    private String primary = "deepseek";

    /**
     * 备用模型提供商
     */
    private String fallback = "qianfan";

    /**
     * 超时时间（毫秒）
     */
    private long timeoutMs = 10000;

    /**
     * 失败阈值（连续失败次数触发降级）
     */
    private int failureThreshold = 2;

    /**
     * 降级冷却时间（毫秒）
     */
    private long cooldownMs = 300000;
}
