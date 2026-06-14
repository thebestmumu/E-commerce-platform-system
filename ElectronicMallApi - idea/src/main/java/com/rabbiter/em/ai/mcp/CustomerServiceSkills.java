package com.rabbiter.em.ai.mcp;

import cn.hutool.json.JSONUtil;
import com.rabbiter.em.ai.core.SseEmitterContext;
import com.rabbiter.em.ai.rag.KnowledgeBaseService;
import com.rabbiter.em.ai.rag.RagService;
import com.rabbiter.em.service.TicketService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 客服技能工具集 - AI 客服的专用技能
 * 
 * ===== 与 MallToolService 的区别 =====
 * - MallToolService：商城业务工具（查商品、加购物车、下单等）
 * - CustomerServiceSkills：客服专用工具（查知识库、转人工、创建工单等）
 * 
 * ===== 前端类比 =====
 * 就像一个客服组件的 methods 集合，每个方法都是一个客服能做的操作。
 * AI 会根据用户问题，自主决定调用哪个技能。
 * 
 * 例如：
 * 用户："怎么退货？" → AI 调用 checkReturnPolicy()
 * 用户："转人工" → AI 调用 transferToHuman()
 * 用户："发个工单" → AI 调用 createTicket()
 */
@Component
public class CustomerServiceSkills {

    private static final Logger log = LoggerFactory.getLogger(CustomerServiceSkills.class);

    /** 知识库服务 - 向量检索 */
    @Resource
    private KnowledgeBaseService knowledgeBaseService;

    /** RAG 问答服务 - 基于知识库生成回答 */
    @Resource
    private RagService ragService;

    /** 工单服务 - 创建客服工单 */
    @Resource
    private TicketService ticketService;

    /**
     * 查询知识库（通用工具）
     * 
     * 这个工具内部会调用 RagService，执行完整的 RAG 流程：
     * 1. 向量检索相关文档
     * 2. 构建上下文
     * 3. AI 基于上下文生成回答
     * 
     * 类比：就像一个智能搜索框，输入问题，返回基于知识库的答案
     */
    @Tool("查询知识库，回答关于退换货、发货、支付、售后、账户等常见问题。当用户询问政策、规则、流程时调用")
    public String queryKnowledgeBase(
            @P("用户的问题，如'如何退货'、'什么时候能收到货'、'怎么开发票'等") String query
    ) {
        log.info("[工具调用] queryKnowledgeBase: query={}", query);
        SseEmitterContext.sendThinking("📚 正在查询知识库：「" + query + "」...");
        try {
            // 调用 RAG 服务获取基于知识库的回答
            String answer = ragService.answer(query);
            SseEmitterContext.sendThinking("📖 已获取相关知识，正在分析整理最佳答案...");
            return JSONUtil.toJsonStr(Map.of(
                    "success", true,
                    "answer", answer,
                    "source", "knowledge_base"  // 标记来源，AI 知道这是从知识库来的
            ));
        } catch (Exception e) {
            log.error("知识库查询失败", e);
            return JSONUtil.toJsonStr(Map.of(
                    "success", false,
                    "error", "查询失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 查询退换货政策
     * 
     * 与 queryKnowledgeBase 的区别：
     * - queryKnowledgeBase：向量搜索，找最相关的内容
     * - checkReturnPolicy：直接返回整个分类的所有文档
     * 
     * 适用场景：用户明确问退换货相关问题时，直接返回所有相关政策
     */
    @Tool("查询退换货政策。用户问'怎么退货'、'能退款吗'、'退换货政策'时调用")
    public String checkReturnPolicy() {
        log.info("[工具调用] checkReturnPolicy");
        SseEmitterContext.sendThinking("📋 正在查询退换货政策...");
        try {
            // 获取"return_policy"分类下的所有文档
            List<KnowledgeBaseService.KnowledgeDoc> docs = knowledgeBaseService.getAllDocsByCategory("return_policy");
            SseEmitterContext.sendThinking("📝 已获取退换货政策，正在提炼关键条款...");
            
            // 拼接所有文档内容
            String policy = docs.stream()
                    .map(d -> "【" + d.getTitle() + "】" + d.getContent())
                    .collect(Collectors.joining("\n"));

            return JSONUtil.toJsonStr(Map.of(
                    "success", true,
                    "policy", policy,
                    "category", "return_policy"
            ));
        } catch (Exception e) {
            return JSONUtil.toJsonStr(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /** 查询发货和物流相关信息 */
    @Tool("查询发货和物流相关信息。用户问'什么时候发货'、'物流信息'、'运费多少'时调用")
    public String checkShippingInfo() {
        log.info("[工具调用] checkShippingInfo");
        SseEmitterContext.sendThinking("🚚 正在查询物流信息...");
        try {
            List<KnowledgeBaseService.KnowledgeDoc> docs = knowledgeBaseService.getAllDocsByCategory("shipping");
            SseEmitterContext.sendThinking("📋 已获取物流政策，正在为您提炼关键信息...");
            String info = docs.stream()
                    .map(d -> "【" + d.getTitle() + "】" + d.getContent())
                    .collect(Collectors.joining("\n"));

            return JSONUtil.toJsonStr(Map.of(
                    "success", true,
                    "info", info,
                    "category", "shipping"
            ));
        } catch (Exception e) {
            return JSONUtil.toJsonStr(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /** 查询支持的支付方式 */
    @Tool("查询支持的支付方式。用户问'怎么支付'、'支持哪些支付'时调用")
    public String checkPaymentMethods() {
        log.info("[工具调用] checkPaymentMethods");
        SseEmitterContext.sendThinking("💳 正在查询支付方式...");
        try {
            List<KnowledgeBaseService.KnowledgeDoc> docs = knowledgeBaseService.getAllDocsByCategory("payment");
            SseEmitterContext.sendThinking("✅ 已获取支付方式信息，正在整理...");
            String info = docs.stream()
                    .map(d -> "【" + d.getTitle() + "】" + d.getContent())
                    .collect(Collectors.joining("\n"));

            return JSONUtil.toJsonStr(Map.of(
                    "success", true,
                    "info", info,
                    "category", "payment"
            ));
        } catch (Exception e) {
            return JSONUtil.toJsonStr(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /** 查询售后服务和保修政策 */
    @Tool("查询售后服务和保修政策。用户问'售后'、'保修'、'质量问题'、'投诉'时调用")
    public String checkAfterSalesPolicy() {
        log.info("[工具调用] checkAfterSalesPolicy");
        SseEmitterContext.sendThinking("🔧 正在查询售后政策...");
        try {
            List<KnowledgeBaseService.KnowledgeDoc> docs = knowledgeBaseService.getAllDocsByCategory("after_sales");
            SseEmitterContext.sendThinking("📋 已获取售后政策，正在为您提炼关键条款...");
            String policy = docs.stream()
                    .map(d -> "【" + d.getTitle() + "】" + d.getContent())
                    .collect(Collectors.joining("\n"));

            return JSONUtil.toJsonStr(Map.of(
                    "success", true,
                    "policy", policy,
                    "category", "after_sales"
            ));
        } catch (Exception e) {
            return JSONUtil.toJsonStr(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * 转接人工客服 - 非常重要的工具！
     * 
     * 触发条件：
     * 1. 用户明确要求转人工（如"转人工"、"找人工客服"）
     * 2. AI 无法解决用户问题
     * 
     * 工作流程：
     * 1. 创建工单（记录用户问题）
     * 2. 返回工单编号
     * 3. 发送 action 事件给前端，前端展示"进入在线客服"按钮
     * 
     * 类比：就像前端的路由跳转 + API 调用组合操作
     */
    @Tool("转接人工客服。当用户明确要求找人工客服，或者 AI 无法解决问题时调用。返回转接指引信息")
    public String transferToHuman(
            @P("转接原因") String reason
    ) {
        log.info("[工具调用] transferToHuman: reason={}", reason);
        SseEmitterContext.sendThinking("🔄 正在为您创建工单并转人工客服...");
        try {
            // 获取当前用户 ID（从 ThreadLocal 全局变量中获取）
            Long userId = getCurrentUserId();
            if (userId == null) {
                return JSONUtil.toJsonStr(Map.of(
                        "success", false,
                        "error", "无法获取用户信息，请先登录"
                ));
            }
            
            // 创建工单（类似前端提交表单到后端）
            String ticketNo = ticketService.createTicket(
                    userId, 
                    "other", 
                    "用户请求转人工客服", 
                    "转接原因：" + reason
            );
            
            SseEmitterContext.sendThinking("✅ 工单已创建，编号：" + ticketNo + "，正在为您接入在线客服...");
            
            // 构建工单信息（前端会解析这些数据并展示转人工卡片）
            // 类比：就像前端 emit('action', { type: 'transfer_to_human', data: {...} })
            Map<String, Object> actionData = new HashMap<>();
            actionData.put("ticketNo", ticketNo);
            actionData.put("userId", userId);
            actionData.put("message", "已为您创建工单，现在可以进入在线客服实时沟通");
            actionData.put("showChatButton", true);  // 前端展示按钮
            actionData.put("chatButtonText", "进入在线客服");
            actionData.put("chatButtonUrl", "/user-chat");  // 按钮跳转链接
            actionData.put("channels", List.of(
                    Map.of("type", "online", "label", "在线客服", "desc", "点击下方按钮进入实时聊天"),
                    Map.of("type", "phone", "label", "客服热线", "desc", "400-800-8888"),
                    Map.of("type", "service_hours", "label", "服务时间", "desc", "9:00-21:00")
            ));
            
            // 直接发送 action 事件给前端
            SseEmitterContext.sendAction("transfer_to_human", actionData);
            
            return JSONUtil.toJsonStr(Map.of(
                    "success", true,
                    "action", "transfer_to_human",
                    "actionData", actionData,
                    "content", "已为您成功转接人工客服！\n\n 工单编号：" + ticketNo + "\n\n👨‍💼 联系方式：\n1. **在线客服**：点击下方按钮进入在线客服实时沟通\n2. **客服热线**：拨打 **400-800-8888**（服务时间 9:00-21:00）\n\n您可以直接在线沟通，也可以打电话联系，客服会尽快为您处理问题的~"
            ));
        } catch (Exception e) {
            log.error("转人工客服失败", e);
            return JSONUtil.toJsonStr(Map.of(
                    "success", false,
                    "error", "转接失败：" + e.getMessage()
            ));
        }
    }
    
    /**
     * 获取当前用户 ID
     * 
     * ThreadLocal 说明：
     * - 类似前端的全局变量 window.userId
     * - 但 ThreadLocal 是线程隔离的，每个请求线程有自己的值
     * - SmartCustomerService 在请求开始时设置，工具执行时读取
     */
    private Long getCurrentUserId() {
        // 尝试从 ThreadLocal 获取
        Long userId = UserContext.getUserId();
        if (userId != null) {
            return userId;
        }
        return null;
    }

    /** 查询订单物流跟踪信息 */
    @Tool("查询订单物流跟踪信息。用户想知道订单到哪了、物流进度时调用。需要提供订单号")
    public String queryOrderTracking(
            @P("订单号") String orderNo
    ) {
        return JSONUtil.toJsonStr(Map.of(
                "success", true,
                "orderNo", orderNo,
                "message", "正在为您查询订单 " + orderNo + " 的物流信息...",
                "action", "track_order"  // 前端收到后会展示物流跟踪组件
        ));
    }

    /**
     * 创建客服工单
     * 
     * 类比：就像前端提交一个表单，包含分类、主题、描述等字段
     */
    @Tool("创建客服工单，用于处理复杂问题或投诉。当用户需要正式记录问题或投诉时调用")
    public String createTicket(
            @P("用户 ID") Long userId,
            @P("工单分类：technical(技术)/billing(账单)/product(商品)/complaint(投诉)/other(其他)") String category,
            @P("工单主题，简要描述问题") String subject,
            @P("问题详细描述") String description
    ) {
        log.info("[工具调用] createTicket: userId={}, category={}, subject={}", userId, category, subject);
        SseEmitterContext.sendThinking("📝 正在为您创建工单...");
        try {
            String ticketNo = ticketService.createTicket(userId, category, subject, description);
            SseEmitterContext.sendThinking("✅ 工单创建成功，编号：" + ticketNo);
            return JSONUtil.toJsonStr(Map.of(
                    "success", true,
                    "ticketNo", ticketNo,
                    "message", "工单已创建成功，客服将在 24 小时内联系您"
            ));
        } catch (Exception e) {
            log.error("创建工单失败", e);
            return JSONUtil.toJsonStr(Map.of(
                    "success", false,
                    "error", "创建工单失败：" + e.getMessage()
            ));
        }
    }

    /**
     * 根据城市估算配送时间
     * 
     * 类比：就像一个 Map 查找表，根据城市返回对应的配送时间
     */
    @Tool("根据城市估算配送时间。用户问'送到 XX 要多久'时调用")
    public String estimateDelivery(
            @P("用户所在城市名称，如'北京'、'上海'、'广州'") String city
    ) {
        // 配送时间映射表（类似前端的常量配置）
        Map<String, String> deliveryEstimate = new HashMap<>();
        deliveryEstimate.put("北京", "同城配送1-2天");
        deliveryEstimate.put("上海", "同城配送1-2天");
        deliveryEstimate.put("广州", "同城配送1-2天");
        deliveryEstimate.put("深圳", "同城配送1-2天");
        deliveryEstimate.put("杭州", "省内配送2-3天");
        deliveryEstimate.put("成都", "跨省配送3-5天");
        deliveryEstimate.put("武汉", "跨省配送3-5天");
        deliveryEstimate.put("西安", "跨省配送3-5天");
        deliveryEstimate.put("哈尔滨", "偏远地区5-7天");
        deliveryEstimate.put("乌鲁木齐", "偏远地区5-7天");

        // 查找城市，找不到就返回默认值
        String estimate = deliveryEstimate.getOrDefault(city,
                "跨省配送3-5天（具体以物流公司实际配送时间为准）");

        return JSONUtil.toJsonStr(Map.of(
                "success", true,
                "city", city,
                "estimate", estimate
        ));
    }
}