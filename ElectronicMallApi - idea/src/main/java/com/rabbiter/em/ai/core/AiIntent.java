package com.rabbiter.em.ai.core;

/**
 * AI 意图枚举，定义系统能识别的所有意图类型
 */
public enum AiIntent {
    
    // ===== 商品相关意图 =====
    /** 搜索商品 */
    SEARCH_GOODS("search_goods", "商品搜索"),
    /** 查看商品详情 */
    VIEW_GOOD_DETAIL("view_good_detail", "查看商品详情"),
    /** 商品推荐 */
    RECOMMEND_GOODS("recommend_goods", "商品推荐"),
    /** 具体品类推荐 */
    SPECIFIC_RECOMMEND("specific_recommend", "具体品类推荐"),
    /** 商品比价 */
    COMPARE_GOODS("compare_goods", "商品比价"),
    /** 商品库存查询 */
    CHECK_STOCK("check_stock", "库存查询"),
    
    // ===== 购物车相关意图 =====
    /** 添加到购物车 */
    ADD_TO_CART("add_to_cart", "加入购物车"),
    /** 查看购物车 */
    VIEW_CART("view_cart", "查看购物车"),
    /** 更新购物车商品数量 */
    UPDATE_CART("update_cart", "更新购物车"),
    /** 清空购物车 */
    CLEAR_CART("clear_cart", "清空购物车"),
    
    // ===== 订单相关意图 =====
    /** 创建订单 */
    CREATE_ORDER("create_order", "创建订单"),
    /** 快速下单 */
    QUICK_ORDER("quick_order", "快速下单"),
    /** 查询订单列表 */
    QUERY_ORDERS("query_orders", "查询订单"),
    /** 查询订单详情 */
    QUERY_ORDER_DETAIL("query_order_detail", "订单详情"),
    /** 取消订单 */
    CANCEL_ORDER("cancel_order", "取消订单"),
    /** 确认收货 */
    CONFIRM_RECEIVE("confirm_receive", "确认收货"),
    /** 订单统计 */
    ORDER_STATISTICS("order_statistics", "订单统计"),
    /** 订单状态跟踪 */
    TRACK_ORDER("track_order", "订单状态跟踪"),
    
    // ===== 支付相关意图 =====
    /** 支付订单 */
    PAY_ORDER("pay_order", "支付订单"),
    /** 查询支付状态 */
    CHECK_PAYMENT("check_payment", "支付状态查询"),
    /** 退款申请 */
    APPLY_REFUND("apply_refund", "退款申请"),
    
    // ===== 用户相关意图 =====
    /** 用户信息查询 */
    QUERY_USER_INFO("query_user_info", "用户信息"),
    /** 修改用户信息 */
    UPDATE_USER_INFO("update_user_info", "修改信息"),
    /** 查询收货地址 */
    QUERY_ADDRESS("query_address", "收货地址"),
    /** 添加收货地址 */
    ADD_ADDRESS("add_address", "添加地址"),
    
    // ===== 帮助与客服意图 =====
    /** 常见问题咨询 */
    FAQ("faq", "常见问题"),
    /** 售后服务 */
    AFTER_SALES("after_sales", "售后服务"),
    /** 投诉建议 */
    COMPLAINT("complaint", "投诉建议"),
    /** 操作指导 */
    TUTORIAL("tutorial", "操作指导"),
    
    // ===== 通用意图 =====
    /** 闲聊 */
    CHAT("chat", "闲聊"),
    /** 页面导航 */
    NAVIGATE("navigate", "页面导航"),
    /** 未知意图 */
    UNKNOWN("unknown", "未知意图"),
    /** 需要人工客服 */
    HUMAN_SERVICE("human_service", "人工客服"),
    
    // ===== AI分析意图 =====
    /** 评论舆情分析 */
    ANALYZE_SENTIMENT("analyze_sentiment", "评论舆情分析"),
    /** 销售数据报告 */
    ANALYZE_SALES("analyze_sales", "销售数据报告"),
    /** 订单历史分析 */
    ANALYZE_ORDERS("analyze_orders", "订单历史分析");
    
    private final String code;
    private final String description;
    
    AiIntent(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据 code 获取意图
     */
    public static AiIntent fromCode(String code) {
        for (AiIntent intent : values()) {
            if (intent.code.equals(code)) {
                return intent;
            }
        }
        return UNKNOWN;
    }
}
