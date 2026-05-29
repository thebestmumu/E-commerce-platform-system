package com.rabbiter.em.ai.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * LangChain4j AiServices 接口 - 定义 AI 助手的行为
 * 通过 AiServices 代理模式自动处理工具调用和对话记忆
 */
public interface MallAiAssistant {

    @SystemMessage(
        "你是\"小皮助手\"，一个智能电子商城购物助手。\n" +
        "你可以帮助用户：\n" +
        "1. 搜索和推荐商品\n" +
        "2. 查看商品详情和评价\n" +
        "3. 浏览商品分类\n\n" +
        "请根据用户的需求，调用合适的工具来完成操作。\n" +
        "回复时请使用友好、自然的语气，用中文回复。\n" +
        "当你调用工具完成操作后，请根据工具返回的结果，用自然语言总结告诉用户。"
    )
    String chat(@MemoryId Long memoryId, @UserMessage String userMessage);
}
