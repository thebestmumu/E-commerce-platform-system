package com.rabbiter.em.ai.controller;

import com.rabbiter.em.ai.service.McpPurchaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * MCP 智能购买控制器 - 独立于AI对话和智能帮助
 * 专门用于通过MCP工具调用完成真实商品购买流程
 */
@RestController
@RequestMapping("/api/mcp/purchase")
public class McpPurchaseController {
    
    private static final Logger log = LoggerFactory.getLogger(McpPurchaseController.class);
    
    @Autowired
    private McpPurchaseService mcpPurchaseService;
    
    /**
     * MCP 智能购买流式对话
     * 用户通过自然语言描述购买需求，MCP自动完成推荐→选择→加购→下单→支付全流程
     * @param request 购买请求
     * @param response HTTP 响应
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public void purchaseChat(@RequestBody Map<String, Object> request, HttpServletResponse response) throws IOException {
        log.info("收到MCP购买请求：{}", request);
        
        response.setContentType("text/event-stream;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no");
        response.flushBuffer();
        
        mcpPurchaseService.streamPurchaseChat(request, response.getWriter());
    }
    
    /**
     * 获取MCP购买流程状态
     */
    @GetMapping("/status")
    public Map<String, Object> getPurchaseStatus(@RequestParam Long userId) {
        return mcpPurchaseService.getPurchaseStatus(userId);
    }
    
    /**
     * 取消当前购买流程
     */
    @PostMapping("/cancel")
    public Map<String, Object> cancelPurchase(@RequestBody Map<String, Long> params) {
        Long userId = params.get("userId");
        Map<String, Object> result = new HashMap<>();
        if (userId != null) {
            mcpPurchaseService.cancelPurchase(userId);
            result.put("success", true);
            result.put("message", "购买流程已取消");
        } else {
            result.put("success", false);
            result.put("message", "用户ID不能为空");
        }
        return result;
    }
}
