package com.rabbiter.em.ai.controller;

import com.rabbiter.em.ai.core.AiContextManager;
import com.rabbiter.em.ai.entity.ChatRequest;
import com.rabbiter.em.ai.entity.ChatResponse;
import com.rabbiter.em.ai.service.AiService;
import com.rabbiter.em.ai.service.SmartCustomerService;
import com.rabbiter.em.annotation.RateLimit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * AI 助手控制器
 */
@RestController
@RequestMapping("/api/ai")
public class AiAssistantController {
    
    private static final Logger log = LoggerFactory.getLogger(AiAssistantController.class);
    
    @Autowired
    private AiService aiService;
    
    @Autowired
    private AiContextManager contextManager;
    
    @Autowired
    private com.rabbiter.em.ai.service.AiBusinessService aiBusinessService;

    @Autowired
    private SmartCustomerService smartCustomerService;

    @Autowired
    private com.rabbiter.em.ai.service.AiAssistantService aiAssistantService;
    
    /**
     * 聊天对话（旧系统 - 规则引擎 + DeepSeek）
     * @param request 聊天请求
     * @return 聊天响应
     */
    @PostMapping("/chat")
    @RateLimit(prefix = "ai_chat", key = "#userId", maxRequests = 60, windowSeconds = 60, 
               message = "您发送消息太快了，请稍后再试")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        log.info("旧系统：/api/ai/chat → AiService (旧)");
        return aiService.chat(request);
    }
    
    /**
     * 流式聊天对话（旧系统 - PrintWriter 流式输出）
     */
    @PostMapping(value = "/chat/stream", produces = "text/event-stream;charset=UTF-8")
    public SseEmitter streamChat(@RequestBody(required = false) ChatRequest request, HttpServletResponse response) {
        if (request == null) {
            request = new ChatRequest();
            request.setMessage("你好");
        }
        log.info("旧系统: /api/ai/chat/stream → AiAssistantService (旧)");

        // ★ 关键修复：必须先设置编码，再获取 writer，否则中文会变成 ???
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/event-stream;charset=UTF-8");

        ChatRequest finalRequest = request;
        SseEmitter emitter = new SseEmitter(0L);
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                aiAssistantService.streamChatWithWriter(finalRequest, response.getWriter());
            } catch (Exception e) {
                log.error("旧系统流式处理异常", e);
                try {
                    response.getWriter().write("event: error\ndata: {\"error\":\"" + e.getMessage() + "\"}\n\n");
                    response.getWriter().flush();
                } catch (Exception ignored) {}
            }
        });
        return emitter;
    }

    /**
     * 获取对话历史
     */
    @GetMapping("/history")
    public Map<String, Object> getHistory(@RequestParam Long userId) {
        Map<String, Object> result = new HashMap<>();
        List<AiContextManager.ContextMessage> history = contextManager.getMessages(userId);
        result.put("success", true);
        result.put("data", history);
        return result;
    }

    /**
     * 清空对话历史
     */
    @PostMapping("/history/clear")
    public Map<String, Object> clearHistory(@RequestBody Map<String, Long> params) {
        Map<String, Object> result = new HashMap<>();
        Long userId = params.get("userId");
        if (userId != null) {
            contextManager.clearContext(userId);
            result.put("success", true);
            result.put("message", "对话历史已清空");
        } else {
            result.put("success", false);
            result.put("message", "用户 ID 不能为空");
        }
        return result;
    }

    /**
     * 个性化推荐接口
     * @param request 推荐请求（包含 userId）
     * @return 推荐结果
     */
    @PostMapping("/recommend")
    public ChatResponse recommend(@RequestBody ChatRequest request) {
        try {
            Long userId = request.getUserId();
            if (userId == null) {
                ChatResponse errorResponse = new ChatResponse();
                errorResponse.setSuccess(false);
                errorResponse.setMessage("用户 ID 不能为空");
                return errorResponse;
            }
            
            // 调用 AI 业务服务获取个性化推荐
            Object recommendationResult = aiService.getPersonalizedRecommendation(userId);
            
            // 构建响应
            ChatResponse response = new ChatResponse();
            response.setSuccess(true);
            response.setMessage("推荐成功");
            response.setAction("recommend");
            
            // 将推荐结果放在 actionData 中
            if (response.getActionData() == null) {
                response.setActionData(new HashMap<>());
            }
            response.getActionData().put("items", recommendationResult);
            
            return response;
        } catch (Exception e) {
            ChatResponse errorResponse = new ChatResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("推荐失败：" + e.getMessage());
            return errorResponse;
        }
    }
    
    /**
     * 获取快捷指令
     */
    private List<String> getQuickCommands(String action) {
        if (action == null) {
            return java.util.Arrays.asList("查看订单", "商品推荐", "售后服务", "常见问题");
        }
        
        switch (action) {
            case "viewOrders":
                return java.util.Arrays.asList("订单详情", "取消订单", "确认收货", "申请退款");
            case "search":
                return java.util.Arrays.asList("按价格排序", "只看有货", "商品对比", "查看详情");
            case "addToCart":
                return java.util.Arrays.asList("查看购物车", "继续购物", "去结算", "修改数量");
            case "recommend":
                return java.util.Arrays.asList("换一批", "加入购物车", "查看详情", "价格从低到高");
            default:
                return java.util.Arrays.asList("查看订单", "商品推荐", "售后服务", "常见问题");
        }
    }
    
    /**
     * 加载更多订单（用于订单分析分页）
     * @param params 包含 userId, offset, limit
     * @return 订单数据
     */
    @PostMapping("/orders/load-more")
    public Map<String, Object> loadMoreOrders(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = Long.valueOf(params.get("userId").toString());
            int offset = Integer.parseInt(params.get("offset").toString());
            int limit = Integer.parseInt(params.get("limit").toString());
            
            // 调用业务服务加载更多订单
            Map<String, Object> orderData = aiBusinessService.analyzeOrderHistory(userId, offset, limit);
            
            result.put("success", true);
            result.put("data", orderData);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "加载失败：" + e.getMessage());
        }
        return result;
    }
    
    /**
     * 加载更多订单跟踪（用于订单追踪分页）
     * @param params 包含 userId, page, limit
     * @return 订单数据
     */
    @PostMapping("/orders/track-more")
    public Map<String, Object> loadMoreTrackOrders(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = Long.valueOf(params.get("userId").toString());
            int page = Integer.parseInt(params.get("page").toString());
            int limit = Integer.parseInt(params.get("limit").toString());
            
            Object orderData = aiBusinessService.trackOrderStatus("最新订单", userId, page, limit);
            
            result.put("success", true);
            result.put("data", orderData);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "加载失败：" + e.getMessage());
        }
        return result;
    }
    
    /**
     * 健康检查
     * @return 健康状态
     */
    @RequestMapping("/health")
    public String health() {
        return "AI assistant service is running";
    }

    // ==================== 智能客服系统 API（RAG + ReAct + Skills + MCP）====================

    @PostMapping("/smart/chat")
    public ChatResponse smartChat(@RequestBody ChatRequest request) {
        log.info("[智能客服] 收到请求，用户: {}, 消息: {}", request.getUserId(), request.getMessage());
        return smartCustomerService.processMessage(request);
    }

    @PostMapping(value = "/smart/chat/stream", produces = "text/event-stream;charset=UTF-8")
    public SseEmitter smartStreamChat(@RequestBody ChatRequest request) {
        log.info("[智能客服-流式] 用户: {}, 消息: {}", request.getUserId(), request.getMessage());
        return smartCustomerService.streamProcessMessage(request);
    }

    @PostMapping("/smart/knowledge")
    public ChatResponse queryKnowledge(@RequestBody ChatRequest request) {
        log.info("[知识库] 查询，消息: {}", request.getMessage());
        return smartCustomerService.queryKnowledge(request);
    }

    @PostMapping("/smart/knowledge/{category}")
    public ChatResponse queryKnowledgeByCategory(
            @PathVariable String category,
            @RequestBody ChatRequest request) {
        log.info("[知识库] {} 分类查询，消息: {}", category, request.getMessage());
        return smartCustomerService.queryKnowledgeByCategory(request, category);
    }

    @GetMapping("/smart/knowledge/categories")
    public Map<String, Object> getKnowledgeCategories() {
        return smartCustomerService.getKnowledgeCategories();
    }

    @PostMapping("/smart/transfer/human")
    public ChatResponse transferToHuman(@RequestBody ChatRequest request) {
        log.info("[转人工] 用户: {}", request.getUserId());
        ChatResponse response = new ChatResponse();
        response.setSuccess(true);
        response.setMessage("正在为您转接人工客服，请稍候...");
        response.setAction("transfer_to_human");
        Map<String, Object> data = new HashMap<>();
        data.put("channels", List.of(
                Map.of("type", "online", "label", "在线客服", "desc", "点击右下角'联系客服'图标"),
                Map.of("type", "phone", "label", "客服热线", "desc", "400-800-8888"),
                Map.of("type", "service_hours", "label", "服务时间", "desc", "9:00-21:00")
        ));
        response.setActionData(data);
        return response;
    }
}
