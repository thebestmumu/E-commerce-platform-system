package com.rabbiter.em.ai.core;

import java.util.List;
import java.util.Map;

/**
 * AI 意图识别结果
 */
public class AiIntentResult {
    
    /**
     * 识别的意图
     */
    private AiIntent intent;
    
    /**
     * 置信度（0-1）
     */
    private double confidence;
    
    /**
     * 提取的参数
     */
    private Map<String, Object> parameters;
    
    /**
     * 原始问题
     */
    private String originalQuery;
    
    /**
     * 推荐的商品列表（如果有）
     */
    private List<Object> recommendations;
    
    /**
     * 是否需要进一步澄清
     */
    private boolean needClarification;
    
    /**
     * 澄清问题
     */
    private String clarificationQuestion;
    
    /**
     * 意图分类
     */
    private String category;
    
    /**
     * 响应模板
     */
    private String responseTemplate;
    
    // Getters and Setters
    public AiIntent getIntent() {
        return intent;
    }
    
    public void setIntent(AiIntent intent) {
        this.intent = intent;
    }
    
    public double getConfidence() {
        return confidence;
    }
    
    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }
    
    public Map<String, Object> getParameters() {
        return parameters;
    }
    
    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }
    
    public String getOriginalQuery() {
        return originalQuery;
    }
    
    public void setOriginalQuery(String originalQuery) {
        this.originalQuery = originalQuery;
    }
    
    public List<Object> getRecommendations() {
        return recommendations;
    }
    
    public void setRecommendations(List<Object> recommendations) {
        this.recommendations = recommendations;
    }
    
    public boolean isNeedClarification() {
        return needClarification;
    }
    
    public void setNeedClarification(boolean needClarification) {
        this.needClarification = needClarification;
    }
    
    public String getClarificationQuestion() {
        return clarificationQuestion;
    }
    
    public void setClarificationQuestion(String clarificationQuestion) {
        this.clarificationQuestion = clarificationQuestion;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getResponseTemplate() {
        return responseTemplate;
    }
    
    public void setResponseTemplate(String responseTemplate) {
        this.responseTemplate = responseTemplate;
    }
}
