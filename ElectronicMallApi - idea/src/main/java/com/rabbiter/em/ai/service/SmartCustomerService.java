package com.rabbiter.em.ai.service;

import cn.hutool.json.JSONUtil;
import com.rabbiter.em.ai.core.AiContextManager;
import com.rabbiter.em.ai.core.SseEmitterContext;
import com.rabbiter.em.ai.entity.ChatRequest;
import com.rabbiter.em.ai.entity.ChatResponse;
import com.rabbiter.em.ai.mcp.UserContext;
import com.rabbiter.em.ai.rag.KnowledgeBaseService;
import com.rabbiter.em.ai.rag.RagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 智能客服服务 - 整个 AI Agent 的总调度中心
 * 
 * 类比前端概念：
 * - 相当于 Vue 的 methods 或 actions，负责编排整个业务流程
 * - 接收前端请求 → 调用 AI → 返回结果
 * 
 * 工作流程：
 * 1. 接收用户消息
 * 2. 调用 MallAiAssistant（AI Agent）进行推理
 * 3. AI 会自动决定是否需要调用工具（查商品、查订单、查知识库等）
 * 4. 获取 AI 的最终回答
 * 5. 流式返回给前端（类似 WebSocket 逐字推送）
 */
@Service
public class SmartCustomerService {

    private static final Logger log = LoggerFactory.getLogger(SmartCustomerService.class);

    // SSE 超时时间：300秒（5分钟），类似前端的 request timeout
    private static final long SSE_TIMEOUT = 300_000L;

    /**
     * AI Agent 代理对象 - 核心！
     * 
     * 类比前端：
     * - 就像一个封装好的 API 调用函数，但内部会自动执行 ReAct 循环
     * - 你只需要调用 chat() 方法，AI 会自动决定调用哪些工具
     * 
     * @Resource 类似 Vue 的 inject 或 Vuex 的 mapState，Spring 自动注入这个依赖
     */
    @Resource
    private MallAiAssistant mallAiAssistant;

    /** RAG 知识库问答服务 */
    @Resource
    private RagService ragService;

    /** 知识库底层服务（向量检索） */
    @Resource
    private KnowledgeBaseService knowledgeBaseService;

    /** 对话上下文管理器（保存/读取对话历史） */
    @Resource
    private AiContextManager contextManager;

    /**
     * 应用启动时执行，类似 Vue 的 mounted() 生命周期
     * 打印系统初始化信息到控制台
     */
    @PostConstruct
    public void init() {
        log.info("");
        log.info("============================================================");
        log.info("  智能客服系统已就绪 (SmartCustomerService)");
        log.info("  架构: @AiService + ReAct + RAG + MCP");
        log.info("  模型: DeepSeek via LangChain4j OpenAiChatModel");
        log.info("  MallAiAssistant 代理: {}", mallAiAssistant != null ? "已创建" : "未创建");
        log.info("  知识库: {}", knowledgeBaseService != null ? "已加载" : "未加载");
        log.info("============================================================");
        log.info("");
    }

    /**
     * 处理用户消息（非流式版本 - 等 AI 全部生成完才返回）
     * 
     * 类比前端：
     * - 就像一个普通的 async/await API 调用
     * - 等待 AI 完整回答后才返回给前端
     * 
     * @param request 包含 userId 和 message
     * @return AI 的完整回答
     */
    public ChatResponse processMessage(ChatRequest request) {
        Long userId = request.getUserId();
        String message = request.getMessage();
        long startTime = System.currentTimeMillis();

        log.info("=== SmartCustomerService(AiService) 处理请求 ===");
        log.info("用户: {}, 消息: {}", userId, message);

        ChatResponse response = new ChatResponse();
        response.setSuccess(true);

        try {
            // 设置用户 ID 到 ThreadLocal（类似前端的全局变量 window.userId）
            // 这样后续 AI 调用的工具都能获取到当前用户 ID
            UserContext.setUserId(userId);
            
            // 保存用户消息到对话历史（类似 localStorage 记录聊天记录）
            contextManager.addMessage(userId, "user", message);

            // 获取当前时间，注入给 AI（让 AI 知道现在是几点）
            String currentTime = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            // ===== 核心调用：AI Agent 开始推理 =====
            // 这一步内部会执行 ReAct 循环：
            // 1. AI 理解用户问题
            // 2. AI 决定是否需要调用工具
            // 3. 如果需要，调用工具获取数据
            // 4. AI 基于工具结果生成回答
            String answer = mallAiAssistant.chat(userId, message, currentTime);

            // 保存 AI 回复到对话历史
            contextManager.addMessage(userId, "assistant", answer);

            // 构建返回给前端的数据
            response.setMessage(answer);
            response.setContent(answer);
            response.setAction("chat");

            Map<String, Object> actionData = new HashMap<>();
            actionData.put("rounds", "auto");
            response.setActionData(actionData);

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("AiService 处理完成，耗时 {}ms", elapsed);

        } catch (Exception e) {
            log.error("SmartCustomerService(AiService) 处理异常", e);
            response.setSuccess(false);
            response.setMessage("抱歉，系统处理出错，请稍后再试。");
        } finally {
            // 清除 ThreadLocal，防止内存泄漏（类似前端清理全局变量）
            UserContext.clear();
        }

        return response;
    }

    /**
     * 处理用户消息（流式版本 - 逐字返回给前端）
     * 
     * 类比前端：
     * - 就像 WebSocket 或 EventSource，服务端可以持续推送数据
     * - 前端不用等 AI 全部生成完，可以实时看到 AI 逐字输出
     * - 类似 ChatGPT 的打字机效果
     * 
     * SSE (Server-Sent Events) 说明：
     * - 一种服务端向客户端推送事件的技术
     * - 前端用 new EventSource() 或 fetch + ReadableStream 接收
     * - 后端用 SseEmitter 发送事件
     * 
     * @return SseEmitter 类似一个"管道"，可以通过它持续向前端发送数据
     */
    public SseEmitter streamProcessMessage(ChatRequest request) {
        // 处理用户 ID 和消息的默认值
        Long finalUserId = request.getUserId() != null ? request.getUserId() : 0L;
        String rawMsg = request.getMessage();
        String finalMessage = (rawMsg != null && !rawMsg.trim().isEmpty()) ? rawMsg : "你好";

        // 创建 SSE 发射器（类似创建一个 WebSocket 连接）
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        // 异步执行 AI 推理（类似前端的 setTimeout 或 Promise，不阻塞主线程）
        CompletableFuture.runAsync(() -> {
            // 把 emitter 保存到 ThreadLocal，这样 AI 工具中也能访问到它
            SseEmitterContext.setEmitter(emitter);
            try {
                // 设置用户 ID 到 ThreadLocal（类似 window.userId = finalUserId）
                UserContext.setUserId(finalUserId);
                
                // 发送"思考中"事件给前端（前端会显示"AI 正在理解您的问题..."）
                sendSseEvent(emitter, "thinking",
                        JSONUtil.toJsonStr(Map.of("content", "🤔 AI 正在理解您的问题...", "type", "thinking", "thinkingSteps", List.of(Map.of("step", 0, "content", "AI 正在理解您的问题...")))));

                log.info("SmartCustomerService 流式处理: userId={}, message={}", finalUserId, finalMessage);

                // 保存用户消息到对话历史
                contextManager.addMessage(finalUserId, "user", finalMessage);

                String currentTime = LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                // 发送"推理中"事件
                sendSseEvent(emitter, "thinking",
                        JSONUtil.toJsonStr(Map.of("content", "🧠 AI 正在推理分析，准备调用工具...", "type", "thinking", "thinkingSteps", List.of(Map.of("step", 1, "content", "AI 正在推理分析，准备调用工具...")))));

                // ===== 核心调用：AI Agent 开始推理 =====
                // 这里会阻塞等待 AI 完成所有推理和工具调用
                String answer = mallAiAssistant.chat(finalUserId, finalMessage, currentTime);

                // 发送"推理完成"事件
                sendSseEvent(emitter, "thinking",
                        JSONUtil.toJsonStr(Map.of("content", "✍️ AI 推理完成，正在生成回答...", "type", "thinking", "thinkingSteps", List.of(Map.of("step", 2, "content", "AI 推理完成，正在生成回答...")))));

                // ===== 流式输出：逐字发送给前端 =====
                streamTextByChar(answer, emitter);

                // 保存 AI 回复到对话历史
                contextManager.addMessage(finalUserId, "assistant", answer);

                // 发送完成信号（前端收到 [DONE] 后停止加载动画）
                sendSseEvent(emitter, "done", "[DONE]");
                log.info("=== SmartCustomerService(AiService) 流式处理完成 ===");

            } catch (Exception e) {
                log.error("SmartCustomerService 流式处理异常", e);
                try {
                    // 发送错误事件给前端
                    sendSseEvent(emitter, "error",
                            JSONUtil.toJsonStr(Map.of("error", e.getMessage())));
                } catch (Exception ignored) {}
            } finally {
                // 清理 ThreadLocal（类似前端清理全局变量）
                UserContext.clear();
                SseEmitterContext.clear();
                try {
                    emitter.complete(); // 关闭 SSE 连接
                } catch (Exception ignored) {}
            }
        });

        // 设置超时回调（类似前端的 setTimeout 超时处理）
        emitter.onTimeout(() -> {
            log.warn("SSE 连接超时，userId: {}", finalUserId);
            emitter.complete();
        });

        // 设置错误回调（类似前端的 catch 错误处理）
        emitter.onError(throwable -> {
            log.warn("SSE 连接异常: {}", throwable.getMessage());
            emitter.complete();
        });

        return emitter;
    }

    public ChatResponse queryKnowledge(ChatRequest request) {
        ChatResponse response = new ChatResponse();
        response.setSuccess(true);

        try {
            String answer = ragService.answer(request.getMessage());
            response.setMessage(answer);
            response.setContent(answer);
            response.setAction("knowledge");
        } catch (Exception e) {
            log.error("知识库查询失败", e);
            response.setSuccess(false);
            response.setMessage("知识库查询失败: " + e.getMessage());
        }

        return response;
    }

    public ChatResponse queryKnowledgeByCategory(ChatRequest request, String category) {
        ChatResponse response = new ChatResponse();
        response.setSuccess(true);

        try {
            String answer = ragService.answerWithCategory(category, request.getMessage());
            response.setMessage(answer);
            response.setContent(answer);
            response.setAction("knowledge_" + category);
        } catch (Exception e) {
            log.error("分类知识库查询失败", e);
            response.setSuccess(false);
            response.setMessage("查询失败: " + e.getMessage());
        }

        return response;
    }

    public Map<String, Object> getKnowledgeCategories() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        Map<String, List<Map<String, String>>> categories = new LinkedHashMap<>();

        for (Map.Entry<String, List<KnowledgeBaseService.KnowledgeDoc>> entry :
                knowledgeBaseService.getAllDocs().entrySet()) {
            List<Map<String, String>> docs = new ArrayList<>();
            for (KnowledgeBaseService.KnowledgeDoc doc : entry.getValue()) {
                Map<String, String> d = new HashMap<>();
                d.put("id", doc.getId());
                d.put("title", doc.getTitle());
                docs.add(d);
            }
            categories.put(entry.getKey(), docs);
        }

        result.put("categories", categories);
        return result;
    }

    /**
     * 发送 SSE 事件给前端
     * 
     * 类比前端：
     * - 就像 emit('eventName', data) 触发一个自定义事件
     * - 前端通过 EventSource.addEventListener('eventName', callback) 接收
     * 
     * @param emitter SSE 发射器
     * @param event 事件名称（如 'thinking', 'message', 'done', 'error'）
     * @param data 事件数据（JSON 字符串）
     */
    private void sendSseEvent(SseEmitter emitter, String event, String data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(event)      // 事件名称
                    .data(data));     // 事件数据
        } catch (Exception e) {
            log.warn("SSE 发送失败: {}", e.getMessage());
        }
    }

    /**
     * 逐字流式输出文本给前端
     * 
     * 类比前端：
     * - 就像 setInterval(() => { output += nextChar() }, 10)
     * - 每 10 个字符发送一次，模拟打字机效果
     * 
     * @param text 完整的 AI 回答文本
     * @param emitter SSE 发射器
     */
    private void streamTextByChar(String text, SseEmitter emitter) {
        if (text == null) return;
        try {
            StringBuilder buffer = new StringBuilder();
            // 逐字遍历文本
            for (int i = 0; i < text.length(); i++) {
                buffer.append(text.charAt(i));
                // 每 10 个字符发送一次（或到达末尾时）
                if (i % 10 == 9 || i == text.length() - 1) {
                    sendSseEvent(emitter, "message",
                            JSONUtil.toJsonStr(Map.of(
                                    "content", buffer.toString(),  // 这批次文本
                                    "type", "text",
                                    "index", i                      // 当前字符位置
                            )));
                    buffer.setLength(0);  // 清空缓冲区
                    Thread.sleep(10);     // 暂停 10ms，控制输出速度
                }
            }
        } catch (Exception e) {
            log.warn("流式输出中断: {}", e.getMessage());
        }
    }
}