package com.rabbiter.em.ai.config;

import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * Langfuse 追踪监听器 - 将 LangChain4j 的 AI 调用追踪到 Langfuse
 * 
 * 工作原理：
 * 1. 实现 ChatModelListener 接口，监听 AI 模型的请求/响应/错误事件
 * 2. 在请求开始时创建 OpenTelemetry span
 * 3. 在请求结束时更新 span 的状态和属性
 * 4. span 自动通过 OpenTelemetry 导出到 Langfuse
 * 
 * 类比前端：
 * - 就像 Axios 拦截器，在请求前后自动执行逻辑
 * - 请求开始 → 创建 span（记录开始时间）
 * - 请求结束 → 更新 span（记录耗时、token 用量等）
 */
@Slf4j
@Configuration
@ConditionalOnBean(OpenTelemetry.class)
public class LangfuseTracingConfig {

    @Resource
    private OpenTelemetry openTelemetry;

    @Bean
    public ChatModelListener langfuseChatModelListener() {
        Tracer tracer = openTelemetry.getTracer("mall-ai-service");

        return new ChatModelListener() {
            
            /**
             * AI 请求开始时触发
             * 创建 span 记录请求信息
             */
            @Override
            public void onRequest(ChatModelRequestContext requestContext) {
                try {
                    // 创建 span（类似前端 performance.mark('ai-request-start')）
                    Span span = tracer.spanBuilder("langchain4j.chat")
                            .setSpanKind(SpanKind.CLIENT)
                            .setAttribute("gen_ai.system", "deepseek")
                            .setAttribute("gen_ai.request.model", requestContext.request().model())
                            .setAttribute("gen_ai.request.temperature", requestContext.request().temperature())
                            .setAttribute("gen_ai.request.max_tokens", requestContext.request().maxTokens())
                            .setAttribute("message.count", requestContext.request().messages().size())
                            .startSpan();
                    
                    // 将 span 存储到上下文，供 onResponse 使用
                    requestContext.attributes().put("span", span);
                    requestContext.attributes().put("startTime", System.currentTimeMillis());
                    
                    log.debug("Langfuse span 创建: {}", requestContext.request().model());
                    
                } catch (Exception e) {
                    log.warn("Langfuse onRequest 异常: {}", e.getMessage());
                }
            }

            /**
             * AI 请求成功时触发
             * 更新 span 记录响应信息和 token 用量
             */
            @Override
            public void onResponse(ChatModelResponseContext responseContext) {
                try {
                    // 从上下文获取 span（注意：0.35.0 版本用 attributes() 直接获取）
                    Span span = (Span) responseContext.attributes().get("span");
                    if (span == null) return;
                    
                    // 记录响应信息
                    var tokenUsage = responseContext.response().tokenUsage();
                    if (tokenUsage != null) {
                        span.setAttribute("gen_ai.usage.input_tokens", tokenUsage.inputTokenCount());
                        span.setAttribute("gen_ai.usage.output_tokens", tokenUsage.outputTokenCount());
                        span.setAttribute("gen_ai.usage.total_tokens", 
                                tokenUsage.inputTokenCount() + tokenUsage.outputTokenCount());
                    }
                    
                    span.setAttribute("gen_ai.response.model", responseContext.response().model());
                    span.setAttribute("gen_ai.response.finish_reason", 
                            String.valueOf(responseContext.response().finishReason()));
                    
                    // 记录耗时（类似前端 performance.measure()）
                    Long startTime = (Long) responseContext.attributes().get("startTime");
                    if (startTime != null) {
                        long duration = System.currentTimeMillis() - startTime;
                        span.setAttribute("response.duration_ms", duration);
                    }
                    
                    // 标记 span 为成功状态
                    span.setStatus(StatusCode.OK);
                    span.end();  // 结束 span（类似前端 performance.mark('ai-request-end')）
                    
                    log.debug("Langfuse span 完成: {} tokens, {}ms", 
                            tokenUsage != null ? tokenUsage.totalTokenCount() : "N/A",
                            startTime != null ? System.currentTimeMillis() - startTime : "N/A");
                    
                } catch (Exception e) {
                    log.warn("Langfuse onResponse 异常: {}", e.getMessage());
                }
            }

            /**
             * AI 请求失败时触发
             * 更新 span 记录错误信息
             */
            @Override
            public void onError(ChatModelErrorContext errorContext) {
                try {
                    // 从上下文获取 span
                    Span span = (Span) errorContext.attributes().get("span");
                    if (span == null) return;
                    
                    // 记录错误信息
                    span.setStatus(StatusCode.ERROR, errorContext.error().getMessage());
                    span.recordException(errorContext.error());
                    span.end();
                    
                    log.warn("Langfuse span 错误: {}", errorContext.error().getMessage());
                    
                } catch (Exception e) {
                    log.warn("Langfuse onError 异常: {}", e.getMessage());
                }
            }
        };
    }
}
