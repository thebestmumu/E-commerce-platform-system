package com.rabbiter.em.ai.entity;

import java.util.List;

/**
 * AI 聊天请求实体类
 */
public class ChatRequest {
    /**
     * 用户消息
     */
    private String message;
    
    /**
     * 用户ID（可选）
     */
    private Long userId;
    
    /**
     * 对话历史（可选）
     */
    private List<Message> history;
    
    /**
     * 当前模式：chat（对话）或 help（智能帮助）
     */
    private String mode;
    
    /**
     * 消息实体类
     */
    public static class Message {
        /**
         * 角色：user 或 assistant
         */
        private String role;
        
        /**
         * 消息内容
         */
        private String content;
        
        // Getter and Setter methods
        public String getRole() {
            return role;
        }
        
        public void setRole(String role) {
            this.role = role;
        }
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
    }
    
    // Getter and Setter methods
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public List<Message> getHistory() {
        return history;
    }
    
    public void setHistory(List<Message> history) {
        this.history = history;
    }
    
    public String getMode() {
        return mode;
    }
    
    public void setMode(String mode) {
        this.mode = mode;
    }
}
