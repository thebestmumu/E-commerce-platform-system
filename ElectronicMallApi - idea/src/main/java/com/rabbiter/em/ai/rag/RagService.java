package com.rabbiter.em.ai.rag;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
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
 * RAG (Retrieval-Augmented Generation) 问答服务
 * 
 * ===== 什么是 RAG？ =====
 * RAG = 检索 + 生成
 * 1. 检索：从知识库中查找与用户问题相关的文档
 * 2. 生成：把找到的文档作为上下文，让 AI 基于这些内容生成回答
 * 
 * ===== 为什么需要 RAG？ =====
 * - AI 本身不知道你的商城退货政策、发货规则等具体信息
 * - 通过 RAG，把知识库内容"喂"给 AI，AI 就能基于真实规则回答
 * - 避免 AI "幻觉"（胡说八道），提高回答准确性
 * 
 * ===== 前端类比 =====
 * 就像前端组件需要数据才能渲染：
 * 1. 先从 API 获取数据（检索知识库）
 * 2. 把数据传给组件模板（构建上下文）
 * 3. 组件基于数据渲染 UI（AI 生成回答）
 */
@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    /** 知识库服务 - 负责向量检索 */
    @Resource
    private KnowledgeBaseService knowledgeBaseService;

    /** AI 语言模型（DeepSeek） - 负责生成回答 */
    @Resource
    private ChatLanguageModel chatModel;

    /** 最多返回 3 个最相关的文档片段 */
    private static final int MAX_RAG_RESULTS = 3;

    /**
     * 基于知识库回答用户问题（RAG 核心方法）
     * 
     * 工作流程：
     * 1. 用户问："怎么退货？"
     * 2. 从知识库检索相关文档 → 找到"七天无理由退货"、"退货流程"等
     * 3. 把文档内容拼接成上下文字符串
     * 4. 把上下文 + 用户问题一起发给 AI
     * 5. AI 基于知识库内容生成回答
     * 
     * @param userQuery 用户的问题，如"怎么退货？"
     * @return AI 基于知识库生成的回答
     */
    public String answer(String userQuery) {
        try {
            // 步骤1：从知识库检索相关文档（向量相似度搜索）
            // 类比：就像前端搜索功能，但这里是用向量相似度而不是关键词匹配
            List<KnowledgeBaseService.KnowledgeDoc> relevantDocs = knowledgeBaseService.search(userQuery, MAX_RAG_RESULTS);

            // 如果没找到相关文档，直接让 AI 自己回答（没有知识库辅助）
            if (relevantDocs.isEmpty()) {
                log.info("RAG 未找到相关文档，直接使用 LLM 回答");
                return chatModel.generate(userQuery);
            }

            // 步骤2：把找到的文档拼接成上下文字符串
            // 结果类似："【七天无理由退货】自签收之日起7天内...\n【退货流程】1. 登录账号..."
            String context = buildContext(relevantDocs);
            log.info("RAG 找到 {} 个相关文档，构建上下文完成", relevantDocs.size());

            // 步骤3：构建消息列表
            List<ChatMessage> messages = new ArrayList<>();
            
            // SystemMessage - 系统指令 + 知识库上下文
            // 类比：就像给 AI 一份"参考资料"，让它基于这份资料回答问题
            messages.add(new SystemMessage(
                    "你是\"小皮助手\"，一个专业的电商智能客服。\n" +
                    "请基于以下知识库内容回答用户问题。如果知识库内容不足以回答问题，" +
                    "请诚实告知用户你不知道，并建议转接人工客服。\n" +
                    "回答要简洁清晰，使用友好的语气。\n\n" +
                    "=== 知识库内容 ===\n" + context  // ← 这里注入了检索到的知识库内容
            ));
            
            // UserMessage - 用户的问题
            messages.add(new UserMessage(userQuery));

            // 步骤4：调用 AI 生成回答
            ChatResponse response = chatModel.chat(ChatRequest.builder()
                    .messages(messages)
                    .build());

            String answer = response.aiMessage().text();
            log.info("RAG 回答生成完成，长度：{} 字符", answer.length());
            return answer;

        } catch (Exception e) {
            log.error("RAG 回答生成失败", e);
            return "抱歉，我暂时无法回答您的问题，请稍后再试或联系人工客服。";
        }
    }

    /**
     * 按分类查询知识库
     * 
     * @param category 分类名称，如 "return_policy"、"shipping"
     * @param userQuery 用户问题
     * @return AI 回答
     */
    public String answerWithCategory(String category, String userQuery) {
        // 先在指定分类内搜索
        List<KnowledgeBaseService.KnowledgeDoc> docs = knowledgeBaseService.searchByCategory(category, userQuery, MAX_RAG_RESULTS);

        // 如果没搜到，获取该分类所有文档
        if (docs.isEmpty()) {
            docs = knowledgeBaseService.getAllDocsByCategory(category);
        }

        // 如果该分类也没有文档，退回到全局搜索
        if (docs.isEmpty()) {
            return answer(userQuery);
        }

        String context = buildContext(docs);

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new SystemMessage(
                "你是\"小皮助手\"，一个专业的电商智能客服。\n" +
                "用户咨询的问题属于「" + category + "」类别。\n" +
                "请基于以下知识库内容回答。\n\n" +
                "=== 知识库内容 ===\n" + context
        ));
        messages.add(new UserMessage(userQuery));

        ChatResponse response = chatModel.chat(ChatRequest.builder()
                .messages(messages)
                .build());

        return response.aiMessage().text();
    }

    /**
     * 构建 RAG 上下文字符串（供其他模块调用）
     */
    public String buildRagContext(String userQuery) {
        List<KnowledgeBaseService.KnowledgeDoc> relevantDocs = knowledgeBaseService.search(userQuery, MAX_RAG_RESULTS);
        if (relevantDocs.isEmpty()) {
            return "";
        }
        return buildContext(relevantDocs);
    }

    /**
     * 把文档列表拼接成上下文字符串
     * 
     * 输入：[
     *   {title: "七天无理由退货", content: "自签收之日起7天内..."},
     *   {title: "退货流程", content: "1. 登录账号..."}
     * ]
     * 
     * 输出：
     * "【七天无理由退货】自签收之日起7天内...\n【退货流程】1. 登录账号..."
     */
    private String buildContext(List<KnowledgeBaseService.KnowledgeDoc> docs) {
        return docs.stream()
                .map(doc -> "【" + doc.getTitle() + "】" + doc.getContent())  // 拼接标题和内容
                .collect(Collectors.joining("\n"));  // 用换行符连接多个文档
    }
}