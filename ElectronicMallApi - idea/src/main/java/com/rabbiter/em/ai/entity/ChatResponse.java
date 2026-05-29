package com.rabbiter.em.ai.entity;

import com.rabbiter.em.ai.core.AiIntentResult;
import com.rabbiter.em.ai.service.AiHelpService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 聊天响应实体类
 */
public class ChatResponse {
    /**
     * 是否成功
     */
    private Boolean success;
    
    /**
     * 消息
     */
    private String message;
    
    /**
     * AI 回复内容
     */
    private String content;
    
    /**
     * 执行的操作类型：search（搜索）、addCart（添加购物车）、queryOrder（查询订单）、navigate（导航）
     */
    private String action;
    
    /**
     * 操作参数
     */
    private Map<String, Object> actionData;
    
    /**
     * 推荐的商品列表
     */
    private List<Object> recommendations;
    
    /**
     * 搜索结果
     */
    private List<Object> searchResults;
    
    /**
     * FAQ 信息
     */
    private AiHelpService.FAQ faq;
    
    /**
     * 意图识别上下文
     */
    private AiIntentResult context;
    
    /**
     * 快捷指令列表
     */
    private List<String> quickCommands;
    
    // Getter and Setter methods
    public Boolean getSuccess() {
        return success;
    }
    
    public void setSuccess(Boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    public Map<String, Object> getActionData() {
        return actionData;
    }
    
    public void setActionData(Map<String, Object> actionData) {
        this.actionData = actionData;
    }
    
    public List<Object> getRecommendations() {
        return recommendations;
    }
    
    public void setRecommendations(List<Object> recommendations) {
        this.recommendations = recommendations;
    }
    
    public List<Object> getSearchResults() {
        return searchResults;
    }
    
    public void setSearchResults(List<Object> searchResults) {
        this.searchResults = searchResults;
    }
    
    public AiHelpService.FAQ getFaq() {
        return faq;
    }
    
    public void setFaq(AiHelpService.FAQ faq) {
        this.faq = faq;
    }
    
    public AiIntentResult getContext() {
        return context;
    }
    
    public void setContext(AiIntentResult context) {
        this.context = context;
    }
    
    public List<String> getQuickCommands() {
        return quickCommands;
    }
    
    public void setQuickCommands(List<String> quickCommands) {
        this.quickCommands = quickCommands;
    }
    
    /**
     * 设置操作数据（简化版）
     */
    public void setActionData(String key, Object value) {
        if (this.actionData == null) {
            this.actionData = new HashMap<>();
        }
        this.actionData.put(key, value);
    }
    
    /**
     * 创建成功的响应
     */
    public static ChatResponse success(String message) {
        ChatResponse response = new ChatResponse();
        response.setSuccess(true);
        response.setMessage(message);
        response.setContent(message); // 同时设置 content 字段，确保流式输出能正确读取
        return response;
    }
    
    /**
     * 创建失败的响应
     */
    public static ChatResponse error(String message) {
        ChatResponse response = new ChatResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
    
    /**
     * Action 内部类
     */
    public static class Action {
        private String type;
        private Object params;
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public Object getParams() {
            return params;
        }
        
        public void setParams(Object params) {
            this.params = params;
        }
    }
}
