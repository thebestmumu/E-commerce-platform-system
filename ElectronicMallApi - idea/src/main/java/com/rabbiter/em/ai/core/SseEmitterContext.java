package com.rabbiter.em.ai.core;

import cn.hutool.json.JSONUtil;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class SseEmitterContext {

    private static final ThreadLocal<SseEmitter> currentEmitter = new ThreadLocal<>();
    private static final ThreadLocal<AtomicInteger> stepCounter = new ThreadLocal<>();

    public static void setEmitter(SseEmitter emitter) {
        currentEmitter.set(emitter);
        stepCounter.set(new AtomicInteger(0));
    }

    public static SseEmitter getEmitter() {
        return currentEmitter.get();
    }

    public static void sendThinking(String content) {
        SseEmitter emitter = currentEmitter.get();
        if (emitter == null) return;
        try {
            AtomicInteger counter = stepCounter.get();
            int step = counter != null ? counter.incrementAndGet() : 1;
            emitter.send(SseEmitter.event()
                    .name("thinking")
                    .data(JSONUtil.toJsonStr(Map.of(
                            "type", "thinking",
                            "content", content,
                            "thinkingSteps", List.of(Map.of("step", step, "content", content))
                    ))));
        } catch (Exception e) {
            // SSE 发送失败不影响主流程
        }
    }

    /**
     * 发送 action 事件，用于前端展示商品卡片等交互组件
     * @param action     动作类型，如 "recommend_goods"、"search_goods" 等
     * @param actionData 动作数据，前端 handleAction 的 params
     */
    public static void sendAction(String action, Object actionData) {
        SseEmitter emitter = currentEmitter.get();
        if (emitter == null) return;
        try {
            emitter.send(SseEmitter.event()
                    .name("action")
                    .data(JSONUtil.toJsonStr(Map.of(
                            "action", action,
                            "actionData", actionData
                    ))));
        } catch (Exception e) {
            // SSE 发送失败不影响主流程
        }
    }

    public static void clear() {
        currentEmitter.remove();
        stepCounter.remove();
    }
}