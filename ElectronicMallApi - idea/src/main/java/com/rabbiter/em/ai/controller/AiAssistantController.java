package com.rabbiter.em.ai.controller;

import com.rabbiter.em.ai.core.AiContextManager;
import com.rabbiter.em.ai.entity.ChatRequest;
import com.rabbiter.em.ai.entity.ChatResponse;
import com.rabbiter.em.ai.service.AiAssistantService;
import com.rabbiter.em.ai.service.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private AiAssistantService aiAssistantService;
    
    @Autowired
    private AiContextManager contextManager;
    
    @Autowired
    private com.rabbiter.em.ai.service.AiBusinessService aiBusinessService;
    
    /**
     * 聊天对话（新版）
     * @param request 聊天请求
     * @return 聊天响应
     */
    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        log.info("收到聊天请求，用户：{}, 消息：{}", request.getUserId(), request.getMessage());
        
        // 使用新的 AI 助手服务
        ChatResponse response = aiAssistantService.chat(request);
        
        // 添加快捷指令
        response.setQuickCommands(getQuickCommands(response.getAction()));
        
        log.info("返回响应：{}", response.getMessage());
        return response;
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
     * 流式聊天对话（使用原生 Servlet 实现，确保 SSE 正确工作）
     * @param request 聊天请求
     * @param response HTTP 响应
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public void streamChat(@RequestBody(required = false) ChatRequest request, HttpServletResponse response) throws IOException {
        // 如果没有请求体（GET 请求），创建一个默认请求
        if (request == null) {
            request = new ChatRequest();
            request.setMessage("你好");
        }
        
        // 设置 SSE 响应头
        response.setContentType("text/event-stream;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no"); // 禁用 Nginx 缓冲
        
        // 立即刷新响应头，确保客户端知道连接已建立
        response.flushBuffer();
        
        // 调用新的 AI 助手服务层，使用双路径（LangChain4j + 百度文心一言）
        aiAssistantService.streamChatWithWriter(request, response.getWriter());
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
}
