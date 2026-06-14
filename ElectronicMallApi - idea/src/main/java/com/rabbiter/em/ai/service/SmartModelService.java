package com.rabbiter.em.ai.service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.regex.Pattern;

/**
 * 智能模型降级服务 V2.0
 * 基于问题复杂度动态选择模型
 * 
 * 策略：
 * - 简单问题：使用文心一言（快速、低成本）
 * - 复杂问题：使用 DeepSeek（强大、高精度）
 */
@Slf4j
@Service
public class SmartModelService {

    @Autowired
    @Qualifier("chatModel")
    private ChatLanguageModel deepSeekModel;  // 主模型（处理复杂问题）

    @Autowired(required = false)
    @Qualifier("qianfanChatModel")
    private ChatLanguageModel qianfanModel;   // 备用模型（处理简单问题）

    /**
     * 简单问题判断规则
     * 匹配这些模式的认为是简单问题
     */
    private static final Pattern[] SIMPLE_PATTERNS = {
        // 打招呼
        Pattern.compile("^(你好 | 您好 | hello|hi|嗨|早上好 | 下午好 | 晚上好)"),
        
        // 简单问候
        Pattern.compile("(在吗 | 有人吗 | 客服在吗 | 人工在吗)"),
        
        // 感谢
        Pattern.compile("(谢谢 | 感谢 | 辛苦了 | 麻烦了)"),
        
        // 告别
        Pattern.compile("(再见 | 拜拜 | 下次见 | 先这样 | 那我先去)"),
        
        // 简单确认
        Pattern.compile("^(好的 | 好的谢谢 | 明白了 | 知道了 | 好的好的 | 嗯嗯 | 哦哦)"),
        
        // 简单否定
        Pattern.compile("^(不用了 | 不需要了 | 算了 | 没事了 | 没事)"),
        
        // 简单询问
        Pattern.compile("^([在嘛 | 请问 | 问一下 | 问问])"),
        
        // 表情符号
        Pattern.compile("^[👍👌😊😄😁🙏]+$"),
    };

    /**
     * 复杂问题关键词（需要 DeepSeek 处理）
     */
    private static final String[] COMPLEX_KEYWORDS = {
        "为什么", "怎么", "如何", "怎样", "怎么办",
        "详细", "解释", "说明", "分析",
        "比较", "对比", "区别", "差异",
        "推荐", "建议", "方案", "策略",
        "计算", "公式", "算法",
        "代码", "编程", "技术",
        "投诉", "举报", "严重", "紧急"
    };

    @PostConstruct
    public void init() {
        log.info("========================================");
        log.info("智能模型选择服务已初始化");
        log.info("  - 复杂问题模型：DeepSeek ✅");
        log.info("  - 简单问题模型：{}", qianfanModel != null ? "文心一言 ✅" : "未配置，将统一使用 DeepSeek");
        log.info("  - 简单问题规则数：{}", SIMPLE_PATTERNS.length);
        log.info("  - 复杂问题关键词数：{}", COMPLEX_KEYWORDS.length);
        log.info("========================================");
        
        if (qianfanModel == null) {
            log.warn("⚠️  文心一言模型未配置");
            log.warn("⚠️  所有问题将使用 DeepSeek 处理");
            log.warn("💡 如需启用文心一言，请在 application.yml 中配置：");
            log.warn("   qianfan:");
            log.warn("     api-key: your_api_key");
            log.warn("     secret-key: your_secret_key");
        }
    }

    /**
     * 根据问题复杂度选择模型
     * @param question 用户问题
     * @return 应该使用的模型
     */
    public ChatLanguageModel selectModel(String question) {
        if (question == null || question.trim().isEmpty()) {
            return getDefaultModel();
        }

        String trimmedQuestion = question.trim();

        // 1. 检查是否是简单问题
        if (isSimpleQuestion(trimmedQuestion)) {
            log.debug("判断为简单问题，使用轻量模型");
            ChatLanguageModel simpleModel = getSimpleModel();
            if (simpleModel != null) {
                return simpleModel;
            }
        }

        // 2. 默认使用强大模型
        log.debug("判断为复杂问题，使用强大模型");
        return deepSeekModel;
    }

    /**
     * 判断是否是简单问题
     */
    private boolean isSimpleQuestion(String question) {
        // 检查长度（短问题可能是简单问题）
        if (question.length() <= 10) {
            // 检查是否匹配简单模式
            for (Pattern pattern : SIMPLE_PATTERNS) {
                if (pattern.matcher(question).find()) {
                    return true;
                }
            }
        }

        // 检查是否包含复杂关键词
        for (String keyword : COMPLEX_KEYWORDS) {
            if (question.contains(keyword)) {
                return false;  // 包含复杂关键词，不是简单问题
            }
        }

        // 检查句子复杂度（问号数量、句子长度等）
        int questionMarkCount = question.length() - question.replace("?", "").length();
        questionMarkCount += question.length() - question.replace("？", "").length();
        
        if (questionMarkCount > 1) {
            return false;  // 多个问题，可能是复杂问题
        }

        // 默认判断为简单问题
        return question.length() <= 20;
    }

    /**
     * 获取简单问题模型（文心一言）
     */
    private ChatLanguageModel getSimpleModel() {
        if (qianfanModel == null) {
            log.warn("文心一言模型未配置，回退到 DeepSeek");
            return deepSeekModel;
        }
        return qianfanModel;
    }

    /**
     * 获取默认模型（复杂问题模型）
     */
    private ChatLanguageModel getDefaultModel() {
        return deepSeekModel;
    }

    /**
     * 获取当前模型配置状态
     */
    public String getStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("智能模型选择状态：\n");
        sb.append("  - 复杂问题模型：DeepSeek ✅\n");
        sb.append("  - 简单问题模型：").append(qianfanModel != null ? "文心一言 ✅" : "未配置 ❌").append("\n");
        sb.append("  - 简单问题规则：").append(SIMPLE_PATTERNS.length).append(" 条\n");
        sb.append("  - 复杂问题关键词：").append(COMPLEX_KEYWORDS.length).append(" 个\n");
        return sb.toString();
    }
}
