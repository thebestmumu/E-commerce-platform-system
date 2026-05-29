package com.rabbiter.em.ai.service;

import com.rabbiter.em.ai.core.AiContextManager;
import com.rabbiter.em.ai.core.AiIntent;
import com.rabbiter.em.ai.core.AiIntentResult;
import com.rabbiter.em.ai.core.RuleBasedIntentEngine;
import com.rabbiter.em.ai.entity.ChatRequest;
import com.rabbiter.em.ai.entity.ChatResponse;
import com.rabbiter.em.entity.Good;
import com.rabbiter.em.service.CartService;
import com.rabbiter.em.service.GoodService;
import com.rabbiter.em.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 助手核心服务
 * 整合意图识别、上下文管理、业务处理等功能
 * V2.0 - 基于 LangChain4j + 文心一言
 */
@Service
public class AiAssistantService {
    
    private static final Logger log = LoggerFactory.getLogger(AiAssistantService.class);
    
    @Autowired
    private RuleBasedIntentEngine intentEngine;
    
    @Autowired
    private AiContextManager contextManager;
    
    @Autowired
    private AiHelpService helpService;
    
    @Autowired
    private GoodService goodService;
    
    @Autowired
    private CartService cartService;
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private AiBusinessService aiBusinessService;
    
    // LangChain4j 服务（如果启用）
    @Autowired(required = false)
    private com.rabbiter.em.ai.service.LangChainAiService langChainAiService;

    // MCP 工具调用服务
    @Autowired(required = false)
    private McpStreamService mcpStreamService;

    @Value("${langchain4j.enabled:false}")
    private boolean langChainEnabled;

    @Value("${ai.mcp.enabled:false}")
    private boolean mcpEnabled;
    
    @Value("${ai.assistant.default-provider:langchain4j}")
    private String defaultProvider;
    
    @Value("${ai.baidu.api-key}")
    private String baiduApiKey;
    
    @Value("${ai.baidu.secret-key}")
    private String baiduSecretKey;
    
    /**
     * 处理聊天请求
     * @param request 聊天请求
     * @return 聊天响应
     */
    public ChatResponse chat(ChatRequest request) {
        Long userId = request.getUserId();
        String message = request.getMessage();
        
        log.info("========================================");
        log.info("收到 AI 请求");
        log.info("用户 ID: {}", userId);
        log.info("消息内容：{}", message);
        log.info("默认提供商：{}", defaultProvider);
        log.info("LangChain4j 启用状态：{}", langChainEnabled);
        log.info("LangChainAiService 是否可用：{}", langChainAiService != null);
        log.info("========================================");
        
        // 如果默认使用 LangChain4j
        if ("langchain4j".equals(defaultProvider) && langChainEnabled && langChainAiService != null) {
            log.info(">>> 选择策略：使用 LangChain4j + DeepSeek 处理请求（主方案）");
            log.info(">>> 路径：AiAssistantService -> LangChainAiService -> DeepSeek API");
            try {
                ChatResponse response = handleWithLangChain4j(request);
                log.info("<<< LangChain4j 处理成功");
                log.info("<<< 返回消息：{}", response.getMessage() != null ? response.getMessage().substring(0, Math.min(50, response.getMessage().length())) + "..." : "null");
                return response;
            } catch (Exception e) {
                log.warn("<<< LangChain4j 处理失败，准备降级到百度文心一言");
                log.warn("<<< 失败原因：{}", e.getMessage());
                log.warn("<<< 降级路径：AiAssistantService -> handleWithBaidu -> 百度文心一言");
                // 降级到百度文心一言
                return handleWithBaidu(request);
            }
        }
        
        // 否则使用百度文心一言
        log.info(">>> 选择策略：使用百度文心一言处理请求（备用方案）");
        log.info(">>> 路径：AiAssistantService -> handleWithBaidu -> 百度文心一言 -> 规则引擎");
        ChatResponse response = handleWithBaidu(request);
        log.info("<<< 百度文心一言处理完成");
        log.info("<<< 返回消息：{}", response.getMessage() != null ? response.getMessage().substring(0, Math.min(50, response.getMessage().length())) + "..." : "null");
        return response;
    }
    
    /**
     * 使用 LangChain4j 处理请求
     */
    private ChatResponse handleWithLangChain4j(ChatRequest request) {
        try {
            log.info("========================================");
            log.info("handleWithLangChain4j 开始处理");
            log.info("用户 ID: {}", request.getUserId());
            log.info("消息内容：{}", request.getMessage());
            log.info("========================================");
            
            // 1. 保存用户消息到上下文
            log.info(">>> 步骤 1：保存用户消息到上下文");
            contextManager.addMessage(request.getUserId(), "user", request.getMessage());
            log.info("<<< 上下文保存完成");
            
            // 2. 识别意图
            log.info(">>> 步骤 2：识别意图");
            AiIntentResult intentResult = intentEngine.recognizeIntent(request);
            log.info("<<< 意图识别完成");
            log.info("<<< 识别意图：{}", intentResult != null ? intentResult.getIntent() : "null");
            log.info("<<< 置信度：{}", intentResult != null ? intentResult.getConfidence() : "null");
            
            // 3. 根据意图处理
            log.info(">>> 步骤 3：根据意图处理");
            ChatResponse response = processIntent(request.getUserId(), intentResult);
            log.info("<<< 业务处理完成");
            log.info("<<< 响应消息：{}", response.getMessage() != null ? response.getMessage().substring(0, Math.min(50, response.getMessage().length())) + "..." : "null");
            log.info("<<< 响应动作：{}", response.getAction());
            
            // 4. 保存 AI 响应到上下文
            log.info(">>> 步骤 4：保存 AI 响应到上下文");
            contextManager.addMessage(request.getUserId(), "assistant", response.getMessage());
            log.info("<<< 上下文保存完成");
            
            // 5. 附加上下文信息
            log.info(">>> 步骤 5：附加上下文信息");
            response.setContext(intentResult);
            log.info("<<< 上下文附加完成");
            
            log.info("========================================");
            log.info("handleWithLangChain4j 处理完成");
            log.info("========================================");
            
            return response;
            
        } catch (Exception e) {
            log.error("LangChain4j 处理失败：{}", e.getMessage(), e);
            throw e; // 抛出异常，触发降级
        }
    }
    
    /**
     * 使用百度文心一言处理请求（备用方案）
     */
    private ChatResponse handleWithBaidu(ChatRequest request) {
        log.info("========================================");
        log.info("handleWithBaidu 开始处理");
        log.info("用户 ID: {}", request.getUserId());
        log.info("消息内容：{}", request.getMessage());
        log.info("========================================");
        
        try {
            // 1. 保存用户消息到上下文
            log.info(">>> 步骤 1：保存用户消息到上下文");
            contextManager.addMessage(request.getUserId(), "user", request.getMessage());
            log.info("<<< 上下文保存完成");
            
            // 2. 识别意图
            log.info(">>> 步骤 2：识别意图");
            AiIntentResult intentResult = intentEngine.recognizeIntent(request);
            log.info("<<< 意图识别完成");
            log.info("<<< 识别意图：{}", intentResult != null ? intentResult.getIntent() : "null");
            log.info("<<< 置信度：{}", intentResult != null ? intentResult.getConfidence() : "null");
            
            // 3. 根据意图处理
            log.info(">>> 步骤 3：根据意图处理");
            ChatResponse response = processIntent(request.getUserId(), intentResult);
            log.info("<<< 业务处理完成");
            log.info("<<< 响应消息：{}", response.getMessage() != null ? response.getMessage().substring(0, Math.min(50, response.getMessage().length())) + "..." : "null");
            log.info("<<< 响应动作：{}", response.getAction());
            
            // 4. 保存 AI 响应到上下文
            log.info(">>> 步骤 4：保存 AI 响应到上下文");
            contextManager.addMessage(request.getUserId(), "assistant", response.getMessage());
            log.info("<<< 上下文保存完成");
            
            // 5. 附加上下文信息
            log.info(">>> 步骤 5：附加上下文信息");
            response.setContext(intentResult);
            log.info("<<< 上下文附加完成");
            
            log.info("========================================");
            log.info("handleWithBaidu 处理完成");
            log.info("========================================");
            
            return response;
            
        } catch (Exception e) {
            log.error("========================================");
            log.error("handleWithBaidu 处理失败");
            log.error("错误类型：{}", e.getClass().getSimpleName());
            log.error("错误信息：{}", e.getMessage());
            log.error("========================================");
            log.error("堆栈跟踪：", e);
            throw e;
        }
    }
    
    /**
     * 根据意图处理请求
     */
    private ChatResponse processIntent(Long userId, AiIntentResult intentResult) {
        AiIntent intent = intentResult.getIntent();
        
        log.info("处理意图：{} - {}", intent.getCode(), intent.getDescription());
        
        switch (intent) {
            // ===== 商品相关 =====
            case SEARCH_GOODS:
                return handleSearchGoods(userId, intentResult);
            case RECOMMEND_GOODS:
                return handleRecommendGoods(userId, intentResult);
            case SPECIFIC_RECOMMEND:
                return handleSpecificRecommend(userId, intentResult);
            case VIEW_GOOD_DETAIL:
                return handleViewGoodDetail(userId, intentResult);
            case CHECK_STOCK:
                return handleCheckStock(userId, intentResult);
            
            // ===== 购物车相关 =====
            case ADD_TO_CART:
                return handleAddToCart(userId, intentResult);
            case VIEW_CART:
                return handleViewCart(userId, intentResult);
            
            // ===== 订单相关 =====
            case QUERY_ORDERS:
                return handleQueryOrders(userId, intentResult);
            case QUERY_ORDER_DETAIL:
                return handleQueryOrderDetail(userId, intentResult);
            case ORDER_STATISTICS:
                return handleOrderStatistics(userId, intentResult);
            case ANALYZE_ORDERS:
                return handleAnalyzeOrders(userId, intentResult);
            
            // ===== 帮助相关 =====
            case FAQ:
                return handleFaq(userId, intentResult);
            case AFTER_SALES:
                return handleAfterSales(userId, intentResult);
            case TUTORIAL:
                return handleTutorial(userId, intentResult);
            
            // ===== 通用 =====
            case CHAT:
                return handleChat(userId, intentResult);
            
            // ===== 未知意图 =====
            case UNKNOWN:
            default:
                return handleUnknown(userId, intentResult);
        }
    }
    
    /**
     * 处理商品搜索
     */
    private ChatResponse handleSearchGoods(Long userId, AiIntentResult intentResult) {
        String keyword = (String) intentResult.getParameters().get("keyword");
        
        ChatResponse response = new ChatResponse();
        response.setSuccess(true);
        
        if (keyword != null && !keyword.isEmpty()) {
            log.info("搜索商品，关键词：{}", keyword);
            
            // 调用业务服务搜索商品
            Object searchResult = aiBusinessService.search(keyword);
            
            response.setMessage("为您找到以下与\"" + keyword + "\"相关的商品：");
            response.setAction("search_goods");
            response.setActionData(new HashMap<String, Object>() {{
                put("keyword", keyword);
                put("goods", searchResult);
            }});
        } else {
            response.setMessage("请问您想搜索什么商品呢？");
        }
        
        return response;
    }
    
    /**
     * 处理商品推荐
     */
    private ChatResponse handleRecommendGoods(Long userId, AiIntentResult intentResult) {
        ChatResponse response = new ChatResponse();
        response.setSuccess(true);
        
        // 调用业务服务获取推荐商品
        Object recommendResult = aiBusinessService.getPersonalizedRecommendation(userId);
        
        if (recommendResult != null && recommendResult instanceof java.util.List) {
            java.util.List<?> goodsList = (java.util.List<?>) recommendResult;
            
            // 构建包含商品ID的回复消息，方便后续"第x个"功能使用
            StringBuilder msgBuilder = new StringBuilder();
            msgBuilder.append("根据您的喜好，为您推荐以下").append(goodsList.size()).append("个热门商品：");
            for (int i = 0; i < goodsList.size() && i < 10; i++) {
                Object goodObj = goodsList.get(i);
                Object goodIdObj = null;
                String goodName = "";
                if (goodObj instanceof com.rabbiter.em.entity.Good) {
                    com.rabbiter.em.entity.Good good = (com.rabbiter.em.entity.Good) goodObj;
                    goodIdObj = good.getId();
                    goodName = good.getName();
                } else if (goodObj instanceof java.util.Map) {
                    java.util.Map<String, Object> goodMap = (java.util.Map<String, Object>) goodObj;
                    goodIdObj = goodMap.get("id");
                    if (goodIdObj == null) goodIdObj = goodMap.get("goodId");
                    Object nameObj = goodMap.get("name");
                    goodName = nameObj != null ? nameObj.toString() : "";
                }
                msgBuilder.append("\n").append(i + 1).append(". ").append(goodName);
                if (goodIdObj != null) {
                    msgBuilder.append(" - 商品ID:").append(goodIdObj);
                }
            }
            
            response.setMessage(msgBuilder.toString());
            response.setAction("recommend_goods");
            response.setActionData(new HashMap<String, Object>() {{
                put("goods", goodsList);
                put("count", goodsList.size());
            }});
        } else {
            response.setMessage("抱歉，暂时没有为您推荐的商品。");
            response.setAction("recommend_goods");
        }
        
        return response;
    }
    
    /**
     * 处理具体品类推荐
     */
    private ChatResponse handleSpecificRecommend(Long userId, AiIntentResult intentResult) {
        log.info("处理具体品类推荐，参数：{}", intentResult.getParameters());
        
        // 直接调用 AiBusinessService.executeBusiness 来处理推荐逻辑
        ChatResponse response = aiBusinessService.executeBusiness(intentResult, userId);
        
        log.info("具体品类推荐完成，action: {}, has actionData: {}", 
                 response.getAction(), response.getActionData() != null);
        
        return response;
    }
    
    /**
     * 处理查看商品详情
     */
    private ChatResponse handleViewGoodDetail(Long userId, AiIntentResult intentResult) {
        ChatResponse response = new ChatResponse();
        response.setSuccess(true);
        
        Object goodIdObj = intentResult.getParameters().get("goodId");
        
        // 如果没有 goodId，但有 index，从上下文中获取商品列表
        if (goodIdObj == null && intentResult.getParameters().containsKey("index")) {
            int index = (Integer) intentResult.getParameters().get("index");
            Long goodId = getGoodIdFromContextByIndex(userId, index);
            
            if (goodId != null) {
                goodIdObj = goodId;
                log.info("从上下文中获取第 {} 个商品的 ID: {}", index, goodId);
            }
        }
        
        if (goodIdObj != null) {
            final String finalGoodId = goodIdObj.toString();
            response.setMessage("正在为您加载商品详情...");
            response.setAction("viewGood");
            response.setActionData(new HashMap<String, Object>() {{
                put("goodId", finalGoodId);
            }});
        } else {
            response.setMessage("请问您想查看哪个商品的详情呢？");
        }
        
        return response;
    }
    
    /**
     * 处理库存查询
     */
    private ChatResponse handleCheckStock(Long userId, AiIntentResult intentResult) {
        ChatResponse response = new ChatResponse();
        response.setSuccess(true);
        response.setMessage("正在为您查询库存信息...");
        return response;
    }
    
    /**
     * 处理添加到购物车
     */
    private ChatResponse handleAddToCart(Long userId, AiIntentResult intentResult) {
        ChatResponse response = new ChatResponse();
        response.setSuccess(true);
        
        Object goodIdObj = intentResult.getParameters().get("goodId");
        Object countObj = intentResult.getParameters().get("count");
        
        // 如果没有 goodId，但有 index，从上下文中获取商品列表
        if (goodIdObj == null && intentResult.getParameters().containsKey("index")) {
            int index = (Integer) intentResult.getParameters().get("index");
            Long goodId = getGoodIdFromContextByIndex(userId, index);
            
            if (goodId != null) {
                goodIdObj = goodId;
                log.info("从上下文中获取第 {} 个商品的 ID: {}", index, goodId);
            }
        }
        
        if (goodIdObj != null) {
            final String finalGoodId = goodIdObj.toString();
            int count = countObj != null ? (Integer) countObj : 1;
            response.setMessage("正在将商品添加到购物车（数量：" + count + "）...");
            response.setAction("addToCart");
            response.setActionData(new HashMap<String, Object>() {{
                put("goodId", finalGoodId);
                put("count", count);
            }});
        } else {
            response.setMessage("请问您想将哪个商品加入购物车呢？");
        }
        
        return response;
    }
    
    /**
     * 处理查看购物车
     */
    private ChatResponse handleViewCart(Long userId, AiIntentResult intentResult) {
        ChatResponse response = new ChatResponse();
        response.setSuccess(true);
        response.setMessage("正在为您加载购物车...");
        response.setAction("viewCart");
        return response;
    }
    
    /**
     * 处理查询订单
     */
    private ChatResponse handleQueryOrders(Long userId, AiIntentResult intentResult) {
        log.info("查询订单，用户 ID: {}", userId);
        
        // 调用业务服务查询订单
        Object ordersResult = aiBusinessService.queryOrders(userId.intValue());
        
        ChatResponse response = new ChatResponse();
        response.setSuccess(true);
        
        if (ordersResult instanceof List && !((List<?>) ordersResult).isEmpty()) {
            int orderCount = ((List<?>) ordersResult).size();
            response.setMessage("为您找到最近 " + orderCount + " 个订单：");
            response.setAction("query_orders");
            response.setActionData(new HashMap<String, Object>() {{
                put("orders", ordersResult);
                put("count", orderCount);
            }});
        } else {
            response.setMessage("您暂无订单记录。");
        }
        
        return response;
    }
    
    /**
     * 处理查询订单详情
     */
    private ChatResponse handleQueryOrderDetail(Long userId, AiIntentResult intentResult) {
        ChatResponse response = new ChatResponse();
        response.setSuccess(true);
        
        Object orderNo = intentResult.getParameters().get("orderNo");
        if (orderNo != null) {
            response.setMessage("正在为您查询订单 " + orderNo + " 的详情...");
            response.setAction("viewOrderDetail");
            response.setActionData(new HashMap<String, Object>() {{
                put("orderNo", orderNo.toString());
            }});
        } else {
            response.setMessage("请问您想查询哪个订单的详情呢？请提供订单号。");
        }
        
        return response;
    }
    
    /**
     * 处理订单统计
     */
    private ChatResponse handleOrderStatistics(Long userId, AiIntentResult intentResult) {
        ChatResponse response = new ChatResponse();
        response.setSuccess(true);
        response.setMessage("正在为您分析订单数据和消费统计...");
        response.setAction("orderAnalytics");
        return response;
    }
    
    /**
     * 处理订单分析
     */
    private ChatResponse handleAnalyzeOrders(Long userId, AiIntentResult intentResult) {
        log.info("处理订单分析，用户 ID: {}", userId);
        
        // 调用业务服务分析订单
        Map<String, Object> analysisResult = aiBusinessService.analyzeOrderHistory(userId);
        
        ChatResponse response = new ChatResponse();
        response.setSuccess(true);
        
        if (analysisResult != null && Boolean.TRUE.equals(analysisResult.get("success"))) {
            int totalOrders = (int) analysisResult.get("totalOrders");
            response.setMessage("为您分析了最近 " + totalOrders + " 个订单的数据：");
            response.setAction("analyze_orders");
            response.setActionData(analysisResult);
        } else {
            response.setMessage("订单分析失败，请稍后重试。");
        }
        
        return response;
    }
    
    /**
     * 处理常见问题
     */
    private ChatResponse handleFaq(Long userId, AiIntentResult intentResult) {
        ChatResponse response = new ChatResponse();
        response.setSuccess(true);
        
        String question = intentResult.getOriginalQuery();
        List<AiHelpService.FAQ> faqs = helpService.searchFaqs(question);
        
        if (!faqs.isEmpty()) {
            AiHelpService.FAQ faq = faqs.get(0);
            response.setMessage(faq.getAnswer());
            response.setFaq(faq);
        } else {
            response.setMessage("抱歉，我没有找到相关问题的答案。您可以尝试换个问法，或者联系人工客服。");
        }
        
        return response;
    }
    
    /**
     * 处理售后服务
     */
    private ChatResponse handleAfterSales(Long userId, AiIntentResult intentResult) {
        ChatResponse response = new ChatResponse();
        response.setSuccess(true);
        response.setMessage("请问您遇到了什么售后问题？我可以为您提供以下帮助：\n" +
                "1. 退货退款\n" +
                "2. 换货申请\n" +
                "3. 维修保修\n" +
                "4. 投诉建议\n\n" +
                "请详细描述您的问题，我会尽力为您解答。");
        return response;
    }
    
    /**
     * 处理操作指导
     */
    private ChatResponse handleTutorial(Long userId, AiIntentResult intentResult) {
        ChatResponse response = new ChatResponse();
        response.setSuccess(true);
        response.setMessage("我可以为您提供以下操作指导：\n" +
                "1. 如何下单购物\n" +
                "2. 如何支付订单\n" +
                "3. 如何申请退款\n" +
                "4. 如何查看物流\n" +
                "5. 如何修改个人信息\n\n" +
                "请问您需要了解哪个操作的详细步骤？");
        return response;
    }
    
    /**
     * 从消息中提取商品索引
     * 如"第一个" → 1, "第二个" → 2, "第3个" → 3
     */
    private Integer extractIndexFromMessage(String message) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("第([\\d一二三四五六七八九十]+)[款个]");
        java.util.regex.Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            String numStr = matcher.group(1);
            return parseChineseNumber(numStr);
        }
        return null;
    }
    
    /**
     * 将中文数字转换为阿拉伯数字
     */
    private int parseChineseNumber(String chineseNum) {
        switch (chineseNum) {
            case "一": return 1;
            case "二": return 2;
            case "三": return 3;
            case "四": return 4;
            case "五": return 5;
            case "六": return 6;
            case "七": return 7;
            case "八": return 8;
            case "九": return 9;
            case "十": return 10;
            default:
                try {
                    return Integer.parseInt(chineseNum);
                } catch (NumberFormatException e) {
                    return 1;
                }
        }
    }
    
    /**
     * 从上下文中根据索引获取商品 ID
     * @param userId 用户 ID
     * @param index 商品索引（1-based）
     * @return 商品 ID
     */
    private Long getGoodIdFromContextByIndex(Long userId, int index) {
        try {
            log.info("开始从上下文中获取第 {} 个商品的 ID", index);
            
            // 获取对话上下文
            List<AiContextManager.ContextMessage> contextMessages = contextManager.getMessages(userId);
            if (contextMessages == null || contextMessages.isEmpty()) {
                log.warn("没有可用的上下文消息");
                return null;
            }
            
            log.info("上下文消息总数: {}", contextMessages.size());
            
            // 从最近到最远遍历 AI 消息，找到第一个包含商品列表的推荐上下文
            // 因为中间可能夹杂着"分析报告"、"订单查询"等不含商品 ID 的回复
            // 如果只取最后一条 AI 消息，会错过更早的推荐记录
            String lastAiMessage = null;
            for (int i = contextMessages.size() - 1; i >= 0; i--) {
                log.info("消息 {}: role={}, content={}", i, contextMessages.get(i).getRole(), 
                    contextMessages.get(i).getContent() != null ? contextMessages.get(i).getContent().substring(0, Math.min(100, contextMessages.get(i).getContent().length())) : "null");
                if ("assistant".equals(contextMessages.get(i).getRole())) {
                    String content = contextMessages.get(i).getContent();
                    if (content != null && (content.contains("【推荐商品列表】") || content.contains("商品ID:"))) {
                        lastAiMessage = content;
                        log.info("找到包含商品信息的 AI 消息: {}", content.substring(0, Math.min(100, content.length())));
                        break;
                    }
                }
            }
            
            if (lastAiMessage == null) {
                log.warn("没有找到 AI 回复消息");
                return null;
            }
            
            log.info("最后一条 AI 消息: {}", lastAiMessage);
            
            // 从 AI 消息中提取商品 ID
            java.util.List<Long> goodIds = new java.util.ArrayList<>();
            
            // 方式一：解析结构化格式 【推荐商品列表】[id1,id2,id3,...]
            // 这种格式由 recommend/specific_recommend 保存，更精确可靠
            try {
                java.util.regex.Pattern structPattern = java.util.regex.Pattern.compile("【推荐商品列表】\\[(.*?)\\]");
                java.util.regex.Matcher structMatcher = structPattern.matcher(lastAiMessage);
                if (structMatcher.find()) {
                    String jsonArray = structMatcher.group(1);
                    log.info("找到结构化推荐列表: {}", jsonArray);
                    if (jsonArray != null && !jsonArray.isEmpty()) {
                        String[] parts = jsonArray.split(",");
                        for (String part : parts) {
                            String trimmed = part.trim();
                            if (!trimmed.isEmpty()) {
                                try {
                                    goodIds.add(Long.parseLong(trimmed));
                                } catch (NumberFormatException e) {
                                    log.warn("解析商品ID失败: {}", trimmed);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("解析结构化推荐列表失败，尝试旧格式: {}", e.getMessage());
            }
            
            // 方式二：如果结构化格式没找到，回退到旧格式 "商品ID:123"
            if (goodIds.isEmpty()) {
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("商品ID:(\\d+)");
                java.util.regex.Matcher matcher = pattern.matcher(lastAiMessage);
                while (matcher.find()) {
                    goodIds.add(Long.parseLong(matcher.group(1)));
                }
            }
            
            log.info("提取到的商品 ID 列表: {}", goodIds);
            
            if (goodIds.isEmpty()) {
                log.warn("从上下文中没有找到商品 ID");
                return null;
            }
            
            // index 是 1-based（用户说"第一个"），转换为 0-based（数组索引）
            int zeroBasedIndex = index - 1;
            if (zeroBasedIndex < 0 || zeroBasedIndex >= goodIds.size()) {
                log.warn("索引 {} 超出范围，商品总数: {}", index, goodIds.size());
                return null;
            }
            
            log.info("用户说'第{}个'，对应数组索引: {}，商品 ID: {}", index, zeroBasedIndex, goodIds.get(zeroBasedIndex));
            return goodIds.get(zeroBasedIndex);
            
        } catch (Exception e) {
            log.error("从上下文中获取商品 ID 失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 处理闲聊
     */
    private ChatResponse handleChat(Long userId, AiIntentResult intentResult) {
        ChatResponse response = new ChatResponse();
        response.setSuccess(true);
        
        String message = intentResult.getOriginalQuery().toLowerCase();
        
        if (message.contains("你好") || message.contains("您好") || message.contains("hello") || message.contains("hi")) {
            response.setMessage("您好！我是小皮助手，很高兴为您服务。请问有什么可以帮助您的吗？\n\n" +
                    "您可以问我：\n" +
                    "• 推荐一些商品\n" +
                    "• 查询我的订单\n" +
                    "• 如何申请退款\n" +
                    "• 商品有货吗\n" +
                    "• 运费是多少");
        } else if (message.contains("谢谢") || message.contains("感谢")) {
            response.setMessage("不客气！这是我应该做的。如有其他问题，随时可以问我哦！😊");
        } else {
            response.setMessage("您好！我是小皮助手，很高兴为您服务。请问有什么可以帮助您的吗？");
        }
        
        return response;
    }
    
    /**
     * 处理未知意图
     */
    private ChatResponse handleUnknown(Long userId, AiIntentResult intentResult) {
        ChatResponse response = new ChatResponse();
        response.setSuccess(true);
        
        if (intentResult.isNeedClarification()) {
            response.setMessage(intentResult.getClarificationQuestion());
        } else {
            response.setMessage("抱歉，我没有理解您的意思。您可以问我：\n" +
                    "• 商品推荐\n" +
                    "• 订单查询\n" +
                    "• 购物车操作\n" +
                    "• 售后服务\n" +
                    "• 常见问题\n\n" +
                    "请问您具体想了解什么呢？");
        }
        
        return response;
    }
    
    /**
     * 获取用户的对话历史
     */
    public List<AiContextManager.ContextMessage> getHistory(Long userId) {
        return contextManager.getMessages(userId);
    }
    
    /**
     * 清空对话历史
     */
    public void clearHistory(Long userId) {
        contextManager.clearContext(userId);
    }
    
    /**
     * 流式聊天对话（DeepSeek + LangChain4j 主路径，百度备选）
     * @param request 聊天请求
     * @param writer PrintWriter 用于写入响应流
     */
    public void streamChatWithWriter(ChatRequest request, java.io.PrintWriter writer) {
        try {
            System.out.println("=== 开始流式聊天（DeepSeek 主路径版本）===");
            System.out.println("用户消息：" + request.getMessage());
            System.out.println("LangChain4j 启用状态：" + langChainEnabled);
            System.out.println("LangChainAiService 是否可用：" + (langChainAiService != null));
            System.out.println("MCP 启用状态：" + mcpEnabled);
            System.out.println("McpStreamService 是否可用：" + (mcpStreamService != null));

            // MCP 工具调用模式（优先级最高）
            String mode = request.getMode();
            if (mcpEnabled && mcpStreamService != null && !"help".equals(mode)) {
                System.out.println(">>> 选择策略：使用 MCP 工具调用模式处理请求");
                try {
                    mcpStreamService.streamChat(request, writer);
                    System.out.println("<<< MCP 流式处理成功");
                    return;
                } catch (Exception e) {
                    System.out.println("<<< MCP 流式处理失败，降级到传统模式");
                    System.out.println("<<< 失败原因：" + e.getMessage());
                    // 继续走原有的处理逻辑
                }
            }
            
            // 如果是智能帮助模式，使用 DeepSeek 进行完整的 AI 意图识别流程
            if ("help".equals(mode)) {
                System.out.println("智能帮助模式：使用 DeepSeek 进行意图识别");
                handleHelpModeWithStreaming(request, writer);
                return;
            }
            
            // 对话模式：优先使用 DeepSeek，失败降级到百度
            if (langChainEnabled && langChainAiService != null) {
                System.out.println(">>> 选择策略：使用 DeepSeek + LangChain4j 处理请求（主方案）");
                System.out.println(">>> 路径：AiAssistantService -> LangChainAiService -> DeepSeek API");
                try {
                    streamWithLangChain4j(request, writer);
                    System.out.println("<<< DeepSeek 流式处理成功");
                } catch (Exception e) {
                    System.out.println("<<< DeepSeek 流式处理失败，降级到百度文心一言");
                    System.out.println("<<< 失败原因：" + e.getMessage());
                    System.out.println("<<< 降级路径：AiAssistantService -> streamWithBaidu -> 百度文心一言");
                    streamWithBaidu(request, writer);
                }
            } else {
                System.out.println(">>> 选择策略：使用百度文心一言处理请求（备用方案）");
                System.out.println(">>> 路径：AiAssistantService -> streamWithBaidu -> 百度文心一言");
                streamWithBaidu(request, writer);
            }
            
            System.out.println("=== 流式聊天完成 ===");
            
        } catch (Exception e) {
            System.err.println("流式聊天异常：" + e.getMessage());
            e.printStackTrace();
            try {
                writer.write("event: error\n");
                writer.write("data: {\"error\":\"处理失败\"}\n\n");
                writer.flush();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }
    
    /**
     * 智能帮助模式（流式版本）- 使用 DeepSeek 完整复用原有的 AI 意图识别逻辑
     */
    private void handleHelpModeWithStreaming(ChatRequest request, java.io.PrintWriter writer) {
        try {
            System.out.println("=== 开始智能帮助模式（DeepSeek 版本）===");
            
            // 0. 保存用户消息到上下文
            if (request.getUserId() != null) {
                contextManager.addMessage(request.getUserId(), "user", request.getMessage());
                System.out.println("用户消息已保存到上下文");
            }
            
            // 1. 使用 DeepSeek 进行意图识别（复用原有的大段 prompt 逻辑）
            String intent = recognizeIntentWithDeepSeek(request.getMessage());
            System.out.println("识别出的用户意图：" + intent);
            
            // 2. 如果用户在进行聊天，直接进入对话模式
            if ("chat".equals(intent)) {
                System.out.println("用户意图为聊天，进入对话模式（DeepSeek）");
                // 直接使用 DeepSeek 对话，不经过规则引擎重新识别（避免简短商品词误判）
                try {
                    String aiResponse = langChainAiService.chat(request.getUserId(), request.getMessage());
                    if (aiResponse != null && !aiResponse.isEmpty()) {
                        streamAiResponse(aiResponse, writer);
                    }
                } catch (Exception e) {
                    System.out.println("DeepSeek 对话失败：" + e.getMessage());
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("content", "抱歉，我暂时无法回复您的问题。");
                    String json = cn.hutool.json.JSONUtil.toJsonStr(map);
                    writer.write("event: message\n");
                    writer.write("data: " + json + "\n\n");
                }
                writer.write("event: done\n");
                writer.write("data: [DONE]\n\n");
                writer.flush();
                return;
            }
            
            // 3. 提取实体
            System.out.println("开始提取实体");
            Object entities = extractEntities(request.getMessage(), intent);
            System.out.println("提取到的实体：" + entities);
            
            // 4. 执行业务操作并流式输出（使用新的流式方法）
            // 对于不需要提取实体的意图（如 analyze_orders、query_orders、track_order、analyze_sentiment、analyze_sales、view_good_detail、add_to_cart、quick_order），直接执行业务逻辑
            if (!"unknown".equals(intent) && (entities != null || "analyze_orders".equals(intent) || "query_orders".equals(intent) || "track_order".equals(intent) || "analyze_sentiment".equals(intent) || "analyze_sales".equals(intent) || "view_good_detail".equals(intent) || "add_to_cart".equals(intent) || "quick_order".equals(intent))) {
                System.out.println("开始执行业务操作（流式版本）：" + intent);
                
                // 特殊处理订单分析（不需要提取实体）
                if ("analyze_orders".equals(intent)) {
                    System.out.println("执行订单分析操作");
                    
                    // 1. 先发送思考过程
                    System.out.println("发送思考过程");
                    java.util.List<String> thinkingSteps = new java.util.ArrayList<>();
                    thinkingSteps.add("识别用户意图：分析订单历史");
                    thinkingSteps.add("查询用户订单数据");
                    thinkingSteps.add("统计订单状态分布");
                    thinkingSteps.add("生成消费分析报告");
                    
                    java.util.Map<String, Object> thinkMap = new java.util.HashMap<>();
                    thinkMap.put("thinkingSteps", thinkingSteps);
                    thinkMap.put("type", "thinking");
                    String thinkJson = cn.hutool.json.JSONUtil.toJsonStr(thinkMap);
                    writer.write("event: message\n");
                    writer.write("data: " + thinkJson + "\n\n");
                    writer.flush();
                    
                    // 等待 300ms
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    
                    // 2. 直接调用业务服务，不重新识别意图
                    java.util.Map<String, Object> orderData = aiBusinessService.analyzeOrderHistory(request.getUserId());
                    
                    // 3. 发送 action 给前端
                    if (orderData != null) {
                        System.out.println("发送订单分析 action");
                        java.util.Map<String, Object> actionMap = new java.util.HashMap<>();
                        actionMap.put("action", "analyze_orders");
                        actionMap.put("actionData", orderData);
                        String actionJson = cn.hutool.json.JSONUtil.toJsonStr(actionMap);
                        writer.write("event: action\n");
                        writer.write("data: " + actionJson + "\n\n");
                        writer.flush();
                        
                        // 流式输出 AI 回复
                        String aiResponse = "已为您分析订单数据，请查看下方的分析报告。";
                        streamAiResponse(aiResponse, writer);
                    }
                    
                    // 发送 done 标志
                    writer.write("event: done\n");
                    writer.write("data: [DONE]\n\n");
                    writer.flush();
                    return;
                }
                
                // 特殊处理评论分析和销售分析（绕过规则引擎）
                if ("analyze_sentiment".equals(intent) || "analyze_sales".equals(intent)) {
                    System.out.println("执行评论/销售分析操作");
                    
                    // 先检查是否是"第x个"场景，从上下文中获取商品ID
                    Integer analysisIndex = extractIndexFromMessage(request.getMessage());
                    Long resolvedGoodId = null;
                    
                    if (analysisIndex != null) {
                        // 从上下文中获取商品ID
                        resolvedGoodId = getGoodIdFromContextByIndex(request.getUserId(), analysisIndex);
                        if (resolvedGoodId != null) {
                            System.out.println("第" + analysisIndex + "个 -> 从上下文解析商品ID: " + resolvedGoodId);
                        }
                    }
                    
                    // 直接调用业务服务
                    java.util.Map<String, Object> params = (java.util.Map<String, Object>) entities;
                    Object result = null;
                    
                    if ("analyze_sentiment".equals(intent)) {
                        Long goodId = resolvedGoodId;
                        if (goodId == null && params != null) {
                            goodId = (Long) params.get("goodId");
                        }
                        result = aiBusinessService.analyzeReviewSentiment(goodId);
                    } else if ("analyze_sales".equals(intent)) {
                        Long goodId = resolvedGoodId;
                        if (goodId == null && params != null) {
                            goodId = (Long) params.get("goodId");
                        }
                        Integer days = params != null ? (Integer) params.get("days") : null;
                        if (days == null) days = 30;
                        result = aiBusinessService.generateSalesReport(goodId, days);
                    }
                    
                    // 发送 action 给前端
                    if (result != null) {
                        System.out.println("发送分析结果 action");
                        java.util.Map<String, Object> actionMap = new java.util.HashMap<>();
                        actionMap.put("action", intent);
                        actionMap.put("actionData", result);
                        String actionJson = cn.hutool.json.JSONUtil.toJsonStr(actionMap);
                        writer.write("event: action\n");
                        writer.write("data: " + actionJson + "\n\n");
                        writer.flush();
                        
                        // 流式输出 AI 回复（支持第x个动态文案）
                        Integer index = extractIndexFromMessage(request.getMessage());
                        String aiResponse = getAiResponseForIntent(intent, index);
                        streamAiResponse(aiResponse, writer);
                    }
                } else {
                    // 对于 view_good_detail、add_to_cart、quick_order 意图，直接使用 DeepSeek 的识别结果
                    // 对于其他意图，使用规则引擎识别意图并提取参数
                    AiIntentResult intentResult;
                    
                    if ("view_good_detail".equals(intent) || "add_to_cart".equals(intent) || "quick_order".equals(intent) || "navigate".equals(intent) || "track_order".equals(intent)) {
                        // 直接使用 DeepSeek 的识别结果
                        intentResult = new AiIntentResult();
                        intentResult.setIntent(AiIntent.valueOf(intent.toUpperCase()));
                        intentResult.setConfidence(0.9);
                        intentResult.setOriginalQuery(request.getMessage());
                        intentResult.setParameters(new HashMap<>());
                        intentResult.setCategory("goods");
                        intentResult.setNeedClarification(false);
                        
                        // 从消息中提取 index 参数
                        Integer index = extractIndexFromMessage(request.getMessage());
                        if (index != null) {
                            intentResult.getParameters().put("index", index);
                            System.out.println("从消息中提取索引：" + index);
                        }
                        
                        System.out.println("使用 DeepSeek 识别结果：" + intent);
                    } else {
                        // 使用规则引擎识别意图
                        ChatRequest businessRequest = new ChatRequest();
                        businessRequest.setUserId(request.getUserId());
                        businessRequest.setMessage(request.getMessage());
                        intentResult = intentEngine.recognizeIntent(businessRequest);
                    }
                    
                    // 关键修复：将之前提取的实体合并到 intentResult 的参数中
                    if (entities != null && intentResult.getParameters() != null) {
                        if ("search_goods".equals(intent) && entities instanceof String) {
                            // 搜索场景：将提取的关键词放入参数
                            intentResult.getParameters().put("keyword", entities);
                            System.out.println("将提取的关键词合并到参数中：" + entities);
                        } else if ("navigate".equals(intent) && entities instanceof String) {
                            // 导航场景：将提取的页面名称放入参数
                            intentResult.getParameters().put("page", entities);
                            System.out.println("将提取的导航目标合并到参数中：" + entities);
                        } else if ("track_order".equals(intent) && entities instanceof String) {
                            // 订单跟踪场景：将提取的订单号放入参数
                            intentResult.getParameters().put("orderId", entities);
                            System.out.println("将提取的订单号合并到参数中：" + entities);
                        } else if (entities instanceof java.util.Map) {
                            // 其他场景：合并 Map 参数
                            java.util.Map<String, Object> entityMap = (java.util.Map<String, Object>) entities;
                            for (java.util.Map.Entry<String, Object> entry : entityMap.entrySet()) {
                                intentResult.getParameters().put(entry.getKey(), entry.getValue());
                            }
                            System.out.println("将提取的实体合并到参数中：" + entities);
                        }
                    }
                    
                    // 规则引擎校正：当规则引擎修正了 DeepSeek 的意图时，以规则引擎为准
                    // 例如 DeepSeek 将"推荐价格2000以下的手机"识别为 search_goods
                    // 但规则引擎能正确识别为 specific_recommend
                    if (intentResult != null && intentResult.getIntent() != null
                        && !"unknown".equals(intentResult.getIntent().getCode())
                        && !intent.equals(intentResult.getIntent().getCode())) {
                        System.out.println("规则引擎校正意图: " + intent + " -> " + intentResult.getIntent().getCode());
                        intent = intentResult.getIntent().getCode();
                    }
                    
                    // 执行业务操作
                    ChatResponse response = aiBusinessService.executeBusiness(intentResult, request.getUserId());
                
                // 特殊处理推荐和搜索场景：模拟淘宝问问的流式输出
                if (("recommend_goods".equals(intent) || "specific_recommend".equals(intent)) && response != null && response.getActionData() != null) {
                    // 检查是否需要确认推荐（价格过滤后无商品）
                    Object actionDataObj = response.getActionData();
                    if (actionDataObj instanceof java.util.Map) {
                        java.util.Map<String, Object> actionData = (java.util.Map<String, Object>) actionDataObj;
                        Boolean needConfirm = (Boolean) actionData.get("needConfirm");
                        
                        System.out.println("needConfirm值：" + needConfirm);
                        
                        if (Boolean.TRUE.equals(needConfirm)) {
                            // 发送确认推荐消息
                            String category = (String) actionData.get("category");
                            System.out.println("发送确认推荐消息，品类：" + category);
                            
                            java.util.Map<String, Object> confirmMap = new java.util.HashMap<>();
                            confirmMap.put("type", "confirmRecommend");
                            confirmMap.put("category", category);
                            confirmMap.put("content", "小皮没有找到对应的商品哦，需要给您推荐性价比高的" + category + "吗？");
                            
                            String confirmJson = cn.hutool.json.JSONUtil.toJsonStr(confirmMap);
                            System.out.println("确认推荐JSON：" + confirmJson);
                            
                            writer.write("event: message\n");
                            writer.write("data: " + confirmJson + "\n\n");
                            writer.flush();
                            System.out.println("确认推荐消息已发送");
                            
                            // 发送 done 标志
                            writer.write("event: done\n");
                            writer.write("data: [DONE]\n\n");
                            writer.flush();
                            System.out.println("DONE标志已发送");
                            return;
                        }
                    }
                    
                    // 1. 先发送思考消息
                    System.out.println("发送思考消息");
                    java.util.Map<String, Object> thinkMap = new java.util.HashMap<>();
                    thinkMap.put("content", "正在为您分析需求，挑选合适的商品...");
                    thinkMap.put("type", "thinking");
                    String thinkJson = cn.hutool.json.JSONUtil.toJsonStr(thinkMap);
                    writer.write("event: message\n");
                    writer.write("data: " + thinkJson + "\n\n");
                    writer.flush();
                    
                    // 等待 500ms
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    
                    // 2. 分段输出文案（逐字）
                    System.out.println("输出第 1 段文案");
                    String category = intent.equals("specific_recommend") ? extractKeyword(request.getMessage()) : "";
                    String copywriting1 = "根据您的要求，我为您精选了以下" + (category.isEmpty() ? "热销" : category) + "商品。";
                    streamTextByChar(copywriting1, writer);
                    
                    // 如果是具体品类推荐，添加筛选条件说明
                    if ("specific_recommend".equals(intent) && response.getActionData() instanceof java.util.Map) {
                        java.util.Map<?, ?> actionData = (java.util.Map<?, ?>) response.getActionData();
                        Object filtersObj = actionData.get("filters");
                        if (filtersObj instanceof java.util.Map) {
                            java.util.Map<?, ?> filters = (java.util.Map<?, ?>) filtersObj;
                            StringBuilder filterDesc = new StringBuilder();
                            
                            if (filters.containsKey("minPrice") || filters.containsKey("maxPrice")) {
                                filterDesc.append("价格范围：");
                                if (filters.containsKey("minPrice")) {
                                    filterDesc.append(filters.get("minPrice")).append("元");
                                }
                                if (filters.containsKey("minPrice") && filters.containsKey("maxPrice")) {
                                    filterDesc.append("-");
                                }
                                if (filters.containsKey("maxPrice")) {
                                    filterDesc.append(filters.get("maxPrice")).append("元");
                                }
                            }
                            
                            if (filters.containsKey("standard")) {
                                if (filterDesc.length() > 0) filterDesc.append("，");
                                filterDesc.append("规格：").append(filters.get("standard"));
                            }
                            
                            if (filterDesc.length() > 0) {
                                streamTextByChar("筛选条件：" + filterDesc.toString() + "。", writer);
                            }
                        }
                        
                        // 添加思考过程展示
                        Object thinkingStepsObj = actionData.get("thinkingSteps");
                        if (thinkingStepsObj instanceof java.util.List) {
                            java.util.List<?> thinkingSteps = (java.util.List<?>) thinkingStepsObj;
                            streamTextByChar("\n\n💡 思考过程：", writer);
                            for (Object step : thinkingSteps) {
                                streamTextByChar("\n• " + step.toString(), writer);
                            }
                        }
                    }
                    
                    System.out.println("输出第 2 段文案");
                    String copywriting2 = "\n\n首推这款商品，销量领先，口碑极佳。";
                    streamTextByChar(copywriting2, writer);
                    
                    System.out.println("输出第 3 段文案");
                    String copywriting3 = "\n\n您可以点击下方商品卡片查看详情，或加入购物车。";
                    streamTextByChar(copywriting3, writer);
                    
                    // 3. 发送 action 展示商品卡片
                    System.out.println("发送商品卡片 action");
                    java.util.Map<String, Object> actionMap = new java.util.HashMap<>();
                    actionMap.put("action", intent);
                    actionMap.put("actionData", response.getActionData());
                    String actionJson = cn.hutool.json.JSONUtil.toJsonStr(actionMap);
                    writer.write("event: action\n");
                    writer.write("data: " + actionJson + "\n\n");
                    writer.flush();
                    
                    // 保存 AI 回复到上下文（用于后续的"第一个"、"第二个"等指代）
                    if (request.getUserId() != null && response.getActionData() != null) {
                        // 构建包含结构化商品 ID 列表的上下文，用于后续"第x个"功能的精确匹配
                        StringBuilder aiReply = new StringBuilder();
                        aiReply.append("根据您的要求，我为您精选了以下商品。");
                        java.util.List<Long> recommendGoodIds = new java.util.ArrayList<>();
                        
                        System.out.println("=== 开始保存上下文 ===");
                        System.out.println("actionData 类型: " + response.getActionData().getClass().getName());
                        
                        if (response.getActionData() instanceof java.util.Map) {
                            java.util.Map<String, Object> actionData = (java.util.Map<String, Object>) response.getActionData();
                            System.out.println("actionData keys: " + actionData.keySet());
                            
                            // 尝试从 goods 字段获取商品列表
                            Object goodsObj = actionData.get("goods");
                            System.out.println("goodsObj 是否为 null: " + (goodsObj == null));
                            
                            if (goodsObj == null) {
                                // 尝试从 topGood 和 otherGoods 获取
                                Object topGood = actionData.get("topGood");
                                Object otherGoods = actionData.get("otherGoods");
                                
                                System.out.println("topGood 是否为 null: " + (topGood == null));
                                System.out.println("otherGoods 是否为 null: " + (otherGoods == null));
                                
                                if (topGood instanceof java.util.Map) {
                                    java.util.Map<String, Object> topGoodMap = (java.util.Map<String, Object>) topGood;
                                    Object goodIdObj = topGoodMap.get("id");
                                    if (goodIdObj == null) {
                                        goodIdObj = topGoodMap.get("goodId");
                                    }
                                    if (goodIdObj != null) {
                                        Long id = Long.valueOf(goodIdObj.toString());
                                        recommendGoodIds.add(id);
                                        System.out.println("从 topGood 获取商品ID: " + id);
                                    }
                                }
                                
                                if (otherGoods instanceof java.util.List) {
                                    java.util.List<?> otherGoodsList = (java.util.List<?>) otherGoods;
                                    for (int i = 0; i < otherGoodsList.size(); i++) {
                                        Object goodObj = otherGoodsList.get(i);
                                        if (goodObj instanceof java.util.Map) {
                                            java.util.Map<String, Object> goodMap = (java.util.Map<String, Object>) goodObj;
                                            Object goodIdObj = goodMap.get("id");
                                            if (goodIdObj == null) {
                                                goodIdObj = goodMap.get("goodId");
                                            }
                                            if (goodIdObj != null) {
                                                recommendGoodIds.add(Long.valueOf(goodIdObj.toString()));
                                            }
                                        }
                                    }
                                }
                            } else if (goodsObj instanceof java.util.List) {
                                java.util.List<?> goodsList = (java.util.List<?>) goodsObj;
                                for (int i = 0; i < goodsList.size(); i++) {
                                    Object goodObj = goodsList.get(i);
                                    if (goodObj instanceof java.util.Map) {
                                        java.util.Map<String, Object> goodMap = (java.util.Map<String, Object>) goodObj;
                                        Object goodIdObj = goodMap.get("id");
                                        if (goodIdObj == null) {
                                            goodIdObj = goodMap.get("goodId");
                                        }
                                        if (goodIdObj != null) {
                                            recommendGoodIds.add(Long.valueOf(goodIdObj.toString()));
                                        }
                                    } else if (goodObj instanceof com.rabbiter.em.entity.Good) {
                                        com.rabbiter.em.entity.Good good = (com.rabbiter.em.entity.Good) goodObj;
                                        if (good.getId() != null) {
                                            recommendGoodIds.add(good.getId());
                                        }
                                    }
                                }
                            }
                        }
                        
                        // 在上下文中保存结构化格式：【推荐商品列表】[id1,id2,id3,...]
                        // 这种格式比零散的"商品ID:X"更可靠，能精确匹配"第x个"
                        if (!recommendGoodIds.isEmpty()) {
                            aiReply.append("【推荐商品列表】").append(cn.hutool.json.JSONUtil.toJsonStr(recommendGoodIds));
                        }
                        
                        System.out.println("保存到上下文的 AI 回复: " + aiReply.toString());
                        contextManager.addMessage(request.getUserId(), "assistant", aiReply.toString());
                        System.out.println("=== 上下文保存完成 ===");
                    }
                } else {
                    // 其他场景：先流式输出 AI 回复文案，再发送 action
                    Integer index = intentResult.getParameters() != null ? 
                        (Integer) intentResult.getParameters().get("index") : null;
                    String aiResponse = getAiResponseForIntent(intent, index);
                    streamAiResponse(aiResponse, writer);
                    
                    System.out.println("发送 action 给前端：" + intent);
                    java.util.Map<String, Object> actionMap = new java.util.HashMap<>();
                    actionMap.put("action", intent);
                    actionMap.put("actionData", response.getActionData());
                    actionMap.put("content", aiResponse);
                    String actionJson = cn.hutool.json.JSONUtil.toJsonStr(actionMap);
                    writer.write("event: action\n");
                    writer.write("data: " + actionJson + "\n\n");
                    writer.flush();
                    
                    // 保存 AI 回复到上下文（用于后续的"第一个"、"第二个"等指代）
                    // response.getMessage() 中包含了 search_goods 场景的"商品ID:X"信息
                    if (request.getUserId() != null && response != null && response.getMessage() != null) {
                        contextManager.addMessage(request.getUserId(), "assistant", response.getMessage());
                        System.out.println("保存 AI 回复到上下文: " + response.getMessage());
                    }
                }
                } // 闭合 else 块（规则引擎处理）
            } else {
                // 未知意图，让 DeepSeek 生成回复
                String aiResponsePrompt = "用户的意图不明确：" + request.getMessage() + "\n请生成一个友好的回复，引导用户更清晰地表达需求。";
                String aiResponse = getDeepSeekAiResponse(aiResponsePrompt);
                streamAiResponse(aiResponse, writer);
            }
            
            // 发送 done 标志
            writer.write("event: done\n");
            writer.write("data: [DONE]\n\n");
            writer.flush();
            
            System.out.println("=== 智能帮助模式完成 ===");
            
        } catch (Exception e) {
            System.err.println("智能帮助模式异常：" + e.getMessage());
            e.printStackTrace();
            try {
                writer.write("event: error\n");
                writer.write("data: {\"error\":\"" + e.getMessage() + "\"}\n\n");
                writer.flush();
            } catch (Exception ex) {
                // 忽略
            }
        }
    }
    
    /**
     * 执行业务操作
     */
    private Object executeBusinessOperation(String intent, Object entities, ChatRequest request) {
        try {
            if ("search".equals(intent)) {
                // 搜索需要返回前端期望的格式
                Object result = aiBusinessService.search(entities.toString());
                java.util.Map<String, Object> searchData = new java.util.HashMap<>();
                searchData.put("goods", result);
                searchData.put("total", result instanceof java.util.List ? ((java.util.List<?>) result).size() : 0);
                return searchData;
            } else if ("addCart".equals(intent)) {
                java.util.Map<String, Object> params = new java.util.HashMap<>();
                if (entities instanceof java.util.Map) {
                    params.putAll((java.util.Map<? extends String, ?>) entities);
                } else {
                    params.put("goodId", entities);
                }
                if (request.getUserId() != null) {
                    params.put("userId", request.getUserId());
                }
                return aiBusinessService.addCart(params);
            } else if ("batchAddCart".equals(intent)) {
                java.util.List<java.util.Map<String, Object>> paramsList = new java.util.ArrayList<>();
                if (entities instanceof java.util.List) {
                    paramsList = (java.util.List<java.util.Map<String, Object>>) entities;
                } else if (entities instanceof java.util.Map) {
                    paramsList.add((java.util.Map<String, Object>) entities);
                }
                if (request.getUserId() != null) {
                    for (java.util.Map<String, Object> params : paramsList) {
                        params.put("userId", request.getUserId());
                    }
                }
                return aiBusinessService.batchAddCart(paramsList);
            } else if ("queryOrder".equals(intent)) {
                java.util.Map<String, Object> params = new java.util.HashMap<>();
                if (entities instanceof java.util.Map) {
                    params.putAll((java.util.Map<? extends String, ?>) entities);
                } else {
                    params.put("orderNo", entities);
                }
                if (request.getUserId() != null) {
                    params.put("userId", request.getUserId());
                }
                return aiBusinessService.queryOrder(params);
            } else if ("navigate".equals(intent)) {
                return aiBusinessService.navigate(entities.toString());
            } else if ("logout".equals(intent)) {
                return aiBusinessService.logout(request.getUserId());
            } else if ("recommend".equals(intent)) {
                // 直接返回商品列表，让 actionData.params 就是商品列表
                return aiBusinessService.getPersonalizedRecommendation(request.getUserId());
            } else if ("quickOrder".equals(intent)) {
                java.util.Map<String, Object> params = new java.util.HashMap<>();
                if (entities instanceof java.util.Map) {
                    params.putAll((java.util.Map<? extends String, ?>) entities);
                }
                if (request.getUserId() != null) {
                    params.put("userId", request.getUserId());
                }
                return aiBusinessService.quickOrder(params);
            } else if ("trackOrder".equals(intent)) {
                String orderId = entities != null ? entities.toString() : "最新订单";
                return aiBusinessService.trackOrderStatus(orderId, request.getUserId());
            } else if ("analyzeOrders".equals(intent)) {
                return aiBusinessService.analyzeOrderHistory(request.getUserId());
            } else if ("analyzeSentiment".equals(intent)) {
                java.util.Map<String, Object> params = new java.util.HashMap<>();
                if (entities instanceof java.util.Map) {
                    params.putAll((java.util.Map<? extends String, ?>) entities);
                }
                Long goodId = (Long) params.get("goodId");
                return aiBusinessService.analyzeReviewSentiment(goodId);
            } else if ("analyzeSales".equals(intent)) {
                java.util.Map<String, Object> params = new java.util.HashMap<>();
                if (entities instanceof java.util.Map) {
                    params.putAll((java.util.Map<? extends String, ?>) entities);
                }
                Long goodId = (Long) params.get("goodId");
                Integer days = (Integer) params.get("days");
                if (days == null) days = 30;
                return aiBusinessService.generateSalesReport(goodId, days);
            }
        } catch (Exception e) {
            System.err.println("执行业务操作失败：" + e.getMessage());
        }
        return null;
    }
    
    /**
     * 获取意图对应的 AI 回复（支持带索引的动态文案）
     */
    private String getAiResponseForIntent(String intent) {
        return getAiResponseForIntent(intent, null);
    }

    /**
     * 获取意图对应的 AI 回复（支持带索引的动态文案）
     * @param intent 意图编码
     * @param index 商品索引（1-based），为 null 时使用默认静态文案
     */
    private String getAiResponseForIntent(String intent, Integer index) {
        if (index != null) {
            switch (intent) {
                case "analyze_sales":
                    return "将为你生成第" + index + "个商品的销售报告";
                case "add_to_cart":
                    return "将为你将第" + index + "个商品加入购物车";
                case "quick_order":
                    return "将为你跳转第" + index + "个商品的购买界面";
            }
        }
        switch (intent) {
            case "search_goods":
                return "已为您找到相关商品，请查看搜索结果。";
            case "add_to_cart":
                return "商品已成功添加到购物车！";
            case "batch_add_cart":
                return "商品已成功添加到购物车！";
            case "query_order":
                return "已为您查询到订单信息，请查看详情。";
            case "navigate":
                return "正在为您导航到指定页面...";
            case "logout":
                return "已为您退出登录。";
            case "recommend_goods":
                return "根据您的喜好，为您推荐以下商品。";
            case "specific_recommend":
                return "已为您找到相关商品，请查看推荐结果。";
            case "quick_order":
                return "订单已快速生成，请确认支付。";
            case "track_order":
                return "这是您的订单物流信息。";
            case "analyze_orders":
                return "这是您的订单分析报告。";
            case "analyze_sentiment":
                return "已为您分析该商品的评论舆情，请查看分析结果。";
            case "analyze_sales":
                return "已为您生成销售数据报告，请查看分析结果。";
            default:
                return "抱歉，我暂时无法处理该请求。";
        }
    }
    
    /**
     * 流式输出 AI 回复（模拟打字机效果）
     */
    private void streamAiResponse(String aiResponse, java.io.PrintWriter writer) throws Exception {
        if (aiResponse != null && !aiResponse.isEmpty()) {
            char[] chars = aiResponse.toCharArray();
            StringBuilder buffer = new StringBuilder();
            
            for (int i = 0; i < chars.length; i++) {
                buffer.append(chars[i]);
                
                if (i % 3 == 2 || chars[i] == '。' || chars[i] == '，' || chars[i] == '！' || 
                    chars[i] == '？' || chars[i] == '；' || chars[i] == ':' || chars[i] == ',') {
                    
                    String chunk = buffer.toString();
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("content", chunk);
                    String json = cn.hutool.json.JSONUtil.toJsonStr(map);
                    
                    writer.write("event: message\n");
                    writer.write("data: " + json + "\n\n");
                    writer.flush();
                    
                    buffer.setLength(0);
                    Thread.sleep(50);
                }
            }
            
            if (buffer.length() > 0) {
                String chunk = buffer.toString();
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("content", chunk);
                String json = cn.hutool.json.JSONUtil.toJsonStr(map);
                writer.write("event: message\n");
                writer.write("data: " + json + "\n\n");
                writer.flush();
            }
        }
    }
    
    /**
     * 逐字输出文本（简单版本）
     */
    private void streamTextByChar(String text, java.io.PrintWriter writer) throws Exception {
        if (text != null && !text.isEmpty()) {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("content", text);
            String json = cn.hutool.json.JSONUtil.toJsonStr(map);
            writer.write("event: message\n");
            writer.write("data: " + json + "\n\n");
            writer.flush();
            Thread.sleep(300);
        }
    }
    
    /**
     * 使用 LangChain4j 进行流式聊天（支持意图识别和业务执行）
     */
    private void streamWithLangChain4j(ChatRequest request, java.io.PrintWriter writer) throws Exception {
        System.out.println(">>> 调用 LangChain4j 流式接口...");
        
        // 1. 使用规则引擎识别意图
        AiIntentResult intentResult = intentEngine.recognizeIntent(request);
        System.out.println("识别出的用户意图：" + intentResult.getIntent().getCode());
        System.out.println("提取到的参数：" + intentResult.getParameters());
        
        // 2. 如果是聊天意图，直接使用 DeepSeek 对话
        if (intentResult.getIntent() == AiIntent.CHAT || intentResult.getIntent() == AiIntent.UNKNOWN) {
            System.out.println(">>> 聊天意图，直接使用 DeepSeek 对话");
            String aiResponse = langChainAiService.chat(request.getUserId(), request.getMessage());
            
            if (aiResponse != null && !aiResponse.isEmpty()) {
                char[] chars = aiResponse.toCharArray();
                StringBuilder buffer = new StringBuilder();
                
                for (int i = 0; i < chars.length; i++) {
                    buffer.append(chars[i]);
                    
                    if (i % 3 == 2 || chars[i] == '。' || chars[i] == '，' || chars[i] == '！' || 
                        chars[i] == '？' || chars[i] == '；' || chars[i] == ':' || chars[i] == ',') {
                        
                        String chunk = buffer.toString();
                        java.util.Map<String, Object> map = new java.util.HashMap<>();
                        map.put("content", chunk);
                        String json = cn.hutool.json.JSONUtil.toJsonStr(map);
                        
                        writer.write("event: message\n");
                        writer.write("data: " + json + "\n\n");
                        writer.flush();
                        buffer.setLength(0);
                        Thread.sleep(50);
                    }
                }
                
                if (buffer.length() > 0) {
                    String chunk = buffer.toString();
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("content", chunk);
                    String json = cn.hutool.json.JSONUtil.toJsonStr(map);
                    writer.write("event: message\n");
                    writer.write("data: " + json + "\n\n");
                    writer.flush();
                }
            }
            
            writer.write("event: done\n");
            writer.write("data: [DONE]\n\n");
            writer.flush();
            return;
        }
        
        // 3. 执行业务操作
        System.out.println("开始执行业务操作：" + intentResult.getIntent().getCode());
        ChatResponse response = aiBusinessService.executeBusiness(intentResult, request.getUserId());
        System.out.println("业务操作完成，action：" + response.getAction());
        System.out.println("actionData：" + response.getActionData());
        
        // 4. 特殊处理推荐和搜索场景
        if (("recommend_goods".equals(intentResult.getIntent().getCode()) || "specific_recommend".equals(intentResult.getIntent().getCode()) || "search_goods".equals(intentResult.getIntent().getCode())) 
                && response != null && response.getActionData() != null) {
            
            // 4.1 先发送思考消息
            java.util.Map<String, Object> thinkMap = new java.util.HashMap<>();
            thinkMap.put("content", "正在为您分析需求，挑选合适的商品...");
            thinkMap.put("type", "thinking");
            String thinkJson = cn.hutool.json.JSONUtil.toJsonStr(thinkMap);
            writer.write("event: message\n");
            writer.write("data: " + thinkJson + "\n\n");
            writer.flush();
            Thread.sleep(500);
            
            // 4.2 分段输出文案
            String category = intentResult.getParameters() != null ? 
                (String) intentResult.getParameters().get("requirement") : "";
            String keyword = intentResult.getParameters() != null ? 
                (String) intentResult.getParameters().get("keyword") : "";
            
            String copywriting = "";
            if ("search_goods".equals(intentResult.getIntent().getCode())) {
                copywriting = "已为您找到与\"" + (keyword != null && !keyword.isEmpty() ? keyword : "搜索词") + "\"相关的商品。";
            } else {
                copywriting = "根据您的要求，我为您精选了以下" + (category != null && !category.isEmpty() ? category : "热销") + "商品。";
            }
            
            // 逐字输出文案
            char[] chars = copywriting.toCharArray();
            StringBuilder buffer = new StringBuilder();
            for (int i = 0; i < chars.length; i++) {
                buffer.append(chars[i]);
                if (i % 3 == 2 || chars[i] == '。' || chars[i] == '，') {
                    String chunk = buffer.toString();
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("content", chunk);
                    String json = cn.hutool.json.JSONUtil.toJsonStr(map);
                    writer.write("event: message\n");
                    writer.write("data: " + json + "\n\n");
                    writer.flush();
                    buffer.setLength(0);
                    Thread.sleep(50);
                }
            }
            if (buffer.length() > 0) {
                String chunk = buffer.toString();
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("content", chunk);
                String json = cn.hutool.json.JSONUtil.toJsonStr(map);
                writer.write("event: message\n");
                writer.write("data: " + json + "\n\n");
                writer.flush();
            }
            
            // 4.3 发送筛选条件
            if (intentResult.getParameters() != null) {
                java.util.Map<String, Object> filters = new java.util.HashMap<>();
                if (intentResult.getParameters().containsKey("minPrice") || intentResult.getParameters().containsKey("maxPrice")) {
                    StringBuilder priceDesc = new StringBuilder("价格范围：");
                    if (intentResult.getParameters().containsKey("minPrice")) {
                        priceDesc.append(intentResult.getParameters().get("minPrice")).append("元以上");
                    }
                    if (intentResult.getParameters().containsKey("minPrice") && intentResult.getParameters().containsKey("maxPrice")) {
                        priceDesc.append("-");
                    }
                    if (intentResult.getParameters().containsKey("maxPrice")) {
                        priceDesc.append(intentResult.getParameters().get("maxPrice")).append("元以下");
                    }
                    filters.put("price", priceDesc.toString());
                }
                if (intentResult.getParameters().containsKey("standard")) {
                    filters.put("standard", "规格：" + intentResult.getParameters().get("standard"));
                }
                
                if (!filters.isEmpty()) {
                    String filterText = filters.values().stream().map(Object::toString).collect(java.util.stream.Collectors.joining("，"));
                    java.util.Map<String, Object> filterMap = new java.util.HashMap<>();
                    filterMap.put("content", "筛选条件：" + filterText + "。");
                    String filterJson = cn.hutool.json.JSONUtil.toJsonStr(filterMap);
                    writer.write("event: message\n");
                    writer.write("data: " + filterJson + "\n\n");
                    writer.flush();
                    Thread.sleep(300);
                }
            }
            
            // 4.4 发送推荐提示
            String tip = "首推这款商品，销量领先，口碑极佳。您可以点击下方商品卡片查看详情，或加入购物车。";
            chars = tip.toCharArray();
            buffer = new StringBuilder();
            for (int i = 0; i < chars.length; i++) {
                buffer.append(chars[i]);
                if (i % 3 == 2 || chars[i] == '。' || chars[i] == '，') {
                    String chunk = buffer.toString();
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("content", chunk);
                    String json = cn.hutool.json.JSONUtil.toJsonStr(map);
                    writer.write("event: message\n");
                    writer.write("data: " + json + "\n\n");
                    writer.flush();
                    buffer.setLength(0);
                    Thread.sleep(50);
                }
            }
            if (buffer.length() > 0) {
                String chunk = buffer.toString();
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("content", chunk);
                String json = cn.hutool.json.JSONUtil.toJsonStr(map);
                writer.write("event: message\n");
                writer.write("data: " + json + "\n\n");
                writer.flush();
            }
            
            // 4.5 发送商品数据（使用 action 事件格式，让前端 handleAction 处理）
            java.util.Map<String, Object> actionMap = new java.util.HashMap<>();
            actionMap.put("action", intentResult.getIntent().getCode()); // specific_recommend
            actionMap.put("actionData", response.getActionData());
            String actionJson = cn.hutool.json.JSONUtil.toJsonStr(actionMap);
            writer.write("event: action\n");
            writer.write("data: " + actionJson + "\n\n");
            writer.flush();
            System.out.println("发送商品卡片 action");
        } else if ("analyze_orders".equals(intentResult.getIntent().getCode()) && response != null && response.getActionData() != null) {
            // 订单分析场景：发送思考消息 + action 事件
            System.out.println("处理订单分析场景");
            
            // 1. 先发送思考消息
            java.util.Map<String, Object> thinkMap = new java.util.HashMap<>();
            thinkMap.put("content", "正在分析您的订单数据...");
            thinkMap.put("type", "thinking");
            String thinkJson = cn.hutool.json.JSONUtil.toJsonStr(thinkMap);
            writer.write("event: message\n");
            writer.write("data: " + thinkJson + "\n\n");
            writer.flush();
            Thread.sleep(500);
            
            // 2. 分段输出文案
            String copywriting = "这是您的订单分析报告。";
            char[] chars = copywriting.toCharArray();
            StringBuilder buffer = new StringBuilder();
            for (int i = 0; i < chars.length; i++) {
                buffer.append(chars[i]);
                if (i % 3 == 2 || chars[i] == '。' || chars[i] == '，') {
                    String chunk = buffer.toString();
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("content", chunk);
                    String json = cn.hutool.json.JSONUtil.toJsonStr(map);
                    writer.write("event: message\n");
                    writer.write("data: " + json + "\n\n");
                    writer.flush();
                    buffer.setLength(0);
                    Thread.sleep(50);
                }
            }
            if (buffer.length() > 0) {
                String chunk = buffer.toString();
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("content", chunk);
                String json = cn.hutool.json.JSONUtil.toJsonStr(map);
                writer.write("event: message\n");
                writer.write("data: " + json + "\n\n");
                writer.flush();
            }
            
            // 3. 发送订单分析数据（使用 action 事件格式）
            java.util.Map<String, Object> actionMap = new java.util.HashMap<>();
            actionMap.put("action", "analyze_orders");
            actionMap.put("actionData", response.getActionData());
            String actionJson = cn.hutool.json.JSONUtil.toJsonStr(actionMap);
            writer.write("event: action\n");
            writer.write("data: " + actionJson + "\n\n");
            writer.flush();
            System.out.println("发送订单分析 action");
        } else if ("track_order".equals(intentResult.getIntent().getCode()) && response != null && response.getActionData() != null) {
            // 订单跟踪场景：发送思考消息 + action 事件（包含分页字段）
            System.out.println("处理订单跟踪场景");
            
            // 1. 先发送思考消息
            java.util.Map<String, Object> thinkMap = new java.util.HashMap<>();
            thinkMap.put("content", "正在查询您的订单列表...");
            thinkMap.put("type", "thinking");
            String thinkJson = cn.hutool.json.JSONUtil.toJsonStr(thinkMap);
            writer.write("event: message\n");
            writer.write("data: " + thinkJson + "\n\n");
            writer.flush();
            Thread.sleep(300);
            
            // 2. 分段输出文案
            String copywriting = "这是您的订单列表，请查看。";
            {
                char[] chars = copywriting.toCharArray();
                StringBuilder buffer = new StringBuilder();
                for (int i = 0; i < chars.length; i++) {
                    buffer.append(chars[i]);
                    if (i % 3 == 2 || chars[i] == '。' || chars[i] == '，') {
                        String chunk = buffer.toString();
                        java.util.Map<String, Object> map = new java.util.HashMap<>();
                        map.put("content", chunk);
                        String json = cn.hutool.json.JSONUtil.toJsonStr(map);
                        writer.write("event: message\n");
                        writer.write("data: " + json + "\n\n");
                        writer.flush();
                        buffer.setLength(0);
                        Thread.sleep(50);
                    }
                }
                if (buffer.length() > 0) {
                    String chunk = buffer.toString();
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("content", chunk);
                    String json = cn.hutool.json.JSONUtil.toJsonStr(map);
                    writer.write("event: message\n");
                    writer.write("data: " + json + "\n\n");
                    writer.flush();
                }
            }
            
            // 3. 发送订单跟踪数据（使用 action 事件格式，包含分页字段）
            java.util.Map<String, Object> trackActionMap = new java.util.HashMap<>();
            trackActionMap.put("action", "track_order");
            trackActionMap.put("actionData", response.getActionData());
            String trackActionJson = cn.hutool.json.JSONUtil.toJsonStr(trackActionMap);
            writer.write("event: action\n");
            writer.write("data: " + trackActionJson + "\n\n");
            writer.flush();
            System.out.println("发送订单跟踪 action");
        } else {
            // 其他业务操作，直接发送响应
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("content", response.getMessage());
            String json = cn.hutool.json.JSONUtil.toJsonStr(map);
            writer.write("event: message\n");
            writer.write("data: " + json + "\n\n");
            writer.flush();
        }
        
        // 发送结束标志
        writer.write("event: done\n");
        writer.write("data: [DONE]\n\n");
        writer.flush();
        System.out.println("<<< 发送 [DONE] 标志");
    }
    
    /**
     * 使用百度文心一言进行流式聊天
     */
    private void streamWithBaidu(ChatRequest request, java.io.PrintWriter writer) throws Exception {
        System.out.println(">>> 调用百度文心一言流式接口...");
        
        // 获取百度 API 的 accessToken
        String accessToken = getBaiduAccessToken();
        
        // 构建请求体
        cn.hutool.json.JSONObject requestBody = new cn.hutool.json.JSONObject();
        cn.hutool.json.JSONArray messages = new cn.hutool.json.JSONArray();
        
        // 添加系统消息
        cn.hutool.json.JSONObject systemMsg = new cn.hutool.json.JSONObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", "你是小皮助手，是一个电商购物助手。你可以帮助用户搜索商品、添加购物车、查询订单、导航到指定页面，以及解答购物相关的问题。请保持友好、热情、专业的态度，用简洁清晰的语言回答用户问题。");
        messages.add(systemMsg);
        
        // 添加用户消息
        cn.hutool.json.JSONObject userMsg = new cn.hutool.json.JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", request.getMessage());
        messages.add(userMsg);
        
        requestBody.put("model", "ernie-3.5-8k");
        requestBody.put("messages", messages);
        requestBody.put("stream", true);
        
        // 调用百度流式 API
        java.net.URL url = new java.net.URL("https://qianfan.baidubce.com/v2/chat/completions");
        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);
        connection.setDoOutput(true);
        connection.setChunkedStreamingMode(0);
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(0);
        
        // 写入请求体
        try (java.io.OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.toString().getBytes("UTF-8");
            os.write(input, 0, input.length);
            os.flush();
        }
        
        System.out.println("百度 API 响应码：" + connection.getResponseCode());
        
        // 读取流式响应
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(connection.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) continue;
                
                String trimmedLine = line.startsWith("data:") ? line.substring(5).trim() : line;
                
                if ("[DONE]".equals(trimmedLine)) {
                    writer.write("event: done\n");
                    writer.write("data: [DONE]\n\n");
                    writer.flush();
                    break;
                }
                
                // 解析响应内容
                try {
                    cn.hutool.json.JSONObject json = cn.hutool.json.JSONUtil.parseObj(trimmedLine);
                    cn.hutool.json.JSONArray choices = json.getJSONArray("choices");
                    if (choices != null && !choices.isEmpty()) {
                        cn.hutool.json.JSONObject firstChoice = choices.getJSONObject(0);
                        cn.hutool.json.JSONObject delta = firstChoice.getJSONObject("delta");
                        if (delta != null) {
                            String content = delta.getStr("content", "");
                            
                            if (content != null && !content.isEmpty()) {
                                // 使用与原来相同的格式：推送 JSON 对象，包含 content 字段
                                java.util.Map<String, Object> map = new java.util.HashMap<>();
                                map.put("content", content);
                                String jsonStr = cn.hutool.json.JSONUtil.toJsonStr(map);
                                
                                writer.write("event: message\n");
                                writer.write("data: " + jsonStr + "\n\n");
                                writer.flush();
                                
                                System.out.print(content);
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("解析响应失败：" + e.getMessage());
                }
            }
        }
        
        System.out.println("\n<<< 百度文心一言流式处理完成");
    }
    
    /**
     * 获取百度 API 的访问令牌
     */
    private String getBaiduAccessToken() {
        try {
            // 调用百度 OAuth 2.0 接口获取 access_token
            String tokenUrl = "https://aip.baidubce.com/oauth/2.0/token?grant_type=client_credentials&client_id=" + 
                             baiduApiKey + "&client_secret=" + baiduSecretKey;
            
            String responseBody = cn.hutool.http.HttpUtil.createGet(tokenUrl).execute().body();
            cn.hutool.json.JSONObject tokenResponse = cn.hutool.json.JSONUtil.parseObj(responseBody);
            String accessToken = tokenResponse.getStr("access_token");
            
            if (accessToken == null || accessToken.isEmpty()) {
                throw new RuntimeException("获取百度 API 访问令牌失败：" + tokenResponse.toString());
            }
            
            System.out.println("成功获取百度 API 访问令牌");
            return accessToken;
            
        } catch (Exception e) {
            System.err.println("获取百度 API 访问令牌失败：" + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("获取百度 API 访问令牌失败", e);
        }
    }
    
    /**
     * 执行业务操作并流式输出结果（智能帮助模式）
     */
    private void executeBusinessOperationWithStreaming(String intent, ChatRequest request, java.io.PrintWriter writer) {
        try {
            System.out.println("开始执行业务操作（流式版本）：" + intent);
            
            // 使用规则引擎识别意图并提取参数
            AiIntentResult intentResult = intentEngine.recognizeIntent(request);
            String actualIntent = intentResult.getIntent().getCode();
            System.out.println("识别到的意图：" + actualIntent);
            
            // 执行业务操作
            ChatResponse response = aiBusinessService.executeBusiness(intentResult, request.getUserId());
            
            // 特殊处理推荐和搜索场景：模拟淘宝问问的流式输出
            if (("recommend_goods".equals(actualIntent) || "specific_recommend".equals(actualIntent)) && response != null && response.getActionData() != null) {
                // 检查是否需要确认推荐（价格过滤后无商品）
                Object actionDataObj = response.getActionData();
                if (actionDataObj instanceof java.util.Map) {
                    java.util.Map<String, Object> actionData = (java.util.Map<String, Object>) actionDataObj;
                    Boolean needConfirm = (Boolean) actionData.get("needConfirm");
                    
                    System.out.println("needConfirm值：" + needConfirm);
                    
                    if (Boolean.TRUE.equals(needConfirm)) {
                        // 发送确认推荐消息
                        String category = (String) actionData.get("category");
                        System.out.println("发送确认推荐消息，品类：" + category);
                        
                        java.util.Map<String, Object> confirmMap = new java.util.HashMap<>();
                        confirmMap.put("type", "confirmRecommend");
                        confirmMap.put("category", category);
                        confirmMap.put("content", "小皮没有找到对应的商品哦，需要给您推荐性价比高的" + category + "吗？");
                        
                        String confirmJson = cn.hutool.json.JSONUtil.toJsonStr(confirmMap);
                        System.out.println("确认推荐JSON：" + confirmJson);
                        
                        writer.write("event: message\n");
                        writer.write("data: " + confirmJson + "\n\n");
                        writer.flush();
                        System.out.println("确认推荐消息已发送");
                        
                        // 发送 done 标志
                        writer.write("event: done\n");
                        writer.write("data: [DONE]\n\n");
                        writer.flush();
                        System.out.println("DONE标志已发送");
                        return;
                    }
                }
                
                // 1. 先发送思考消息
                System.out.println("发送思考消息");
                java.util.Map<String, Object> thinkMap = new java.util.HashMap<>();
                thinkMap.put("content", "正在为您分析需求，挑选合适的商品...");
                thinkMap.put("type", "thinking");
                writer.write("event: message\n");
                writer.write("data: " + cn.hutool.json.JSONUtil.toJsonStr(thinkMap) + "\n\n");
                writer.flush();
                Thread.sleep(500);
                
                // 2. 生成详细的推荐文案（分段）
                String[] paragraphs = generateDetailedRecommendationText(request.getMessage(), response, intent);
                
                // 3. 逐段输出文案
                for (int p = 0; p < paragraphs.length; p++) {
                    String paragraph = paragraphs[p];
                    System.out.println("输出第 " + (p + 1) + " 段文案：" + paragraph);
                    
                    char[] chars = paragraph.toCharArray();
                    StringBuilder buffer = new StringBuilder();
                    
                    for (int i = 0; i < chars.length; i++) {
                        buffer.append(chars[i]);
                        
                        if (i % 2 == 1 || chars[i] == '。' || chars[i] == '，' || chars[i] == '！' || 
                            chars[i] == '？' || chars[i] == '；') {
                            
                            String chunk = buffer.toString();
                            java.util.Map<String, Object> map = new java.util.HashMap<>();
                            map.put("content", chunk);
                            String json = cn.hutool.json.JSONUtil.toJsonStr(map);
                            
                            writer.write("event: message\n");
                            writer.write("data: " + json + "\n\n");
                            writer.flush();
                            
                            buffer.setLength(0);
                            Thread.sleep(20);
                        }
                    }
                    
                    // 段落之间稍作停顿
                    if (p < paragraphs.length - 1) {
                        Thread.sleep(300);
                    }
                }
                
                // 4. 然后发送商品卡片
                System.out.println("发送商品卡片 action");
                java.util.Map<String, Object> actionMap = new java.util.HashMap<>();
                actionMap.put("action", response.getAction());
                actionMap.put("actionData", response.getActionData());
                String actionJson = cn.hutool.json.JSONUtil.toJsonStr(actionMap);
                
                writer.write("event: action\n");
                writer.write("data: " + actionJson + "\n\n");
                writer.flush();
                
            } else {
                // 其他场景：正常发送 action
                if (response != null && response.getAction() != null) {
                    System.out.println("发送 action 给前端：" + response.getAction());
                    java.util.Map<String, Object> actionMap = new java.util.HashMap<>();
                    actionMap.put("action", response.getAction());
                    actionMap.put("actionData", response.getActionData());
                    String actionJson = cn.hutool.json.JSONUtil.toJsonStr(actionMap);
                    
                    writer.write("event: action\n");
                    writer.write("data: " + actionJson + "\n\n");
                    writer.flush();
                }
                
                // 流式输出 AI 回复
                System.out.println("开始流式输出业务操作结果");
                
                if (response != null && response.getMessage() != null) {
                    String aiResponse = response.getMessage();
                    
                    // 模拟流式效果：逐字推送响应内容
                    char[] chars = aiResponse.toCharArray();
                    StringBuilder buffer = new StringBuilder();
                    
                    for (int i = 0; i < chars.length; i++) {
                        buffer.append(chars[i]);
                        
                        if (i % 3 == 2 || chars[i] == '。' || chars[i] == '，' || chars[i] == '！' || 
                            chars[i] == '？' || chars[i] == '；' || chars[i] == ':' || chars[i] == ',') {
                            
                            String chunk = buffer.toString();
                            
                            java.util.Map<String, Object> map = new java.util.HashMap<>();
                            map.put("content", chunk);
                            String json = cn.hutool.json.JSONUtil.toJsonStr(map);
                            
                            writer.write("event: message\n");
                            writer.write("data: " + json + "\n\n");
                            writer.flush();
                            
                            buffer.setLength(0);
                            Thread.sleep(50);
                        }
                    }
                    
                    // 推送剩余内容
                    if (buffer.length() > 0) {
                        String chunk = buffer.toString();
                        java.util.Map<String, Object> map = new java.util.HashMap<>();
                        map.put("content", chunk);
                        String json = cn.hutool.json.JSONUtil.toJsonStr(map);
                        
                        writer.write("event: message\n");
                        writer.write("data: " + json + "\n\n");
                        writer.flush();
                    }
                }
            }
            
            // 发送 done 标志
            writer.write("event: done\n");
            writer.write("data: [DONE]\n\n");
            writer.flush();
            
            System.out.println("业务操作执行完成");
            
        } catch (Exception e) {
            System.err.println("执行业务操作失败：" + e.getMessage());
            e.printStackTrace();
            try {
                writer.write("event: error\n");
                writer.write("data: {\"error\":\"" + e.getMessage() + "\"}\n\n");
                writer.flush();
            } catch (Exception ex) {
                // 忽略
            }
        }
    }
    
    /**
     * 生成详细的推荐文案（分段版本，类似淘宝问问）
     */
    private String[] generateDetailedRecommendationText(String userMessage, ChatResponse response, String intent) {
        String keyword = extractKeyword(userMessage);
        
        // 获取商品列表
        java.util.List<Good> goods = new java.util.ArrayList<>();
        if (response.getActionData() instanceof java.util.Map) {
            Object goodsObj = ((java.util.Map<?, ?>) response.getActionData()).get("goods");
            if (goodsObj instanceof java.util.List) {
                goods = (java.util.List<Good>) goodsObj;
            }
        }
        
        // 提取价格信息
        String priceInfo = extractPriceInfo(userMessage);
        
        // 第一段：淘宝问问风格 - 简洁明了的推荐说明
        String paragraph1 = "为您推荐几款口碑超好的" + (keyword.isEmpty() ? "热门" : keyword) + 
                           (priceInfo.isEmpty() ? "" : "，价格都在" + priceInfo + "以内") + "，闭眼入不踩雷！";
        
        // 第二段：商品亮点介绍（如果有商品）
        String paragraph2 = "";
        if (!goods.isEmpty()) {
            Good topGood = goods.get(0);
            // 提取商品特点
            String feature = extractFeature(topGood.getDescription());
            
            // 获取评论信息
            String reviewInfo = "";
            if (topGood.getFirstReview() != null && topGood.getFirstReview().getContent() != null) {
                reviewInfo = "，首评说：" + truncateReview(topGood.getFirstReview().getContent(), 30);
            } else if (topGood.getGoodRating() != null) {
                reviewInfo = "，好评率" + String.format("%.1f", topGood.getGoodRating().doubleValue()) + "分";
            }
            
            paragraph2 = "首推这款「" + topGood.getName() + "」，" + feature + reviewInfo + "，销量超高！";
        }
        
        // 第三段：引导语
        String paragraph3 = "看看有没有心动的？直接加购或者查看详情吧～";
        
        return new String[] { paragraph1, paragraph2, paragraph3 };
    }
    
    /**
     * 截断评论文本
     */
    private String truncateReview(String content, int maxLength) {
        if (content == null || content.isEmpty()) return "";
        if (content.length() <= maxLength) return content;
        return content.substring(0, maxLength) + "...";
    }
    
    /**
     * 提取价格信息
     */
    private String extractPriceInfo(String message) {
        if (message == null || message.isEmpty()) return "";
        
        // 匹配 "XX 以下"、"低于 XX"、"不超过 XX" 等
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?:价格 (?:在)?|低于|不超过|以内|以下)(\\d+)");
        java.util.regex.Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return matcher.group(1) + "元";
        }
        
        return "";
    }
    
    /**
     * 提取商品特点
     */
    private String extractFeature(String description) {
        if (description == null || description.isEmpty()) return "性价比超高";
        
        // 提取关键词
        String[] features = {"纯棉", "舒适", "透气", "宽松", "保暖", "加厚", "时尚", "百搭", 
                            "简约", "韩版", "休闲", "运动", "商务", "潮流", "显瘦", "修身"};
        
        for (String feature : features) {
            if (description.contains(feature)) {
                return feature + "舒适";
            }
        }
        
        return "性价比超高";
    }
    
    /**
     * 从用户消息中提取关键词
     */
    private String extractKeyword(String message) {
        if (message == null || message.isEmpty()) return "";
        
        // 扩展的商品类别关键词（按类别分组）
        String[][] categoryGroups = {
            // 服装鞋包
            {"卫衣", "T恤", "衬衫", "外套", "夹克", "风衣", "羽绒服", "棉衣", "毛衣", "针织衫", 
             "裤子", "牛仔裤", "休闲裤", "运动裤", "短裤", "裙子", "连衣裙", "半身裙",
             "鞋子", "运动鞋", "皮鞋", "帆布鞋", "靴子", "凉鞋", "拖鞋", "高跟鞋",
             "包包", "背包", "手提包", "钱包", "斜挎包", "双肩包"},
            // 数码电子
            {"手机", "笔记本", "电脑", "平板", "iPad", "耳机", "音箱", "音响", 
             "鼠标", "键盘", "显示器", "相机", "摄像机", "无人机", "智能手表", "手环",
             "充电宝", "数据线", "充电器", "U盘", "硬盘", "内存卡"},
            // 食品饮料
            {"零食", "巧克力", "牛肉干", "薯片", "饼干", "蛋糕", "面包", "糖果",
             "水果", "坚果", "干果", "茶叶", "咖啡", "牛奶", "酸奶", "果汁",
             "白酒", "红酒", "啤酒", "饮料", "矿泉水"},
            // 家居生活
            {"毛巾", "浴巾", "床单", "被套", "枕头", "被子", "毯子", "窗帘",
             "杯子", "水杯", "保温杯", "碗", "盘子", "筷子", "勺子", "锅", "炒锅",
             "洗衣液", "洗发水", "沐浴露", "牙膏", "牙刷", "洗面奶", "护肤品"},
            // 运动户外
            {"运动服", "瑜伽服", "健身服", "跑步鞋", "篮球鞋", "足球", "篮球", "排球",
             "羽毛球", "乒乓球", "网球", "健身器材", "哑铃", "瑜伽垫", "帐篷", "睡袋"}
        };
        
        // 扁平化所有关键词
        List<String> allKeywords = new ArrayList<>();
        for (String[] group : categoryGroups) {
            for (String keyword : group) {
                allKeywords.add(keyword);
            }
        }
        
        // 查找匹配的关键词
        for (String keyword : allKeywords) {
            if (message.contains(keyword)) {
                return keyword;
            }
        }
        
        // 如果没有找到具体关键词，尝试提取可能的商品名称
        // 移除常见的动词和修饰词
        String[] removeWords = {"推荐", "我想买", "给我", "来点", "看看", "找", "搜索", "查询"};
        String cleanedMessage = message;
        for (String word : removeWords) {
            cleanedMessage = cleanedMessage.replace(word, "");
        }
        cleanedMessage = cleanedMessage.trim();
        
        // 如果清理后还有内容，返回清理后的内容
        if (!cleanedMessage.isEmpty() && cleanedMessage.length() > 1) {
            return cleanedMessage;
        }
        
        return "";
    }
    
    /**
     * 使用 DeepSeek 进行意图识别（复用原有的大段 prompt 逻辑）
     * @param message 用户消息
     * @return 意图
     */
    private String recognizeIntentWithDeepSeek(String message) {
        try {
            // 构建 prompt，包含所有可能的意图选项
            System.out.println("开始构建意图识别 prompt（DeepSeek）");
            String prompt = "你是一个电商智能助手，需要准确识别用户的意图。请从以下选项中选择最符合用户意图的操作：\n\n" +
                            "【意图选项】\n" +
                            "1. search_goods（搜索商品）- 当用户想要搜索、查找、寻找某类商品时（**主动搜索行为**）\n" +
                            "2. add_to_cart（添加购物车）- 当用户想要将某个具体商品加入购物车时\n" +
                            "3. batch_add_cart（批量添加购物车）- 当用户想要同时添加多个商品到购物车时\n" +
                            "4. query_order（查询订单）- 当用户想要查询订单列表、查看订单状态、找到我的订单时（**简单查询**）\n" +
                            "   ⚠️ **重要**：只是查看订单，不包含'分析'、'统计'、'报告'等词\n" +
                            "   - '我的订单' → query_order\n" +
                            "   - '查询订单' → query_order\n" +
                            "   - '查看订单列表' → query_order\n" +
                            "5. navigate（页面导航）- 当用户想要**跳转**到某个页面、**去**首页、**到**个人中心等时\n" +
                            "6. logout（退出登录）- 当用户想要退出、注销登录时\n" +
                            "7. recommend_goods（通用推荐）- 当用户想要推荐商品、有什么好东西、推荐一些商品等**无具体品类**时\n" +
                            "8. specific_recommend（具体品类推荐）- 当用户想要推荐**具体品类**的商品时（如卫衣、手机、零食等）\n" +
                            "   ⚠️ **重要**：只要用户说'推荐 XXX'（XXX 是具体商品），就选择 specific_recommend，不要选择 search_goods\n" +
                            "   - '推荐一些卫衣' → specific_recommend\n" +
                            "   - '推荐手机' → specific_recommend\n" +
                            "   - '推荐零食' → specific_recommend\n" +
                            "9. quick_order（一键下单）- 当用户想要快速购买、直接下单、立即购买时\n" +
                            "10. track_order（订单状态跟踪）- 当用户想要查询、追踪、查看订单列表、**查看物流信息**、**查看配送进度**、**查看地址**时\n" +
                            "   - '订单追踪' → track_order\n" +
                            "   - '追踪我的订单' → track_order\n" +
                            "   - '查看订单' → track_order\n" +
                            "   - '我的订单物流' → track_order\n" +
                            "   - '查看发货地址' → track_order\n" +
                            "   ⚠️ **重要**：不包含'分析'、'统计'等关键词时，优先选择 track_order\n" +
                            "   ⚠️ **重要**：track_order 会展示订单卡片列表，每个卡片包含订单号、金额、状态、收货地址和发货地址，并支持百度地图查看物流路线\n" +
                            "11. analyze_orders（订单历史分析）- 当用户想要**分析**订单数据、查看消费报告、了解购买习惯、进行订单统计时（**深度分析**）\n" +
                            "   ⚠️ **重要**：包含'分析'、'统计'、'报告'等关键词时，必须选择 analyze_orders\n" +
                            "   - '分析订单' → analyze_orders\n" +
                            "   - '订单分析' → analyze_orders\n" +
                            "   - '分析我的订单' → analyze_orders\n" +
                            "   - '消费分析' → analyze_orders\n" +
                            "   - '订单统计' → analyze_orders\n" +
                            "   - '购物报告' → analyze_orders\n" +
                            "   ⚠️ **重要**：analyze_orders 会展示订单统计、状态分布柱状图、月度消费趋势和消费洞察报告\n" +
                            "12. analyze_sentiment（评论舆情分析）- 当用户想要分析商品评论、查看评分分布、情感分析时（通常需要提供商品ID）\n" +
                            "   - '分析商品305的评论' → analyze_sentiment\n" +
                            "   - '看看这个商品的评分分布' → analyze_sentiment\n" +
                            "   - '商品305的口碑怎么样' → analyze_sentiment\n" +
                            "13. analyze_sales（销售数据分析）- 当用户想要查看销售报告、销量趋势、销售排行时（通常需要提供商品ID）\n" +
                            "   - '生成商品305的销售报告' → analyze_sales\n" +
                            "   - '查看最近30天的销售数据' → analyze_sales\n" +
                            "   - '分析一下销售趋势' → analyze_sales\n" +
                            "14. view_good_detail（查看商品详情）- 当用户想要查看某个商品的详细信息、规格、评论等时\n" +
                            "   ⚠️ **重要**：用户说'第一个'、'第二个'、'第X个'等指代词时，选择 view_good_detail\n" +
                            "   - '第一个' → view_good_detail\n" +
                            "   - '第二个' → view_good_detail\n" +
                            "   - '第三款' → view_good_detail\n" +
                            "   - '详情' → view_good_detail\n" +
                            "15. chat（普通聊天）- 当用户只是闲聊、问候、或者**询问问题**、**了解信息**时\n" +
                            "16. unknown（暂不明白用户意图）- 当完全无法理解用户意思时\n\n" +
                            "【关键判断规则 - 必须严格遵守】\n" +
                            "❌ 如果用户的问题是**询问性质**的（包含'有什么用'、'是什么'、'怎么样'、'为什么'、'吗'等疑问词），选择 chat\n" +
                            "✅ 如果用户表达的是**操作意愿**的（包含'去'、'到'、'打开'、'进入'、'跳转'等动词），选择 navigate\n" +
                            "- 例如：'首页有什么用' → chat（询问信息，不是跳转）\n" +
                            "- 例如：'去首页' → navigate（执行操作）\n" +
                            "- 例如：'首页有什么' → chat（询问信息）\n" +
                            "- 例如：'到首页看看' → navigate（执行操作）\n" +
                            "✅ **推荐场景判断规则（重要）**：\n" +
                            "- 如果用户说'推荐商品'、'推荐一些商品'、'有什么好东西'、'帮我推荐'等**通用推荐**（没有具体商品类别），选择 recommend_goods\n" +
                            "- 如果用户提到**具体商品类别**（如卫衣、手机、零食、笔记本、鞋子、衣服等），选择 specific_recommend\n" +
                            "- 简单判断：有具体商品名称→specific_recommend，无具体商品名称→recommend_goods\n" +
                            "- 例如：'推荐一些商品' → recommend_goods（通用推荐）\n" +
                            "- 例如：'推荐一些卫衣' → specific_recommend（具体品类：卫衣）\n" +
                            "- 例如：'推荐手机' → specific_recommend（具体品类：手机）\n" +
                            "- 例如：'有什么好吃的' → specific_recommend（具体品类：吃的/零食）\n" +
                            "- 例如：'帮我推荐笔记本' → specific_recommend（具体品类：笔记本）\n" +
                            "✅ **订单意图判断规则（非常重要）**：\n" +
                            "- 如果用户想要**查看订单列表**、**追踪物流**、**看地址**，选择 track_order（展示订单列表+百度地图）\n" +
                            "- 如果用户想要**分析**、**统计**消费数据，选择 analyze_orders（展示分析报告+图表）\n" +
                            "- '我的订单' → track_order（查看订单列表）\n" +
                            "- '查询订单' → track_order（查看订单列表）\n" +
                            "- '订单追踪' → track_order（查看订单列表+物流）\n" +
                            "- '追踪订单' → track_order（查看订单列表+物流）\n" +
                            "- '分析订单' → analyze_orders（深度分析，含图表）\n" +
                            "- '订单分析' → analyze_orders（深度分析，含图表）\n" +
                            "- '分析我的订单' → analyze_orders（深度分析，含图表）\n" +
                            "- '消费分析' → analyze_orders（深度分析，含图表）\n" +
                            "- '订单统计' → analyze_orders（深度分析，含图表）\n" +
                            "✅ **评论分析判断规则**：\n" +
                            "- 如果用户提到'评论'、'评分'、'口碑'、'评价'、'舆情'等词，并且有商品ID或提到具体商品，选择 analyze_sentiment\n" +
                            "- 例如：'分析商品305的评论' → analyze_sentiment\n" +
                            "- 例如：'看看这个商品的评分' → analyze_sentiment\n" +
                            "✅ **销售分析判断规则**：\n" +
                            "- 如果用户提到'销售'、'销量'、'报告'、'趋势'、'排行'等词，选择 analyze_sales\n" +
                            "- 例如：'生成销售报告' → analyze_sales\n" +
                            "- 例如：'查看销售趋势' → analyze_sales\n" +
                            "✅ **上下文指代识别规则（非常重要）**：\n" +
                            "- 如果用户说'第一个'、'第二个'、'第三款'等（第X个/款），表示查看之前推荐的商品详情，选择 view_good_detail\n" +
                            "- 如果用户说'第一个加入购物车'、'第二个加购'、'第X个放入购物车'等，表示将指定商品加入购物车，选择 add_to_cart\n" +
                            "- 如果用户说'第一个购买'、'第二个下单'、'第X个买'等，表示购买指定商品，选择 quick_order\n" +
                            "- 如果用户说'加入购物车'、'加购'但没有指定商品，表示将刚才推荐的商品加入购物车，选择 add_to_cart\n" +
                            "- 如果用户说'详情'、'看看'、'看一下'，表示查看之前推荐的商品详情，选择 view_good_detail\n" +
                            "- 如果用户说'买这个'、'要这个'、'就要这个'，表示购买之前推荐的商品，选择 add_to_cart\n" +
                            "- 例如：AI 推荐了商品后，用户说'第二个' → view_good_detail\n" +
                            "- 例如：AI 推荐了商品后，用户说'第一个加入购物车' → add_to_cart\n" +
                            "- 例如：AI 推荐了商品后，用户说'第二个购买' → quick_order\n" +
                            "- 例如：AI 推荐了商品后，用户说'加入购物车' → add_to_cart\n" +
                            "- 例如：AI 推荐了商品后，用户说'详情' → view_good_detail\n" +
                            "- 例如：AI 推荐了商品后，用户说'买这个' → add_to_cart\n" +
                            "- 如果用户只是闲聊或问一些与购物无关的问题，选择 chat\n" +
                            "- 如果用户的问题模糊不清，难以判断，选择 unknown\n\n" +
                            "【示例 - 仔细学习】\n" +
                            "用户：'给我推荐一些商品' → recommend_goods\n" +
                            "用户：'首页有什么用' → chat（询问信息，不是跳转）\n" +
                            "用户：'首页有什么功能' → chat（询问信息）\n" +
                            "用户：'去首页' → navigate（执行跳转）\n" +
                            "用户：'到首页看看' → navigate（执行跳转）\n" +
                            "用户：'我想买手机' → search_goods\n" +
                            "用户：'我的订单在哪' → chat（询问位置信息）\n" +
                            "用户：'查询我的订单' → track_order\n" +
                            "用户：'我的订单' → track_order\n" +
                            "用户：'订单追踪' → track_order\n" +
                            "用户：'追踪我的订单' → track_order\n" +
                            "用户：'分析订单' → analyze_orders\n" +
                            "用户：'订单分析' → analyze_orders\n" +
                            "用户：'分析我的订单' → analyze_orders\n" +
                            "用户：'帮我下单' → quick_order\n" +
                            "用户：'你好' → chat\n" +
                            "用户：'今天天气怎么样' → chat\n" +
                            "用户：'这个商品怎么样' → chat（询问信息）\n" +
                            "用户：'看看这个商品' → search_goods（查看商品）\n" +
                            "用户：'推荐一些卫衣' → specific_recommend（具体品类推荐）\n" +
                            "用户：'推荐手机' → specific_recommend（具体品类推荐）\n" +
                            "用户：'推荐零食' → specific_recommend（具体品类推荐）\n" +
                            "用户：'分析商品305的评论' → analyze_sentiment（评论分析）\n" +
                            "用户：'生成商品305的销售报告' → analyze_sales（销售分析）\n" +
                            "用户：'查看销售趋势' → analyze_sales（销售分析）\n" +
                            "用户：'第二个' → view_good_detail（查看推荐商品详情）\n" +
                            "用户：'第一个' → view_good_detail（查看推荐商品详情）\n" +
                            "用户：'第一个加入购物车' → add_to_cart（将第一个商品加入购物车）\n" +
                            "用户：'第二个购买' → quick_order（购买第二个商品）\n" +
                            "用户：'加入购物车' → add_to_cart（将推荐商品加入购物车）\n" +
                            "用户：'详情' → view_good_detail（查看推荐商品详情）\n" +
                            "用户：'买这个' → add_to_cart（将推荐商品加入购物车）\n\n" +
                            "【用户消息】\n" +
                            "" + message + "\n\n" +
                            "请只返回一个英文单词（search_goods、add_to_cart、batch_add_cart、query_order、navigate、logout、recommend_goods、specific_recommend、quick_order、track_order、analyze_orders、analyze_sentiment、analyze_sales、view_good_detail、chat 或 unknown），不要返回任何其他内容。";
            System.out.println("发送了prompt");
            
            // 获取 DeepSeek AI 响应
            System.out.println("发送意图识别请求到 DeepSeek");
            String aiResponse = getDeepSeekAiResponse(prompt);
            System.out.println("DeepSeek 返回的意图识别结果：" + aiResponse);
            
            // 处理 AI 响应
            aiResponse = aiResponse.trim();
            System.out.println("处理后的意图识别结果：" + aiResponse);
            
            // 验证响应（支持大小写不敏感）
            java.util.Set<String> validIntents = java.util.Collections.unmodifiableSet(
                new java.util.HashSet<>(java.util.Arrays.asList(
                    "search_goods", "add_to_cart", "batch_add_cart", "query_order", 
                    "navigate", "logout", "recommend_goods", "specific_recommend", "quick_order", 
                    "track_order", "analyze_orders", "analyze_sentiment", "analyze_sales", 
                    "view_good_detail", "chat", "unknown"
                )));
            
            // 大小写不敏感的验证
            String matchedIntent = null;
            for (String validIntent : validIntents) {
                if (validIntent.equalsIgnoreCase(aiResponse)) {
                    matchedIntent = validIntent;
                    break;
                }
            }
            
            if (matchedIntent != null) {
                System.out.println("意图识别结果有效，返回：" + matchedIntent);
                return matchedIntent;
            } else {
                System.out.println("意图识别结果无效，降级到传统意图识别");
                // 降级到传统意图识别
                String traditionalIntent = recognizeIntent(message);
                System.out.println("传统意图识别结果：" + traditionalIntent);
                return traditionalIntent;
            }
        } catch (Exception e) {
            System.err.println("DeepSeek 意图识别失败：" + e.getMessage());
            e.printStackTrace();
            // 降级到传统意图识别
            System.out.println("DeepSeek 意图识别失败，降级到传统意图识别");
            String traditionalIntent = recognizeIntent(message);
            System.out.println("传统意图识别结果：" + traditionalIntent);
            return traditionalIntent;
        }
    }
    
    /**
     * 基于上下文识别意图（流式接口专用）
     * 处理"第一个"、"第二个"、"加入购物车"等需要上下文理解的场景
     */
    private String recognizeContextualIntentForStream(String message) {
        message = message.trim();
        
        // 场景1：用户输入"第X个"、"第X款"、"第一个"、"第二个"等
        java.util.regex.Pattern indexPattern = java.util.regex.Pattern.compile("^第([\\d一二三四五六七八九十]+)[款个]$");
        java.util.regex.Matcher indexMatcher = indexPattern.matcher(message);
        if (indexMatcher.find()) {
            System.out.println("上下文识别：用户选择第 " + indexMatcher.group(1) + " 个商品，意图为查看商品详情");
            return "view_good_detail";
        }
        
        // 场2：用户输入"加入购物车"、"加购"等，但没有指定商品
        if (message.matches(".*(加入.*购物车|加购|放入购物车).*") && 
            !message.matches(".*第[\\d一二三四五六七八九十]+[款个].*")) {
            System.out.println("上下文识别：用户要求加入购物车");
            return "add_to_cart";
        }
        
        // 场景3：用户输入"详情"、"看看"、"看一下"等简短词
        if (message.matches("^(详情|看看|看一下|看看详情|查看详情)$")) {
            System.out.println("上下文识别：用户要求查看详情");
            return "view_good_detail";
        }
        
        // 场景4：用户输入"买这个"、"要这个"、"就要这个"等
        if (message.matches(".*(买这个|要这个|就要这个|这个我要).*")) {
            System.out.println("上下文识别：用户要求购买");
            return "add_to_cart";
        }
        
        return null;
    }
    
    /**
     * 基于上下文识别意图（非流式接口专用）
     * 处理"第一个"、"第二个"、"加入购物车"等需要上下文理解的场景
     */
    private AiIntentResult recognizeContextualIntentForChat(ChatRequest request) {
        String message = request.getMessage().trim();
        Long userId = request.getUserId();
        
        // 获取对话上下文
        List<AiContextManager.ContextMessage> contextMessages = contextManager.getMessages(userId);
        if (contextMessages == null || contextMessages.isEmpty()) {
            return null;
        }
        
        // 获取最后一条 AI 消息
        String lastAiMessage = null;
        for (int i = contextMessages.size() - 1; i >= 0; i--) {
            if ("assistant".equals(contextMessages.get(i).getRole())) {
                lastAiMessage = contextMessages.get(i).getContent();
                break;
            }
        }
        
        if (lastAiMessage == null) {
            return null;
        }
        
        // 场景1：用户输入"第X个"、"第X款"、"第一个"、"第二个"等
        java.util.regex.Pattern indexPattern = java.util.regex.Pattern.compile("^第([\\d一二三四五六七八九十]+)[款个]$");
        java.util.regex.Matcher indexMatcher = indexPattern.matcher(message);
        if (indexMatcher.find()) {
            String indexStr = indexMatcher.group(1);
            int index = parseChineseNumber(indexStr);
            
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("index", index);
            
            // 判断上下文：如果上一条 AI 消息包含"推荐"、"搜索"、"找到"等关键词
            // 则识别为查看商品详情
            if (lastAiMessage.contains("推荐") || lastAiMessage.contains("搜索") || 
                lastAiMessage.contains("找到") || lastAiMessage.contains("商品")) {
                AiIntentResult result = new AiIntentResult();
                result.setIntent(AiIntent.VIEW_GOOD_DETAIL);
                result.setConfidence(0.9);
                result.setOriginalQuery(message);
                result.setParameters(parameters);
                result.setCategory("goods");
                result.setNeedClarification(false);
                log.info("上下文识别：用户选择第 {} 个商品，意图为查看商品详情", index);
                return result;
            }
        }
        
        // 场景2：用户输入"加入购物车"、"加购"等，但没有指定商品
        if (message.matches(".*(加入.*购物车|加购|放入购物车).*") && 
            !message.matches(".*第[\\d一二三四五六七八九十]+[款个].*")) {
            
            // 检查上下文是否包含商品推荐
            if (lastAiMessage.contains("推荐") || lastAiMessage.contains("搜索") || 
                lastAiMessage.contains("找到") || lastAiMessage.contains("商品")) {
                
                AiIntentResult result = new AiIntentResult();
                result.setIntent(AiIntent.ADD_TO_CART);
                result.setConfidence(0.85);
                result.setOriginalQuery(message);
                result.setParameters(new HashMap<>());
                result.setCategory("cart");
                result.setNeedClarification(false);
                log.info("上下文识别：用户要求加入购物车，结合上下文推断操作推荐商品");
                return result;
            }
        }
        
        // 场景3：用户输入"详情"、"看看"、"看一下"等简短词
        if (message.matches("^(详情|看看|看一下|看看详情|查看详情)$")) {
            if (lastAiMessage.contains("推荐") || lastAiMessage.contains("搜索") || 
                lastAiMessage.contains("找到") || lastAiMessage.contains("商品")) {
                
                AiIntentResult result = new AiIntentResult();
                result.setIntent(AiIntent.VIEW_GOOD_DETAIL);
                result.setConfidence(0.85);
                result.setOriginalQuery(message);
                result.setParameters(new HashMap<>());
                result.setCategory("goods");
                result.setNeedClarification(false);
                log.info("上下文识别：用户要求查看详情，结合上下文推断查看推荐商品");
                return result;
            }
        }
        
        // 场景4：用户输入"买这个"、"要这个"、"就要这个"等
        if (message.matches(".*(买这个|要这个|就要这个|这个我要).*")) {
            if (lastAiMessage.contains("推荐") || lastAiMessage.contains("搜索") || 
                lastAiMessage.contains("找到") || lastAiMessage.contains("商品")) {
                
                AiIntentResult result = new AiIntentResult();
                result.setIntent(AiIntent.ADD_TO_CART);
                result.setConfidence(0.85);
                result.setOriginalQuery(message);
                result.setParameters(new HashMap<>());
                result.setCategory("cart");
                result.setNeedClarification(false);
                log.info("上下文识别：用户要求购买，结合上下文推断加入购物车");
                return result;
            }
        }
        
        return null;
    }
    
    /**
     * 传统意图识别（降级方案）
     */
    private String recognizeIntent(String message) {
        message = message.toLowerCase();
        
        // 检测是否是询问性质的问题（优先级最高）
        boolean isQuestion = message.contains("有什么用") || message.contains("是什么") || 
                            message.contains("怎么样") || message.contains("为什么") || 
                            message.contains("吗") || message.contains("如何") ||
                            message.contains("怎么") || message.contains("哪些") ||
                            message.contains("什么");
        
        // 如果是询问性质的问题，直接返回 chat
        if (isQuestion) {
            return "chat";
        }
        
        // 推荐意图（优先级高）
        if (message.contains("推荐") || message.contains("有什么好") || message.contains("介绍一下") || 
            message.contains("值得买") || message.contains("好物")) {
            return "recommend";
        }
        
        // 搜索意图
        if (message.contains("搜索") || message.contains("找") || message.contains("查询") || 
            message.contains("看看") || message.contains("想买") || message.contains("有没有")) {
            return "search";
        }
        
        // 添加购物车意图
        if (message.contains("加购") || message.contains("添加购物车") || message.contains("放入购物车")) {
            return "addCart";
        }
        
        // 一键下单意图
        if (message.contains("下单") || message.contains("购买") || message.contains("买") || 
            message.contains("直接买") || message.contains("立即购买")) {
            return "quickOrder";
        }
        
        // 订单状态跟踪意图
        if (message.contains("物流") || message.contains("配送") || message.contains("快递") || 
            message.contains("送到哪") || message.contains("订单状态")) {
            return "trackOrder";
        }
        
        // 订单查询意图
        if (message.contains("订单") || message.contains("我的订单") || message.contains("查询订单")) {
            return "queryOrder";
        }
        
        // 订单分析意图
        if (message.contains("分析") || message.contains("报告") || message.contains("消费") || 
            message.contains("购买习惯") || message.contains("购物报告")) {
            return "analyzeOrders";
        }
        
        // 导航意图（必须是操作意愿，不是询问）
        if (message.contains("去") || message.contains("到") || message.contains("打开") || 
            message.contains("进入") || message.contains("跳转") || message.contains("页面")) {
            return "navigate";
        }
        
        // 退出登录意图
        if (message.contains("退出") || message.contains("注销") || message.contains("登出")) {
            return "logout";
        }
        
        // 其他意图，默认聊天
        return "chat";
    }
    
    /**
     * 提取实体
     */
    private Object extractEntities(String message, String intent) {
        switch (intent) {
            case "search_goods":
                return extractSearchKeyword(message);
            case "add_to_cart":
                return extractProductInfo(message);
            case "query_order":
                return extractOrderId(message);
            case "navigate":
                return extractNavigateTarget(message);
            case "track_order":
                return extractOrderId(message);
            case "quick_order":
                return extractOrderInfo(message);
            case "recommend_goods":
                return extractRecommendKeyword(message);
            case "specific_recommend":
                return extractSpecificRecommendParams(message);
            case "analyze_sentiment":
                return extractGoodIdForAnalysis(message);
            case "analyze_sales":
                return extractSalesParamsForAnalysis(message);
            default:
                return null;
        }
    }
    
    /**
     * 提取搜索关键词
     */
    private String extractSearchKeyword(String message) {
        String[] keywords = {"搜索", "找", "查询", "看看"};
        for (String keyword : keywords) {
            if (message.contains(keyword)) {
                return message.replace(keyword, "").trim();
            }
        }
        return message;
    }
    
    /**
     * 提取商品信息
     */
    private java.util.Map<String, Object> extractProductInfo(String message) {
        java.util.Map<String, Object> productInfo = new java.util.HashMap<>();
        
        String[] keywords = {"加购", "添加购物车", "放入购物车"};
        for (String keyword : keywords) {
            if (message.contains(keyword)) {
                String productName = message.replace(keyword, "").trim();
                productInfo.put("name", productName);
                break;
            }
        }
        
        return productInfo;
    }
    
    /**
     * 提取订单号
     */
    private String extractOrderId(String message) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\d{8,20}");
        java.util.regex.Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }
    
    /**
     * 提取导航目标
     */
    private String extractNavigateTarget(String message) {
        String[] targets = {"首页", "个人中心", "购物车", "订单", "商品列表", "收藏夹"};
        for (String target : targets) {
            if (message.contains(target)) {
                return target;
            }
        }
        return "首页";
    }
    
    /**
     * 提取订单信息
     */
    private java.util.Map<String, Object> extractOrderInfo(String message) {
        java.util.Map<String, Object> orderInfo = new java.util.HashMap<>();
        orderInfo.put("message", message);
        return orderInfo;
    }
    
    /**
     * 提取推荐关键词
     */
    private String extractRecommendKeyword(String message) {
        String[] keywords = {"推荐", "有什么好", "介绍一下", "值得买", "好物"};
        for (String keyword : keywords) {
            if (message.contains(keyword)) {
                return message.replace(keyword, "").trim();
            }
        }
        return "热门商品";
    }
    
    /**
     * 提取具体品类推荐参数（品类、价格范围、排序方式等）
     * 使用AI进行智能提取，支持任意品类和复杂的参数描述
     */
    private java.util.Map<String, Object> extractSpecificRecommendParams(String message) {
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        
        try {
            // 构建AI提取参数的prompt
            String extractPrompt = "你是一个电商智能助手的参数提取器。请从用户消息中提取以下参数，并以JSON格式返回：\n\n" +
                    "【需要提取的参数】\n" +
                    "1. category（品类）- 用户想要推荐的商品类别（如鞋子、手机、卫衣等）\n" +
                    "2. minPrice（最低价格）- 价格下限，数字类型，单位元（如果没有则返回null）\n" +
                    "3. maxPrice（最高价格）- 价格上限，数字类型，单位元（如果没有则返回null）\n" +
                    "4. sortBy（排序方式）- sales（按销量）、rating（按好评）、price（按价格）、default（默认综合排序）\n\n" +
                    "【提取规则】\n" +
                    "- 品类：提取用户提到的具体商品类别，如鞋子、运动鞋、手机等\n" +
                    "- 价格：识别XXX以内、不超过XXX、低于XXX、XXX以下作为maxPrice\n" +
                    "- 价格：识别XXX以上、高于XXX、超过XXX作为minPrice\n" +
                    "- 价格：识别XXX到XXX、XXX-XXX作为价格区间\n" +
                    "- 排序：如果用户提到销量、卖得好、热门，sortBy为sales\n" +
                    "- 排序：如果用户提到好评、评分、口碑，sortBy为rating\n" +
                    "- 排序：如果用户提到价格、便宜、贵，sortBy为price\n" +
                    "- 排序：如果没有特别说明，sortBy为default\n\n" +
                    "【返回格式】\n" +
                    "只返回JSON对象，不要返回其他内容。格式如下：\n" +
                    "{\"category\":\"品类名称\",\"minPrice\":数字或null,\"maxPrice\":数字或null,\"sortBy\":\"排序方式\"}\n\n" +
                    "【示例】\n" +
                    "用户：推荐价格在500以内的鞋子 -> {\"category\":\"鞋子\",\"minPrice\":null,\"maxPrice\":500,\"sortBy\":\"default\"}\n" +
                    "用户：推荐100到300元的运动鞋，按销量排序 -> {\"category\":\"运动鞋\",\"minPrice\":100,\"maxPrice\":300,\"sortBy\":\"sales\"}\n" +
                    "用户：推荐便宜的手机 -> {\"category\":\"手机\",\"minPrice\":null,\"maxPrice\":null,\"sortBy\":\"price\"}\n" +
                    "用户：推荐好评多的卫衣 -> {\"category\":\"卫衣\",\"minPrice\":null,\"maxPrice\":null,\"sortBy\":\"rating\"}\n\n" +
                    "【用户消息】\n" +
                    message + "\n\n" +
                    "请返回JSON：";
            
            // 调用AI提取参数
            String aiResponse = getDeepSeekAiResponse(extractPrompt);
            log.info("AI提取的推荐参数：{}", aiResponse);
            
            // 解析AI返回的JSON
            cn.hutool.json.JSONObject json = cn.hutool.json.JSONUtil.parseObj(aiResponse);
            
            // 提取品类
            String category = json.getStr("category", "");
            params.put("requirement", category);
            
            // 提取价格范围
            if (json.containsKey("minPrice") && !json.isNull("minPrice")) {
                params.put("minPrice", json.getDouble("minPrice"));
            }
            if (json.containsKey("maxPrice") && !json.isNull("maxPrice")) {
                params.put("maxPrice", json.getDouble("maxPrice"));
            }
            
            // 提取排序方式
            String sortBy = json.getStr("sortBy", "default");
            params.put("sortBy", sortBy);
            
            log.info("AI提取的具体品类推荐参数：{}", params);
            
        } catch (Exception e) {
            log.error("AI提取推荐参数失败，降级到规则提取：{}", e.getMessage());
            
            // 降级到规则提取
            params.put("requirement", extractKeyword(message));
            params.put("sortBy", "default");
            
            // 尝试用正则提取价格
            java.util.regex.Pattern maxPricePattern = java.util.regex.Pattern.compile("(?:以内|不超过|低于|以下)[:：]?\\s*(\\d+)");
            java.util.regex.Matcher maxPriceMatcher = maxPricePattern.matcher(message);
            if (maxPriceMatcher.find()) {
                params.put("maxPrice", Double.parseDouble(maxPriceMatcher.group(1)));
            }
            
            java.util.regex.Pattern minPricePattern = java.util.regex.Pattern.compile("(?:以上|高于|超过)[:：]?\\s*(\\d+)");
            java.util.regex.Matcher minPriceMatcher = minPricePattern.matcher(message);
            if (minPriceMatcher.find()) {
                params.put("minPrice", Double.parseDouble(minPriceMatcher.group(1)));
            }
        }
        
        return params;
    }
    
    /**
     * 提取商品ID（用于评论分析和销售分析）
     */
    private java.util.Map<String, Object> extractGoodIdForAnalysis(String message) {
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        
        // 使用正则表达式提取商品ID
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?:商品|ID|id)[:：]?\\s*(\\d+)");
        java.util.regex.Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            params.put("goodId", Long.parseLong(matcher.group(1)));
        } else {
            // 尝试提取消息中的任意数字
            pattern = java.util.regex.Pattern.compile("\\b(\\d{1,10})\\b");
            matcher = pattern.matcher(message);
            if (matcher.find()) {
                params.put("goodId", Long.parseLong(matcher.group(1)));
            }
        }
        
        return params;
    }
    
    /**
     * 提取销售分析参数（商品ID和天数）
     */
    private java.util.Map<String, Object> extractSalesParamsForAnalysis(String message) {
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        
        // 提取商品ID
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?:商品|ID|id)[:：]?\\s*(\\d+)");
        java.util.regex.Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            params.put("goodId", Long.parseLong(matcher.group(1)));
        } else {
            // 尝试提取消息中的任意数字
            pattern = java.util.regex.Pattern.compile("\\b(\\d{1,10})\\b");
            matcher = pattern.matcher(message);
            if (matcher.find()) {
                params.put("goodId", Long.parseLong(matcher.group(1)));
            }
        }
        
        // 提取天数
        pattern = java.util.regex.Pattern.compile("(\\d+)\\s*天");
        matcher = pattern.matcher(message);
        if (matcher.find()) {
            params.put("days", Integer.parseInt(matcher.group(1)));
        } else {
            params.put("days", 30); // 默认30天
        }
        
        return params;
    }
    
    /**
     * 获取 DeepSeek AI 响应（非流式，用于意图识别）
     */
    private String getDeepSeekAiResponse(String prompt) throws Exception {
        try {
            System.out.println(">>> 调用 DeepSeek API 获取响应...");
            
            // 使用 LangChain4j 直接生成响应
            String response = langChainAiService.chat(null, prompt);
            
            System.out.println("<<< DeepSeek API 响应成功");
            return response;
        } catch (Exception e) {
            System.err.println("获取 DeepSeek AI 响应失败：" + e.getMessage());
            throw e;
        }
    }
}
