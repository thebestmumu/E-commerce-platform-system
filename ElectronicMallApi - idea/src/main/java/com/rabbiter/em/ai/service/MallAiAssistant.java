package com.rabbiter.em.ai.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * AI Agent 声明式接口 - 整个智能客服的核心！
 * 
 * ===== 前端类比 =====
 * 这个接口就像是一个 API 调用函数的声明，但没有实现体。
 * 类似于 TypeScript 的 interface 声明，但 LangChain4j 会在启动时自动生成实现类（代理对象）。
 * 
 * ===== 工作原理 =====
 * 1. Spring 启动时，AiServicesConfig 会调用 AiServices.builder(MallAiAssistant.class)
 * 2. LangChain4j 会创建一个"代理对象"（类似前端的 Proxy 或拦截器）
 * 3. 当你调用 mallAiAssistant.chat() 时，代理对象内部会自动执行：
 *    a. 加载对话历史（从 Redis）
 *    b. 构建消息列表（SystemMessage + 历史 + 用户消息）
 *    c. 注册所有 @Tool 方法（让 AI 知道有哪些工具可用）
 *    d. 调用 DeepSeek API
 *    e. 如果 AI 决定调用工具，执行工具并继续循环
 *    f. 直到 AI 返回最终答案
 * 
 * ===== 为什么只有接口没有实现？ =====
 * 这就是 LangChain4j 的魔法！它通过注解声明式地定义 AI 行为，
 * 框架自动生成实现代码，你不需要手写 ReAct 循环的逻辑。
 * 类似于 Vue 的 <script setup>，你只需要声明状态和函数，框架处理响应式。
 */
public interface MallAiAssistant {

    /**
     * SystemMessage - 系统提示词（AI 的"人设"和"行为准则"）
     * 
     * 类比前端：
     * - 就像组件的 props 默认值或全局配置
     * - 每次调用 AI 时都会带上这些指令
     * - AI 必须遵守这些规则
     * 
     * {{currentTime}} 是变量占位符，会在运行时被替换为实际时间
     */
    @SystemMessage({
        "你是\"小皮助手\"，一个专业、友好的电商客服助手。",
        "",
        "行为准则（必须遵守）：",
        "1. 始终用中文回复，语气自然友好，像真人客服",
        "2. 用自然段落描述，禁止使用 markdown 表格（如 | --- | :--- |）",
        "3. 禁止滥用 emoji，每句话最多在句尾用 1 个",
        "4. 工具返回数据后，用自然语言总结，不要直接展示 JSON",
        "5. 涉及订单、购物车、支付等操作时，先与用户确认再执行",
        "6. 如果用户需求不明确，先问清楚再调用工具",
        "7. 不知道答案时坦诚告知，可建议转人工客服",
        "",
        "【转人工客服规则 - 非常重要】",
        "当用户明确要求转人工客服（如'转人工'、'找人工客服'、'人工服务'等）时：",
        "- 必须调用 transferToHuman 工具，不要自己生成回复",
        "- 调用工具时传入转接原因（用户的问题描述）",
        "- 工具会自动创建工单并返回工单编号",
        "- 收到工具返回后，用自然语言告知用户工单已创建",
        "",
        "当前时间：{{currentTime}}"
    })
    
    /**
     * AI 对话方法 - 调用这个方法就会触发 AI 推理
     * 
     * @MemoryId - 对话记忆 ID（通常是 userId）
     *   类比：就像 localStorage 的 key，LangChain4j 会根据这个 ID 自动加载/保存对话历史
     *   例如：memoryId=123 → 从 Redis 读取 key="ai:memory:123" 的历史消息
     * 
     * @UserMessage - 用户发送的消息
     *   类比：就像 input 输入框的值，是用户实际输入的内容
     * 
     * @V("currentTime") - 变量注入
     *   类比：就像 Vue 的 v-bind 绑定变量到模板
     *   这里把 currentTime 参数注入到 @SystemMessage 中的 {{currentTime}} 占位符
     */
    String chat(
            @MemoryId Long memoryId,
            @UserMessage String userMessage,
            @V("currentTime") String currentTime
    );
}