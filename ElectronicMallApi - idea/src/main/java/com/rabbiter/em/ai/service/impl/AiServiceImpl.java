package com.rabbiter.em.ai.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.rabbiter.em.ai.entity.ChatRequest;
import com.rabbiter.em.ai.entity.ChatResponse;
import com.rabbiter.em.ai.service.AiBusinessService;
import com.rabbiter.em.ai.service.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 服务实现类
 */
@Service
public class AiServiceImpl implements AiService {
    
    /**
     * 日志记录器
     */
    private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);
    
    /**
     * DeepSeek API 密钥（通过 langchain4j.open-ai.api-key 配置）
     */
    @Value("${langchain4j.open-ai.api-key}")
    private String apiKey;
    
    /**
     * DeepSeek API URL（兼容 OpenAI 格式）
     */
    private static final String DEEPSEEK_API_URL = "https://api.deepseek.com/v1/chat/completions";
    
    /**
     * AI 业务服务
     */
    @Autowired
    private AiBusinessService aiBusinessService;
    
    @Override
    public ChatResponse chat(ChatRequest request) {
        try {
            // 获取当前模式
            String mode = request.getMode();
            
            // 构建响应
            ChatResponse response = null;
            
            // 根据模式处理请求
            if ("help".equals(mode)) {
                // 智能帮助模式：使用AI进行语义识别，然后执行相应操作
                response = handleHelpMode(request);
            } else {
                // 对话模式：直接使用AI进行对话
                response = handleChatMode(request);
            }
            
            return response;
        } catch (Exception e) {
            log.error("AI chat error: {}", e.getMessage(), e);
            ChatResponse errorResponse = new ChatResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("小皮助手暂时无法回答您的问题，请稍后再试");
            return errorResponse;
        }
    }
    
    /**
     * 处理对话模式请求
     * @param request 聊天请求
     * @return 聊天响应
     */
    private ChatResponse handleChatMode(ChatRequest request) {
        try {
            // 构建历史消息
            java.util.List<ChatRequest.Message> history = request.getHistory();
            if (history == null) {
                history = new java.util.ArrayList<>();
            }
            
            // 添加最新消息
            ChatRequest.Message latestMessage = new ChatRequest.Message();
            latestMessage.setRole("user");
            latestMessage.setContent(request.getMessage());
            history.add(latestMessage);
            
            // 转换为 DeepSeek API 所需的格式
            java.util.List<cn.hutool.json.JSONObject> messages = new java.util.ArrayList<>();
            for (ChatRequest.Message msg : history) {
                cn.hutool.json.JSONObject msgObj = new cn.hutool.json.JSONObject();
                msgObj.put("role", msg.getRole());
                msgObj.put("content", msg.getContent());
                messages.add(msgObj);
            }
            
            // 获取AI响应
            String aiResponse = getAiResponse(request.getMessage(), history.toArray(new ChatRequest.Message[0]));
            
            // 构建响应
            return ChatResponse.success(aiResponse);
        } catch (Exception e) {
            log.error("Handle chat mode error: {}", e.getMessage(), e);
            return ChatResponse.success(getDefaultResponse(request.getMessage()));
        }
    }
    
    /**
     * 处理智能帮助模式请求
     * @param request 聊天请求
     * @return 聊天响应
     */
    private ChatResponse handleHelpMode(ChatRequest request) {
        try {
            // 使用AI进行语义识别
            log.info("开始识别用户意图: {}", request.getMessage());
            String intent = recognizeIntentWithAI(request.getMessage());
            log.info("识别出的用户意图: {}", intent);
            
            // 如果用户在进行聊天，直接进入对话模式
            if ("chat".equals(intent)) {
                log.info("用户意图为聊天，进入对话模式");
                return handleChatMode(request);
            }
            
            // 提取实体
            log.info("开始提取实体");
            Object entities = extractEntities(request.getMessage(), intent);
            log.info("提取到的实体: {}", entities);
            
            // 构建响应
            ChatResponse response = null;
            
            // 根据意图执行业务操作并添加操作
            if (!"unknown".equals(intent) && entities != null) {
                ChatResponse.Action action = new ChatResponse.Action();
                action.setType(intent);
                
                // 执行业务操作
                Object result = null;
                String aiResponse = "";
                
                if ("search".equals(intent)) {
                    log.info("执行搜索操作");
                    result = aiBusinessService.search(entities.toString());
                    aiResponse = "已为您找到相关商品，请查看搜索结果。";
                } else if ("addCart".equals(intent)) {
                    log.info("执行添加购物车操作");
                    // 构建购物车参数
                    java.util.Map<String, Object> params = new java.util.HashMap<>();
                    if (entities instanceof java.util.Map) {
                        params.putAll((java.util.Map<? extends String, ?>) entities);
                    } else {
                        params.put("goodId", entities);
                    }
                    // 添加用户ID
                    if (request.getUserId() != null) {
                        params.put("userId", request.getUserId());
                    }
                    result = aiBusinessService.addCart(params);
                    aiResponse = "商品已成功添加到购物车！";
                } else if ("batchAddCart".equals(intent)) {
                    log.info("执行批量添加购物车操作");
                    // 构建批量添加购物车参数
                    java.util.List<java.util.Map<String, Object>> paramsList = new java.util.ArrayList<>();
                    if (entities instanceof java.util.List) {
                        paramsList = (java.util.List<java.util.Map<String, Object>>) entities;
                    } else if (entities instanceof java.util.Map) {
                        // 如果是单个商品，也支持批量添加
                        paramsList.add((java.util.Map<String, Object>) entities);
                    }
                    // 添加用户ID到每个商品参数中
                    if (request.getUserId() != null) {
                        for (java.util.Map<String, Object> params : paramsList) {
                            params.put("userId", request.getUserId());
                        }
                    }
                    result = aiBusinessService.batchAddCart(paramsList);
                    aiResponse = "商品已成功添加到购物车！";
                } else if ("queryOrder".equals(intent)) {
                    log.info("执行订单查询操作");
                    // 构建订单查询参数
                    java.util.Map<String, Object> params = new java.util.HashMap<>();
                    if (entities instanceof java.util.Map) {
                        params.putAll((java.util.Map<? extends String, ?>) entities);
                    } else {
                        params.put("orderNo", entities);
                    }
                    // 添加用户ID
                    if (request.getUserId() != null) {
                        params.put("userId", request.getUserId());
                    }
                    result = aiBusinessService.queryOrder(params);
                    aiResponse = "已为您查询到订单信息，请查看详情。";
                } else if ("navigate".equals(intent)) {
                    log.info("执行页面导航操作");
                    // 正确提取页面名称
                    String pageName = "";
                    if (entities instanceof java.util.Map) {
                        java.util.Map<String, Object> entityMap = (java.util.Map<String, Object>) entities;
                        Object pageObj = entityMap.get("page");
                        if (pageObj != null) {
                            pageName = pageObj.toString();
                        } else {
                            // 如果没有 page 字段，尝试从其他字段获取
                            pageName = entityMap.values().stream()
                                .filter(v -> v != null)
                                .map(Object::toString)
                                .findFirst()
                                .orElse("首页");
                        }
                    } else if (entities != null) {
                        pageName = entities.toString();
                    } else {
                        pageName = "首页";
                    }
                    log.info("导航页面名称：{}", pageName);
                    result = aiBusinessService.navigate(pageName);
                    aiResponse = "正在为您导航到" + pageName + "...";
                } else if ("logout".equals(intent)) {
                    log.info("执行退出登录操作");
                    result = aiBusinessService.logout(request.getUserId());
                    aiResponse = "已为您退出登录。";
                } else {
                    // 其他意图，让AI生成回复
                    log.info("执行其他操作");
                    String aiResponsePrompt = "请针对用户的需求生成一个友好的回复：" + request.getMessage();
                    aiResponse = getAiResponse(aiResponsePrompt);
                }
                
                // 构建响应
                response = ChatResponse.success(aiResponse);
                
                // 设置操作参数
                if (result != null) {
                    action.setParams(result);
                    response.setAction(action.getType());
                    response.setActionData("params", result);
                }
            } else {
                // 意图为未知意图，让AI生成回复
                log.info("用户意图未知，生成友好回复");
                String aiResponsePrompt = "用户的意图不明确：" + request.getMessage() + "\n请生成一个友好的回复，引导用户更清晰地表达需求。";
                String aiResponse = getAiResponse(aiResponsePrompt);
                response = ChatResponse.success(aiResponse);
            }
            
            log.info("处理完成，返回响应: {}", response);
            return response;
        } catch (Exception e) {
            log.error("Handle help mode error: {}", e.getMessage(), e);
            // 即使AI调用失败，也不让AI进行简单的关键词判断，而是返回友好提示
            return ChatResponse.success("抱歉，我暂时无法理解您的需求，请您稍后再试，或者换一种方式表达。");
        }
    }
    
    /**
     * 使用AI进行意图识别
     * @param message 用户消息
     * @return 意图
     */
    private String recognizeIntentWithAI(String message) {
        try {
            // 构建prompt，包含所有可能的意图选项
            log.info("开始构建意图识别 prompt");
            String prompt = "你是一个电商智能助手，需要准确识别用户的意图。请从以下选项中选择最符合用户意图的操作：\n\n" +
                            "【意图选项】\n" +
                            "1. search（搜索商品）- 当用户想要搜索、查找、寻找某类商品时\n" +
                            "2. addCart（添加购物车）- 当用户想要将某个具体商品加入购物车时\n" +
                            "3. batchAddCart（批量添加购物车）- 当用户想要同时添加多个商品到购物车时\n" +
                            "4. queryOrder（查询订单）- 当用户想要查询订单状态、订单信息、我的订单时\n" +
                            "5. navigate（页面导航）- 当用户想要**跳转**到某个页面、**去**首页、**到**个人中心等时\n" +
                            "6. logout（退出登录）- 当用户想要退出、注销登录时\n" +
                            "7. recommend（个性化推荐）- 当用户想要推荐商品、有什么好东西、推荐一些商品时\n" +
                            "8. quickOrder（一键下单）- 当用户想要快速购买、直接下单、立即购买时\n" +
                            "9. trackOrder（订单状态跟踪）- 当用户想要查询特定订单的物流、配送进度时\n" +
                            "10. analyzeOrders（订单历史分析）- 当用户想要查看购物报告、消费分析、购买习惯时\n" +
                            "11. analyzeSentiment（评论舆情分析）- 当用户想要分析商品评论、查看评分分布、情感分析时（需要提供商品ID）\n" +
                            "12. analyzeSales（销售数据分析）- 当用户想要查看销售报告、销量趋势、销售排行时（需要提供商品ID）\n" +
                            "13. chat（普通聊天）- 当用户只是闲聊、问候、或者**询问问题**、**了解信息**时\n" +
                            "14. unknown（暂不明白用户意图）- 当完全无法理解用户意思时\n\n" +
                            "【关键判断规则 - 必须严格遵守】\n" +
                            "❌ 如果用户的问题是**询问性质**的（包含'有什么用'、'是什么'、'怎么样'、'为什么'、'吗'等疑问词），选择 chat\n" +
                            "✅ 如果用户表达的是**操作意愿**的（包含'去'、'到'、'打开'、'进入'、'跳转'等动词），选择 navigate\n" +
                            "- 例如：'首页有什么用' → chat（询问信息）\n" +
                            "- 例如：'去首页' → navigate（执行操作）\n" +
                            "- 例如：'首页有什么' → chat（询问信息）\n" +
                            "- 例如：'到首页看看' → navigate（执行操作）\n" +
                            "- 如果用户提到'推荐'、'有什么好'、'介绍一下'等词，优先选择 recommend\n" +
                            "- 如果用户提到'订单'但没有明确说查询，优先选择 queryOrder\n" +
                            "- 如果用户只是闲聊或问一些与购物无关的问题，选择 chat\n" +
                            "- 如果用户的问题模糊不清，难以判断，选择 unknown\n\n" +
                            "【示例 - 仔细学习】\n" +
                            "用户：'给我推荐一些商品' → recommend\n" +
                            "用户：'首页有什么用' → chat（询问信息，不是跳转）\n" +
                            "用户：'首页有什么功能' → chat（询问信息）\n" +
                            "用户：'去首页' → navigate（执行跳转）\n" +
                            "用户：'到首页看看' → navigate（执行跳转）\n" +
                            "用户：'我想买手机' → search\n" +
                            "用户：'我的订单在哪' → chat（询问位置信息）\n" +
                            "用户：'查询我的订单' → queryOrder\n" +
                            "用户：'帮我下单' → quickOrder\n" +
                            "用户：'你好' → chat\n" +
                            "用户：'今天天气怎么样' → chat\n" +
                            "用户：'这个商品怎么样' → chat（询问信息）\n" +
                            "用户：'看看这个商品' → search（查看商品）\n\n" +
                            "【用户消息】\n" +
                            "" + message + "\n\n" +
                            "请只返回一个英文单词（search、addCart、batchAddCart、queryOrder、navigate、logout、recommend、quickOrder、trackOrder、analyzeOrders、chat 或 unknown），不要返回任何其他内容。";
            log.info("发送了prompt");
            
            // 获取AI响应
            log.info("发送意图识别请求到AI");
            ChatRequest.Message[] history = new ChatRequest.Message[1];
            ChatRequest.Message msg = new ChatRequest.Message();
            msg.setRole("user");
            msg.setContent(prompt);
            history[0] = msg;
            
            String aiResponse = getAiResponse(prompt, history);
            log.info("AI返回的意图识别结果: {}", aiResponse);
            
            // 处理AI响应
            aiResponse = aiResponse.trim().toLowerCase();
            log.info("处理后的意图识别结果: {}", aiResponse);
            
            // 验证响应
            java.util.Set<String> validIntents = java.util.Collections.unmodifiableSet(
                new java.util.HashSet<>(java.util.Arrays.asList(
                    "search", "addCart", "batchAddCart", "queryOrder", 
                    "navigate", "logout", "recommend", "quickOrder", 
                    "trackOrder", "analyzeOrders", "chat", "unknown"
                )));
            
            if (validIntents.contains(aiResponse)) {
                log.info("意图识别结果有效，返回: {}", aiResponse);
                return aiResponse;
            } else {
                log.info("意图识别结果无效，降级到传统意图识别");
                // 降级到传统意图识别
                String traditionalIntent = recognizeIntent(message);
                log.info("传统意图识别结果: {}", traditionalIntent);
                return traditionalIntent;
            }
        } catch (Exception e) {
            log.error("Recognize intent with AI error: {}", e.getMessage(), e);
            // 降级到传统意图识别
            log.info("AI意图识别失败，降级到传统意图识别");
            String traditionalIntent = recognizeIntent(message);
            log.info("传统意图识别结果: {}", traditionalIntent);
            return traditionalIntent;
        }
    }
    
    /**
     * 处理传统意图识别
     * @param request 聊天请求
     * @return 聊天响应
     */
    private ChatResponse handleTraditionalIntentRecognition(ChatRequest request) {
        try {
            // 识别意图
            String intent = recognizeIntent(request.getMessage());
            
            // 提取实体
            Object entities = extractEntities(request.getMessage(), intent);
            
            // 构建响应
            ChatResponse response = null;
            
            // 根据意图执行业务操作并添加操作
            if (entities != null) {
                ChatResponse.Action action = new ChatResponse.Action();
                action.setType(intent);
                
                // 执行业务操作
                Object result = null;
                String aiResponse = "";
                
                if ("search".equals(intent)) {
                    result = aiBusinessService.search(entities.toString());
                    aiResponse = "已为您找到相关商品，请查看搜索结果。";
                } else if ("addCart".equals(intent)) {
                    // 构建购物车参数
                    java.util.Map<String, Object> params = new java.util.HashMap<>();
                    if (entities instanceof java.util.Map) {
                        params.putAll((java.util.Map<? extends String, ?>) entities);
                    } else {
                        params.put("goodId", entities);
                    }
                    // 添加用户ID
                    if (request.getUserId() != null) {
                        params.put("userId", request.getUserId());
                    }
                    result = aiBusinessService.addCart(params);
                    aiResponse = "商品已成功添加到购物车！";
                } else if ("queryOrder".equals(intent)) {
                    // 构建订单查询参数
                    java.util.Map<String, Object> params = new java.util.HashMap<>();
                    if (entities instanceof java.util.Map) {
                        params.putAll((java.util.Map<? extends String, ?>) entities);
                    } else {
                        params.put("orderNo", entities);
                    }
                    // 添加用户ID
                    if (request.getUserId() != null) {
                        params.put("userId", request.getUserId());
                    }
                    result = aiBusinessService.queryOrder(params);
                    aiResponse = "已为您查询到订单信息，请查看详情。";
                } else if ("navigate".equals(intent)) {
                    result = aiBusinessService.navigate(entities.toString());
                    aiResponse = "正在为您导航到指定页面...";
                } else if ("analyzeSentiment".equals(intent)) {
                    // 评论舆情分析 - 需要商品ID
                    java.util.Map<String, Object> params = new java.util.HashMap<>();
                    if (entities instanceof java.util.Map) {
                        params.putAll((java.util.Map<? extends String, ?>) entities);
                    } else {
                        params.put("goodId", entities);
                    }
                    
                    Long goodId = (Long) params.get("goodId");
                    if (goodId == null) {
                        aiResponse = "请提供商品ID，例如：评论分析 商品ID:123";
                        result = null;
                    } else {
                        result = aiBusinessService.analyzeReviewSentiment(goodId);
                        aiResponse = "已为您分析该商品的评论舆情，请查看分析结果。";
                    }
                } else if ("analyzeSales".equals(intent)) {
                    // 销售数据分析 - 需要商品ID
                    java.util.Map<String, Object> params = new java.util.HashMap<>();
                    if (entities instanceof java.util.Map) {
                        params.putAll((java.util.Map<? extends String, ?>) entities);
                    } else {
                        params.put("goodId", entities);
                    }
                    
                    Long goodId = (Long) params.get("goodId");
                    if (goodId == null) {
                        aiResponse = "请提供商品ID，例如：销售报告 商品ID:123";
                        result = null;
                    } else {
                        params.put("days", 30); // 默认30天
                        result = aiBusinessService.generateSalesReport(goodId, (Integer) params.get("days"));
                        aiResponse = "已为您生成该商品的销售数据报告，请查看分析结果。";
                    }
                } else {
                    // 其他意图，使用默认回复
                    aiResponse = getDefaultResponse(request.getMessage());
                }
                
                // 构建响应
                response = ChatResponse.success(aiResponse);
                
                // 设置操作参数
                if (result != null) {
                    action.setParams(result);
                    response.setAction(action.getType());
                    response.setActionData("params", result);
                }
            } else {
                // 无实体，使用默认回复
                String aiResponse = getDefaultResponse(request.getMessage());
                response = ChatResponse.success(aiResponse);
            }
            
            return response;
        } catch (Exception e) {
            log.error("Handle traditional intent recognition error: {}", e.getMessage(), e);
            return ChatResponse.success(getDefaultResponse(request.getMessage()));
        }
    }
    
    /**
     * 获取默认回复
     * @param message 用户消息
     * @return 默认回复
     */
    private String getDefaultResponse(String message) {
        // 关键词匹配，返回与项目相关的回复
        if (message.contains("你好") || message.contains("嗨") || message.contains("哈喽")) {
            return "你好！我是小皮助手，很高兴为您服务。请问有什么可以帮助您的？";
        } else if (message.contains("再见") || message.contains("拜拜") || message.contains("晚安")) {
            return "再见！祝您购物愉快，有任何问题随时找我。";
        } else if (message.contains("帮助") || message.contains("怎么用") || message.contains("功能")) {
            return "我是小皮助手，可以帮您：\n1. 搜索商品（如：帮我找手机）\n2. 添加购物车（如：把这个手机添加到购物车）\n3. 查询订单（如：我的订单到哪了）\n4. 导航到页面（如：带我去首页）\n5. 了解购物信息（如：退换货政策是什么）";
        } else if (message.contains("退换货") || message.contains("退款") || message.contains("退货")) {
            return "我们的退换货政策是：商品在收到后7天内，未拆封、未使用的情况下可以申请退货。如有质量问题，可在收到后15天内申请退换货。详情请查看网站底部的退换货政策。";
        } else if (message.contains("配送") || message.contains("快递") || message.contains("发货")) {
            return "我们的配送政策是：订单确认后1-3个工作日内发货，默认使用顺丰快递。详情请查看网站底部的配送政策。";
        } else if (message.contains("价格") || message.contains("优惠") || message.contains("促销")) {
            return "我们会定期推出促销活动，请关注网站首页的活动公告，或订阅我们的邮件通知，获取最新的优惠信息。";
        } else if (message.contains("会员") || message.contains("积分") || message.contains("等级")) {
            return "我们的会员系统分为普通会员、银卡会员、金卡会员和钻石会员四个等级，根据消费金额自动升级。会员可享受积分累计、生日特权、专属折扣等福利。";
        } else {
            return "抱歉，我暂时无法回答您的问题，请尝试其他关键词或稍后再试。";
        }
    }
    
    @Override
    public String getAiResponse(String message, ChatRequest.Message... history) {
        try {
            // 获取访问令牌（新的认证方式：直接返回 API Key）
            String apiKey = getAccessToken();
            
            // 构建请求参数
            JSONObject requestBody = new JSONObject();
            JSONArray messages = new JSONArray();
            
            // 如果有历史记录，添加历史记录
            if (history != null && history.length > 0) {
                for (ChatRequest.Message msg : history) {
                    JSONObject msgObj = new JSONObject();
                    msgObj.put("role", msg.getRole());
                    msgObj.put("content", msg.getContent());
                    messages.add(msgObj);
                }
            } else {
                // 如果没有历史记录，直接添加消息
                JSONObject latestMsg = new JSONObject();
                latestMsg.put("role", "user");
                latestMsg.put("content", message);
                messages.add(latestMsg);
            }
            
            requestBody.put("model", "deepseek-chat");
            requestBody.put("messages", messages);
            
            // 发送请求
            String response = HttpRequest.post(DEEPSEEK_API_URL)
                    .contentType("application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .body(requestBody.toString())
                    .execute()
                    .body();
            
            // 解析响应
            JSONObject responseObj = JSONUtil.parseObj(response);
            if (responseObj.containsKey("choices")) {
                JSONArray choices = responseObj.getJSONArray("choices");
                if (choices.size() > 0) {
                    JSONObject choice = choices.getJSONObject(0);
                    if (choice.containsKey("message")) {
                        JSONObject messageObj = choice.getJSONObject("message");
                        if (messageObj.containsKey("content")) {
                            return messageObj.getStr("content");
                        }
                    }
                }
                throw new RuntimeException("AI response error: no content");
            } else if (responseObj.containsKey("error")) {
                JSONObject error = responseObj.getJSONObject("error");
                if (error.containsKey("message")) {
                    throw new RuntimeException(error.getStr("message"));
                } else {
                    throw new RuntimeException("AI response error: " + error.toString());
                }
            } else {
                throw new RuntimeException("AI response error");
            }
        } catch (Exception e) {
            log.error("Get AI response error: {}", e.getMessage(), e);
            return "抱歉，我暂时无法回答你的问题，请稍后再试。";
        }
    }
    
    @Override
    public String recognizeIntent(String message) {
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
    
    @Override
    public Object extractEntities(String message, String intent) {
        switch (intent) {
            case "search":
                // 提取搜索关键词
                return extractSearchKeyword(message);
            case "addCart":
                // 提取商品信息
                return extractProductInfo(message);
            case "queryOrder":
                // 提取订单号
                return extractOrderId(message);
            case "navigate":
                // 提取导航目标
                return extractNavigateTarget(message);
            case "trackOrder":
                // 提取订单号
                return extractOrderId(message);
            case "quickOrder":
                // 提取订单信息（商品、数量、地址等）
                return extractOrderInfo(message);
            case "recommend":
                // 提取推荐关键词（如商品类型）
                return extractRecommendKeyword(message);
            case "analyzeSentiment":
                // 提取商品ID用于评论分析
                return extractGoodId(message);
            case "analyzeSales":
                // 提取商品ID和天数用于销售分析
                return extractSalesParams(message);
            default:
                return null;
        }
    }
    
    @Override
    public Object getPersonalizedRecommendation(Long userId) {
        // 调用AI业务服务获取个性化推荐
        return aiBusinessService.getPersonalizedRecommendation(userId);
    }
    
    /**
     * 提取搜索关键词
     */
    private String extractSearchKeyword(String message) {
        // 简单的关键词提取，实际项目中可以使用更复杂的 NLP 技术
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
    private JSONObject extractProductInfo(String message) {
        JSONObject productInfo = new JSONObject();
        
        // 简单的商品信息提取，实际项目中可以使用更复杂的 NLP 技术
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
        // 简单的订单号提取，实际项目中可以使用正则表达式匹配订单号格式
        Pattern pattern = Pattern.compile("\\d{8,20}");
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }
    
    /**
     * 提取订单信息（用于一键下单）
     */
    private JSONObject extractOrderInfo(String message) {
        JSONObject orderInfo = new JSONObject();
        
        // 简单的订单信息提取，实际项目中可以使用更复杂的 NLP 技术
        // 提取商品名称
        String[] productKeywords = {"买", "下单", "购买", "订购"};
        String productName = message;
        for (String keyword : productKeywords) {
            if (message.contains(keyword)) {
                productName = message.replace(keyword, "").trim();
                break;
            }
        }
        orderInfo.put("productName", productName);
        
        // 提取数量
        if (message.contains("个") || message.contains("件")) {
            try {
                String[] parts = message.split("[个件]");
                if (parts.length > 0) {
                    String numStr = parts[0].replaceAll("[^0-9]", "");
                    if (!numStr.isEmpty()) {
                        orderInfo.put("quantity", Integer.parseInt(numStr));
                    }
                }
            } catch (Exception e) {
                orderInfo.put("quantity", 1);
            }
        } else {
            orderInfo.put("quantity", 1);
        }
        
        // 提取地址（简化版，实际项目中应该使用更复杂的地址识别）
        if (message.contains("寄到") || message.contains("送到") || message.contains("地址")) {
            orderInfo.put("address", "默认地址"); // 简化处理
        }
        
        return orderInfo;
    }
    
    /**
     * 提取导航目标
     */
    private String extractNavigateTarget(String message) {
        // 简单的导航目标提取，实际项目中可以使用更复杂的 NLP 技术
        String[] keywords = {"去", "进入", "打开", "页面"};
        for (String keyword : keywords) {
            if (message.contains(keyword)) {
                return message.replace(keyword, "").trim();
            }
        }
        return message;
    }
    
    /**
     * 提取推荐关键词
     */
    private String extractRecommendKeyword(String message) {
        // 简单的推荐关键词提取
        String[] keywords = {"推荐", "有什么好", "介绍一下", "值得买", "好物"};
        for (String keyword : keywords) {
            if (message.contains(keyword)) {
                return message.replace(keyword, "").trim();
            }
        }
        return message;
    }
    
    /**
     * 提取商品ID
     */
    private java.util.Map<String, Object> extractGoodId(String message) {
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        
        // 使用正则表达式提取数字作为商品ID
        Pattern pattern = Pattern.compile("(?:商品|ID|id)[:：]?\\s*(\\d+)");
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            params.put("goodId", Long.parseLong(matcher.group(1)));
        } else {
            // 尝试提取消息中的任意数字
            pattern = Pattern.compile("\\b(\\d{1,10})\\b");
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
    private java.util.Map<String, Object> extractSalesParams(String message) {
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        
        // 提取商品ID
        Pattern pattern = Pattern.compile("(?:商品|ID|id)[:：]?\\s*(\\d+)");
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            params.put("goodId", Long.parseLong(matcher.group(1)));
        } else {
            // 尝试提取消息中的任意数字
            pattern = Pattern.compile("\\b(\\d{1,10})\\b");
            matcher = pattern.matcher(message);
            if (matcher.find()) {
                params.put("goodId", Long.parseLong(matcher.group(1)));
            }
        }
        
        // 提取天数
        pattern = Pattern.compile("(\\d+)\\s*天");
        matcher = pattern.matcher(message);
        if (matcher.find()) {
            params.put("days", Integer.parseInt(matcher.group(1)));
        } else {
            params.put("days", 30); // 默认30天
        }
        
        return params;
    }
    
    /**
     * 获取访问令牌
     */
    private String getAccessToken() {
        // 新的认证方式：直接返回 API Key
        return apiKey;
    }
    
    /**
     * 流式聊天对话（使用原生 Servlet 实现，确保 SSE 正确工作）
     * @param request 聊天请求
     * @param writer PrintWriter 用于写入响应流
     */
    @Override
    public void streamChatWithWriter(ChatRequest request, PrintWriter writer) {
        try {
            log.info("=== 开始流式聊天（同步版本）===");
            log.info("用户消息：{}", request.getMessage());
            
            // ✅ 关键：先进行意图识别
            String mode = request.getMode(); // 从前端传入的模式：chat 或 help
            String intent = "chat"; // 默认为聊天
            
            // 如果是智能帮助模式，进行意图识别
            if ("help".equals(mode)) {
                log.info("智能帮助模式：开始识别意图");
                intent = recognizeIntentWithAI(request.getMessage());
                log.info("识别出的用户意图：{}", intent);
                
                // 如果不是聊天意图，先执行业务操作，然后流式输出结果
                if (!"chat".equals(intent) && !"unknown".equals(intent)) {
                    log.info("执行非聊天意图：{}", intent);
                    executeBusinessOperationWithStreaming(intent, request, writer);
                    return; // 业务操作已完成，直接返回
                }
            }
            
            // 1. 构建请求
            String apiKey = getAccessToken();
            
            JSONObject requestBody = new JSONObject();
            JSONArray messages = new JSONArray();
            
            // ✅ 关键：添加系统设定消息（放在最前面）
            JSONObject systemMsg = new JSONObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", "你是小皮助手，是一个电商购物助手。你可以帮助用户搜索商品、添加购物车、查询订单、导航到指定页面，以及解答购物相关的问题。请保持友好、热情、专业的态度，用简洁清晰的语言回答用户问题。");
            messages.add(systemMsg);
            
            // 添加用户消息
            JSONObject msgObj = new JSONObject();
            msgObj.put("role", "user");
            msgObj.put("content", request.getMessage());
            messages.add(msgObj);
            
            requestBody.put("model", "deepseek-chat");
            requestBody.put("messages", messages);
            requestBody.put("stream", true);
            
            // 2. 调用 DeepSeek 流式接口
            java.net.URL url = new java.net.URL(DEEPSEEK_API_URL);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setDoOutput(true);
            connection.setChunkedStreamingMode(0);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(0);
            
            log.debug("DeepSeek API URL: {}", DEEPSEEK_API_URL);
            log.debug("API Key: {}", apiKey != null ? apiKey.substring(0, 10) + "..." : "null");
            log.debug("请求体：{}", requestBody.toString());
            
            // 写入请求体
            try (java.io.OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.toString().getBytes("UTF-8");
                os.write(input, 0, input.length);
                os.flush();
            }
            
            log.debug("DeepSeek API 响应码：{}", connection.getResponseCode());
            log.debug("DeepSeek API 响应消息：{}", connection.getResponseMessage());
            
            // 3. 逐行读取并实时推送给前端（同步执行，不开子线程）
            boolean hasReceivedDone = false; // 标记是否已收到 DONE
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                String line;
                
                while ((line = reader.readLine()) != null) {
                    log.trace("DeepSeek 返回原始数据：{}", line);
                    
                    if (line.isEmpty()) continue;
                    
                    // 去除 "data: " 前缀
                    String trimmedLine = line;
                    if (line.startsWith("data:")) {
                        trimmedLine = line.substring(5).trim();
                    }
                    
                    // 结束标志（检查原始行和去除前缀后的行）
                    if ("[DONE]".equals(line.trim()) || "[DONE]".equals(trimmedLine)) {
                        log.info("收到 DeepSeek 返回的 [DONE] 标志");
                        hasReceivedDone = true;
                        // ✅ 规范性：[DONE] 标志也应该包含 event 字段
                        writer.write("event: done\n");
                        writer.write("data: [DONE]\n\n");
                        writer.flush();
                        // 检查是否有错误
                        if (writer.checkError()) {
                            log.warn("推送 [DONE] 时检测到错误");
                        }
                        break;
                    }
                    
                    // 解析 DeepSeek 返回的内容片段
                    try {
                        // ✅ 关键修复：使用 trim 后的数据解析 JSON
                        JSONObject obj = JSONUtil.parseObj(trimmedLine);
                        JSONArray choices = obj.getJSONArray("choices");
                        if (choices == null || choices.isEmpty()) {
                            log.debug("choices 为空，跳过");
                            continue;
                        }
                        
                        JSONObject delta = choices.getJSONObject(0).getJSONObject("delta");
                        log.trace("delta 对象：{}", delta.toString());
                        
                        // ✅ 关键：delta 可能没有 content 字段（如只有 role）
                        String content = delta.get("content") != null ? delta.getStr("content") : null;
                        
                        if (content != null && !content.isEmpty()) {
                            log.debug("实时推送给前端：{}", content);
                            
                            // 使用标准 SSE 格式发送
                            Map<String, Object> map = new HashMap<>();
                            map.put("content", content);
                            String json = JSONUtil.toJsonStr(map);
                            
                            // ✅ 规范性：SSE 标准格式，必须包含 event 和 data，并且以两个换行结束
                            writer.write("event: message\n");
                            writer.write("data: " + json + "\n\n");
                            writer.flush();
                            
                            // 检查是否有错误
                            if (writer.checkError()) {
                                log.warn("推送内容时检测到错误：{}", content);
                                break;
                            }
                            
                            log.trace("已推送给前端：{}", content);
                        } else {
                            log.debug("content 为空或不存在，跳过");
                        }
                    } catch (Exception e) {
                        log.warn("解析消息失败：{}", e.getMessage());
                        log.debug("原始行：{}", line);
                        log.debug("trim 后：{}", trimmedLine);
                    }
                }
                
                // ✅ 关键修复：只有当没有收到 [DONE] 时，才手动发送 done 标志
                if (!hasReceivedDone) {
                    log.info("DeepSeek 流式数据读取完成（未收到 [DONE]），手动发送 DONE 标志");
                    writer.write("event: done\n");
                    writer.write("data: [DONE]\n\n");
                    writer.flush();
                } else {
                    log.debug("已收到 [DONE] 标志，不再重复发送");
                }
                
            } finally {
                connection.disconnect();
                log.debug("DeepSeek 连接已断开");
            }
            
        } catch (Exception e) {
            log.error("流式聊天整体失败：{}", e.getMessage(), e);
            // 尝试发送错误消息给前端
            try {
                writer.write("event: error\n");
                writer.write("data: {\"error\":\"" + e.getMessage() + "\"}\n\n");
                writer.flush();
            } catch (Exception ex) {
                log.warn("发送错误消息给前端失败", ex);
            }
        } finally {
            // ✅ 规范性：确保 PrintWriter 被关闭
            writer.close();
            log.debug("PrintWriter 已关闭");
        }
    }
    
    /**
     * 执行业务操作并流式输出结果（复用 handleHelpMode 的完整逻辑）
     */
    private void executeBusinessOperationWithStreaming(String intent, ChatRequest request, PrintWriter writer) {
        try {
            log.info("开始执行业务操作（流式版本）：{}", intent);
            
            // ✅ 关键：复用 handleHelpMode 的完整业务逻辑
            // 创建一个临时的 ChatResponse，模拟 handleHelpMode 的执行结果
            ChatResponse response = simulateHelpMode(intent, request);
            
            // ✅ 关键：先发送 action 给前端，让前端执行操作
            if (response != null && response.getAction() != null) {
                log.info("发送 action 给前端：{}", response.getAction());
                Map<String, Object> actionMap = new HashMap<>();
                actionMap.put("action", response.getAction());
                actionMap.put("actionData", response.getActionData());
                String actionJson = JSONUtil.toJsonStr(actionMap);
                
                writer.write("event: action\n");
                writer.write("data: " + actionJson + "\n\n");
                writer.flush();
            }
            
            // 流式输出结果
            log.info("开始流式输出业务操作结果");
            
            // 输出 AI 回复
            if (response != null && response.getContent() != null) {
                sendStreamMessage(writer, response.getContent());
            }
            
            // 发送 done 标志
            writer.write("event: done\n");
            writer.write("data: [DONE]\n\n");
            writer.flush();
            
            log.info("业务操作执行完成");
            
        } catch (Exception e) {
            log.error("执行业务操作失败：{}", e.getMessage(), e);
            try {
                writer.write("event: error\n");
                writer.write("data: {\"error\":\"" + e.getMessage() + "\"}\n\n");
                writer.flush();
            } catch (Exception ex) {
                log.warn("发送错误消息给前端失败", ex);
            }
        }
    }
    
    /**
     * 模拟 handleHelpMode 的完整业务逻辑
     */
    private ChatResponse simulateHelpMode(String intent, ChatRequest request) {
        try {
            // 提取实体
            Object entities = extractEntities(request.getMessage(), intent);
            log.debug("提取到的实体：{}", entities);
            
            // 执行业务操作
            Object result = null;
            String aiResponse = "";
            
            if ("search".equals(intent)) {
                log.debug("执行搜索操作");
                result = aiBusinessService.search(entities.toString());
                aiResponse = "已为您找到相关商品，请查看搜索结果。";
            } else if ("addCart".equals(intent)) {
                log.debug("执行添加购物车操作");
                java.util.Map<String, Object> params = new java.util.HashMap<>();
                if (entities instanceof java.util.Map) {
                    params.putAll((java.util.Map<String, Object>) entities);
                } else if (entities != null) {
                    params.put("goodId", entities.toString());
                }
                if (request.getUserId() != null) {
                    params.put("userId", request.getUserId());
                }
                result = aiBusinessService.addCart(params);
                aiResponse = "商品已成功添加到购物车！";
            } else if ("batchAddCart".equals(intent)) {
                log.debug("执行批量添加购物车操作");
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
                result = aiBusinessService.batchAddCart(paramsList);
                aiResponse = "商品已成功添加到购物车！";
            } else if ("queryOrder".equals(intent)) {
                log.debug("执行订单查询操作");
                java.util.Map<String, Object> params = new java.util.HashMap<>();
                if (entities instanceof java.util.Map) {
                    params.putAll((java.util.Map<String, Object>) entities);
                } else if (entities != null) {
                    params.put("orderNo", entities.toString());
                }
                if (request.getUserId() != null) {
                    params.put("userId", request.getUserId());
                }
                result = aiBusinessService.queryOrder(params);
                aiResponse = "已为您查询到订单信息，请查看详情。";
            } else if ("navigate".equals(intent)) {
                log.debug("执行页面导航操作");
                // 正确提取页面名称
                String pageName = "";
                if (entities instanceof java.util.Map) {
                    java.util.Map<String, Object> entityMap = (java.util.Map<String, Object>) entities;
                    Object pageObj = entityMap.get("page");
                    if (pageObj != null) {
                        pageName = pageObj.toString();
                    } else {
                        // 如果没有 page 字段，尝试从其他字段获取
                        pageName = entityMap.values().stream()
                            .filter(v -> v != null)
                            .map(Object::toString)
                            .findFirst()
                            .orElse("首页");
                    }
                } else if (entities != null) {
                    pageName = entities.toString();
                } else {
                    pageName = "首页";
                }
                log.debug("导航页面名称：{}", pageName);
                result = aiBusinessService.navigate(pageName);
                aiResponse = "正在为您导航到" + pageName + "...";
            } else if ("logout".equals(intent)) {
                log.debug("执行退出登录操作");
                result = aiBusinessService.logout(request.getUserId());
                aiResponse = "已为您退出登录。";
            } else if ("recommend".equals(intent)) {
                log.debug("执行个性化推荐操作");
                Long userId = request.getUserId() != null ? Long.parseLong(request.getUserId().toString()) : 1L;
                result = aiBusinessService.recommendGoodsForUser(userId);
                aiResponse = "根据您的浏览和购买记录，为您推荐以下商品：";
            } else if ("quickOrder".equals(intent)) {
                log.debug("执行一键下单操作");
                java.util.Map<String, Object> params = new java.util.HashMap<>();
                if (entities instanceof java.util.Map) {
                    params.putAll((java.util.Map<String, Object>) entities);
                }
                if (request.getUserId() != null) {
                    params.put("userId", request.getUserId());
                }
                result = aiBusinessService.quickOrder(params);
                aiResponse = "订单创建成功！正在为您跳转到支付页面...";
            } else if ("trackOrder".equals(intent)) {
                log.debug("执行订单状态跟踪操作");
                String orderNo = entities != null ? entities.toString() : null;
                Long userId = request.getUserId() != null ? Long.parseLong(request.getUserId().toString()) : 1L;
                if (orderNo == null || orderNo.isEmpty()) {
                    aiResponse = "请提供订单号以便我为您查询订单状态。";
                    result = null;
                } else {
                    result = aiBusinessService.trackOrderStatus(orderNo, userId);
                    aiResponse = "您的订单状态如下：";
                }
            } else if ("analyzeOrders".equals(intent)) {
                log.debug("执行订单历史分析操作");
                Long userId = request.getUserId() != null ? Long.parseLong(request.getUserId().toString()) : 1L;
                result = aiBusinessService.analyzeOrderHistory(userId);
                aiResponse = "您的订单分析报告如下：";
            } else {
                // 其他意图，让 AI 生成回复
                log.debug("执行其他操作");
                String aiResponsePrompt = "请针对用户的需求生成一个友好的回复：" + request.getMessage();
                aiResponse = getAiResponse(aiResponsePrompt);
            }
            
            // 构建响应
            ChatResponse response = ChatResponse.success(aiResponse);
            
            // 设置操作参数
            if (result != null) {
                ChatResponse.Action action = new ChatResponse.Action();
                action.setType(intent);
                action.setParams(result);
                response.setAction(action.getType());
                response.setActionData("params", result);
            }
            
            return response;
            
        } catch (Exception e) {
            log.error("模拟 handleHelpMode 失败：{}", e.getMessage(), e);
            ChatResponse errorResponse = new ChatResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("抱歉，我暂时无法处理您的需求");
            return errorResponse;
        }
    }
    
    /**
     * 流式输出消息
     */
    private void sendStreamMessage(PrintWriter writer, String message) {
        if (message == null || message.isEmpty()) return;
        
        // 逐字输出
        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            String charStr = String.valueOf(c);
            
            Map<String, Object> map = new HashMap<>();
            map.put("content", charStr);
            String json = JSONUtil.toJsonStr(map);
            
            writer.write("event: message\n");
            writer.write("data: " + json + "\n\n");
            writer.flush();
            
            // 检查是否有错误
            if (writer.checkError()) {
                log.warn("推送SSE内容时检测到客户端连接异常");
                break;
            }
            
            // 短暂延迟，模拟打字效果
            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
