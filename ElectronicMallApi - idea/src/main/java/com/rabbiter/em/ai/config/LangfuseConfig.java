package com.rabbiter.em.ai.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Langfuse 追踪配置（基于 OpenTelemetry）
 * 
 * Langfuse 是一个开源的 LLM 可观测性平台，类似 LangSmith，支持云端和本地部署。
 * 
 * 当前使用 Langfuse Cloud（免费）：
 * 1. 注册 https://cloud.langfuse.com 或 https://us.cloud.langfuse.com
 * 2. 创建项目，获取 API Key（Public Key 和 Secret Key）
 * 3. 在 application.yml 中配置：
 *    ai.langfuse.public-key: pk-lf-xxx
 *    ai.langfuse.secret-key: sk-lf-xxx
 *    ai.langfuse.host: https://us.cloud.langfuse.com  (US区域)
 * 4. 重启应用，查看追踪：https://us.cloud.langfuse.com/traces
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "ai.langfuse", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LangfuseConfig {

    @Value("${ai.langfuse.public-key:${LANGFUSE_PUBLIC_KEY:}}")
    private String publicKey;

    @Value("${ai.langfuse.secret-key:${LANGFUSE_SECRET_KEY:}}")
    private String secretKey;

    @Value("${ai.langfuse.host:${LANGFUSE_HOST:http://localhost:3000}}")
    private String host;

    @Value("${spring.application.name:mall-customer-service}")
    private String serviceName;

    @Bean
    public OpenTelemetry openTelemetry() {
        // 如果没有配置 API Key，使用匿名模式（仅本地开发）
        boolean hasApiKey = !publicKey.isEmpty() && !secretKey.isEmpty();
        
        if (!hasApiKey) {
            log.warn("============================================================");
            log.warn("Langfuse API Key 未配置，将使用匿名模式（仅本地开发）");
            log.warn("请在 application.yml 中配置：");
            log.warn("  ai.langfuse.public-key: pk-lf-xxx");
            log.warn("  ai.langfuse.secret-key: sk-lf-xxx");
            log.warn("  ai.langfuse.host: http://localhost:3000");
            log.warn("或设置环境变量：");
            log.warn("  export LANGFUSE_PUBLIC_KEY=pk-lf-xxx");
            log.warn("  export LANGFUSE_SECRET_KEY=sk-lf-xxx");
            log.warn("  export LANGFUSE_HOST=http://localhost:3000");
            log.warn("访问 http://localhost:3000 查看追踪面板");
            log.warn("============================================================");
        } else {
            log.info("============================================================");
            log.info("Langfuse 追踪已启用");
            log.info("  - Public Key: {}****{}", 
                publicKey.substring(0, Math.min(4, publicKey.length())),
                publicKey.substring(Math.max(0, publicKey.length() - 4))
            );
            log.info("  - Host: {}", host);
            log.info("  - 服务名：{}", serviceName);
            log.info("  - 查看面板：{}/traces", host);
            log.info("============================================================");
        }

        // 创建 OTLP HTTP 导出器（指向 Langfuse）
        // 注意：Langfuse 使用 HTTP/protobuf 协议，不是 gRPC
        // OtlpHttpSpanExporter 会自动添加 /v1/traces 后缀，所以只需设置到 /api/public/otel
        OtlpHttpSpanExporter exporter;
        if (hasApiKey) {
            String authHeader = "Basic " + java.util.Base64.getEncoder()
                .encodeToString((publicKey + ":" + secretKey).getBytes());
            exporter = OtlpHttpSpanExporter.builder()
                    .setEndpoint(host + "/api/public/otel")
                    .addHeader("Authorization", authHeader)
                    .setTimeout(Duration.ofSeconds(30))
                    .build();
        } else {
            exporter = OtlpHttpSpanExporter.builder()
                    .setEndpoint(host + "/api/public/otel")
                    .setTimeout(Duration.ofSeconds(30))
                    .build();
        }

        // 创建资源标识
        Resource resource = Resource.getDefault().toBuilder()
                .put("service.name", serviceName)
                .build();

        // 创建 Tracer Provider
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(exporter)
                        .setScheduleDelay(Duration.ofSeconds(1))
                        .setMaxExportBatchSize(512)
                        .setMaxQueueSize(2048)
                        .build())
                .setResource(resource)
                .build();

        // 创建 OpenTelemetry 实例
        return OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .buildAndRegisterGlobal();
    }
}
