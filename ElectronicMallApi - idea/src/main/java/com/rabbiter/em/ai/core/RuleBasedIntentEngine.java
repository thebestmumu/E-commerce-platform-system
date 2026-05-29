package com.rabbiter.em.ai.core;

import cn.hutool.core.util.StrUtil;
import com.rabbiter.em.ai.entity.ChatRequest;
import com.rabbiter.em.ai.service.AiCategoryRecognitionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于规则的意图识别引擎
 * 使用AI进行品类识别，替代硬编码关键词匹配
 */
@Component
public class RuleBasedIntentEngine implements AiIntentEngine {
    
    private static final Logger log = LoggerFactory.getLogger(RuleBasedIntentEngine.class);
    
    @Autowired
    private AiCategoryRecognitionService categoryRecognitionService;
    
    @Autowired
    private AiContextManager contextManager;
    
    /**
     * 意图关键词映射表（使用 LinkedHashMap 保证匹配顺序）
     */
    private static final Map<AiIntent, String[]> INTENT_KEYWORDS = new java.util.LinkedHashMap<>();
    
    static {
        // 商品相关
        INTENT_KEYWORDS.put(AiIntent.SEARCH_GOODS, new String[]{"找.*商品", "搜索.*商品", "有没有.*商品", "想买.*商品", "看看.*商品", "搜索.*", "找.*"});
        INTENT_KEYWORDS.put(AiIntent.RECOMMEND_GOODS, new String[]{"推荐.*商品", "有什么好.*", "热门.*商品", "畅销.*商品", "爆款", "帮我推荐"});
        // 具体品类推荐：匹配"推荐" + 任意内容（不硬编码具体品类）
        INTENT_KEYWORDS.put(AiIntent.SPECIFIC_RECOMMEND, new String[]{"推荐.*", "我想买.*", "给我.*", "来点.*", "看看.*"});
        INTENT_KEYWORDS.put(AiIntent.VIEW_GOOD_DETAIL, new String[]{"商品详情", "查看详情", "这个商品.*", "看看.*详情"});
        INTENT_KEYWORDS.put(AiIntent.CHECK_STOCK, new String[]{"有货.*", "库存.*", "还有.*吗", "现货.*"});
        INTENT_KEYWORDS.put(AiIntent.COMPARE_GOODS, new String[]{"对比.*", "哪个更好.*", "区别.*", "比较.*"});
        
        // 购物车相关
        INTENT_KEYWORDS.put(AiIntent.ADD_TO_CART, new String[]{"加入购物车", "添加到购物车", "放入购物车", "加购.*"});
        INTENT_KEYWORDS.put(AiIntent.VIEW_CART, new String[]{"查看购物车", "我的购物车", "购物车.*商品", "购物车.*"});
        INTENT_KEYWORDS.put(AiIntent.UPDATE_CART, new String[]{"修改.*数量", "更新.*购物车", "删除.*购物车"});
        INTENT_KEYWORDS.put(AiIntent.CLEAR_CART, new String[]{"清空购物车", "删除.*购物车商品"});
        
        // AI分析（放在前面，优先匹配）
        INTENT_KEYWORDS.put(AiIntent.ANALYZE_ORDERS, new String[]{"分析订单", "订单分析", "分析.*订单", "订单.*分析", "消费分析", "购物报告", "订单报告", "帮我分析.*订单", "订单.*情况"});
        INTENT_KEYWORDS.put(AiIntent.ANALYZE_SENTIMENT, new String[]{"评论分析", "舆情分析", "情感分析", "评论统计", "评价分析", "分析评论"});
        INTENT_KEYWORDS.put(AiIntent.ANALYZE_SALES, new String[]{"销售报告", "销售分析", "销量统计", "销售数据", "销售趋势", "销量报告"});
        
        // 订单相关
        INTENT_KEYWORDS.put(AiIntent.TRACK_ORDER, new String[]{"订单追踪", "追踪订单", "订单跟踪", "跟踪订单", "查看物流", "物流信息", "发货地址", "配送进度"});
        INTENT_KEYWORDS.put(AiIntent.QUERY_ORDERS, new String[]{"我的订单", "订单列表", "查看订单", "历史订单", "买过.*"});
        INTENT_KEYWORDS.put(AiIntent.QUERY_ORDER_DETAIL, new String[]{"订单详情", "订单.*状态", "这个订单.*", "订单号.*"});
        INTENT_KEYWORDS.put(AiIntent.CANCEL_ORDER, new String[]{"取消订单", "退款.*订单", "不想要.*"});
        INTENT_KEYWORDS.put(AiIntent.CONFIRM_RECEIVE, new String[]{"确认收货", "收到货.*", "签收.*"});
        INTENT_KEYWORDS.put(AiIntent.ORDER_STATISTICS, new String[]{"订单统计", "消费统计", "花了.*钱"});
        
        // 支付相关
        INTENT_KEYWORDS.put(AiIntent.PAY_ORDER, new String[]{"去支付", "付款.*", "支付订单", "结账.*"});
        INTENT_KEYWORDS.put(AiIntent.CHECK_PAYMENT, new String[]{"支付状态", "付款成功.*", "支付成功.*"});
        INTENT_KEYWORDS.put(AiIntent.APPLY_REFUND, new String[]{"申请退款", "退款.*", "退货.*", "售后.*"});
        
        // 用户相关
        INTENT_KEYWORDS.put(AiIntent.QUERY_USER_INFO, new String[]{"我的信息", "个人资料", "账户信息", "用户信息"});
        INTENT_KEYWORDS.put(AiIntent.UPDATE_USER_INFO, new String[]{"修改.*信息", "更新.*资料", "更改.*"});
        INTENT_KEYWORDS.put(AiIntent.QUERY_ADDRESS, new String[]{"收货地址", "配送地址", "我的地址.*"});
        INTENT_KEYWORDS.put(AiIntent.ADD_ADDRESS, new String[]{"添加地址", "新增地址", "新建地址.*"});
        
        // 帮助与客服
        INTENT_KEYWORDS.put(AiIntent.FAQ, new String[]{"常见问题", "怎么.*", "如何.*", ".*怎么办", ".*方法"});
        INTENT_KEYWORDS.put(AiIntent.AFTER_SALES, new String[]{"售后服务", "售后.*", "保修.*", "质保.*"});
        INTENT_KEYWORDS.put(AiIntent.COMPLAINT, new String[]{"投诉.*", "举报.*", "客服.*", "人工.*"});
        INTENT_KEYWORDS.put(AiIntent.TUTORIAL, new String[]{"教程.*", "指南.*", "如何使用.*", ".*步骤"});
        
        // 通用
        INTENT_KEYWORDS.put(AiIntent.CHAT, new String[]{"你好", "在吗", "您好", "hello", "hi"});
    }
    
    /**
     * 参数提取正则表达式
     */
    private static final Map<String, Pattern> PARAM_PATTERNS = new HashMap<>();
    
    static {
        PARAM_PATTERNS.put("goodId", Pattern.compile("商品.*?(\\d+)"));
        PARAM_PATTERNS.put("orderNo", Pattern.compile("订单号.*?([A-Za-z0-9]+)"));
        PARAM_PATTERNS.put("count", Pattern.compile("(\\d+) 个"));
        PARAM_PATTERNS.put("keyword", Pattern.compile("(?:找 | 搜索 | 看看).*?(.+?)(?:商品 | 吗 | 呢|$)"));
    }
    
    @Override
    public AiIntentResult recognizeIntent(ChatRequest request) {
        String message = request.getMessage();
        
        if (StrUtil.isBlank(message)) {
            return createUnknownResult(message);
        }
        
        log.info("开始识别意图，消息：{}", message);
        
        // 0. 优先检查上下文相关的指代意图（如"第二个"、"第一个"、"加入购物车"等）
        AiIntentResult contextResult = recognizeContextualIntent(request);
        if (contextResult != null) {
            log.info("通过上下文识别意图：{} - {}", contextResult.getIntent().getCode(), contextResult.getIntent().getDescription());
            return contextResult;
        }
        
        // 1. 尝试匹配关键词
        for (Map.Entry<AiIntent, String[]> entry : INTENT_KEYWORDS.entrySet()) {
            AiIntent intent = entry.getKey();
            String[] keywords = entry.getValue();
            
            for (String keywordPattern : keywords) {
                if (message.matches(".*" + keywordPattern + ".*")) {
                    log.info("匹配到意图：{} - {}", intent.getCode(), intent.getDescription());
                    
                    // 提取参数
                    Map<String, Object> parameters = extractParameters(message);
                    
                    // 为 specificRecommend 添加 requirement 参数
                    if ("specific_recommend".equals(intent.getCode())) {
                        // 提取品类关键词，而不是整个消息
                        String keyword = extractCategoryKeyword(message);
                        parameters.put("requirement", keyword);
                        
                        // 提取价格范围
                        Double[] priceRange = extractPriceRange(message);
                        if (priceRange[0] != null) {
                            parameters.put("minPrice", priceRange[0]);
                        }
                        if (priceRange[1] != null) {
                            parameters.put("maxPrice", priceRange[1]);
                        }
                        
                        // 提取规格
                        String standard = extractStandard(message);
                        if (standard != null && !standard.isEmpty()) {
                            parameters.put("standard", standard);
                        }
                    }
                    
                    AiIntentResult result = new AiIntentResult();
                    result.setIntent(intent);
                    result.setConfidence(0.8);
                    result.setOriginalQuery(message);
                    result.setParameters(parameters);
                    result.setCategory(getCategory(intent));
                    result.setNeedClarification(false);
                    return result;
                }
            }
        }
        
        // 1.5 检查是否是疑问句（包含"什么、怎么、为什么、如何"等疑问词），识别为聊天意图
        // 防止简短疑问句被误判为搜索商品
        String trimmedMessage = message.trim();
        if (trimmedMessage.matches(".*(什么|怎么|为什么|如何|咋|怎样|是不是|能不能|会不会|有没有|可否|是否).*") ||
            trimmedMessage.matches(".*[吗呢啊呀哦吧哈嘿]")) {
            log.info("检测到疑问句，识别为聊天意图：{}", trimmedMessage);
            AiIntentResult result = new AiIntentResult();
            result.setIntent(AiIntent.CHAT);
            result.setConfidence(0.6);
            result.setOriginalQuery(message);
            result.setParameters(new HashMap<>());
            result.setCategory("general");
            result.setNeedClarification(false);
            return result;
        }
        
        // 2. 如果没有匹配到明确意图，检查是否是简短商品词
        // 对于简短的词汇（如"手机"、"电脑"、"衣服"等），直接识别为搜索意图
        if (trimmedMessage.length() <= 10 && !trimmedMessage.matches(".*[吗呢啊呀哦吧哈嘿].*")) {
            log.info("简短商品词，识别为搜索意图：{}", trimmedMessage);
            
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("keyword", trimmedMessage);
            
            AiIntentResult result = new AiIntentResult();
            result.setIntent(AiIntent.SEARCH_GOODS);
            result.setConfidence(0.7);
            result.setOriginalQuery(message);
            result.setParameters(parameters);
            result.setCategory("goods");
            result.setNeedClarification(false);
            return result;
        }
        
        // 3. 如果仍然没有匹配到，返回未知意图
        return createUnknownResult(message);
    }
    
    /**
     * 提取消息中的参数
     */
    private Map<String, Object> extractParameters(String message) {
        Map<String, Object> parameters = new HashMap<>();
        
        for (Map.Entry<String, Pattern> entry : PARAM_PATTERNS.entrySet()) {
            String paramName = entry.getKey();
            Pattern pattern = entry.getValue();
            
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                parameters.put(paramName, matcher.group(1));
            }
        }
        
        // 提取数量
        if (message.contains("个")) {
            Pattern countPattern = Pattern.compile("(\\d+)");
            Matcher matcher = countPattern.matcher(message);
            if (matcher.find()) {
                String countStr = matcher.group(1);
                try {
                    parameters.put("count", Integer.parseInt(countStr));
                } catch (NumberFormatException e) {
                    // 忽略
                }
            }
        }
        
        return parameters;
    }
    
    /**
     * 从消息中提取品类关键词
     * 使用AI识别，支持任意商品品类，不再硬编码
     */
    private String extractCategoryKeyword(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }
        
        // 使用AI识别品类
        String category = categoryRecognitionService.recognizeCategory(message);
        
        if (category != null && !category.isEmpty()) {
            log.info("AI识别品类：{}", category);
            return category;
        }
        
        return "";
    }
    
    /**
     * 从消息中提取价格范围
     * 例如："推荐 100 元以下的卫衣" -> [null, 100.0]
     *      "推荐 200 到 500 元的手机" -> [200.0, 500.0]
     *      "推荐 50 元以上的零食" -> [50.0, null]
     *      "推荐价格在 200 以下的卫衣" -> [null, 200.0]
     */
    private Double[] extractPriceRange(String message) {
        Double[] result = new Double[]{null, null};
        if (message == null || message.isEmpty()) {
            return result;
        }
        
        // 匹配 "价格在 XX 以下" 或 "价格 XX 以下" 或 "XX 元以下" 或 "XX 块以下"
        java.util.regex.Pattern belowPattern = java.util.regex.Pattern.compile("(?:价格[在]?)?(\\d+(?:\\.\\d+)?)\\s*(?:元|块)?\\s*以下");
        java.util.regex.Matcher belowMatcher = belowPattern.matcher(message);
        if (belowMatcher.find()) {
            result[1] = Double.parseDouble(belowMatcher.group(1));
            return result;
        }
        
        // 匹配 "价格在 XX 以上" 或 "价格 XX 以上" 或 "XX 元以上" 或 "XX 块以上"
        java.util.regex.Pattern abovePattern = java.util.regex.Pattern.compile("(?:价格[在]?)?(\\d+(?:\\.\\d+)?)\\s*(?:元|块)?\\s*以上");
        java.util.regex.Matcher aboveMatcher = abovePattern.matcher(message);
        if (aboveMatcher.find()) {
            result[0] = Double.parseDouble(aboveMatcher.group(1));
            return result;
        }
        
        // 匹配 "XX 元到 XX 元" 或 "XX 块到 XX 块" 或 "XX-XX 元"
        java.util.regex.Pattern rangePattern = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:元|块)?\\s*[到至-]\\s*(\\d+(?:\\.\\d+)?)\\s*(?:元|块)?");
        java.util.regex.Matcher rangeMatcher = rangePattern.matcher(message);
        if (rangeMatcher.find()) {
            result[0] = Double.parseDouble(rangeMatcher.group(1));
            result[1] = Double.parseDouble(rangeMatcher.group(2));
            return result;
        }
        
        return result;
    }
    
    /**
     * 从消息中提取规格关键词
     * 例如："推荐 XL 码的卫衣" -> "XL"
     *      "推荐 42 码的鞋子" -> "42码"
     *      "推荐大瓶的威士忌" -> "大瓶"
     */
    private String extractStandard(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }
        
        // 常见的规格关键词（扩展版）
        String[] standards = {
            // 衣服尺码
            "XS", "S", "M", "L", "XL", "XXL", "XXXL", "XXXXL",
            "小码", "中码", "大码", "加大码", "特大码",
            // 鞋子码数
            "34码", "35码", "36码", "37码", "38码", "39码", "40码", "41码", "42码", "43码", "44码", "45码", "46码",
            // 颜色
            "白色", "黑色", "红色", "蓝色", "绿色", "黄色", "灰色", "粉色", "紫色", "橙色",
            "棕色", "米色", "卡其色", "杏色", "藏青色", "军绿色", "酒红色", "天蓝色",
            "象牙白", "珍珠白", "香槟色", "玫瑰金", "银色", "金色", "铜色",
            // 款式/版型
            "宽松", "修身", "紧身", "直筒", "A字", "喇叭", "阔腿", "高腰", "低腰",
            "圆领", "V领", "方领", "立领", "翻领", "连帽", "无袖", "短袖", "长袖",
            "单排扣", "双排扣", "拉链", "套头", "开衫",
            // 材质
            "纯棉", "棉质", "涤纶", "聚酯纤维", "丝绸", "真丝", "雪纺", "蕾丝",
            "牛仔", "皮革", "PU皮", "真皮", "羊绒", "羊毛", "兔毛", "羽绒",
            "亚麻", "麻质", "针织", "毛线", "法兰绒", "珊瑚绒", "摇粒绒",
            "不锈钢", "陶瓷", "玻璃", "塑料", "木质", "竹制", "硅胶",
            // 季节
            "春季", "夏季", "秋季", "冬季", "春秋", "四季",
            // 性别/人群
            "男款", "女款", "中性", "情侣", "儿童", "婴儿", "老年", "青少年",
            // 瓶装规格
            "大瓶", "小瓶", "单瓶", "套装", "礼盒", "组合装", "家庭装", "旅行装",
            // 容量/重量
            "500ml", "1L", "2L", "5L", "10L",
            "100g", "200g", "500g", "1kg", "2kg", "5kg",
            // 版本/配置
            "标准版", "豪华版", "基础版", "高配", "低配", "旗舰版", "青春版", "专业版",
            // 风格
            "休闲", "运动", "商务", "正装", "时尚", "复古", "简约", "可爱", "甜美",
            "日系", "韩系", "欧美", "街头", "学院风", "民族风", "波西米亚",
            // 功能
            "防水", "防风", "保暖", "透气", "速干", "防晒", "抗菌", "防静电",
            "降噪", "无线", "蓝牙", "智能", "电动", "手动", "自动",
            // 其他
            "新款", "旧款", "经典款", "限量款", "联名款", "同款", "升级版"
        };
        
        // 遍历查找匹配的规格（优先匹配长词）
        java.util.List<String> sortedStandards = new java.util.ArrayList<>();
        for (String std : standards) {
            sortedStandards.add(std);
        }
        sortedStandards.sort((a, b) -> b.length() - a.length());
        
        for (String standard : sortedStandards) {
            if (message.contains(standard)) {
                return standard;
            }
        }
        
        // 尝试提取"码"字前面的数字
        java.util.regex.Pattern codePattern = java.util.regex.Pattern.compile("(\\d+)\\s*码");
        java.util.regex.Matcher codeMatcher = codePattern.matcher(message);
        if (codeMatcher.find()) {
            return codeMatcher.group(1) + "码";
        }
        
        return "";
    }
    
    /**
     * 基于上下文识别意图
     * 处理"第二个"、"第一个"、"加入购物车"等需要上下文理解的场景
     */
    private AiIntentResult recognizeContextualIntent(ChatRequest request) {
        String message = request.getMessage().trim();
        Long userId = request.getUserId();
        
        // 获取对话上下文
        List<AiContextManager.ContextMessage> contextMessages = contextManager.getMessages(userId);
        if (contextMessages == null || contextMessages.isEmpty()) {
            return null;
        }
        
        // 获取最后一条 AI 消息
        String lastAiMessage = null;
        for (int i = contextMessages.size() - 1; i >= 0; i--) {
            if ("assistant".equals(contextMessages.get(i).getRole())) {
                lastAiMessage = contextMessages.get(i).getContent();
                break;
            }
        }
        
        if (lastAiMessage == null) {
            return null;
        }
        
        // 场景1：用户输入"第X个"、"第X款"、"第一个"、"第二个"等
        // 结合上下文，判断是查看商品详情还是加入购物车
        java.util.regex.Pattern indexPattern = java.util.regex.Pattern.compile("^第([\\d一二三四五六七八九十]+)[款个]$");
        java.util.regex.Matcher indexMatcher = indexPattern.matcher(message);
        if (indexMatcher.find()) {
            String indexStr = indexMatcher.group(1);
            int index = parseChineseNumber(indexStr);
            
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("index", index);
            
            // 判断上下文：如果上一条 AI 消息包含"推荐"、"搜索"、"找到"等关键词
            // 则识别为查看商品详情
            if (lastAiMessage.contains("推荐") || lastAiMessage.contains("搜索") || 
                lastAiMessage.contains("找到") || lastAiMessage.contains("商品")) {
                AiIntentResult result = new AiIntentResult();
                result.setIntent(AiIntent.VIEW_GOOD_DETAIL);
                result.setConfidence(0.9);
                result.setOriginalQuery(message);
                result.setParameters(parameters);
                result.setCategory("goods");
                result.setNeedClarification(false);
                log.info("上下文识别：用户选择第 {} 个商品，意图为查看商品详情", index);
                return result;
            }
        }
        
        // 场景2：用户输入"加入购物车"、"加购"等，但没有指定商品
        // 结合上下文，如果上一条是商品推荐/搜索，则默认操作最后一个推荐的商品
        if (message.matches(".*(加入.*购物车|加购|放入购物车).*") && 
            !message.matches(".*第[\\d一二三四五六七八九十]+[款个].*")) {
            
            // 检查上下文是否包含商品推荐
            if (lastAiMessage.contains("推荐") || lastAiMessage.contains("搜索") || 
                lastAiMessage.contains("找到") || lastAiMessage.contains("商品")) {
                
                AiIntentResult result = new AiIntentResult();
                result.setIntent(AiIntent.ADD_TO_CART);
                result.setConfidence(0.85);
                result.setOriginalQuery(message);
                result.setParameters(new HashMap<>());
                result.setCategory("cart");
                result.setNeedClarification(false);
                log.info("上下文识别：用户要求加入购物车，结合上下文推断操作推荐商品");
                return result;
            }
        }
        
        // 场景3：用户输入"详情"、"看看"、"看一下"等简短词
        // 结合上下文，如果上一条是商品推荐，则识别为查看商品详情
        if (message.matches("^(详情|看看|看一下|看看详情|查看详情)$")) {
            if (lastAiMessage.contains("推荐") || lastAiMessage.contains("搜索") || 
                lastAiMessage.contains("找到") || lastAiMessage.contains("商品")) {
                
                AiIntentResult result = new AiIntentResult();
                result.setIntent(AiIntent.VIEW_GOOD_DETAIL);
                result.setConfidence(0.85);
                result.setOriginalQuery(message);
                result.setParameters(new HashMap<>());
                result.setCategory("goods");
                result.setNeedClarification(false);
                log.info("上下文识别：用户要求查看详情，结合上下文推断查看推荐商品");
                return result;
            }
        }
        
        // 场景4：用户输入"买这个"、"要这个"、"就要这个"等
        if (message.matches(".*(买这个|要这个|就要这个|这个我要).*")) {
            if (lastAiMessage.contains("推荐") || lastAiMessage.contains("搜索") || 
                lastAiMessage.contains("找到") || lastAiMessage.contains("商品")) {
                
                AiIntentResult result = new AiIntentResult();
                result.setIntent(AiIntent.ADD_TO_CART);
                result.setConfidence(0.85);
                result.setOriginalQuery(message);
                result.setParameters(new HashMap<>());
                result.setCategory("cart");
                result.setNeedClarification(false);
                log.info("上下文识别：用户要求购买，结合上下文推断加入购物车");
                return result;
            }
        }
        
        return null;
    }
    
    /**
     * 将中文数字转换为阿拉伯数字
     */
    private int parseChineseNumber(String chineseNum) {
        switch (chineseNum) {
            case "一": return 1;
            case "二": return 2;
            case "三": return 3;
            case "四": return 4;
            case "五": return 5;
            case "六": return 6;
            case "七": return 7;
            case "八": return 8;
            case "九": return 9;
            case "十": return 10;
            default:
                try {
                    return Integer.parseInt(chineseNum);
                } catch (NumberFormatException e) {
                    return 1;
                }
        }
    }
    
    /**
     * 创建未知意图结果
     */
    private AiIntentResult createUnknownResult(String message) {
        AiIntentResult result = new AiIntentResult();
        result.setIntent(AiIntent.UNKNOWN);
        result.setConfidence(0.3);
        result.setOriginalQuery(message);
        result.setParameters(new HashMap<>());
        result.setCategory("unknown");
        result.setNeedClarification(true);
        result.setClarificationQuestion("抱歉，我没有理解您的意思。您是想查询商品、订单，还是有其他问题需要帮助？");
        return result;
    }
    
    /**
     * 获取意图分类
     */
    private String getCategory(AiIntent intent) {
        switch (intent) {
            case SEARCH_GOODS:
            case VIEW_GOOD_DETAIL:
            case RECOMMEND_GOODS:
            case COMPARE_GOODS:
            case CHECK_STOCK:
                return "goods";
            case ADD_TO_CART:
            case VIEW_CART:
            case UPDATE_CART:
            case CLEAR_CART:
                return "cart";
            case CREATE_ORDER:
            case TRACK_ORDER:
            case QUERY_ORDERS:
            case QUERY_ORDER_DETAIL:
            case CANCEL_ORDER:
            case CONFIRM_RECEIVE:
            case ORDER_STATISTICS:
                return "order";
            case PAY_ORDER:
            case CHECK_PAYMENT:
            case APPLY_REFUND:
                return "payment";
            case QUERY_USER_INFO:
            case UPDATE_USER_INFO:
            case QUERY_ADDRESS:
            case ADD_ADDRESS:
                return "user";
            case FAQ:
            case AFTER_SALES:
            case COMPLAINT:
            case TUTORIAL:
                return "help";
            default:
                return "general";
        }
    }
    
    @Override
    public AiIntent[] getSupportedIntents() {
        return AiIntent.values();
    }
    
    @Override
    public boolean supportsIntent(AiIntent intent) {
        return INTENT_KEYWORDS.containsKey(intent);
    }
}
