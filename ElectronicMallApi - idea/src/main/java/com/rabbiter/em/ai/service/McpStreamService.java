package com.rabbiter.em.ai.service;

import cn.hutool.json.JSONUtil;
import com.rabbiter.em.ai.core.AiContextManager;
import com.rabbiter.em.ai.entity.ChatRequest;
import com.rabbiter.em.ai.mcp.MallToolService;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP 流式聊天服务
 * 核心流程：用户消息 → LLM(+Tools) → 工具调用 → LLM总结 → SSE推送给前端
 *
 * 与原有功能完全独立，通过 feature flag 开关控制是否启用
 */
@Service
public class McpStreamService {

    private static final Logger log = LoggerFactory.getLogger(McpStreamService.class);

    @Resource
    private ChatLanguageModel chatModel;

    @Resource
    private MallToolService mallToolService;

    @Resource
    private AiContextManager contextManager;

    /**
     * MCP 流式聊天主入口
     * @param request 聊天请求
     * @param writer SSE 输出流
     */
    public void streamChat(ChatRequest request, PrintWriter writer) {
        Long userId = request.getUserId();
        String message = request.getMessage();

        try {
            log.info("=== MCP 流式聊天开始 ===");
            log.info("用户消息：{}", message);

            // 1. 保存用户消息到上下文
            if (userId != null) {
                contextManager.addMessage(userId, "user", message);
            }

            // 2. 构建消息列表（含上下文历史）
            List<ChatMessage> messages = buildMessages(userId, message);

            // 3. 获取工具规格
            List<ToolSpecification> toolSpecs = ToolSpecifications.toolSpecificationsFrom(mallToolService);
            log.info("注册 MCP 工具数量：{}", toolSpecs.size());

            // 4. 发送"思考中"SSE事件
            sendSseEvent(writer, "message",
                    JSONUtil.toJsonStr(Map.of("content", "正在为您处理...", "type", "thinking")));

            // 5. MCP 工具调用循环（最多5轮防止死循环）
            String finalText = null;
            for (int round = 0; round < 5; round++) {
                log.info("MCP 循环第 {} 轮", round + 1);

                // 调用 LLM
                ChatResponse response = chatModel.chat(
                        dev.langchain4j.model.chat.request.ChatRequest.builder()
                                .messages(messages)
                                .toolSpecifications(toolSpecs)
                                .build()
                );

                AiMessage aiMessage = response.aiMessage();

                // 如果 LLM 返回了工具调用请求
                if (aiMessage.hasToolExecutionRequests()) {
                    log.info("LLM 请求调用 {} 个工具", aiMessage.toolExecutionRequests().size());

                    // 把 AI 消息（含工具调用）加入消息列表
                    messages.add(aiMessage);

                    // 执行每个工具调用
                    for (ToolExecutionRequest toolRequest : aiMessage.toolExecutionRequests()) {
                        String toolName = toolRequest.name();
                        String toolArgs = toolRequest.arguments();
                        log.info("执行工具：{}，参数：{}", toolName, toolArgs);

                        // 执行工具
                        String toolResult = executeToolByName(toolName, toolArgs);
                        log.info("工具执行结果：{}", toolResult.substring(0, Math.min(200, toolResult.length())));

                        // 发送 action SSE 事件给前端（触发商品卡片等UI）
                        String actionType = mapToolNameToAction(toolName);
                        Map<String, Object> actionData = parseToolResultToActionData(toolName, toolResult);
                        Map<String, Object> actionMap = new HashMap<>();
                        actionMap.put("action", actionType);
                        actionMap.put("actionData", actionData);
                        sendSseEvent(writer, "action", JSONUtil.toJsonStr(actionMap));

                        // 把工具执行结果加入消息列表
                        messages.add(ToolExecutionResultMessage.from(toolRequest, toolResult));
                    }
                } else {
                    // LLM 返回了最终文本回复
                    finalText = aiMessage.text();
                    log.info("LLM 最终回复：{}", finalText);
                    break;
                }
            }

            // 6. 如果没有获取到回复，给出兜底文案
            if (finalText == null || finalText.isEmpty()) {
                finalText = "抱歉，我暂时无法处理您的请求，请换个方式试试。";
            }

            // 7. 流式输出最终回复（逐字模拟打字效果）
            streamTextByChar(finalText, writer);

            // 8. 保存回复到上下文
            if (userId != null) {
                contextManager.addMessage(userId, "assistant", finalText);
            }

            // 9. 发送完成标志
            sendSseEvent(writer, "done", "[DONE]");
            log.info("=== MCP 流式聊天完成 ===");

        } catch (Exception e) {
            log.error("MCP 流式聊天异常", e);
            try {
                writer.write("event: error\n");
                writer.write("data: {\"error\":\"" + e.getMessage() + "\"}\n\n");
                writer.flush();
            } catch (Exception ex) {
                // 忽略
            }
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
     * 构建消息列表，包含系统提示和上下文历史
     */
    private List<ChatMessage> buildMessages(Long userId, String currentMessage) {
        List<ChatMessage> messages = new ArrayList<>();

        // 系统提示
        messages.add(new SystemMessage(
            "你是\"小皮助手\"，一个智能电子商城购物助手。\n" +
            "你可以帮助用户搜索商品、查看商品详情和评价、浏览商品分类。\n" +
            "请根据用户需求调用合适的工具，用中文友好地回复。\n" +
            "调用工具后，请根据返回结果用自然语言总结告诉用户。"
        ));

        // 加入上下文历史（最近几轮对话）
        if (userId != null) {
            List<AiContextManager.ContextMessage> history = contextManager.getRecentMessages(userId, 6);
            for (AiContextManager.ContextMessage msg : history) {
                if ("user".equals(msg.getRole())) {
                    messages.add(new UserMessage(msg.getContent()));
                } else if ("assistant".equals(msg.getRole())) {
                    messages.add(AiMessage.from(msg.getContent()));
                }
            }
        }

        // 当前用户消息
        messages.add(new UserMessage(currentMessage));

        return messages;
    }

    /**
     * 根据工具名和参数执行工具
     */
    private String executeToolByName(String toolName, String arguments) {
        try {
            switch (toolName) {
                case "searchProducts": {
                    String keyword = extractJsonParam(arguments, "arg0");
                    if (keyword == null) keyword = extractJsonParam(arguments, "keyword");
                    return mallToolService.searchProducts(keyword);
                }
                case "getProductDetail": {
                    String idStr = extractJsonParam(arguments, "arg0");
                    if (idStr == null) idStr = extractJsonParam(arguments, "productId");
                    return mallToolService.getProductDetail(Long.parseLong(idStr));
                }
                case "getCategories":
                    return mallToolService.getCategories();
                case "getRecommendedProducts":
                    return mallToolService.getRecommendedProducts();
                case "getProductReviews": {
                    String idStr = extractJsonParam(arguments, "arg0");
                    if (idStr == null) idStr = extractJsonParam(arguments, "productId");
                    return mallToolService.getProductReviews(Long.parseLong(idStr));
                }
                case "addToCart": {
                    String userIdStr = extractJsonParam(arguments, "arg0");
                    if (userIdStr == null) userIdStr = extractJsonParam(arguments, "userId");
                    String goodIdStr = extractJsonParam(arguments, "arg1");
                    if (goodIdStr == null) goodIdStr = extractJsonParam(arguments, "goodId");
                    String countStr = extractJsonParam(arguments, "arg2");
                    if (countStr == null) countStr = extractJsonParam(arguments, "count");
                    String standard = extractJsonParam(arguments, "arg3");
                    if (standard == null) standard = extractJsonParam(arguments, "standard");
                    Integer count = countStr != null ? Integer.parseInt(countStr) : 1;
                    return mallToolService.addToCart(
                            userIdStr != null ? Long.parseLong(userIdStr) : null,
                            goodIdStr != null ? Long.parseLong(goodIdStr) : null,
                            count,
                            standard
                    );
                }
                case "viewCart": {
                    String userIdStr = extractJsonParam(arguments, "arg0");
                    if (userIdStr == null) userIdStr = extractJsonParam(arguments, "userId");
                    return mallToolService.viewCart(userIdStr != null ? Long.parseLong(userIdStr) : null);
                }
                case "createOrder": {
                    String userIdStr = extractJsonParam(arguments, "arg0");
                    if (userIdStr == null) userIdStr = extractJsonParam(arguments, "userId");
                    String goodsJson = extractJsonParam(arguments, "arg1");
                    if (goodsJson == null) goodsJson = extractJsonParam(arguments, "goodsJson");
                    String linkUser = extractJsonParam(arguments, "arg2");
                    if (linkUser == null) linkUser = extractJsonParam(arguments, "linkUser");
                    String linkPhone = extractJsonParam(arguments, "arg3");
                    if (linkPhone == null) linkPhone = extractJsonParam(arguments, "linkPhone");
                    String linkAddress = extractJsonParam(arguments, "arg4");
                    if (linkAddress == null) linkAddress = extractJsonParam(arguments, "linkAddress");
                    return mallToolService.createOrder(
                            userIdStr != null ? Long.parseLong(userIdStr) : null,
                            goodsJson,
                            linkUser,
                            linkPhone,
                            linkAddress
                    );
                }
                case "viewOrders": {
                    String userIdStr = extractJsonParam(arguments, "arg0");
                    if (userIdStr == null) userIdStr = extractJsonParam(arguments, "userId");
                    return mallToolService.viewOrders(userIdStr != null ? Long.parseLong(userIdStr) : null);
                }
                default:
                    return "{\"error\":\"未知工具：" + toolName + "\"}";
            }
        } catch (Exception e) {
            log.error("工具执行异常：{}", toolName, e);
            return "{\"error\":\"工具执行失败：" + e.getMessage() + "\"}";
        }
    }

    /**
     * 将 MCP 工具名映射为前端 action 类型
     */
    private String mapToolNameToAction(String toolName) {
        switch (toolName) {
            case "searchProducts": return "search";
            case "getProductDetail": return "viewGood";
            case "getRecommendedProducts": return "recommend";
            case "getProductReviews": return "viewGood";
            case "addToCart": return "addToCart";
            case "viewCart": return "viewCart";
            case "createOrder": return "createOrder";
            case "viewOrders": return "viewOrders";
            default: return toolName;
        }
    }

    /**
     * 解析工具结果为前端 actionData 格式
     */
    private Map<String, Object> parseToolResultToActionData(String toolName, String toolResult) {
        Map<String, Object> actionData = new HashMap<>();
        try {
            // 直接将工具返回的 JSON 作为 actionData
            Object parsed = JSONUtil.parseObj(toolResult);
            actionData.putAll((Map<? extends String, ?>) parsed);
        } catch (Exception e) {
            actionData.put("result", toolResult);
        }
        return actionData;
    }

    /**
     * 从 JSON 参数字符串中提取参数值
     */
    private String extractJsonParam(String json, String key) {
        try {
            Object obj = JSONUtil.parseObj(json).get(key);
            return obj != null ? obj.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 发送 SSE 事件
     */
    private void sendSseEvent(PrintWriter writer, String eventType, String data) {
        try {
            writer.write("event: " + eventType + "\n");
            writer.write("data: " + data + "\n\n");
            writer.flush();
        } catch (Exception e) {
            log.error("SSE 发送失败", e);
        }
    }

    /**
     * 逐字流式输出文本（模拟打字效果）
     */
    private void streamTextByChar(String text, PrintWriter writer) {
        if (text == null || text.isEmpty()) return;

        try {
            char[] chars = text.toCharArray();
            StringBuilder buffer = new StringBuilder();

            for (int i = 0; i < chars.length; i++) {
                buffer.append(chars[i]);

                // 每3个字符或遇到标点符号时发送一次
                if (i % 3 == 2 || chars[i] == '。' || chars[i] == '，' || chars[i] == '！' ||
                        chars[i] == '？' || chars[i] == '；' || chars[i] == '\n') {

                    Map<String, Object> map = new HashMap<>();
                    map.put("content", buffer.toString());
                    String json = JSONUtil.toJsonStr(map);

                    writer.write("event: message\n");
                    writer.write("data: " + json + "\n\n");
                    writer.flush();
                    buffer.setLength(0);

                    try { Thread.sleep(30); } catch (InterruptedException ignored) {}
                }
            }

            // 发送剩余内容
            if (buffer.length() > 0) {
                Map<String, Object> map = new HashMap<>();
                map.put("content", buffer.toString());
                String json = JSONUtil.toJsonStr(map);
                writer.write("event: message\n");
                writer.write("data: " + json + "\n\n");
                writer.flush();
            }
        } catch (Exception e) {
            log.error("流式输出异常", e);
        }
    }
}
