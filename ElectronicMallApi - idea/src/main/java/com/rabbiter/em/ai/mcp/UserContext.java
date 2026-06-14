package com.rabbiter.em.ai.mcp;

/**
 * 用户上下文（ThreadLocal）
 * 用于在 AI 工具调用链中传递用户 ID
 */
public class UserContext {
    
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    
    /**
     * 设置当前用户 ID
     * @param userId 用户 ID
     */
    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }
    
    /**
     * 获取当前用户 ID
     * @return 用户 ID，如果未设置则返回 null
     */
    public static Long getUserId() {
        return USER_ID.get();
    }
    
    /**
     * 清除当前用户 ID
     * 必须在请求结束后调用，防止内存泄漏
     */
    public static void clear() {
        USER_ID.remove();
    }
}
