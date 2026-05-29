package com.rabbiter.em.ai.service;

import cn.hutool.json.JSONUtil;
import com.rabbiter.em.ai.core.AiContextManager;
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
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP 智能购买服务 - 独立于AI对话和智能帮助
 * 专门用于通过MCP工具调用完成真实商品购买流程
 * 
 * 购买流程：
 * 1. 用户描述购买需求（如"我想买手机"）
 * 2. MCP调用searchProducts推荐商品
 * 3. 前端展示商品卡片
 * 4. 用户选择商品
 * 5. MCP调用addToCart加入购物车
 * 6. MCP调用createOrder创建订单
 * 7. 前端引导用户完成支付
 */
@Service
public class McpPurchaseService {

    private static final Logger log = LoggerFactory.getLogger(McpPurchaseService.class);

    @Resource
    private ChatLanguageModel chatModel;

    @Resource
    private MallToolService mallToolService;

    @Resource
    private AiContextManager contextManager;

    // 购买流程状态存储
    private final Map<Long, PurchaseSession> purchaseSessions = new ConcurrentHashMap<>();

    /**
     * 购买会话状态
     */
    private static class PurchaseSession {
        Long userId;
        String purchaseIntent; // 购买意图
        List<Map<String, Object>> selectedGoods = new ArrayList<>(); // 已选商品
        String currentStep; // 当前步骤
        Map<String, Object> context = new HashMap<>(); // 上下文信息
        List<Map<String, Object>> lastSearchResults = new ArrayList<>(); // 最近一次搜索结果
        String lastSearchKeyword; // 最近一次搜索关键词

        PurchaseSession(Long userId) {
            this.userId = userId;
            this.currentStep = "init";
        }
    }

    /**
     * MCP 购买流式聊天主入口
     * 直接调用百度优选MCP，不经过LLM工具调用循环
     */
    public void streamPurchaseChat(Map<String, Object> request, PrintWriter writer) {
        Long userId = request.get("userId") != null ? Long.valueOf(request.get("userId").toString()) : null;
        String message = request.get("message") != null ? request.get("message").toString() : "";

        try {
            log.info("=== MCP 购买流程开始（直接调用百度优选MCP）===");
            log.info("用户消息：{}", message);

            // 获取或创建购买会话
            PurchaseSession session = purchaseSessions.computeIfAbsent(userId, PurchaseSession::new);

            // 1. 保存用户消息到上下文
            if (userId != null) {
                contextManager.addMessage(userId, "user", message);
            }

            // 2. 检查是否是选择商品的消息（如"第一款"、"第2个"等）
            if (isProductSelection(message, session)) {
                handleProductSelection(message, session, writer);
                return;
            }
            
            // 2.5 检查是否是提供地址信息的消息（包含"收货人"、"电话"、"地址"等关键词）
            if (isAddressInfo(message, session)) {
                handleAddressInfo(message, session, writer);
                return;
            }
            
            // 2.6 检查是否是立即购买的消息
            if (isBuyNowRequest(message, session)) {
                handleBuyNowRequest(message, session, writer);
                return;
            }

            // 3. 发送"搜索中"SSE事件
            sendSseEvent(writer, "message",
                    JSONUtil.toJsonStr(Map.of("content", "正在为您搜索真实商品...", "type", "thinking")));

            // 4. 直接调用百度优选MCP搜索商品（不经过LLM，直接使用用户原始消息）
            log.info("直接调用百度优选MCP搜索：{}", message);
            
            // 直接使用用户原始消息作为搜索关键词，让百度MCP自己分析
            String keyword = message;
            log.info("使用原始消息作为搜索关键词：{}", keyword);
            
            String searchResult = mallToolService.searchProducts(keyword);
            log.info("百度优选MCP搜索结果：{}", searchResult.substring(0, Math.min(200, searchResult.length())));

            // 5. 解析搜索结果
            Map<String, Object> parsedResult = parseSearchResult(searchResult);
            
            // 6. 保存搜索结果到会话上下文（用于后续选择）
            if (parsedResult.containsKey("goods")) {
                session.lastSearchResults = (List<Map<String, Object>>) parsedResult.get("goods");
                session.lastSearchKeyword = keyword;
                log.info("已保存 {} 个搜索结果到会话上下文", session.lastSearchResults.size());
            }

            // 7. 发送搜索结果给前端
            Map<String, Object> actionMap = new HashMap<>();
            actionMap.put("action", "search_products");
            actionMap.put("actionData", parsedResult);
            actionMap.put("step", "search");
            sendSseEvent(writer, "action", JSONUtil.toJsonStr(actionMap));

            // 8. 更新会话状态
            session.currentStep = "search";
            session.purchaseIntent = message;

            // 9. 生成回复文本
            String finalText = generateSearchResponse(searchResult, keyword);
            log.info("生成回复：{}", finalText);

            // 10. 流式输出回复
            streamTextByChar(finalText, writer);

            if (userId != null) {
                contextManager.addMessage(userId, "assistant", finalText);
            }

            sendSseEvent(writer, "done", "[DONE]");
            log.info("=== MCP 购买流程完成 ===");

        } catch (Exception e) {
            log.error("MCP 购买流程异常", e);
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
     * 判断是否是选择商品的消息
     */
    private boolean isProductSelection(String message, PurchaseSession session) {
        if (session.lastSearchResults == null || session.lastSearchResults.isEmpty()) {
            return false;
        }
        
        // 匹配"第一款"、"第2个"、"第二个"、"第3款"等
        String pattern = "^第[一二三四五六七八九十\\d]+[款个]";
        return message.matches(pattern + ".*") || message.matches(".*" + pattern + ".*");
    }

    /**
     * 处理商品选择（如"第一款"、"第2个"等）
     */
    private void handleProductSelection(String message, PurchaseSession session, PrintWriter writer) {
        try {
            log.info("处理商品选择：{}", message);
            
            // 提取选择的商品编号
            int index = extractProductIndex(message);
            
            if (index < 0 || index >= session.lastSearchResults.size()) {
                String errorText = "抱歉，没有找到对应的商品。请选择1-" + session.lastSearchResults.size() + "之间的商品编号。";
                streamTextByChar(errorText, writer);
                sendSseEvent(writer, "done", "[DONE]");
                writer.close();
                return;
            }
            
            Map<String, Object> selectedProduct = session.lastSearchResults.get(index);
            log.info("用户选择了第 {} 个商品：{}", index + 1, selectedProduct.get("name"));
            
            // 发送商品详情action
            Map<String, Object> actionMap = new HashMap<>();
            actionMap.put("action", "product_detail");
            actionMap.put("actionData", Map.of(
                "product", selectedProduct,
                "index", index + 1,
                "source", "百度优选"
            ));
            actionMap.put("step", "detail");
            sendSseEvent(writer, "action", JSONUtil.toJsonStr(actionMap));
            
            // 生成回复
            StringBuilder sb = new StringBuilder();
            sb.append("好的！您选择的是第").append(index + 1).append("款商品（来自百度优选）：\n\n");
            sb.append("📦 ").append(selectedProduct.get("name")).append("\n");
            if (selectedProduct.containsKey("price")) {
                sb.append("💰 ¥").append(selectedProduct.get("price")).append("\n");
            }
            if (selectedProduct.containsKey("shopName")) {
                sb.append("🏪 ").append(selectedProduct.get("shopName")).append("\n");
            }
            sb.append("\n 您可以：\n");
            sb.append("- 说\"加入购物车\"添加到购物车\n");
            sb.append("- 说\"立即购买\"直接下单\n\n");
            sb.append("如需立即购买，请提供以下信息：\n");
            sb.append("1. 收货人姓名\n");
            sb.append("2. 联系电话\n");
            sb.append("3. 收货地址\n");
            sb.append("例如：收货人张三，电话13800138000，地址北京市朝阳区xxx路xxx号");
            
            streamTextByChar(sb.toString(), writer);
            sendSseEvent(writer, "done", "[DONE]");
            writer.close();
            
        } catch (Exception e) {
            log.error("处理商品选择异常", e);
            try {
                writer.write("event: error\n");
                writer.write("data: {\"error\":\"" + e.getMessage() + "\"}\n\n");
                writer.flush();
                writer.close();
            } catch (Exception ex) {
                // 忽略
            }
        }
    }

    /**
     * 判断是否是提供地址信息的消息
     */
    private boolean isAddressInfo(String message, PurchaseSession session) {
        // 检查是否包含地址相关关键词
        return message.contains("收货人") || message.contains("电话") || 
               message.contains("地址") || message.contains("联系电话") ||
               message.contains("收货地址");
    }
    
    /**
     * 判断是否是立即购买请求
     */
    private boolean isBuyNowRequest(String message, PurchaseSession session) {
        return message.contains("立即购买") || message.contains("直接下单") || 
               message.contains("马上购买") || message.contains("立即下单");
    }
    
    /**
     * 处理地址信息
     */
    private void handleAddressInfo(String message, PurchaseSession session, PrintWriter writer) {
        try {
            log.info("处理地址信息：{}", message);
            
            // 解析地址信息
            String linkUser = extractField(message, "收货人", "姓名");
            String linkPhone = extractField(message, "电话", "联系电话");
            String linkAddress = extractField(message, "地址", "收货地址");
            
            // 保存到会话上下文
            session.context.put("linkUser", linkUser);
            session.context.put("linkPhone", linkPhone);
            session.context.put("linkAddress", linkAddress);
            
            log.info("解析的地址信息 - 收货人：{}, 电话：{}, 地址：{}", linkUser, linkPhone, linkAddress);
            
            // 检查信息是否完整
            if (linkUser == null || linkPhone == null || linkAddress == null) {
                StringBuilder sb = new StringBuilder();
                sb.append("请提供完整的收货信息：\n");
                if (linkUser == null) sb.append("- 收货人姓名\n");
                if (linkPhone == null) sb.append("- 联系电话\n");
                if (linkAddress == null) sb.append("- 收货地址\n");
                sb.append("例如：收货人张三，电话13800138000，地址北京市朝阳区xxx路xxx号");
                
                streamTextByChar(sb.toString(), writer);
                sendSseEvent(writer, "done", "[DONE]");
                writer.close();
                return;
            }
            
            // 信息完整，调用MCP创建订单
            createOrderWithMcp(session, writer);
            
        } catch (Exception e) {
            log.error("处理地址信息异常", e);
            try {
                writer.write("event: error\n");
                writer.write("data: {\"error\":\"" + e.getMessage() + "\"}\n\n");
                writer.flush();
                writer.close();
            } catch (Exception ex) {
                // 忽略
            }
        }
    }
    
    /**
     * 处理立即购买请求
     */
    private void handleBuyNowRequest(String message, PurchaseSession session, PrintWriter writer) {
        try {
            log.info("处理立即购买请求：{}", message);
            
            // 检查是否有已选择的商品
            if (session.lastSearchResults == null || session.lastSearchResults.isEmpty()) {
                streamTextByChar("请先搜索商品并选择要购买的商品。", writer);
                sendSseEvent(writer, "done", "[DONE]");
                writer.close();
                return;
            }
            
            // 检查是否已保存地址信息
            if (session.context.containsKey("linkUser") && 
                session.context.containsKey("linkPhone") && 
                session.context.containsKey("linkAddress")) {
                // 地址信息完整，直接创建订单
                createOrderWithMcp(session, writer);
            } else {
                // 需要用户提供地址信息
                StringBuilder sb = new StringBuilder();
                sb.append("好的，请提供以下收货信息：\n");
                sb.append("1. 收货人姓名\n");
                sb.append("2. 联系电话\n");
                sb.append("3. 收货地址\n");
                sb.append("例如：收货人张三，电话13800138000，地址北京市朝阳区xxx路xxx号");
                
                streamTextByChar(sb.toString(), writer);
                sendSseEvent(writer, "done", "[DONE]");
                writer.close();
            }
            
        } catch (Exception e) {
            log.error("处理立即购买请求异常", e);
            try {
                writer.write("event: error\n");
                writer.write("data: {\"error\":\"" + e.getMessage() + "\"}\n\n");
                writer.flush();
                writer.close();
            } catch (Exception ex) {
                // 忽略
            }
        }
    }
    
    /**
     * 从消息中提取字段值
     */
    private String extractField(String message, String... keywords) {
        for (String keyword : keywords) {
            int index = message.indexOf(keyword);
            if (index >= 0) {
                // 提取关键词后面的内容
                String afterKeyword = message.substring(index + keyword.length()).trim();
                // 找到下一个关键词或结束
                int nextIndex = -1;
                for (String otherKeyword : keywords) {
                    if (!otherKeyword.equals(keyword)) {
                        int idx = afterKeyword.indexOf(otherKeyword);
                        if (idx >= 0 && (nextIndex < 0 || idx < nextIndex)) {
                            nextIndex = idx;
                        }
                    }
                }
                if (nextIndex > 0) {
                    return afterKeyword.substring(0, nextIndex).trim();
                }
                return afterKeyword;
            }
        }
        return null;
    }
    
    /**
     * 调用MCP创建订单
     */
    private void createOrderWithMcp(PurchaseSession session, PrintWriter writer) {
        try {
            log.info("开始调用MCP创建订单");
            
            // 获取商品信息（使用第一个商品作为示例）
            Map<String, Object> product = session.lastSearchResults.get(0);
            String goodsJson = JSONUtil.toJsonStr(List.of(Map.of(
                "goodId", product.get("id"),
                "count", 1,
                "standard", "默认"
            )));
            
            String linkUser = (String) session.context.get("linkUser");
            String linkPhone = (String) session.context.get("linkPhone");
            String linkAddress = (String) session.context.get("linkAddress");
            
            log.info("创建订单参数 - 商品：{}, 收货人：{}, 电话：{}, 地址：{}", 
                    goodsJson, linkUser, linkPhone, linkAddress);
            
            // 调用MCP的createOrder方法
            String orderResult = mallToolService.createOrder(
                session.userId,
                goodsJson,
                linkUser,
                linkPhone,
                linkAddress
            );
            
            log.info("MCP订单创建结果：{}", orderResult);
            
            // 解析订单结果
            Map<String, Object> orderData = JSONUtil.toBean(orderResult, Map.class);
            
            if (orderData.containsKey("orderId") || orderData.containsKey("orderNo")) {
                // 订单创建成功
                StringBuilder sb = new StringBuilder();
                sb.append("订单创建成功！\n\n");
                if (orderData.containsKey("orderId")) {
                    sb.append("订单号：").append(orderData.get("orderId")).append("\n");
                }
                if (orderData.containsKey("orderNo")) {
                    sb.append("订单编号：").append(orderData.get("orderNo")).append("\n");
                }
                if (orderData.containsKey("totalAmount")) {
                    sb.append("订单金额：¥").append(orderData.get("totalAmount")).append("\n");
                }
                sb.append("\n请前往订单页面完成支付。");
                
                streamTextByChar(sb.toString(), writer);
                
                // 发送订单创建成功的action
                Map<String, Object> actionMap = new HashMap<>();
                actionMap.put("action", "order_created");
                actionMap.put("actionData", orderData);
                actionMap.put("step", "order");
                sendSseEvent(writer, "action", JSONUtil.toJsonStr(actionMap));
                
            } else {
                // 订单创建失败
                String errorMsg = orderData.containsKey("error") ? 
                    (String) orderData.get("error") : "订单创建失败";
                streamTextByChar("订单创建失败：" + errorMsg, writer);
            }
            
            sendSseEvent(writer, "done", "[DONE]");
            writer.close();
            
        } catch (Exception e) {
            log.error("调用MCP创建订单异常", e);
            try {
                streamTextByChar("订单创建失败：" + e.getMessage(), writer);
                sendSseEvent(writer, "done", "[DONE]");
                writer.close();
            } catch (Exception ex) {
                // 忽略
            }
        }
    }

    /**
     * 从消息中提取商品编号（如"第一款" -> 0, "第2个" -> 1）
     */
    private int extractProductIndex(String message) {
        // 匹配数字
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("第([\\d一二三四五六七八九十]+)[款个]");
        java.util.regex.Matcher matcher = pattern.matcher(message);
        
        if (matcher.find()) {
            String numStr = matcher.group(1);
            int index = parseChineseNumber(numStr);
            return index - 1; // 转换为0-based索引
        }
        
        return -1;
    }

    /**
     * 解析中文数字
     */
    private int parseChineseNumber(String numStr) {
        // 阿拉伯数字
        try {
            return Integer.parseInt(numStr);
        } catch (NumberFormatException e) {
            // 中文数字
            Map<String, Integer> chineseNums = Map.of(
                "一", 1, "二", 2, "三", 3, "四", 4, "五", 5,
                "六", 6, "七", 7, "八", 8, "九", 9, "十", 10
            );
            return chineseNums.getOrDefault(numStr, -1);
        }
    }

    /**
     * 从用户消息中提取搜索关键词
     */
    private String extractSearchKeyword(String message) {
        // 移除常见的前缀词和后缀词
        String keyword = message
                .replaceAll("我想买|帮我找|搜索|查找|推荐|一部|一些|个|台|部|只|件", "")
                .trim();
        
        // 如果提取后为空或太短，使用原消息
        if (keyword.isEmpty() || keyword.length() < 2) {
            keyword = message;
        }
        
        return keyword;
    }

    /**
     * 解析搜索结果（支持百度优选MCP返回的spuList格式）
     */
    private Map<String, Object> parseSearchResult(String searchResult) {
        try {
            log.info("原始搜索结果：{}", searchResult);
            Map<String, Object> result = JSONUtil.toBean(searchResult, Map.class);
            
            // 如果包含spuList，转换为前端期望的goods格式
            if (result.containsKey("spuList")) {
                List<Map<String, Object>> spuList = (List<Map<String, Object>>) result.get("spuList");
                List<Map<String, Object>> goods = new ArrayList<>();
                
                log.info("百度优选返回spuList数量：{}", spuList.size());
                
                for (int i = 0; i < spuList.size(); i++) {
                    Map<String, Object> spu = spuList.get(i);
                    log.info("商品{}的所有字段：{}", i + 1, spu.keySet());
                    
                    Map<String, Object> good = new HashMap<>();
                    good.put("id", spu.get("spuId"));
                    good.put("name", spu.get("productName"));
                    good.put("price", spu.get("spuPrice"));
                    
                    // 尝试所有可能的图片字段名
                    String imageUrl = null;
                    String[] possibleImageFields = {"image", "imageUrl", "img", "imgUrl", "mainImage", "mainImageUrl", "pic", "picUrl", "thumb", "thumbnail"};
                    for (String field : possibleImageFields) {
                        if (spu.containsKey(field)) {
                            imageUrl = String.valueOf(spu.get(field));
                            log.info("找到图片字段 {} = {}", field, imageUrl);
                            break;
                        }
                    }
                    
                    // 如果还是没有找到，打印所有字段值来调试
                    if (imageUrl == null) {
                        log.warn("未找到图片字段，商品{}的所有字段值：", i + 1);
                        spu.forEach((key, value) -> log.info("  {} = {}", key, value));
                    }
                    
                    good.put("imgs", imageUrl);
                    good.put("shopName", spu.get("shopName"));
                    good.put("sales", spu.get("sales"));
                    good.put("source", "百度优选");
                    
                    // 添加商品跳转链接（百度优选商品详情页）
                    // 优先使用MCP返回的URL字段
                    String productUrl = null;
                    String[] possibleUrlFields = {"detailUrl", "productUrl", "itemUrl", "spuUrl", "url", "linkUrl", "jumpUrl", "h5Url", "wapUrl"};
                    for (String field : possibleUrlFields) {
                        if (spu.containsKey(field) && spu.get(field) != null) {
                            productUrl = String.valueOf(spu.get(field));
                            if (!productUrl.isEmpty()) {
                                log.info("找到URL字段 {} = {}", field, productUrl);
                                break;
                            }
                            productUrl = null;
                        }
                    }
                    
                    // 如果MCP没有返回URL，则手动构建
                    if (productUrl == null || productUrl.isEmpty()) {
                        Object spuId = spu.get("spuId");
                        if (spuId == null) spuId = spu.get("id");
                        if (spuId == null) spuId = spu.get("productId");
                        if (spuId == null) spuId = spu.get("goodsId");
                        if (spuId == null) spuId = spu.get("itemId");
                        
                        if (spuId != null) {
                            productUrl = "https://youxuan.baidu.com/detail/" + spuId;
                            log.info("手动构建商品{}的跳转链接：{}", i + 1, productUrl);
                        } else {
                            log.warn("商品{}未找到ID字段，所有字段：{}", i + 1, spu.keySet());
                        }
                    }
                    
                    if (productUrl != null && !productUrl.isEmpty()) {
                        good.put("spuUrl", productUrl);
                    }
                    
                    // 处理SKU列表
                    if (spu.containsKey("skuList")) {
                        good.put("skuList", spu.get("skuList"));
                    }
                    
                    goods.add(good);
                }
                
                result.put("goods", goods);
                result.put("total", goods.size());
                result.put("source", "百度优选");
                result.put("dataSource", "百度优选MCP");
                
                log.info("解析完成，goods数量：{}", goods.size());
                log.info("第一个商品的imgs字段：{}", goods.isEmpty() ? "无" : goods.get(0).get("imgs"));
            }
            
            return result;
        } catch (Exception e) {
            log.error("解析搜索结果失败", e);
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", searchResult);
            errorMap.put("source", "百度优选MCP");
            return errorMap;
        }
    }

    /**
     * 生成搜索回复文本（支持百度优选MCP返回格式）
     */
    private String generateSearchResponse(String searchResult, String keyword) {
        try {
            Map<String, Object> result = JSONUtil.toBean(searchResult, Map.class);
            
            if (result.containsKey("error")) {
                return "抱歉，搜索\"" + keyword + "\"时遇到问题：" + result.get("error") + "\n\n请尝试更换关键词或稍后再试。";
            }
            
            // 支持spuList格式
            if (result.containsKey("spuList")) {
                List<Map<String, Object>> spuList = (List<Map<String, Object>>) result.get("spuList");
                boolean hasMore = result.containsKey("hasMore") && (Boolean) result.get("hasMore");
                
                StringBuilder sb = new StringBuilder();
                sb.append("为您找到 ").append(spuList.size()).append(" 款「").append(keyword).append("」相关商品（来自百度优选）：\n\n");
                
                for (int i = 0; i < Math.min(5, spuList.size()); i++) {
                    Map<String, Object> spu = spuList.get(i);
                    sb.append(i + 1).append(". ").append(spu.get("productName"));
                    if (spu.containsKey("spuPrice")) {
                        sb.append(" - ¥").append(spu.get("spuPrice"));
                    }
                    sb.append("\n");
                }
                
                if (hasMore) {
                    sb.append("\n... 还有更多商品\n");
                }
                
                sb.append("\n💡 您可以：\n");
                sb.append("- 告诉我您感兴趣的商品编号，我帮您查看详情\n");
                sb.append("- 或者直接加入购物车\n");
                
                return sb.toString();
            }
            
            // 支持goods格式
            if (result.containsKey("goods")) {
                List<Map<String, Object>> goods = (List<Map<String, Object>>) result.get("goods");
                int total = result.containsKey("total") ? (Integer) result.get("total") : goods.size();
                String source = result.containsKey("source") ? (String) result.get("source") : "百度优选";
                
                StringBuilder sb = new StringBuilder();
                sb.append("为您找到 ").append(total).append(" 款「").append(keyword).append("」相关商品（来自").append(source).append("）：\n\n");
                
                for (int i = 0; i < Math.min(5, goods.size()); i++) {
                    Map<String, Object> good = goods.get(i);
                    sb.append(i + 1).append(". ").append(good.get("name"));
                    if (good.containsKey("price")) {
                        sb.append(" - ¥").append(good.get("price"));
                    }
                    sb.append("\n");
                }
                
                if (goods.size() > 5) {
                    sb.append("\n... 还有 ").append(goods.size() - 5).append(" 款商品\n");
                }
                
                sb.append("\n💡 您可以：\n");
                sb.append("- 告诉我您感兴趣的商品编号，我帮您查看详情\n");
                sb.append("- 或者直接加入购物车\n");
                
                return sb.toString();
            }
            
            return "抱歉，没有找到「" + keyword + "」相关商品，请尝试更换关键词。";
            
        } catch (Exception e) {
            return "抱歉，搜索\"" + keyword + "\"时出现错误，请稍后再试。";
        }
    }

    /**
     * 构建购买专用消息列表
     */
    private List<ChatMessage> buildPurchaseMessages(PurchaseSession session, String currentMessage) {
        List<ChatMessage> messages = new ArrayList<>();

        // 购买专用系统提示
        messages.add(new SystemMessage(
            "你是\"小皮购买助手\"，专门帮助用户完成商品购买流程。\n" +
            "你的职责：\n" +
            "1. 根据用户需求搜索和推荐真实商品（优先使用百度优选MCP）\n" +
            "2. 帮助用户将商品加入购物车\n" +
            "3. 协助用户创建订单\n" +
            "4. 引导用户完成支付\n\n" +
            "工作流程：\n" +
            "- 用户说想买什么 → **必须首先调用searchProducts搜索真实商品** → 展示商品卡片\n" +
            "- 如果searchProducts返回错误，告知用户搜索失败，不要使用其他工具替代\n" +
            "- 用户选择商品 → 调用addToCart加入购物车 → 确认已添加\n" +
            "- 用户确认购买 → 调用createOrder创建订单 → 引导支付\n\n" +
            "重要规则：\n" +
            "- 当用户想购买商品时，**必须优先调用searchProducts**，不要使用getRecommendedProducts替代\n" +
            "- getRecommendedProducts仅在用户说\"随便看看\"或\"推荐一些商品\"时使用\n" +
            "- searchProducts会调用百度优选MCP搜索真实商品，返回真实商品信息\n" +
            "- 如果searchProducts返回错误，直接告知用户，不要降级到其他工具\n\n" +
            "请使用中文友好地回复，每次回复要简洁明了。"
        ));

        // 加入上下文历史
        if (session.userId != null) {
            List<AiContextManager.ContextMessage> history = contextManager.getRecentMessages(session.userId, 6);
            for (AiContextManager.ContextMessage msg : history) {
                if ("user".equals(msg.getRole())) {
                    messages.add(new UserMessage(msg.getContent()));
                } else if ("assistant".equals(msg.getRole())) {
                    messages.add(AiMessage.from(msg.getContent()));
                }
            }
        }

        messages.add(new UserMessage(currentMessage));
        return messages;
    }

    /**
     * 获取购买工具规格（只包含购买相关工具）
     */
    private List<ToolSpecification> getPurchaseToolSpecifications() {
        List<ToolSpecification> allSpecs = ToolSpecifications.toolSpecificationsFrom(mallToolService);
        // 过滤出购买相关的工具
        List<String> purchaseTools = List.of(
            "searchProducts", "getProductDetail", "getRecommendedProducts",
            "getProductReviews", "getCategories", "addToCart", "viewCart", "createOrder", "viewOrders"
        );
        return allSpecs.stream()
                .filter(spec -> purchaseTools.contains(spec.name()))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 根据工具名执行购买工具
     */
    private String executePurchaseToolByName(String toolName, String arguments, PurchaseSession session) {
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
                    if (userIdStr == null && session.userId != null) userIdStr = session.userId.toString();
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
                    if (userIdStr == null && session.userId != null) userIdStr = session.userId.toString();
                    return mallToolService.viewCart(userIdStr != null ? Long.parseLong(userIdStr) : null);
                }
                case "createOrder": {
                    String userIdStr = extractJsonParam(arguments, "arg0");
                    if (userIdStr == null) userIdStr = extractJsonParam(arguments, "userId");
                    if (userIdStr == null && session.userId != null) userIdStr = session.userId.toString();
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
                    if (userIdStr == null && session.userId != null) userIdStr = session.userId.toString();
                    return mallToolService.viewOrders(userIdStr != null ? Long.parseLong(userIdStr) : null);
                }
                default:
                    return "{\"error\":\"未知购买工具：" + toolName + "\"}";
            }
        } catch (Exception e) {
            log.error("购买工具执行异常：{}", toolName, e);
            return "{\"error\":\"购买工具执行失败：" + e.getMessage() + "\"}";
        }
    }

    /**
     * 更新购买会话状态
     */
    private void updatePurchaseSession(PurchaseSession session, String toolName, String toolResult) {
        switch (toolName) {
            case "searchProducts":
            case "getRecommendedProducts":
                session.currentStep = "selecting";
                break;
            case "addToCart":
                session.currentStep = "cart_updated";
                break;
            case "createOrder":
                session.currentStep = "order_created";
                break;
        }
    }

    /**
     * 将购买工具名映射为前端 action 类型
     */
    private String mapPurchaseToolNameToAction(String toolName) {
        switch (toolName) {
            case "searchProducts": return "purchase_search";
            case "getProductDetail": return "purchase_viewGood";
            case "getRecommendedProducts": return "purchase_recommend";
            case "getProductReviews": return "purchase_viewReviews";
            case "addToCart": return "purchase_addToCart";
            case "viewCart": return "purchase_viewCart";
            case "createOrder": return "purchase_createOrder";
            case "viewOrders": return "purchase_viewOrders";
            default: return toolName;
        }
    }

    /**
     * 解析购买工具结果为前端 actionData 格式
     */
    private Map<String, Object> parsePurchaseToolResultToActionData(String toolName, String toolResult) {
        Map<String, Object> actionData = new HashMap<>();
        try {
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
     * 逐字流式输出文本
     */
    private void streamTextByChar(String text, PrintWriter writer) {
        if (text == null || text.isEmpty()) return;

        try {
            char[] chars = text.toCharArray();
            StringBuilder buffer = new StringBuilder();

            for (int i = 0; i < chars.length; i++) {
                buffer.append(chars[i]);

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

    /**
     * 获取购买流程状态
     */
    public Map<String, Object> getPurchaseStatus(Long userId) {
        Map<String, Object> result = new HashMap<>();
        PurchaseSession session = purchaseSessions.get(userId);
        if (session != null) {
            result.put("success", true);
            result.put("currentStep", session.currentStep);
            result.put("selectedGoods", session.selectedGoods);
            result.put("context", session.context);
        } else {
            result.put("success", false);
            result.put("message", "无活跃购买流程");
        }
        return result;
    }

    /**
     * 取消购买流程
     */
    public void cancelPurchase(Long userId) {
        purchaseSessions.remove(userId);
        if (userId != null) {
            contextManager.clearContext(userId);
        }
    }
}
