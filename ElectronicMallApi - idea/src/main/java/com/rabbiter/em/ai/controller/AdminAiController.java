package com.rabbiter.em.ai.controller;

import cn.hutool.json.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

/**
 * 管理员AI功能控制器
 * 独立于用户端AI功能，专门处理管理员的三个AI工具
 */
@RestController
@RequestMapping("/api/ai/admin")
public class AdminAiController {
    
    private static final Logger log = LoggerFactory.getLogger(AdminAiController.class);
    
    @Value("${langchain4j.open-ai.api-key}")
    private String deepseekApiKey;
    
    @Value("${langchain4j.open-ai.base-url}")
    private String deepseekBaseUrl;
    
    @Autowired
    private com.rabbiter.em.service.GoodService goodService;
    
    @Autowired
    private com.rabbiter.em.service.OrderService orderService;
    
    /**
     * AI文案生成（非流式）
     */
    @PostMapping("/copywriting")
    public Map<String, Object> generateCopywriting(@RequestBody Map<String, Object> params) {
        log.info("管理员AI文案生成请求：{}", params);
        Map<String, Object> result = new HashMap<>();
        try {
            String type = (String) params.getOrDefault("type", "标题");
            String productName = (String) params.getOrDefault("productName", "");
            String category = (String) params.getOrDefault("category", "");
            String prompt = buildCopywritingPrompt(type, productName, category);
            String aiResponse = callDeepSync(prompt);
            result.put("success", true);
            result.put("data", aiResponse);
            result.put("type", type);
        } catch (Exception e) {
            log.error("AI文案生成失败：{}", e.getMessage());
            result.put("success", false);
            result.put("message", "生成失败：" + e.getMessage());
        }
        return result;
    }
    
    /**
     * AI文案生成（流式）
     */
    @PostMapping(value = "/copywriting/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public void streamCopywriting(@RequestBody Map<String, Object> params, HttpServletResponse response) throws IOException {
        response.setContentType("text/event-stream;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no");
        response.flushBuffer();
        
        String type = (String) params.getOrDefault("type", "标题");
        String productName = (String) params.getOrDefault("productName", "");
        String category = (String) params.getOrDefault("category", "");
        String prompt = buildCopywritingPrompt(type, productName, category);
        
        streamToClient(prompt, response.getWriter());
    }
    
    /**
     * 竞品价格分析（非流式）
     */
    @PostMapping("/price-analysis")
    public Map<String, Object> priceAnalysis(@RequestBody Map<String, Object> params) {
        log.info("管理员竞品价格分析请求：{}", params);
        Map<String, Object> result = new HashMap<>();
        try {
            String productName = (String) params.getOrDefault("productName", "");
            Double currentPrice = params.containsKey("currentPrice") ? Double.parseDouble(params.get("currentPrice").toString()) : null;
            List<Map<String, Object>> priceData = getSimilarGoodsPrice(productName);
            String prompt = buildPriceAnalysisPrompt(productName, currentPrice, priceData);
            String aiResponse = callDeepSync(prompt);
            result.put("success", true);
            result.put("data", aiResponse);
            result.put("priceData", priceData);
        } catch (Exception e) {
            log.error("竞品价格分析失败：{}", e.getMessage());
            result.put("success", false);
            result.put("message", "分析失败：" + e.getMessage());
        }
        return result;
    }
    
    /**
     * 竞品价格分析（流式）
     */
    @PostMapping(value = "/price-analysis/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public void streamPriceAnalysis(@RequestBody Map<String, Object> params, HttpServletResponse response) throws IOException {
        response.setContentType("text/event-stream;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no");
        response.flushBuffer();
        
        String productName = (String) params.getOrDefault("productName", "");
        Double currentPrice = params.containsKey("currentPrice") ? Double.parseDouble(params.get("currentPrice").toString()) : null;
        List<Map<String, Object>> priceData = getSimilarGoodsPrice(productName);
        
        Map<String, Object> extraData = new HashMap<>();
        extraData.put("priceData", priceData);
        
        String prompt = buildPriceAnalysisPrompt(productName, currentPrice, priceData);
        streamToClientWithExtraData(prompt, response.getWriter(), extraData);
    }
    
    /**
     * 库存销量预测（非流式）
     */
    @PostMapping("/sales-forecast")
    public Map<String, Object> salesForecast(@RequestBody Map<String, Object> params) {
        log.info("管理员库存销量预测请求：{}", params);
        Map<String, Object> result = new HashMap<>();
        try {
            String productName = (String) params.getOrDefault("productName", "");
            Integer currentStock = params.containsKey("currentStock") ? Integer.parseInt(params.get("currentStock").toString()) : null;
            Map<String, Object> salesData = getSalesHistory(productName);
            String prompt = buildSalesForecastPrompt(productName, currentStock, salesData);
            String aiResponse = callDeepSync(prompt);
            result.put("success", true);
            result.put("data", aiResponse);
            result.put("salesData", salesData);
        } catch (Exception e) {
            log.error("库存销量预测失败：{}", e.getMessage());
            result.put("success", false);
            result.put("message", "预测失败：" + e.getMessage());
        }
        return result;
    }
    
    /**
     * 库存销量预测（流式）
     */
    @PostMapping(value = "/sales-forecast/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public void streamSalesForecast(@RequestBody Map<String, Object> params, HttpServletResponse response) throws IOException {
        response.setContentType("text/event-stream;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no");
        response.flushBuffer();
        
        String productName = (String) params.getOrDefault("productName", "");
        Integer currentStock = params.containsKey("currentStock") ? Integer.parseInt(params.get("currentStock").toString()) : null;
        Map<String, Object> salesData = getSalesHistory(productName);
        
        Map<String, Object> extraData = new HashMap<>();
        extraData.put("salesData", salesData);
        
        String prompt = buildSalesForecastPrompt(productName, currentStock, salesData);
        streamToClientWithExtraData(prompt, response.getWriter(), extraData);
    }
    
    /**
     * 构建文案生成Prompt
     */
    private String buildCopywritingPrompt(String type, String productName, String category) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个专业的电商文案策划师。请为以下商品生成文案：\n\n");
        prompt.append("商品名称：").append(productName).append("\n");
        if (category != null && !category.isEmpty()) {
            prompt.append("品类：").append(category).append("\n");
        }
        prompt.append("\n请生成以下内容：\n");
        switch (type) {
            case "标题":
                prompt.append("1. 5个吸引人的商品标题（每个不超过30字）\n");
                prompt.append("2. 标题要突出商品卖点和优势\n");
                prompt.append("3. 适当使用emoji表情增加吸引力\n");
                break;
            case "卖点":
                prompt.append("1. 5个核心卖点描述（每个不超过20字）\n");
                prompt.append("2. 卖点要简洁有力，突出商品特色\n");
                prompt.append("3. 从用户角度描述使用场景和好处\n");
                break;
            case "话术":
                prompt.append("1. 3条客服回复话术（用于回答用户常见问题）\n");
                prompt.append("2. 话术要亲切专业，体现服务态度\n");
                prompt.append("3. 包含产品推荐和促销引导\n");
                break;
            default:
                prompt.append("1. 商品标题\n2. 核心卖点\n3. 客服话术\n");
        }
        return prompt.toString();
    }
    
    /**
     * 构建价格分析Prompt
     */
    private String buildPriceAnalysisPrompt(String productName, Double currentPrice, List<Map<String, Object>> priceData) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个电商价格分析师。请分析以下商品的市场价格情况：\n\n");
        prompt.append("商品名称：").append(productName).append("\n");
        if (currentPrice != null) {
            prompt.append("当前售价：¥").append(currentPrice).append("\n");
        }
        prompt.append("\n同类商品价格分布：\n");
        for (Map<String, Object> item : priceData) {
            prompt.append("- ").append(item.get("name")).append("：¥").append(item.get("price")).append("\n");
        }
        prompt.append("\n请提供以下分析：\n");
        prompt.append("1. 市场价格区间分析\n");
        prompt.append("2. 当前价格竞争力评估\n");
        prompt.append("3. 定价建议（最优价格区间）\n");
        prompt.append("4. 促销策略建议\n");
        return prompt.toString();
    }
    
    /**
     * 构建销量预测Prompt
     */
    private String buildSalesForecastPrompt(String productName, Integer currentStock, Map<String, Object> salesData) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个电商数据分析师。请分析以下商品的销量和库存情况：\n\n");
        prompt.append("商品名称：").append(productName).append("\n");
        if (currentStock != null) {
            prompt.append("当前库存：").append(currentStock).append("件\n");
        }
        if (salesData.containsKey("totalSales")) {
            prompt.append("总销量：").append(salesData.get("totalSales")).append("件\n");
        }
        if (salesData.containsKey("monthlySales")) {
            prompt.append("月均销量：").append(salesData.get("monthlySales")).append("件\n");
        }
        if (salesData.containsKey("dailySales")) {
            prompt.append("日均销量：").append(salesData.get("dailySales")).append("件\n");
        }
        prompt.append("\n请提供以下分析：\n");
        prompt.append("1. 未来7天/30天销量预测\n");
        prompt.append("2. 库存预警（是否需要补货）\n");
        prompt.append("3. 备货建议（建议补货数量和时间）\n");
        prompt.append("4. 销售趋势分析\n");
        return prompt.toString();
    }
    
    /**
     * 获取同类商品价格数据
     */
    private List<Map<String, Object>> getSimilarGoodsPrice(String productName) {
        List<Map<String, Object>> priceData = new ArrayList<>();
        try {
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.rabbiter.em.entity.Good> wrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            wrapper.like("name", productName)
                   .or()
                   .like("description", productName)
                   .orderByDesc("sales")
                   .last("LIMIT 10");
            List<com.rabbiter.em.entity.Good> goods = goodService.list(wrapper);
            for (com.rabbiter.em.entity.Good good : goods) {
                Map<String, Object> item = new HashMap<>();
                item.put("name", good.getName());
                // 使用 getMinPrice 获取含折扣的最低价格
                java.math.BigDecimal realPrice = goodService.getMinPrice(good.getId());
                item.put("price", realPrice != null ? realPrice.doubleValue() : 0);
                item.put("sales", good.getSales());
                priceData.add(item);
            }
        } catch (Exception e) {
            log.error("获取同类商品价格失败：{}", e.getMessage());
        }
        return priceData;
    }
    
    /**
     * 获取历史销量数据
     */
    private Map<String, Object> getSalesHistory(String productName) {
        Map<String, Object> salesData = new HashMap<>();
        try {
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.rabbiter.em.entity.Good> wrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            wrapper.like("name", productName)
                   .or()
                   .like("description", productName)
                   .last("LIMIT 1");
            List<com.rabbiter.em.entity.Good> goods = goodService.list(wrapper);
            if (!goods.isEmpty()) {
                com.rabbiter.em.entity.Good good = goods.get(0);
                salesData.put("totalSales", good.getSales());
                Integer totalSales = good.getSales() != null ? good.getSales() : 0;
                salesData.put("monthlySales", Math.round(totalSales / 3.0));
                salesData.put("dailySales", Math.round(totalSales / 90.0));
            }
        } catch (Exception e) {
            log.error("获取历史销量数据失败：{}", e.getMessage());
        }
        return salesData;
    }
    
    /**
     * 同步调用DeepSeek API
     */
    private String callDeepSync(String prompt) {
        try {
            String url = deepseekBaseUrl + "/chat/completions";
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "deepseek-chat");
            requestBody.put("messages", Arrays.asList(
                new HashMap<String, Object>() {{
                    put("role", "user");
                    put("content", prompt);
                }}
            ));
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2000);
            
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + deepseekApiKey);
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(60000);
            
            String jsonBody = JSONUtil.toJsonStr(requestBody);
            conn.getOutputStream().write(jsonBody.getBytes("UTF-8"));
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            
            cn.hutool.json.JSONObject resp = JSONUtil.parseObj(sb.toString());
            cn.hutool.json.JSONArray choices = resp.getJSONArray("choices");
            if (choices != null && !choices.isEmpty()) {
                return choices.getJSONObject(0).getJSONObject("message").getStr("content");
            }
            return "AI生成失败，请稍后重试";
        } catch (Exception e) {
            log.error("调用DeepSeek API失败：{}", e.getMessage());
            return "AI服务暂时不可用，请稍后重试";
        }
    }
    
    /**
     * 流式调用DeepSeek API
     */
    private void streamToClient(String prompt, PrintWriter writer) {
        try {
            String url = deepseekBaseUrl + "/chat/completions";
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + deepseekApiKey);
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(0);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "deepseek-chat");
            requestBody.put("messages", Arrays.asList(
                new HashMap<String, Object>() {{
                    put("role", "user");
                    put("content", prompt);
                }}
            ));
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2000);
            requestBody.put("stream", true);
            
            String jsonBody = JSONUtil.toJsonStr(requestBody);
            conn.getOutputStream().write(jsonBody.getBytes("UTF-8"));
            
            boolean hasReceivedDone = false;
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            String line;
            
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) continue;
                
                String trimmedLine = line;
                if (line.startsWith("data:")) {
                    trimmedLine = line.substring(5).trim();
                }
                
                if ("[DONE]".equals(line.trim()) || "[DONE]".equals(trimmedLine)) {
                    hasReceivedDone = true;
                    writer.write("event: done\n");
                    writer.write("data: [DONE]\n\n");
                    writer.flush();
                    break;
                }
                
                try {
                    cn.hutool.json.JSONObject obj = JSONUtil.parseObj(trimmedLine);
                    cn.hutool.json.JSONArray choices = obj.getJSONArray("choices");
                    if (choices == null || choices.isEmpty()) continue;
                    
                    cn.hutool.json.JSONObject delta = choices.getJSONObject(0).getJSONObject("delta");
                    String content = delta.get("content") != null ? delta.getStr("content") : null;
                    
                    if (content != null && !content.isEmpty()) {
                        Map<String, Object> map = new HashMap<>();
                        map.put("content", content);
                        writer.write("event: message\n");
                        writer.write("data: " + JSONUtil.toJsonStr(map) + "\n\n");
                        writer.flush();
                    }
                } catch (Exception e) {
                    log.debug("解析流式消息失败：{}", e.getMessage());
                }
            }
            
            if (!hasReceivedDone) {
                writer.write("event: done\n");
                writer.write("data: [DONE]\n\n");
                writer.flush();
            }
            
            reader.close();
            conn.disconnect();
        } catch (Exception e) {
            log.error("流式调用失败：{}", e.getMessage());
            try {
                writer.write("event: error\n");
                writer.write("data: {\"error\":\"" + e.getMessage() + "\"}\n\n");
                writer.write("event: done\n");
                writer.write("data: [DONE]\n\n");
                writer.flush();
            } catch (Exception ex) {
                log.error("发送错误信息失败：{}", ex.getMessage());
            }
        } finally {
            writer.close();
        }
    }
    
    /**
     * 流式调用DeepSeek API（附带额外数据）
     */
    private void streamToClientWithExtraData(String prompt, PrintWriter writer, Map<String, Object> extraData) {
        try {
            String url = deepseekBaseUrl + "/chat/completions";
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + deepseekApiKey);
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(0);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "deepseek-chat");
            requestBody.put("messages", Arrays.asList(
                new HashMap<String, Object>() {{
                    put("role", "user");
                    put("content", prompt);
                }}
            ));
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2000);
            requestBody.put("stream", true);
            
            String jsonBody = JSONUtil.toJsonStr(requestBody);
            conn.getOutputStream().write(jsonBody.getBytes("UTF-8"));
            
            boolean hasReceivedDone = false;
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            String line;
            
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) continue;
                
                String trimmedLine = line;
                if (line.startsWith("data:")) {
                    trimmedLine = line.substring(5).trim();
                }
                
                if ("[DONE]".equals(line.trim()) || "[DONE]".equals(trimmedLine)) {
                    hasReceivedDone = true;
                    writer.write("event: done\n");
                    writer.write("data: [DONE]\n\n");
                    writer.flush();
                    break;
                }
                
                try {
                    cn.hutool.json.JSONObject obj = JSONUtil.parseObj(trimmedLine);
                    cn.hutool.json.JSONArray choices = obj.getJSONArray("choices");
                    if (choices == null || choices.isEmpty()) continue;
                    
                    cn.hutool.json.JSONObject delta = choices.getJSONObject(0).getJSONObject("delta");
                    String content = delta.get("content") != null ? delta.getStr("content") : null;
                    
                    if (content != null && !content.isEmpty()) {
                        Map<String, Object> map = new HashMap<>();
                        map.put("content", content);
                        writer.write("event: message\n");
                        writer.write("data: " + JSONUtil.toJsonStr(map) + "\n\n");
                        writer.flush();
                    }
                } catch (Exception e) {
                    log.debug("解析流式消息失败：{}", e.getMessage());
                }
            }
            
            if (!hasReceivedDone) {
                writer.write("event: done\n");
                writer.write("data: [DONE]\n\n");
                writer.flush();
            }
            
            reader.close();
            conn.disconnect();
        } catch (Exception e) {
            log.error("流式调用失败：{}", e.getMessage());
            try {
                writer.write("event: error\n");
                writer.write("data: {\"error\":\"" + e.getMessage() + "\"}\n\n");
                writer.write("event: done\n");
                writer.write("data: [DONE]\n\n");
                writer.flush();
            } catch (Exception ex) {
                log.error("发送错误信息失败：{}", ex.getMessage());
            }
        } finally {
            writer.close();
        }
    }
}
