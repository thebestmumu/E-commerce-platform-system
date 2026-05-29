package com.rabbiter.em.ai.service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * AI 品类识别服务
 * 使用 DeepSeek AI 识别用户想要购买的商品品类，替代硬编码关键词匹配
 */
@Service
public class AiCategoryRecognitionService {
    
    private static final Logger log = LoggerFactory.getLogger(AiCategoryRecognitionService.class);
    
    @Autowired(required = false)
    private ChatLanguageModel chatModel;
    
    /**
     * 使用AI识别用户消息中的商品品类
     * @param message 用户消息
     * @return 识别出的品类名称，如果无法识别返回空字符串
     */
    public String recognizeCategory(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }
        
        // 如果AI模型不可用，降级到简单规则匹配
        if (chatModel == null) {
            log.warn("AI模型不可用，使用简单规则匹配");
            return simpleRuleMatch(message);
        }
        
        try {
            String prompt = buildRecognitionPrompt(message);
            log.info("AI品类识别请求：{}", message);
            
            String response = chatModel.generate(prompt);
            
            if (response != null && !response.isEmpty()) {
                // 清理AI返回的结果
                String category = response.trim()
                    .replaceAll("[\"'\\n\\r]", "")
                    .replaceAll("^[\\s\\p{Punct}]+|[\\s\\p{Punct}]+$", "")
                    .trim();
                
                log.info("AI识别结果：{}", category);
                return category;
            }
        } catch (Exception e) {
            log.error("AI品类识别失败：{}", e.getMessage());
        }
        
        // 降级到简单规则匹配
        return simpleRuleMatch(message);
    }
    
    /**
     * 构建AI识别提示词
     */
    private String buildRecognitionPrompt(String message) {
        return "你是一个电商平台的商品品类识别助手。请从用户的消息中识别出用户想要购买或了解的商品品类。\n" +
               "\n" +
               "【识别规则】\n" +
               "1. 只返回具体的商品品类名称，不要返回其他内容\n" +
               "2. 如果用户提到多个品类，返回最主要的那个\n" +
               "3. 如果用户没有提到具体品类，返回空字符串\n" +
               "4. 品类名称要简洁准确，如\"卫衣\"、\"手机\"、\"零食\"\n" +
               "5. 不要返回\"推荐\"、\"购买\"等动作词，只返回品类名\n" +
               "\n" +
               "【示例】\n" +
               "用户：\"推荐一些卫衣\" -> 卫衣\n" +
               "用户：\"我想买手机\" -> 手机\n" +
               "用户：\"有什么好吃的零食\" -> 零食\n" +
               "用户：\"推荐笔记本电脑\" -> 笔记本电脑\n" +
               "用户：\"帮我推荐\" -> \n" +
               "用户：\"今天天气怎么样\" -> \n" +
               "用户：\"推荐200元以下的运动鞋\" -> 运动鞋\n" +
               "\n" +
               "【用户消息】\n" +
               message + "\n" +
               "\n" +
               "请只返回品类名称，不要返回任何其他内容：";
    }
    
    /**
     * 简单规则匹配（降级方案）
     */
    private String simpleRuleMatch(String message) {
        // 尝试提取"推荐"后面的词
        String[] keywords = {"推荐", "买", "想要", "找", "搜索", "看看"};
        
        for (String keyword : keywords) {
            int index = message.indexOf(keyword);
            if (index >= 0) {
                String after = message.substring(index + keyword.length()).trim();
                // 去除修饰词
                after = after.replaceAll("一下|一些|几个|什么|好的|不错的|的|了|吗|呢|啊|吧|哦|呀|我|你|他|她|它|们|个|款|种|类", "").trim();
                if (!after.isEmpty() && after.length() <= 10) {
                    return after;
                }
            }
        }
        
        return "";
    }
    
    /**
     * 识别排序方式
     * @param message 用户消息
     * @return "sales"（销量）、"rating"（好评）、"price"（价格）、"default"（综合）
     */
    public String recognizeSortType(String message) {
        if (message == null || message.isEmpty()) {
            return "default";
        }
        
        if (chatModel == null) {
            return simpleSortMatch(message);
        }
        
        try {
            String prompt = "你是一个电商排序方式识别助手。请从用户消息中识别用户想要的商品排序方式。\n" +
                           "\n" +
                           "【排序方式】\n" +
                           "- sales：按销量排序（销量高、卖得好、热门、爆款、畅销）\n" +
                           "- rating：按好评排序（好评、评分高、口碑好、评价好）\n" +
                           "- price：按价格排序（便宜、低价、高价、贵、实惠、性价比）\n" +
                           "- default：综合排序（没有特别要求、综合、默认）\n" +
                           "\n" +
                           "【示例】\n" +
                           "用户：\"推荐销量高的\" -> sales\n" +
                           "用户：\"推荐好评的\" -> rating\n" +
                           "用户：\"推荐便宜的\" -> price\n" +
                           "用户：\"推荐一些商品\" -> default\n" +
                           "用户：\"推荐热门商品\" -> sales\n" +
                           "用户：\"推荐口碑好的\" -> rating\n" +
                           "用户：\"推荐性价比高的\" -> price\n" +
                           "\n" +
                           "【用户消息】\n" +
                           message + "\n" +
                           "\n" +
                           "请只返回排序方式（sales、rating、price 或 default），不要返回其他内容：";
            
            String response = chatModel.generate(prompt);
            if (response != null) {
                String sortType = response.trim().toLowerCase();
                if (sortType.contains("sales")) return "sales";
                if (sortType.contains("rating")) return "rating";
                if (sortType.contains("price")) return "price";
            }
        } catch (Exception e) {
            log.error("AI排序识别失败：{}", e.getMessage());
        }
        
        return simpleSortMatch(message);
    }
    
    /**
     * 简单排序匹配（降级方案）
     */
    private String simpleSortMatch(String message) {
        if (message.contains("销量") || message.contains("热门") || message.contains("爆款") || 
            message.contains("畅销") || message.contains("卖得好")) {
            return "sales";
        }
        if (message.contains("好评") || message.contains("口碑") || message.contains("评分") || 
            message.contains("评价好")) {
            return "rating";
        }
        if (message.contains("便宜") || message.contains("低价") || message.contains("实惠") || 
            message.contains("性价比") || message.contains("价格")) {
            return "price";
        }
        return "default";
    }
}
