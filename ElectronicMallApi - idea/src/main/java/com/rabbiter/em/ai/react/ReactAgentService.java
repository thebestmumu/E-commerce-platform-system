package com.rabbiter.em.ai.react;

import com.rabbiter.em.ai.core.AiContextManager;
import com.rabbiter.em.ai.mcp.CustomerServiceSkills;
import com.rabbiter.em.ai.mcp.MallToolService;
import com.rabbiter.em.ai.rag.KnowledgeBaseService;
import com.rabbiter.em.ai.rag.RagService;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ReAct Agent 服务 - 手动实现的 ReAct 循环
 * 
 * ===== 什么是 ReAct？ =====
 * ReAct = Reasoning（推理）+ Acting（行动）
 * 
 * 工作流程：
 * 1. 推理：AI 思考用户问题，决定需要做什么
 * 2. 行动：调用工具获取数据
 * 3. 观察：查看工具返回结果
 * 4. 重复 1-3，直到 AI 有足够信息生成最终回答
 * 
 * ===== 与 MallAiAssistant 的区别 =====
 * - MallAiAssistant：LangChain4j 自动生成的代理（声明式，推荐用这个）
 * - ReactAgentService：手动实现的 ReAct 循环（命令式，用于理解原理）
 * 
 * ===== 前端类比 =====
 * 就像一个 while 循环，每次循环：
 * - 调用 API（AI 推理）
 * - 根据返回结果决定下一步（调用工具 or 结束）
 * - 直到得到最终答案
 */
@Service
public class ReactAgentService {

    private static final Logger log = LoggerFactory.getLogger(ReactAgentService.class);

    /** AI 语言模型（DeepSeek） */
    @Resource
    private ChatLanguageModel chatModel;

    /** 商城业务工具 */
    @Resource
    private MallToolService mallToolService;

    /** 客服技能工具 */
    @Resource
    private CustomerServiceSkills customerServiceSkills;

    /** 对话上下文管理器 */
    @Resource
    private AiContextManager contextManager;

    /** RAG 知识库服务 */
    @Resource
    private RagService ragService;

    /** 知识库底层服务 */
    @Resource
    private KnowledgeBaseService knowledgeBaseService;

    /** 最多执行 8 轮 ReAct 循环（防止无限循环） */
    private static final int MAX_REACT_ROUNDS = 8;

    /**
     * 执行 ReAct Agent
     * 
     * @param userId 用户 ID
     * @param userMessage 用户消息
     * @param historyMessages 历史对话消息
     * @return ReAct 执行结果（包含最终回答和工具调用记录）
     */
    public ReactResult execute(Long userId, String userMessage, List<ChatMessage> historyMessages) {
        log.info("=== ReAct Agent 开始执行 ===");
        log.info("用户 ID: {}, 消息: {}", userId, userMessage);

        try {
            // 步骤1：获取所有可用工具的规格描述
            // 类比：就像获取所有 API 函数的签名（函数名、参数、返回值）
            List<ToolSpecification> allToolSpecs = getAllToolSpecifications();
            log.info("ReAct Agent 注册工具数: {}", allToolSpecs.size());

            // 步骤2：构建消息列表
            List<ChatMessage> messages = new ArrayList<>();

            // 添加系统提示词（AI 的"人设"和行为准则）
            messages.add(buildSystemPrompt(userId));

            // 添加历史对话（最多保留最近 6 条，避免上下文太长）
            if (historyMessages != null && !historyMessages.isEmpty()) {
                int startIdx = Math.max(0, historyMessages.size() - 6);
                messages.addAll(historyMessages.subList(startIdx, historyMessages.size()));
            }

            // 添加用户当前消息
            messages.add(new UserMessage(userMessage));

            String finalAnswer = null;
            int round = 0;
            List<ToolCallRecord> toolCallRecords = new ArrayList<>();  // 记录工具调用历史

            // ===== ReAct 核心循环 =====
            while (round < MAX_REACT_ROUNDS) {
                round++;
                log.info("ReAct 推理循环 - 第 {} 轮", round);

                // 调用 AI（带上消息列表和工具规格）
                ChatResponse response = chatModel.chat(ChatRequest.builder()
                        .messages(messages)
                        .toolSpecifications(allToolSpecs)  // 告诉 AI 有哪些工具可用
                        .build());

                AiMessage aiMessage = response.aiMessage();

                // 判断：AI 是要调用工具，还是直接回复？
                if (aiMessage.hasToolExecutionRequests()) {
                    // AI 决定调用工具
                    List<ToolExecutionRequest> toolRequests = aiMessage.toolExecutionRequests();
                    log.info("LLM 请求调用 {} 个工具", toolRequests.size());

                    // 把 AI 的工具调用请求加入消息列表
                    messages.add(aiMessage);

                    // 执行每个工具调用
                    for (ToolExecutionRequest toolRequest : toolRequests) {
                        String toolName = toolRequest.name();
                        String toolArgs = toolRequest.arguments();
                        log.info(">> 工具调用: {}({})", toolName, toolArgs);

                        // 反射调用对应的 Java 方法
                        String toolResult = executeTool(toolName, toolArgs, userId);
                        log.info("<< 工具结果: {}",
                                toolResult.substring(0, Math.min(150, toolResult.length())));

                        // 把工具执行结果加入消息列表
                        messages.add(ToolExecutionResultMessage.from(toolRequest, toolResult));

                        // 记录工具调用（用于返回给前端展示）
                        toolCallRecords.add(new ToolCallRecord(toolName, toolArgs, toolResult));
                    }
                    // 继续下一轮循环，让 AI 基于工具结果做决策
                } else {
                    // AI 直接回复了最终答案（不再需要调用工具）
                    finalAnswer = aiMessage.text();
                    log.info("ReAct 得到最终回答: {}", finalAnswer);
                    break;  // 退出循环
                }
            }

            // 如果循环结束还没有最终答案，返回默认提示
            if (finalAnswer == null || finalAnswer.isEmpty()) {
                finalAnswer = "抱歉，我暂时无法处理您的请求，建议转接人工客服处理。";
            }

            log.info("=== ReAct Agent 执行完成，共 {} 轮推理 ===", round);

            // 构建返回结果
            ReactResult result = new ReactResult();
            result.setAnswer(finalAnswer);
            result.setToolCalls(toolCallRecords);
            result.setRounds(round);
            return result;

        } catch (Exception e) {
            log.error("ReAct Agent 执行异常", e);
            ReactResult result = new ReactResult();
            result.setAnswer("抱歉，系统处理出错，请稍后再试。错误：" + e.getMessage());
            result.setError(e.getMessage());
            return result;
        }
    }

    private SystemMessage buildSystemPrompt(Long userId) {
        String basePrompt = """
                你是"小皮助手"，一个智能电商平台的专业客服助手，具备 ReAct（推理+行动）能力。
                
                ## 核心能力
                你拥有以下能力：
                1. 搜索推荐商品 - 帮助用户找商品
                2. 查看商品详情和评价
                3. 管理购物车（添加/查看）
                4. 订单管理（查看/创建）
                5. 商品分类浏览
                6. 机票酒店搜索（飞猪MCP）
                7. 知识库问答（退换货政策、发货、支付等）
                8. 转接人工客服
                
                ## 行为准则
                - 始终用中文回复，语气友好专业
                - 每次回答前先思考用户真正需要什么
                - 如果需要用户提供更多信息才能执行操作，先问清楚
                - 工具调用结果返回后，用自然语言总结给用户
                - 如果用户情绪激动，先安抚再处理问题
                - 处理订单/购物车操作时，确认用户身份后再执行
                - 不知道答案时诚实告知，建议转人工客服
                
                ## 当前约束
                当前时间：%s
                """;

        String currentTime = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        return new SystemMessage(String.format(basePrompt, currentTime));
    }

    private List<ToolSpecification> getAllToolSpecifications() {
        List<ToolSpecification> specs = new ArrayList<>();

        try {
            specs.addAll(ToolSpecifications.toolSpecificationsFrom(mallToolService));
        } catch (Exception e) {
            log.warn("加载 MallToolService 工具规格失败", e);
        }

        try {
            specs.addAll(ToolSpecifications.toolSpecificationsFrom(customerServiceSkills));
        } catch (Exception e) {
            log.warn("加载 CustomerServiceSkills 工具规格失败", e);
        }

        return specs;
    }

    private String executeTool(String toolName, String arguments, Long userId) {
        try {
            switch (toolName) {
                case "searchProducts": {
                    String keyword = extractParam(arguments, "keyword", "arg0");
                    return mallToolService.searchProducts(keyword);
                }
                case "getProductDetail": {
                    Long pid = Long.parseLong(extractParam(arguments, "productId", "arg0"));
                    return mallToolService.getProductDetail(pid);
                }
                case "getCategories":
                    return mallToolService.getCategories();
                case "getRecommendedProducts":
                    return mallToolService.getRecommendedProducts();
                case "getProductReviews": {
                    Long pid = Long.parseLong(extractParam(arguments, "productId", "arg0"));
                    return mallToolService.getProductReviews(pid);
                }
                case "addToCart": {
                    Long uid = extractLongParam(arguments, "userId", "arg0", userId);
                    Long gid = Long.parseLong(extractParam(arguments, "goodId", "arg1"));
                    Integer count = extractIntParam(arguments, "count", "arg2", 1);
                    String standard = extractParam(arguments, "standard", "arg3", "默认");
                    return mallToolService.addToCart(uid, gid, count, standard);
                }
                case "viewCart": {
                    Long uid = extractLongParam(arguments, "userId", "arg0", userId);
                    return mallToolService.viewCart(uid);
                }
                case "createOrder": {
                    Long uid = extractLongParam(arguments, "userId", "arg0", userId);
                    String goods = extractParam(arguments, "goodsJson", "arg1");
                    String linkUser = extractParam(arguments, "linkUser", "arg2");
                    String linkPhone = extractParam(arguments, "linkPhone", "arg3");
                    String linkAddress = extractParam(arguments, "linkAddress", "arg4");
                    return mallToolService.createOrder(uid, goods, linkUser, linkPhone, linkAddress);
                }
                case "viewOrders": {
                    Long uid = extractLongParam(arguments, "userId", "arg0", userId);
                    return mallToolService.viewOrders(uid);
                }
                case "searchFlights": {
                    String from = extractParam(arguments, "departureCity", "arg0");
                    String to = extractParam(arguments, "arrivalCity", "arg1");
                    String date = extractParam(arguments, "departureDate", "arg2");
                    return mallToolService.searchFlights(from, to, date);
                }
                case "searchHotels": {
                    String city = extractParam(arguments, "city", "arg0");
                    String checkIn = extractParam(arguments, "checkInDate", "arg1");
                    String checkOut = extractParam(arguments, "checkOutDate", "arg2");
                    return mallToolService.searchHotels(city, checkIn, checkOut);
                }

                case "queryKnowledgeBase": {
                    String query = extractParam(arguments, "query", "arg0");
                    return customerServiceSkills.queryKnowledgeBase(query);
                }
                case "checkReturnPolicy":
                    return customerServiceSkills.checkReturnPolicy();
                case "checkShippingInfo":
                    return customerServiceSkills.checkShippingInfo();
                case "checkPaymentMethods":
                    return customerServiceSkills.checkPaymentMethods();
                case "checkAfterSalesPolicy":
                    return customerServiceSkills.checkAfterSalesPolicy();
                case "transferToHuman":
                    return customerServiceSkills.transferToHuman("用户要求转人工客服");
                case "queryOrderTracking": {
                    String orderNo = extractParam(arguments, "orderNo", "arg0");
                    return customerServiceSkills.queryOrderTracking(orderNo);
                }
                case "estimateDelivery": {
                    String city = extractParam(arguments, "city", "arg0");
                    return customerServiceSkills.estimateDelivery(city);
                }

                default:
                    return "{\"error\":\"未知工具: " + toolName + "\"}";
            }
        } catch (Exception e) {
            log.error("工具执行失败: {}", toolName, e);
            return "{\"error\":\"工具执行失败: " + e.getMessage() + "\"}";
        }
    }

    private String extractParam(String json, String namedParam, String positionalParam) {
        return extractParam(json, namedParam, positionalParam, null);
    }

    private String extractParam(String json, String namedParam, String positionalParam, String defaultValue) {
        try {
            if (json != null && json.contains("\"" + namedParam + "\"")) {
                com.alibaba.fastjson.JSONObject obj = com.alibaba.fastjson.JSON.parseObject(json);
                String val = obj.getString(namedParam);
                if (val != null && !val.isEmpty()) return val;
            }
        } catch (Exception ignored) {}
        return defaultValue;
    }

    private Long extractLongParam(String json, String namedParam, String positionalParam, Long defaultValue) {
        try {
            if (json != null && json.contains("\"" + namedParam + "\"")) {
                com.alibaba.fastjson.JSONObject obj = com.alibaba.fastjson.JSON.parseObject(json);
                return obj.getLong(namedParam);
            }
        } catch (Exception ignored) {}
        return defaultValue;
    }

    private Integer extractIntParam(String json, String namedParam, String positionalParam, Integer defaultValue) {
        try {
            if (json != null && json.contains("\"" + namedParam + "\"")) {
                com.alibaba.fastjson.JSONObject obj = com.alibaba.fastjson.JSON.parseObject(json);
                return obj.getInteger(namedParam);
            }
        } catch (Exception ignored) {}
        return defaultValue;
    }

    public static class ReactResult {
        private String answer;
        private List<ToolCallRecord> toolCalls;
        private int rounds;
        private String error;

        public String getAnswer() { return answer; }
        public void setAnswer(String answer) { this.answer = answer; }
        public List<ToolCallRecord> getToolCalls() { return toolCalls; }
        public void setToolCalls(List<ToolCallRecord> toolCalls) { this.toolCalls = toolCalls; }
        public int getRounds() { return rounds; }
        public void setRounds(int rounds) { this.rounds = rounds; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }

    public static class ToolCallRecord {
        private String toolName;
        private String arguments;
        private String result;

        public ToolCallRecord() {}

        public ToolCallRecord(String toolName, String arguments, String result) {
            this.toolName = toolName;
            this.arguments = arguments;
            this.result = result;
        }

        public String getToolName() { return toolName; }
        public void setToolName(String toolName) { this.toolName = toolName; }
        public String getArguments() { return arguments; }
        public void setArguments(String arguments) { this.arguments = arguments; }
        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }
    }
}