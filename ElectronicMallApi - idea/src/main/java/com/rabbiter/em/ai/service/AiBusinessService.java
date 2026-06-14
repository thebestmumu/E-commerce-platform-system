package com.rabbiter.em.ai.service;

import com.rabbiter.em.entity.Cart;
import com.rabbiter.em.entity.Good;
import com.rabbiter.em.entity.Order;
import com.rabbiter.em.entity.OrderItem;
import com.rabbiter.em.entity.Review;
import com.rabbiter.em.entity.Standard;
import com.rabbiter.em.entity.CategoryIcon;
import com.rabbiter.em.entity.Category;
import com.rabbiter.em.mapper.GoodMapper;
import com.rabbiter.em.mapper.OrderMapper;
import com.rabbiter.em.mapper.ReviewMapper;
import com.rabbiter.em.mapper.StandardMapper;
import com.rabbiter.em.mapper.CategoryIconMapper;
import com.rabbiter.em.mapper.CategoryMapper;
import com.rabbiter.em.service.CartService;
import com.rabbiter.em.service.OrderService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 业务服务
 * 为 AI 助手提供业务工具调用
 */
@Service
public class AiBusinessService {
    
    private static final Logger log = LoggerFactory.getLogger(AiBusinessService.class);
    
    @Autowired
    private GoodMapper goodMapper;
    
    @Autowired
    private OrderMapper orderMapper;
    
    @Autowired
    private ReviewMapper reviewMapper;
    
    @Autowired
    private StandardMapper standardMapper;
    
    @Autowired
    private CategoryIconMapper categoryIconMapper;
    
    @Autowired
    private CategoryMapper categoryMapper;
    
    @Autowired
    private com.rabbiter.em.ai.core.AiContextManager contextManager;
    
    @Autowired
    private com.rabbiter.em.service.CartService cartService;
    
    @Autowired
    private com.rabbiter.em.service.OrderService orderService;
    
    /**
     * 搜索商品
     */
    public Object search(String keyword) {
        log.info("搜索商品：{}", keyword);
        
        try {
            // 提取关键词（去掉"推荐一些"、"帮我找"等前缀）
            String cleanKeyword = extractSearchKeyword(keyword);
            log.info("提取后的搜索关键词：{}", cleanKeyword);
            
            // 品类消歧：解决"苹果"等歧义词问题
            String disambiguatedKeyword = disambiguateCategory(cleanKeyword, keyword);
            log.info("消歧后的关键词：{}", disambiguatedKeyword);
            
            // 使用已有的同义词映射扩展搜索关键词
            List<String> expandedKeywords = getCategorySynonyms(disambiguatedKeyword);
            
            // 如果没有匹配到同义词，使用原始关键词
            if (expandedKeywords.isEmpty()) {
                expandedKeywords.add(disambiguatedKeyword);
            }
            
            log.info("扩展后的搜索关键词：{}", expandedKeywords);
            
            // 构建 OR 查询条件
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Good> wrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            
            for (int i = 0; i < expandedKeywords.size(); i++) {
                String kw = expandedKeywords.get(i);
                if (i == 0) {
                    wrapper.and(w -> w.like("name", kw).or().like("description", kw));
                } else {
                    wrapper.or(w -> w.like("name", kw).or().like("description", kw));
                }
            }
            
            List<Good> goods = goodMapper.selectList(
                wrapper.orderByDesc("sales").last("LIMIT 10")
            );
            
            // 如果没有搜索到商品，返回热销商品
            if (goods == null || goods.isEmpty()) {
                log.info("未找到相关商品，返回热销商品");
                goods = goodMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Good>()
                        .orderByDesc("sales")
                        .last("LIMIT 10")
                );
            }
            
            // 加载每个商品的价格（含折扣）
            for (Good good : goods) {
                loadGoodPrice(good);
            }
            
            log.info("搜索到 {} 个商品", goods.size());
            return goods;
        } catch (Exception e) {
            log.error("搜索商品失败：{}", e.getMessage());
            return "搜索失败";
        }
    }
    
    /**
     * 品类消歧：解决歧义词问题
     * 例如："苹果" 在手机上下文中 -> 苹果手机，在水果上下文中 -> 苹果水果
     * @param keyword 提取后的关键词
     * @param originalInput 用户原始输入（用于上下文分析）
     * @return 消歧后的关键词
     */
    private String disambiguateCategory(String keyword, String originalInput) {
        if (keyword == null || keyword.isEmpty()) {
            return keyword;
        }
        
        String context = originalInput != null ? originalInput : "";
        
        // "苹果"的消歧
        if (keyword.contains("苹果") && !keyword.contains("手机")) {
            if (context.contains("手机") || context.contains("iPhone") || context.contains("电话")) {
                log.info("品类消歧：苹果 + 手机上下文 -> 苹果手机");
                return "苹果手机";
            } else if (context.contains("吃") || context.contains("水果") || context.contains("甜") || context.contains("味")) {
                log.info("品类消歧：苹果 + 水果上下文 -> 苹果");
                return "苹果";
            } else if (context.contains("电脑") || context.contains("Mac") || context.contains("笔记本")) {
                log.info("品类消歧：苹果 + 电脑上下文 -> 苹果电脑");
                return "苹果电脑";
            } else if (context.contains("手表") || context.contains("Watch")) {
                log.info("品类消歧：苹果 + 手表上下文 -> 苹果手表");
                return "苹果手表";
            } else if (context.contains("平板") || context.contains("iPad")) {
                log.info("品类消歧：苹果 + 平板上下文 -> 苹果平板");
                return "苹果平板";
            }
            // 默认情况下，如果是单独的"苹果"，且没有上下文，优先当作手机处理
            if (keyword.equals("苹果")) {
                log.info("品类消歧：苹果（无上下文）-> 苹果手机（默认）");
                return "苹果手机";
            }
        }
        
        // "小米"的消歧
        if (keyword.contains("小米")) {
            if (context.contains("电视") || context.contains("家电")) {
                log.info("品类消歧：小米 + 家电上下文 -> 小米电视");
                return "小米电视";
            } else if (context.contains("平板")) {
                log.info("品类消歧：小米 + 平板上下文 -> 小米平板");
                return "小米平板";
            }
            // 默认当作手机
            if (keyword.equals("小米")) {
                log.info("品类消歧：小米（无上下文）-> 小米手机（默认）");
                return "小米手机";
            }
        }
        
        // "华为"的消歧
        if (keyword.contains("华为")) {
            if (context.contains("平板") || context.contains("电脑")) {
                log.info("品类消歧：华为 + 电脑上下文 -> 华为电脑");
                return "华为电脑";
            }
            // 默认当作手机
            if (keyword.equals("华为")) {
                log.info("品类消歧：华为（无上下文）-> 华为手机（默认）");
                return "华为手机";
            }
        }
        
        return keyword;
    }
    
    /**
     * 从用户输入中提取搜索关键词
     */
    private String extractSearchKeyword(String input) {
        if (input == null || input.isEmpty()) return "";
        
        // 去掉常见的推荐前缀
        String[] prefixes = {"推荐一些", "推荐", "帮我找", "帮我推荐", "我想买", "我想看", "看看", "有什么", "找一下", "找"};
        String result = input;
        
        for (String prefix : prefixes) {
            if (result.startsWith(prefix)) {
                result = result.substring(prefix.length());
                break;
            }
        }
        
        // 去掉语气词和标点
        result = result.replaceAll("[的了吗呢啊呀哦]", "").trim();
        
        // 如果结果为空，返回原始输入
        return result.isEmpty() ? input : result;
    }
    
    /**
     * 查询订单
     */
    public Object queryOrders(Integer userId) {
        log.info("查询订单，用户 ID: {}", userId);
        
        try {
            List<com.rabbiter.em.entity.Order> orders = orderMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.rabbiter.em.entity.Order>()
                    .eq("user_id", userId)
                    .orderByDesc("create_time")
                    .last("LIMIT 10")
            );
            
            return orders;
        } catch (Exception e) {
            log.error("查询订单失败：{}", e.getMessage());
            return "查询失败";
        }
    }
    
    /**
     * 获取商品详情
     */
    public Object getGoodDetail(Long goodId) {
        log.info("获取商品详情：{}", goodId);
        
        try {
            Good good = goodMapper.selectById(goodId);
            return good != null ? good : "商品不存在";
        } catch (Exception e) {
            log.error("获取商品详情失败：{}", e.getMessage());
            return "获取失败";
        }
    }
    
    /**
     * 个性化推荐
     */
    public Object getPersonalizedRecommendation(Long userId) {
        log.info("获取个性化推荐，用户 ID: {}", userId);
        
        try {
            // 获取热销商品作为推荐
            List<Good> goods = goodMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Good>()
                    .orderByDesc("sales")
                    .last("LIMIT 10")
            );
            
            // 为每个商品加载价格和评论信息
            for (Good good : goods) {
                loadGoodPrice(good);
                loadReviewInfo(good);
            }
            
            return goods;
        } catch (Exception e) {
            log.error("个性化推荐失败：{}", e.getMessage());
            return "推荐失败";
        }
    }
    
    /**
     * 快速下单
     */
    public Map<String, Object> quickOrder(Map<String, Object> params) {
        log.info("快速下单：{}", params);
        
        Map<String, Object> result = new HashMap<>();
        Long goodId = params.get("goodId") != null ? Long.valueOf(params.get("goodId").toString()) : null;
        
        if (goodId != null) {
            Good good = goodMapper.selectById(goodId);
            if (good != null) {
                result.put("goodId", goodId);
                result.put("good", good);
                result.put("success", true);
                log.info("快速下单 - 返回商品详情：{}", goodId);
            } else {
                result.put("success", false);
                result.put("message", "商品不存在");
            }
        } else {
            result.put("success", false);
            result.put("message", "商品信息不完整");
        }
        
        return result;
    }
    
    /**
     * 追踪订单状态（获取最近订单信息，支持分页）
     */
    public Object trackOrderStatus(String orderId, Long userId, int page, int limit) {
        log.info("追踪订单状态，订单号：{}, 用户 ID: {}, 页码: {}, 每页: {}", orderId, userId, page, limit);
        
        try {
            if (orderId != null && !"最新订单".equals(orderId)) {
                // 查询指定订单详情
                List<Map<String, Object>> orderDetail = orderMapper.selectByOrderNo(orderId);
                Map<String, Object> result = new HashMap<>();
                result.put("orders", orderDetail);
                result.put("count", orderDetail != null ? orderDetail.size() : 0);
                result.put("total", orderDetail != null ? orderDetail.size() : 0);
                result.put("hasMore", false);
                result.put("page", 1);
                result.put("limit", limit);
                return result;
            } else {
                // 先查询总数
                Long total = orderMapper.selectCount(
                    new QueryWrapper<Order>().eq("user_id", userId)
                );
                // 分页查询
                int offset = (page - 1) * limit;
                List<Order> orders = orderMapper.selectList(
                    new QueryWrapper<Order>()
                        .eq("user_id", userId)
                        .orderByDesc("create_time")
                        .last("LIMIT " + offset + ", " + limit)
                );
                
                Map<String, Object> result = new HashMap<>();
                result.put("orders", orders);
                result.put("count", orders.size());
                result.put("total", total);
                result.put("hasMore", offset + limit < total);
                result.put("page", page);
                result.put("limit", limit);
                return result;
            }
        } catch (Exception e) {
            log.error("追踪订单状态失败：{}", e.getMessage());
            return "查询失败";
        }
    }
    
    /**
     * 追踪订单状态（默认第一页）
     */
    public Object trackOrderStatus(String orderId, Long userId) {
        return trackOrderStatus(orderId, userId, 1, 10);
    }
    
    /**
     * 分析订单历史（默认返回前10个订单）
     */
    public Map<String, Object> analyzeOrderHistory(Long userId) {
        return analyzeOrderHistory(userId, 0, 10);
    }
    
    /**
     * 分析订单历史（支持分页）
     * @param userId 用户ID
     * @param offset 偏移量
     * @param limit 限制数量
     */
    public Map<String, Object> analyzeOrderHistory(Long userId, int offset, int limit) {
        log.info("分析订单历史，用户 ID: {}, offset: {}, limit: {}", userId, offset, limit);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 查询所有订单（用于分析）
            List<com.rabbiter.em.entity.Order> allOrders = orderMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.rabbiter.em.entity.Order>()
                    .eq("user_id", userId)
                    .orderByDesc("create_time")
            );
            
            // 查询当前页的订单
            List<com.rabbiter.em.entity.Order> orders = orderMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.rabbiter.em.entity.Order>()
                    .eq("user_id", userId)
                    .orderByDesc("create_time")
                    .last("LIMIT " + limit + " OFFSET " + offset)
            );
            
            // 基础统计
            BigDecimal totalAmount = BigDecimal.ZERO;
            int pendingPay = 0;
            int shipped = 0;
            int completed = 0;
            int cancelled = 0;
            
            // 月度消费统计
            Map<String, BigDecimal> monthlyConsumption = new java.util.LinkedHashMap<>();
            // 商品类别偏好
            Map<String, Integer> categoryPreference = new HashMap<>();
            // 平均订单金额
            BigDecimal avgAmount = BigDecimal.ZERO;
            // 最高单笔消费
            BigDecimal maxAmount = BigDecimal.ZERO;
            
            for (com.rabbiter.em.entity.Order order : allOrders) {
                if (order.getTotalPrice() != null) {
                    totalAmount = totalAmount.add(order.getTotalPrice());
                    
                    // 更新最高消费
                    if (order.getTotalPrice().compareTo(maxAmount) > 0) {
                        maxAmount = order.getTotalPrice();
                    }
                    
                    // 月度统计
                    if (order.getCreateTime() != null) {
                        String month = order.getCreateTime().toString().substring(0, 7); // yyyy-MM
                        monthlyConsumption.merge(month, order.getTotalPrice(), BigDecimal::add);
                    }
                }
                
                String state = order.getState();
                if ("0".equals(state) || "待支付".equals(state)) {
                    pendingPay++;
                } else if ("2".equals(state) || "待收货".equals(state)) {
                    shipped++;
                } else if ("3".equals(state) || "已完成".equals(state)) {
                    completed++;
                } else if ("4".equals(state) || "已取消".equals(state)) {
                    cancelled++;
                }
            }
            
            // 计算平均订单金额
            if (!allOrders.isEmpty()) {
                avgAmount = totalAmount.divide(new BigDecimal(allOrders.size()), 2, BigDecimal.ROUND_HALF_UP);
            }
            
            // 生成分析洞察
            List<String> insights = new ArrayList<>();
            if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
                insights.add("总消费 ¥" + totalAmount);
                insights.add("平均订单 ¥" + avgAmount);
                insights.add("最高单笔 ¥" + maxAmount);
            }
            if (completed > 0) {
                insights.add("完成订单 " + completed + " 个");
            }
            if (pendingPay > 0) {
                insights.add("待支付 " + pendingPay + " 个");
            }
            
            result.put("orders", orders);
            result.put("totalOrders", allOrders.size());
            result.put("totalAmount", totalAmount);
            result.put("avgAmount", avgAmount);
            result.put("maxAmount", maxAmount);
            result.put("pendingPay", pendingPay);
            result.put("shipped", shipped);
            result.put("completed", completed);
            result.put("cancelled", cancelled);
            result.put("monthlyConsumption", monthlyConsumption);
            result.put("insights", insights);
            result.put("success", true);
            result.put("hasMore", offset + limit < allOrders.size());
            result.put("currentOffset", offset);
            result.put("currentLimit", limit);
            
            log.info("订单分析完成，当前页 {} 个订单，总订单数 {}", orders.size(), allOrders.size());
        } catch (Exception e) {
            log.error("分析订单历史失败：{}", e.getMessage());
            result.put("success", false);
            result.put("message", "分析失败");
        }
        
        return result;
    }
    
    /**
     * 添加到购物车
     */
    public Object addCart(Map<String, Object> params) {
        log.info("添加到购物车：{}", params);
        
        // 获取商品ID和用户ID
        Long goodId = params.get("goodId") != null ? Long.valueOf(params.get("goodId").toString()) : null;
        Integer userId = params.get("userId") != null ? Integer.valueOf(params.get("userId").toString()) : null;
        
        if (goodId == null || userId == null) {
            log.warn("商品ID或用户ID为空");
            return "添加失败：商品信息不完整";
        }
        
        // 获取商品详情
        Good good = goodMapper.selectById(goodId);
        if (good == null) {
            log.warn("商品不存在：{}", goodId);
            return "添加失败：商品不存在";
        }
        
        // 获取第一个规格
        String standard = "默认";
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Standard> queryWrapper = 
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        queryWrapper.eq("good_id", goodId);
        queryWrapper.last("LIMIT 1");
        Standard firstStandard = standardMapper.selectOne(queryWrapper);
        if (firstStandard != null) {
            standard = firstStandard.getValue();
        }
        
        // 调用购物车服务添加商品
        try {
            Cart cart = new Cart();
            cart.setUserId(userId.longValue());
            cart.setGoodId(goodId);
            cart.setCount(1);
            cart.setStandard(standard);
            cart.setCreateTime(cn.hutool.core.date.DateUtil.now());
            
            boolean success = cartService.save(cart);
            log.info("添加商品到购物车：商品ID={}, 用户ID={}, 规格={}, 结果={}", goodId, userId, standard, success ? "成功" : "失败");
            return success ? "添加成功" : "添加失败";
        } catch (Exception e) {
            log.error("添加到购物车异常", e);
            return "添加失败：" + e.getMessage();
        }
    }
    
    /**
     * 查看商品详情
     */
    public Map<String, Object> viewGoodDetail(Long goodId) {
        log.info("查看商品详情：{}", goodId);
        
        Map<String, Object> result = new HashMap<>();
        
        // 获取商品详情
        Good good = goodMapper.selectById(goodId);
        if (good == null) {
            log.warn("商品不存在：{}", goodId);
            result.put("success", false);
            result.put("message", "商品不存在");
            return result;
        }
        
        // 获取商品规格
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Standard> standardQueryWrapper = 
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        standardQueryWrapper.eq("good_id", goodId);
        List<Standard> standardList = standardMapper.selectList(standardQueryWrapper);
        
        result.put("success", true);
        result.put("good", good);
        result.put("standards", standardList);
        result.put("goodId", goodId);
        
        return result;
    }
    
    /**
     * 从上下文中根据索引获取商品 ID
     * @param userId 用户 ID
     * @param index 商品索引（1-based）
     * @return 商品 ID
     */
    private Long getGoodIdFromContextByIndex(Long userId, int index) {
        try {
            log.info("开始从上下文中获取第 {} 个商品的 ID", index);
            
            // 获取对话上下文
            List<com.rabbiter.em.ai.core.AiContextManager.ContextMessage> contextMessages = contextManager.getMessages(userId);
            if (contextMessages == null || contextMessages.isEmpty()) {
                log.warn("没有可用的上下文消息");
                return null;
            }
            
            log.info("上下文消息总数: {}", contextMessages.size());
            
            // 从最近到最远遍历 AI 消息，找到第一个包含商品列表的推荐上下文
            // 因为中间可能夹杂着"分析报告"、"订单查询"等不含商品 ID 的回复
            // 如果只取最后一条 AI 消息，会错过更早的推荐记录
            String lastAiMessage = null;
            for (int i = contextMessages.size() - 1; i >= 0; i--) {
                if ("assistant".equals(contextMessages.get(i).getRole())) {
                    String content = contextMessages.get(i).getContent();
                    if (content != null && (content.contains("【推荐商品列表】") || content.contains("商品ID:"))) {
                        lastAiMessage = content;
                        log.info("找到包含商品信息的 AI 消息: {}", content.substring(0, Math.min(100, content.length())));
                        break;
                    }
                }
            }
            
            if (lastAiMessage == null) {
                log.warn("没有找到 AI 回复消息");
                return null;
            }
            
            // 从 AI 消息中提取商品 ID
            List<Long> goodIds = new ArrayList<>();
            
            // 方式一：解析结构化格式 【推荐商品列表】[id1,id2,id3,...]
            // 这种格式由 recommend/specific_recommend 保存，更精确可靠
            try {
                java.util.regex.Pattern structPattern = java.util.regex.Pattern.compile("【推荐商品列表】\\[(.*?)\\]");
                java.util.regex.Matcher structMatcher = structPattern.matcher(lastAiMessage);
                if (structMatcher.find()) {
                    String jsonArray = structMatcher.group(1);
                    log.info("找到结构化推荐列表: {}", jsonArray);
                    // 解析 JSON 数组 [23,45,67]
                    if (jsonArray != null && !jsonArray.isEmpty()) {
                        String[] parts = jsonArray.split(",");
                        for (String part : parts) {
                            String trimmed = part.trim();
                            if (!trimmed.isEmpty()) {
                                try {
                                    goodIds.add(Long.parseLong(trimmed));
                                } catch (NumberFormatException e) {
                                    log.warn("解析商品ID失败: {}", trimmed);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("解析结构化推荐列表失败，尝试旧格式: {}", e.getMessage());
            }
            
            // 方式二：如果结构化格式没找到，回退到旧格式 "商品ID:123"
            if (goodIds.isEmpty()) {
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("商品ID:(\\d+)");
                java.util.regex.Matcher matcher = pattern.matcher(lastAiMessage);
                while (matcher.find()) {
                    goodIds.add(Long.parseLong(matcher.group(1)));
                }
            }
            
            log.info("提取到的商品 ID 列表: {}", goodIds);
            
            if (goodIds.isEmpty()) {
                log.warn("从上下文中没有找到商品 ID");
                return null;
            }
            
            // index 是 1-based（用户说"第一个"），转换为 0-based（数组索引）
            int zeroBasedIndex = index - 1;
            if (zeroBasedIndex < 0 || zeroBasedIndex >= goodIds.size()) {
                log.warn("索引 {} 超出范围，商品总数: {}", index, goodIds.size());
                return null;
            }
            
            log.info("用户说'第{}个'，对应数组索引: {}，商品 ID: {}", index, zeroBasedIndex, goodIds.get(zeroBasedIndex));
            return goodIds.get(zeroBasedIndex);
            
        } catch (Exception e) {
            log.error("从上下文中获取商品 ID 失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 查询购物车
     */
    public Object queryCart(Integer userId) {
        log.info("查询购物车，用户 ID: {}", userId);
        return "购物车数据";
    }
    
    /**
     * 更新购物车
     */
    public Object updateCart(Map<String, Object> params) {
        log.info("更新购物车：{}", params);
        return "更新成功";
    }
    
    /**
     * 清空购物车
     */
    public Object clearCart(Integer userId) {
        log.info("清空购物车，用户 ID: {}", userId);
        return "清空成功";
    }
    
    /**
     * 创建订单
     */
    public Object createOrder(Map<String, Object> params) {
        log.info("创建订单：{}", params);
        return "订单创建成功";
    }
    
    /**
     * 取消订单
     */
    public Object cancelOrder(String orderId) {
        log.info("取消订单：{}", orderId);
        return "取消成功";
    }
    
    /**
     * 确认收货
     */
    public Object confirmReceive(String orderId) {
        log.info("确认收货：{}", orderId);
        return "确认成功";
    }
    
    /**
     * 申请退款
     */
    public Object applyRefund(Map<String, Object> params) {
        log.info("申请退款：{}", params);
        return "退款申请成功";
    }
    
    /**
     * 查询用户信息
     */
    public Object queryUserInfo(Long userId) {
        log.info("查询用户信息：{}", userId);
        return "用户信息";
    }
    
    /**
     * 更新用户信息
     */
    public Object updateUserInfo(Map<String, Object> params) {
        log.info("更新用户信息：{}", params);
        return "更新成功";
    }
    
    /**
     * 查询收货地址
     */
    public Object queryAddress(Long userId) {
        log.info("查询收货地址：{}", userId);
        return "地址列表";
    }
    
    /**
     * 添加收货地址
     */
    public Object addAddress(Map<String, Object> params) {
        log.info("添加收货地址：{}", params);
        return "添加成功";
    }
    
    /**
     * 批量添加到购物车
     */
    public Object batchAddCart(List<Map<String, Object>> items) {
        log.info("批量添加到购物车：{}", items);
        return "批量添加成功";
    }
    
    /**
     * 删除购物车商品
     */
    public Object deleteFromCart(Map<String, Object> params) {
        log.info("删除购物车商品：{}", params);
        return "删除成功";
    }
    
    /**
     * 订单统计
     */
    public Object orderStatistics(Long userId) {
        log.info("订单统计，用户 ID: {}", userId);
        return "统计数据";
    }
    
    /**
     * 支付订单
     */
    public Object payOrder(String orderId) {
        log.info("支付订单：{}", orderId);
        return "支付成功";
    }
    
    /**
     * 检查支付状态
     */
    public Object checkPayment(String orderId) {
        log.info("检查支付状态：{}", orderId);
        return "支付状态正常";
    }
    
    /**
     * 商品推荐（带参数）
     */
    public Object recommendGoods(Map<String, Object> params) {
        log.info("商品推荐：{}", params);
        
        try {
            List<Good> goods = goodMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Good>()
                    .orderByDesc("sales")
                    .last("LIMIT 10")
            );
            
            return goods;
        } catch (Exception e) {
            log.error("商品推荐失败：{}", e.getMessage());
            return "推荐失败";
        }
    }
    
    /**
     * 个性化商品推荐 - 返回 {topGood, otherGoods} 格式
     * @param userId 用户ID
     * @return 推荐结果（包含首推商品和其他商品）
     */
    public Map<String, Object> recommendGoodsForUser(Long userId) {
        log.info("个性化商品推荐 - 用户ID: {}", userId);
        
        List<Good> recommendGoodsList = (List<Good>) getPersonalizedRecommendation(userId);
        
        Map<String, Object> result = new HashMap<>();
        
        if (recommendGoodsList != null && !recommendGoodsList.isEmpty()) {
            Good topGood = recommendGoodsList.get(0);
            log.info("为推荐结果的第一个商品生成销售报告和评论分析 - 商品ID: {}, 商品名: {}", topGood.getId(), topGood.getName());
            
            // 加载第一条五星评论
            loadFirstReview(topGood);
            
            // 生成销售报告
            Map<String, Object> salesReport = generateSalesReport(topGood.getId(), 30);
            
            // 生成评论舆情分析
            Map<String, Object> sentimentAnalysis = analyzeReviewSentiment(topGood.getId());
            
            // 合并数据到topGood
            Map<String, Object> topGoodData = new HashMap<>();
            topGoodData.put("id", topGood.getId());
            topGoodData.put("name", topGood.getName());
            topGoodData.put("sales", topGood.getSales());
            topGoodData.put("price", topGood.getPrice());
            topGoodData.put("imgs", topGood.getImgs());
            topGoodData.put("image", topGood.getImgs());
            topGoodData.put("description", topGood.getDescription());
            topGoodData.put("discount", topGood.getDiscount());
            topGoodData.put("reviewCount", topGood.getReviewCount());
            topGoodData.put("goodRating", topGood.getGoodRating());
            if (topGood.getTags() != null) {
                topGoodData.put("tags", topGood.getTags());
            }
            
            // 添加第一条五星评论
            if (topGood.getFirstReview() != null) {
                topGoodData.put("firstReview", topGood.getFirstReview());
            }
            
            // 合并销售报告数据
            if (salesReport != null) {
                topGoodData.put("salesTrend", salesReport.get("salesTrend"));
                topGoodData.put("totalSales", salesReport.get("totalSales"));
                topGoodData.put("totalRevenue", salesReport.get("totalRevenue"));
                topGoodData.put("salesRank", salesReport.get("salesRank"));
                topGoodData.put("days", salesReport.get("days"));
            }
            
            // 合并评论分析数据
            if (sentimentAnalysis != null) {
                topGoodData.put("sentimentDistribution", sentimentAnalysis.get("sentimentDistribution"));
                topGoodData.put("ratingStats", sentimentAnalysis.get("ratingStats"));
                topGoodData.put("wordCloudData", sentimentAnalysis.get("wordCloudData"));
                topGoodData.put("totalReviews", sentimentAnalysis.get("totalReviews"));
                topGoodData.put("hotTags", sentimentAnalysis.get("hotTags"));
                topGoodData.put("latestReviews", sentimentAnalysis.get("latestReviews"));
            }
            
            // 构建{topGood, otherGoods}格式的结果
            result.put("topGood", topGoodData);
            
            // 将otherGoods转换为Map列表，确保字段名一致
            List<Map<String, Object>> otherGoodsData = new ArrayList<>();
            if (recommendGoodsList.size() > 1) {
                for (int i = 1; i < recommendGoodsList.size(); i++) {
                    Good g = recommendGoodsList.get(i);
                    Map<String, Object> goodMap = new HashMap<>();
                    goodMap.put("id", g.getId());
                    goodMap.put("name", g.getName());
                    goodMap.put("sales", g.getSales());
                    goodMap.put("price", g.getPrice());
                    goodMap.put("imgs", g.getImgs());
                    goodMap.put("image", g.getImgs());
                    goodMap.put("description", g.getDescription());
                    goodMap.put("discount", g.getDiscount());
                    goodMap.put("reviewCount", g.getReviewCount());
                    goodMap.put("goodRating", g.getGoodRating());
                    if (g.getTags() != null) {
                        goodMap.put("tags", g.getTags());
                    }
                    otherGoodsData.add(goodMap);
                }
            }
            result.put("otherGoods", otherGoodsData);
            result.put("count", recommendGoodsList.size());
            
            log.info("推荐结果 - topGood: {}, otherGoods: {}",
                topGoodData.get("name"),
                otherGoodsData.size());
        } else {
            result.put("topGood", null);
            result.put("otherGoods", new ArrayList<>());
            result.put("count", 0);
        }
        
        return result;
    }
    
    /**
     * 智能推荐 - 根据用户需求推荐商品并生成推荐文案
     * @param userId 用户 ID
     * @param requirement 用户需求描述
     * @return 推荐结果（包含文案和商品列表）
     */
    public Map<String, Object> smartRecommend(Long userId, String requirement) {
        log.info("智能推荐，用户 ID: {}, 需求：{}", userId, requirement);
        
        try {
            // 1. 根据需求搜索商品
            List<Good> goods = null;
            
            if (requirement != null && !requirement.isEmpty() && !requirement.equals("undefined")) {
                // 有具体需求，按需求搜索
                goods = goodMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Good>()
                        .and(wrapper -> wrapper
                            .like("name", requirement)
                            .or()
                            .like("description", requirement)
                        )
                        .orderByDesc("sales")
                        .last("LIMIT 12")
                );
            }
            
            // 如果没有找到商品，返回热销商品
            if (goods == null || goods.isEmpty()) {
                goods = goodMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Good>()
                        .orderByDesc("sales")
                        .last("LIMIT 12")
                );
            }
            
            // 2. 构建返回结果
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("goods", goods);
            
            return result;
        } catch (Exception e) {
            log.error("智能推荐失败：{}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 具体品类推荐 - 独立的推荐函数，支持价格限定、规格限定和排序方式
     * @param userId 用户 ID
     * @param category 品类关键词（如"卫衣"、"手机"）
     * @param minPrice 最低价格（可选，单位：元）
     * @param maxPrice 最高价格（可选，单位：元）
     * @param standard 规格限定（可选，如"XL"、"42码"）
     * @param sortBy 排序方式（sales-销量、rating-好评、price-价格、default-综合）
     * @return 推荐结果（包含首推商品和其他商品列表）
     */
    public Map<String, Object> specificCategoryRecommend(Long userId, String category, 
                                                          Double minPrice, Double maxPrice, 
                                                          String standard, String sortBy) {
        log.info("具体品类推荐，用户 ID: {}, 品类：{}, 价格范围：{}-{}, 规格：{}, 排序：{}", 
                 userId, category, minPrice, maxPrice, standard, sortBy);
        
        try {
            // 1. 构建查询条件
            List<Good> goods = null;
            boolean foundGoods = false;
            
            if (category != null && !category.isEmpty() && !category.equals("undefined")) {
                // 品类消歧：解决"苹果"等歧义词问题
                String disambiguatedCategory = disambiguateCategory(category, category);
                log.info("品类消歧：{} -> {}", category, disambiguatedCategory);
                
                // 使用同义词扩展搜索
                List<String> expandedKeywords = getCategorySynonyms(disambiguatedCategory);
                if (expandedKeywords.isEmpty()) {
                    expandedKeywords.add(disambiguatedCategory);
                }
                log.info("品类：{}，同义词扩展：{}", category, expandedKeywords);
                
                // 判断是否是手机品类（用于限制分类ID）
                boolean isPhoneCategory = disambiguatedCategory.contains("手机") || disambiguatedCategory.contains("电话") || disambiguatedCategory.contains("iPhone");
                
                // 构建查询条件，使用同义词扩展
                com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Good> wrapper = 
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
                
                if (isPhoneCategory) {
                    // 手机相关搜索：限制在数码类（category_id=3），避免搜索到水果"苹果"
                    wrapper.eq("category_id", 3);
                    wrapper.and(w -> {
                        for (int i = 0; i < expandedKeywords.size(); i++) {
                            String kw = expandedKeywords.get(i);
                            if (i == 0) {
                                w.like("name", kw).or().like("description", kw);
                            } else {
                                w.or().like("name", kw).or().like("description", kw);
                            }
                        }
                    });
                } else {
                    wrapper.and(w -> {
                        for (int i = 0; i < expandedKeywords.size(); i++) {
                            String kw = expandedKeywords.get(i);
                            if (i == 0) {
                                w.like("name", kw).or().like("description", kw);
                            } else {
                                w.or().like("name", kw).or().like("description", kw);
                            }
                        }
                    });
                }
                wrapper.last("LIMIT 50");
                
                goods = goodMapper.selectList(wrapper);
                log.info("查询（同义词扩展）查询到 {} 个商品", goods != null ? goods.size() : 0);
                
                // 如果还是没有找到商品，尝试分类匹配
                if (goods == null || goods.isEmpty()) {
                    log.info("合并查询未找到商品，尝试分类匹配");
                    Long categoryId = getCategoryByName(category);
                    if (categoryId != null) {
                        wrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
                        wrapper.eq("category_id", categoryId);
                        wrapper.last("LIMIT 50");
                        goods = goodMapper.selectList(wrapper);
                        log.info("分类匹配查询到 {} 个商品", goods != null ? goods.size() : 0);
                    }
                }
                
                if (goods != null && !goods.isEmpty()) {
                    foundGoods = true;
                    // 4. 加载价格信息
                    for (Good good : goods) {
                        loadGoodPrice(good);
                        loadReviewInfo(good);
                    }
                    
                    // 5. 价格范围过滤（加载价格后）
                    int originalCount = goods.size();
                    if (minPrice != null || maxPrice != null) {
                        goods = goods.stream()
                            .filter(g -> {
                                BigDecimal price = g.getPrice();
                                if (price == null) return false;
                                if (minPrice != null && price.doubleValue() < minPrice) return false;
                                if (maxPrice != null && price.doubleValue() > maxPrice) return false;
                                return true;
                            })
                            .collect(java.util.stream.Collectors.toList());
                    }
                    
                    // 5.1 如果价格过滤后没有商品，返回确认推荐标志
                    if (goods.isEmpty() && originalCount > 0) {
                        log.info("价格过滤后没有商品，返回确认推荐标志");
                        java.util.Map<String, Object> result = new java.util.HashMap<>();
                        result.put("needConfirm", true);
                        result.put("category", category);
                        result.put("originalCount", originalCount);
                        result.put("filters", new java.util.HashMap<String, Object>() {{
                            if (minPrice != null) put("minPrice", minPrice);
                            if (maxPrice != null) put("maxPrice", maxPrice);
                        }});
                        result.put("topGood", null);
                        result.put("otherGoods", new java.util.ArrayList<>());
                        result.put("count", 0);
                        return result;
                    }
                    
                    // 6. 根据排序方式排序
                    if (!goods.isEmpty()) {
                        sortGoods(goods, sortBy);
                    }
                    
                    // 7. 如果有规格限定，进一步过滤
                    if (standard != null && !standard.isEmpty()) {
                        goods = filterByStandard(goods, standard);
                    }
                }
            }
            
            // 8. 如果没有找到商品，返回热销商品
            if (!foundGoods) {
                log.info("未找到匹配商品，返回热销商品");
                goods = goodMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Good>()
                        .orderByDesc("sales")
                        .last("LIMIT 12")
                );
                for (Good good : goods) {
                    loadGoodPrice(good);
                    loadReviewInfo(good);
                }
                
                // 构建返回结果
                java.util.Map<String, Object> result = new java.util.HashMap<>();
                if (goods != null && !goods.isEmpty()) {
                    // 将topGood转换为Map
                    Good topGood = goods.get(0);
                    // 加载第一条五星评论
                    loadFirstReview(topGood);
                    
                    Map<String, Object> topGoodData = new HashMap<>();
                    topGoodData.put("id", topGood.getId());
                    topGoodData.put("name", topGood.getName());
                    topGoodData.put("sales", topGood.getSales());
                    topGoodData.put("price", topGood.getPrice());
                    topGoodData.put("imgs", topGood.getImgs());
                    topGoodData.put("image", topGood.getImgs());
                    topGoodData.put("description", topGood.getDescription());
                    topGoodData.put("discount", topGood.getDiscount());
                    topGoodData.put("reviewCount", topGood.getReviewCount());
                    topGoodData.put("goodRating", topGood.getGoodRating());
                    if (topGood.getTags() != null) {
                        topGoodData.put("tags", topGood.getTags());
                    }
                    // 添加第一条五星评论
                    if (topGood.getFirstReview() != null) {
                        topGoodData.put("firstReview", topGood.getFirstReview());
                    }
                    result.put("topGood", topGoodData);
                    
                    // 将otherGoods转换为Map列表
                    List<Map<String, Object>> otherGoodsData = new ArrayList<>();
                    if (goods.size() > 1) {
                        for (int i = 1; i < goods.size(); i++) {
                            Good g = goods.get(i);
                            Map<String, Object> goodMap = new HashMap<>();
                            goodMap.put("id", g.getId());
                            goodMap.put("name", g.getName());
                            goodMap.put("sales", g.getSales());
                            goodMap.put("price", g.getPrice());
                            goodMap.put("imgs", g.getImgs());
                            goodMap.put("image", g.getImgs());
                            goodMap.put("description", g.getDescription());
                            goodMap.put("discount", g.getDiscount());
                            goodMap.put("reviewCount", g.getReviewCount());
                            goodMap.put("goodRating", g.getGoodRating());
                            if (g.getTags() != null) {
                                goodMap.put("tags", g.getTags());
                            }
                            otherGoodsData.add(goodMap);
                        }
                    }
                    result.put("otherGoods", otherGoodsData);
                } else {
                    result.put("topGood", null);
                    result.put("otherGoods", new java.util.ArrayList<>());
                }
                result.put("category", category);
                result.put("count", goods != null ? goods.size() : 0);
                result.put("sortBy", sortBy);
                return result;
            }
            
            log.info("最终返回商品数量：{}", goods != null ? goods.size() : 0);
            if (goods != null && !goods.isEmpty()) {
                log.info("首推商品：{}，价格：{}", goods.get(0).getName(), goods.get(0).getPrice());
                if (goods.size() > 1) {
                    log.info("其他商品数量：{}", goods.size() - 1);
                }
            }
            
            // 10. 构建返回结果
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            
            if (goods != null && !goods.isEmpty()) {
                // 将topGood转换为Map
                Good topGood = goods.get(0);
                // 加载第一条五星评论
                loadFirstReview(topGood);
                
                Map<String, Object> topGoodData = new HashMap<>();
                topGoodData.put("id", topGood.getId());
                topGoodData.put("name", topGood.getName());
                topGoodData.put("sales", topGood.getSales());
                topGoodData.put("price", topGood.getPrice());
                topGoodData.put("imgs", topGood.getImgs());
                topGoodData.put("image", topGood.getImgs());
                topGoodData.put("description", topGood.getDescription());
                topGoodData.put("discount", topGood.getDiscount());
                topGoodData.put("reviewCount", topGood.getReviewCount());
                topGoodData.put("goodRating", topGood.getGoodRating());
                if (topGood.getTags() != null) {
                    topGoodData.put("tags", topGood.getTags());
                }
                // 添加第一条五星评论
                if (topGood.getFirstReview() != null) {
                    topGoodData.put("firstReview", topGood.getFirstReview());
                }
                result.put("topGood", topGoodData);
                
                // 将otherGoods转换为Map列表
                List<Map<String, Object>> otherGoodsData = new ArrayList<>();
                if (goods.size() > 1) {
                    for (int i = 1; i < goods.size(); i++) {
                        Good g = goods.get(i);
                        Map<String, Object> goodMap = new HashMap<>();
                        goodMap.put("id", g.getId());
                        goodMap.put("name", g.getName());
                        goodMap.put("sales", g.getSales());
                        goodMap.put("price", g.getPrice());
                        goodMap.put("imgs", g.getImgs());
                        goodMap.put("image", g.getImgs());
                        goodMap.put("description", g.getDescription());
                        goodMap.put("discount", g.getDiscount());
                        goodMap.put("reviewCount", g.getReviewCount());
                        goodMap.put("goodRating", g.getGoodRating());
                        if (g.getTags() != null) {
                            goodMap.put("tags", g.getTags());
                        }
                        otherGoodsData.add(goodMap);
                    }
                }
                result.put("otherGoods", otherGoodsData);
            } else {
                result.put("topGood", null);
                result.put("otherGoods", new java.util.ArrayList<>());
            }
            
            result.put("category", category);
            result.put("count", goods != null ? goods.size() : 0);
            result.put("sortBy", sortBy);
            
            // 添加筛选条件信息
            java.util.Map<String, Object> filters = new java.util.HashMap<>();
            if (minPrice != null) filters.put("minPrice", minPrice);
            if (maxPrice != null) filters.put("maxPrice", maxPrice);
            if (standard != null) filters.put("standard", standard);
            result.put("filters", filters);
            
            // 添加思考过程（简短版本）
            java.util.List<String> thinkingSteps = new java.util.ArrayList<>();
            
            // 需求分析（简短）
            StringBuilder demandBuilder = new StringBuilder();
            demandBuilder.append("想找").append(category != null ? category : "热销好物");
            if (minPrice != null || maxPrice != null) {
                if (minPrice != null && maxPrice != null) {
                    demandBuilder.append("，").append((int)Math.round(minPrice)).append("-").append((int)Math.round(maxPrice)).append("元");
                } else if (minPrice != null) {
                    demandBuilder.append("，").append((int)Math.round(minPrice)).append("元以上");
                } else {
                    demandBuilder.append("，").append((int)Math.round(maxPrice)).append("元以内");
                }
            }
            thinkingSteps.add(demandBuilder.toString());
            
            // 筛选结果（简短）
            thinkingSteps.add("已筛选" + (goods != null ? goods.size() : 0) + "款高口碑商品");
            
            // 推荐理由（简短）
            thinkingSteps.add("按销量和好评排序，推荐人气爆款");
            
            result.put("thinkingSteps", thinkingSteps);
            
            return result;
        } catch (Exception e) {
            log.error("具体品类推荐失败：{}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 根据规格过滤商品
     * @param goods 商品列表
     * @param standard 规格关键词
     * @return 过滤后的商品列表
     */
    private List<Good> filterByStandard(List<Good> goods, String standard) {
        if (goods == null || goods.isEmpty() || standard == null || standard.isEmpty()) {
            return goods;
        }
        
        List<Good> filteredGoods = new java.util.ArrayList<>();
        
        for (Good good : goods) {
            // 检查商品描述中是否包含规格关键词
            String description = good.getDescription();
            if (description != null && description.contains(standard)) {
                filteredGoods.add(good);
            }
        }
        
        log.info("规格过滤：原始 {} 个商品，过滤后 {} 个商品", goods.size(), filteredGoods.size());
        return filteredGoods;
    }
    
    /**
     * 按综合评分排序商品
     * 综合评分 = 销量分 (40%) + 评论数分 (20%) + 评分分 (40%)
     * @param goods 商品列表
     * @return 排序后的商品列表
     */
    private List<Good> sortByComprehensiveScore(List<Good> goods) {
        if (goods == null || goods.isEmpty()) {
            return goods;
        }
        
        // 为每个商品计算综合评分
        for (Good good : goods) {
            double score = calculateComprehensiveScore(good);
            // 将评分存储在 description 字段后面的临时位置（使用 price 字段存储，因为它是 exist = false）
            // 实际上我们只需要在排序时使用即可
        }
        
        // 按综合评分降序排序
        goods.sort((g1, g2) -> {
            double score1 = calculateComprehensiveScore(g1);
            double score2 = calculateComprehensiveScore(g2);
            return Double.compare(score2, score1); // 降序
        });
        
        // 加载每个商品的价格、图标和第一条五星评论
        for (Good good : goods) {
            loadGoodPrice(good);
            loadGoodIcon(good);
            loadFirstReview(good);
        }
        
        return goods;
    }
    
    /**
     * 计算商品综合评分
     * 评分构成：
     * - 销量分：40% 权重，基于销量对数计算
     * - 评论数分：20% 权重，基于评论数对数计算
     * - 评分分：40% 权重，基于平均评分
     * @param good 商品
     * @return 综合评分（0-100）
     */
    private double calculateComprehensiveScore(Good good) {
        if (good == null) {
            return 0.0;
        }
        
        // 获取各项数据
        int sales = good.getSales() != null ? good.getSales() : 0;
        int reviewCount = good.getReviewCount() != null ? good.getReviewCount() : 0;
        double rating = good.getGoodRating() != null ? good.getGoodRating().doubleValue() : 5.0;
        
        // 1. 销量分（40% 权重）- 使用对数避免销量差距过大
        // 假设最大销量为 10000，则 log10(10000) = 4
        double salesScore = Math.log10(Math.max(sales, 1)) / 4.0 * 40.0;
        
        // 2. 评论数分（20% 权重）- 使用对数避免评论数差距过大
        // 假设最大评论数为 1000，则 log10(1000) = 3
        double reviewScore = Math.log10(Math.max(reviewCount, 1)) / 3.0 * 20.0;
        
        // 3. 评分分（40% 权重）- 直接按比例计算
        // rating 范围是 1-5，转换为 0-40 分
        double ratingScore = (rating / 5.0) * 40.0;
        
        // 综合评分
        double totalScore = salesScore + reviewScore + ratingScore;
        
        log.debug("商品 {} 综合评分：{} (销量分：{}, 评论分：{}, 评分分：{})", 
                  good.getName(), totalScore, salesScore, reviewScore, ratingScore);
        
        return totalScore;
    }
    
    /**
     * 加载商品的第一条五星评论
     * @param good 商品
     */
    private void loadFirstReview(Good good) {
        if (good == null || good.getId() == null) {
            return;
        }
        
        try {
            // 查询第一条五星评论
            Review firstReview = reviewMapper.selectFirstFiveStarReview(good.getId());
            good.setFirstReview(firstReview);
        } catch (Exception e) {
            log.warn("加载商品 {} 的第一条评论失败：{}", good.getId(), e.getMessage());
        }
    }
    
    /**
     * 加载商品价格（从规格中获取最低价格）
     * @param good 商品
     */
    private void loadGoodPrice(Good good) {
        if (good == null || good.getId() == null) {
            return;
        }
        
        try {
            // 查询商品的规格列表
            List<Standard> standards = standardMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Standard>()
                    .eq("good_id", good.getId())
            );
            
            if (standards != null && !standards.isEmpty()) {
                // 找到最低价格
                BigDecimal minPrice = standards.stream()
                    .map(Standard::getPrice)
                    .filter(java.util.Objects::nonNull)
                    .min(BigDecimal::compareTo)
                    .orElse(new BigDecimal("0"));
                    
                // 应用折扣，得到真实价格（折后价）
                if (good.getDiscount() != null && good.getDiscount() > 0) {
                    minPrice = minPrice.multiply(new BigDecimal(good.getDiscount()));
                }
                
                // 设置 price 字段
                good.setPrice(minPrice);
            } else {
                // 没有规格，使用默认价格 0
                BigDecimal zero = new BigDecimal("0");
                good.setPrice(zero);
            }
        } catch (Exception e) {
            log.warn("加载商品 {} 的价格失败：{}", good.getId(), e.getMessage());
            BigDecimal zero = new BigDecimal("0");
            good.setPrice(zero);
        }
    }
    
    /**
     * 加载商品评论信息（评论数量、评分等）
     * @param good 商品
     */
    private void loadReviewInfo(Good good) {
        if (good == null || good.getId() == null) {
            return;
        }
        
        try {
            // 查询评论总数
            Long total = reviewMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Review>()
                    .eq("good_id", good.getId())
            );
            good.setReviewCount(total.intValue());
            
            // 查询平均评分
            BigDecimal avgRating = reviewMapper.selectAvgRating(good.getId());
            if (avgRating != null) {
                good.setGoodRating(avgRating);
            } else {
                good.setGoodRating(new BigDecimal("0"));
            }
            
            // 查询各评分等级的数量
            Long rating5Count = reviewMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Review>()
                    .eq("good_id", good.getId())
                    .eq("rating", 5)
            );
            Long rating4Count = reviewMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Review>()
                    .eq("good_id", good.getId())
                    .eq("rating", 4)
            );
            Long rating3Count = reviewMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Review>()
                    .eq("good_id", good.getId())
                    .eq("rating", 3)
            );
            Long rating2Count = reviewMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Review>()
                    .eq("good_id", good.getId())
                    .eq("rating", 2)
            );
            Long rating1Count = reviewMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Review>()
                    .eq("good_id", good.getId())
                    .eq("rating", 1)
            );
            
            good.setRating5Count(rating5Count.intValue());
            good.setRating4Count(rating4Count.intValue());
            good.setRating3Count(rating3Count.intValue());
            good.setRating2Count(rating2Count.intValue());
            good.setRating1Count(rating1Count.intValue());
            
            // 加载第一条五星评论
            loadFirstReview(good);
            
        } catch (Exception e) {
            log.warn("加载商品 {} 的评论信息失败：{}", good.getId(), e.getMessage());
        }
    }
    
    /**
     * 加载商品图标（如果没有图片，使用分类图标）
     * @param good 商品
     */
    private void loadGoodIcon(Good good) {
        if (good == null || good.getId() == null) {
            return;
        }
        
        try {
            // 如果商品已经有图片，不处理
            if (good.getImgs() != null && !good.getImgs().isEmpty() && !good.getImgs().startsWith("/icons/")) {
                return;
            }
            
            // 根据商品名称匹配分类图标
            String name = good.getName();
            if (name == null || name.isEmpty()) {
                return;
            }
            
            // 尝试模糊匹配分类
            CategoryIcon icon = categoryIconMapper.selectByKeyword(name);
            if (icon != null) {
                // 使用分类图标的 emoji 和背景色创建一个占位图 URL
                String emoji = icon.getIconEmoji() != null ? icon.getIconEmoji() : "📦";
                String bgColor = icon.getBgColor() != null ? icon.getBgColor() : "#f5f5f5";
                // 创建一个 data URL 或者使用 emoji 占位
                good.setImgs("emoji:" + emoji + ":" + bgColor);
                log.info("商品 {} 使用分类图标：{} {}", name, emoji, bgColor);
            } else {
                // 使用默认图标
                good.setImgs("emoji:📦:#f5f5f5");
            }
        } catch (Exception e) {
            log.warn("加载商品 {} 的图标失败：{}", good.getId(), e.getMessage());
        }
    }
    
    /**
     * 商品搜索（带参数）
     */
    public Object searchGoods(Map<String, Object> params) {
        String keyword = (String) params.get("keyword");
        log.info("商品搜索：{}", keyword);
        return search(keyword);
    }
    
    /**
     * 检查库存
     */
    public Object checkStock(Long goodId) {
        log.info("检查库存：{}", goodId);
        return "库存充足";
    }
    
    /**
     * 商品比价
     */
    public Object compareGoods(List<Long> goodIds) {
        log.info("商品比价：{}", goodIds);
        return "比价结果";
    }
    
    /**
     * 获取常见问题
     */
    public Object getFaq(String category) {
        log.info("获取常见问题：{}", category);
        return "FAQ 列表";
    }
    
    /**
     * 售后服务
     */
    public Object afterSales(Map<String, Object> params) {
        log.info("售后服务：{}", params);
        return "售后服务信息";
    }
    
    /**
     * 投诉建议
     */
    public Object complaint(Map<String, Object> params) {
        log.info("投诉建议：{}", params);
        return "提交成功";
    }
    
    /**
     * 操作指导
     */
    public Object tutorial(String topic) {
        log.info("操作指导：{}", topic);
        return "操作指导内容";
    }
    
    /**
     * 人工客服
     */
    public Object humanService(Long userId) {
        log.info("人工客服，用户 ID: {}", userId);
        return "正在转接人工客服...";
    }
    
    /**
     * 导航到指定页面
     */
    public Object navigate(String page) {
        log.info("导航到页面：{}", page);
        
        Map<String, Object> result = new HashMap<>();
        result.put("page", page);
        
        // 根据页面名称映射到路由路径
        String path = "";
        if (page.contains("首页") || page.contains("主页") || page.contains("home")) {
            path = "/topview";
        } else if (page.contains("购物车") || page.contains("cart")) {
            path = "/cart";
        } else if (page.contains("订单") || page.contains("order")) {
            path = "/orderList";
        } else if (page.contains("分类") || page.contains("category")) {
            path = "/goodList";
        } else if (page.contains("个人") || page.contains("person")) {
            path = "/person";
        } else if (page.contains("后台") || page.contains("manage")) {
            path = "/manage/home";
        } else if (page.contains("登录") || page.contains("login")) {
            path = "/login";
        } else {
            // 默认跳转到首页
            path = "/topview";
        }
        
        result.put("path", path);
        log.info("导航路径：{}", path);
        return result;
    }
    
    /**
     * 退出登录
     */
    public Object logout(Long userId) {
        log.info("退出登录，用户 ID: {}", userId);
        return "退出成功";
    }
    
    /**
     * 修改密码
     */
    public Object changePassword(Map<String, Object> params) {
        log.info("修改密码");
        return "修改成功";
    }
    
    /**
     * 收藏商品
     */
    public Object favoriteGood(Long userId, Long goodId) {
        log.info("收藏商品：用户{}, 商品{}", userId, goodId);
        return "收藏成功";
    }
    
    /**
     * 取消收藏
     */
    public Object unfavoriteGood(Long userId, Long goodId) {
        log.info("取消收藏：用户{}, 商品{}", userId, goodId);
        return "取消成功";
    }
    
    /**
     * 查询收藏夹
     */
    public Object queryFavorites(Long userId) {
        log.info("查询收藏夹，用户 ID: {}", userId);
        return "收藏夹列表";
    }
    
    /**
     * 发送验证码
     */
    public Object sendCode(String phone) {
        log.info("发送验证码：{}", phone);
        return "发送成功";
    }
    
    /**
     * 验证验证码
     */
    public Object verifyCode(String phone, String code) {
        log.info("验证验证码：{}, {}", phone, code);
        return "验证成功";
    }
    
    /**
     * 查询订单（带参数）
     */
    public Object queryOrder(Map<String, Object> params) {
        log.info("查询订单：{}", params);
        Integer userId = (Integer) params.get("userId");
        return queryOrders(userId);
    }
    
    /**
     * 查询订单详情
     */
    public Object queryOrderDetail(String orderId) {
        log.info("查询订单详情：{}", orderId);
        return "订单详情";
    }
    
    /**
     * 执行通用业务操作（供 AI 助手调用）
     * @param intentResult 意图识别结果
     * @param userId 用户 ID
     * @return 业务操作结果
     */
    public com.rabbiter.em.ai.entity.ChatResponse executeBusiness(
            com.rabbiter.em.ai.core.AiIntentResult intentResult, Long userId) {
        
        String intent = intentResult.getIntent().getCode();
        java.util.Map<String, Object> parameters = intentResult.getParameters();
        
        log.info("执行业务操作：{}, 参数：{}, 用户 ID: {}", intent, parameters, userId);
        
        try {
            Object result = null;
            String aiResponse = "";
            
            // 根据意图执行相应的业务操作
            switch (intent) {
                case "search_goods":
                    String keyword = parameters != null ? (String) parameters.get("keyword") : null;
                    if (keyword == null || keyword.isEmpty()) {
                        keyword = "热门商品";
                    }
                    List<Good> searchGoodsList = (List<Good>) search(keyword);
                    
                    if (searchGoodsList != null && !searchGoodsList.isEmpty()) {
                        Good topGood = searchGoodsList.get(0);
                        
                        // 加载第一条五星评论
                        loadFirstReview(topGood);
                        
                        // 构建{topGood, otherGoods}格式的结果（不自动生成销售报告和词云）
                        Map<String, Object> topGoodData = new HashMap<>();
                        topGoodData.put("id", topGood.getId());
                        topGoodData.put("name", topGood.getName());
                        topGoodData.put("sales", topGood.getSales());
                        topGoodData.put("price", topGood.getPrice());
                        topGoodData.put("imgs", topGood.getImgs());
                        topGoodData.put("image", topGood.getImgs());
                        topGoodData.put("description", topGood.getDescription());
                        topGoodData.put("discount", topGood.getDiscount());
                        topGoodData.put("reviewCount", topGood.getReviewCount());
                        topGoodData.put("goodRating", topGood.getGoodRating());
                        if (topGood.getTags() != null) {
                            topGoodData.put("tags", topGood.getTags());
                        }
                        if (topGood.getFirstReview() != null) {
                            topGoodData.put("firstReview", topGood.getFirstReview());
                        }
                        
                        Map<String, Object> searchResult = new HashMap<>();
                        searchResult.put("topGood", topGoodData);
                        
                        List<Map<String, Object>> otherGoodsData = new ArrayList<>();
                        if (searchGoodsList.size() > 1) {
                            for (int i = 1; i < searchGoodsList.size(); i++) {
                                Good g = searchGoodsList.get(i);
                                Map<String, Object> goodMap = new HashMap<>();
                                goodMap.put("id", g.getId());
                                goodMap.put("name", g.getName());
                                goodMap.put("sales", g.getSales());
                                goodMap.put("price", g.getPrice());
                                goodMap.put("imgs", g.getImgs());
                                goodMap.put("image", g.getImgs());
                                goodMap.put("description", g.getDescription());
                                goodMap.put("discount", g.getDiscount());
                                goodMap.put("reviewCount", g.getReviewCount());
                                goodMap.put("goodRating", g.getGoodRating());
                                if (g.getTags() != null) {
                                    goodMap.put("tags", g.getTags());
                                }
                                otherGoodsData.add(goodMap);
                            }
                        }
                        searchResult.put("otherGoods", otherGoodsData);
                        searchResult.put("keyword", keyword);
                        searchResult.put("count", searchGoodsList.size());
                        result = searchResult;
                        
                        // 构建AI回复消息，包含所有商品ID，方便后续"第x个"功能使用
                        StringBuilder searchResponse = new StringBuilder();
                        searchResponse.append("已为您找到").append(searchGoodsList.size()).append("个相关商品：");
                        for (int i = 0; i < searchGoodsList.size() && i < 10; i++) {
                            Good g = searchGoodsList.get(i);
                            searchResponse.append("\n").append(i + 1).append(". ").append(g.getName());
                            searchResponse.append(" - 商品ID:").append(g.getId());
                        }
                        searchResponse.append("\n请查看搜索结果，回复\"第x个\"可查看详情。");
                        aiResponse = searchResponse.toString();
                        
                        log.info("搜索结果 - topGood: {}, otherGoods: {}, wordCloudData: {}",
                            topGoodData.get("name"),
                            ((List<?>) searchResult.get("otherGoods")).size(),
                            topGoodData.get("wordCloudData") != null ? ((List<?>) topGoodData.get("wordCloudData")).size() : 0);
                    }
                    break;
                    
                case "view_good_detail":
                    // 处理查看商品详情 - 调用分析商品报告方法 + 评论舆情分析
                    Long viewGoodId = parameters != null ? (Long) parameters.get("goodId") : null;
                    
                    // 如果没有 goodId，但有 index，从上下文中获取商品 ID
                    if (viewGoodId == null && parameters != null && parameters.containsKey("index")) {
                        int index = (Integer) parameters.get("index");
                        viewGoodId = getGoodIdFromContextByIndex(userId, index);
                        if (viewGoodId != null) {
                            log.info("从上下文中获取第 {} 个商品的 ID: {}", index, viewGoodId);
                        }
                    }
                    
                    if (viewGoodId != null) {
                        // 调用分析商品报告方法
                        Map<String, Object> salesReport = generateSalesReport(viewGoodId, 30);
                        log.info("销售报告数据 - salesTrend: {}, totalSales: {}, totalRevenue: {}, targetGood: {}",
                            salesReport.get("salesTrend") != null ? ((List<?>) salesReport.get("salesTrend")).size() : 0,
                            salesReport.get("totalSales"),
                            salesReport.get("totalRevenue"),
                            salesReport.get("targetGood") != null ? "存在" : "不存在");
                        
                        // 调用评论舆情分析方法
                        Map<String, Object> sentimentAnalysis = analyzeReviewSentiment(viewGoodId);
                        log.info("舆情分析数据 - sentimentDistribution: {}, wordCloudData: {}, ratingStats: {}, totalReviews: {}",
                            sentimentAnalysis.get("sentimentDistribution"),
                            sentimentAnalysis.get("wordCloudData") != null ? ((List<?>) sentimentAnalysis.get("wordCloudData")).size() : 0,
                            sentimentAnalysis.get("ratingStats") != null ? ((List<?>) sentimentAnalysis.get("ratingStats")).size() : 0,
                            sentimentAnalysis.get("totalReviews"));
                        
                        // 合并两个结果
                        Map<String, Object> combinedResult = new HashMap<>(salesReport);
                        combinedResult.putAll(sentimentAnalysis);
                        result = combinedResult;
                        
                        log.info("合并后的数据 - wordCloudData: {}, sentimentDistribution: {}, ratingStats: {}",
                            combinedResult.get("wordCloudData") != null ? ((List<?>) combinedResult.get("wordCloudData")).size() : 0,
                            combinedResult.get("sentimentDistribution"),
                            combinedResult.get("ratingStats") != null ? ((List<?>) combinedResult.get("ratingStats")).size() : 0);
                        
                        aiResponse = "已为您生成该商品的销售报告和评论舆情分析，请查看分析结果。";
                    } else {
                        aiResponse = "请问您想查看哪个商品的详情呢？";
                    }
                    break;
                    
                case "add_to_cart":
                    // 处理添加到购物车
                    Long cartGoodId = parameters != null ? (Long) parameters.get("goodId") : null;
                    
                    // 优先使用 index 从上下文获取商品 ID（"第x个"场景）
                    // 注意：index 必须优先于 name，因为 DeepSeek 可能把"第一个"提取为 name
                    // 导致搜索到名字就叫"第一个"的商品，而不是推荐列表中的第1个
                    if (cartGoodId == null && parameters != null && parameters.containsKey("index")) {
                        int index = (Integer) parameters.get("index");
                        cartGoodId = getGoodIdFromContextByIndex(userId, index);
                        if (cartGoodId != null) {
                            log.info("从上下文中获取第 {} 个商品的 ID: {}", index, cartGoodId);
                            parameters.put("goodId", cartGoodId);
                        }
                    }
                    
                    // 如果没有 goodId 也没有 index（或 index 未找到），但有商品名称，通过搜索获取商品 ID
                    if (cartGoodId == null && parameters != null && parameters.containsKey("name")) {
                        String productName = (String) parameters.get("name");
                        log.info("通过名称搜索商品以加入购物车：{}", productName);
                        Object searchResult = search(productName);
                        if (searchResult instanceof List) {
                            List<Good> goods = (List<Good>) searchResult;
                            if (goods != null && !goods.isEmpty()) {
                                cartGoodId = goods.get(0).getId();
                                log.info("通过名称搜索到商品：{} -> ID: {}", productName, cartGoodId);
                                parameters.put("goodId", cartGoodId);
                            }
                        }
                    }
                    
                    parameters.put("userId", userId);
                    result = addCart(parameters);
                    aiResponse = "商品已成功添加到购物车！";
                    break;
                    
                case "quick_order":
                    // 处理快速下单
                    Long orderGoodId = parameters != null ? (Long) parameters.get("goodId") : null;
                    
                    // 如果没有 goodId，但有 index，从上下文中获取商品 ID
                    if (orderGoodId == null && parameters != null && parameters.containsKey("index")) {
                        int index = (Integer) parameters.get("index");
                        orderGoodId = getGoodIdFromContextByIndex(userId, index);
                        if (orderGoodId != null) {
                            log.info("从上下文中获取第 {} 个商品的 ID: {}", index, orderGoodId);
                            parameters.put("goodId", orderGoodId);
                        }
                    }
                    
                    result = quickOrder(parameters);
                    aiResponse = "订单已快速生成，请确认支付。";
                    break;
                    
                case "batch_add_cart":
                    result = batchAddCart((java.util.List<java.util.Map<String, Object>>) parameters.get("items"));
                    aiResponse = "商品已批量添加到购物车！";
                    break;
                    
                case "query_order":
                    result = queryOrders(userId != null ? userId.intValue() : 0);
                    aiResponse = "这是您的订单列表，请查看。";
                    break;
                    
                case "navigate":
                    String page = parameters != null ? (String) parameters.get("page") : "首页";
                    result = navigate(page);
                    aiResponse = "正在跳转到" + page;
                    break;
                    
                case "logout":
                    result = logout(userId);
                    aiResponse = "您已成功退出登录。";
                    break;
                    
                case "recommend_goods":
                    List<Good> recommendGoodsList = (List<Good>) getPersonalizedRecommendation(userId);
                    
                    if (recommendGoodsList != null && !recommendGoodsList.isEmpty()) {
                        Good topGood = recommendGoodsList.get(0);
                        
                        // 加载第一条五星评论
                        loadFirstReview(topGood);
                        
                        Map<String, Object> topGoodData = new HashMap<>();
                        topGoodData.put("id", topGood.getId());
                        topGoodData.put("name", topGood.getName());
                        topGoodData.put("sales", topGood.getSales());
                        topGoodData.put("price", topGood.getPrice());
                        topGoodData.put("imgs", topGood.getImgs());
                        topGoodData.put("image", topGood.getImgs());
                        topGoodData.put("description", topGood.getDescription());
                        topGoodData.put("discount", topGood.getDiscount());
                        topGoodData.put("reviewCount", topGood.getReviewCount());
                        topGoodData.put("goodRating", topGood.getGoodRating());
                        if (topGood.getTags() != null) {
                            topGoodData.put("tags", topGood.getTags());
                        }
                        if (topGood.getFirstReview() != null) {
                            topGoodData.put("firstReview", topGood.getFirstReview());
                        }
                        
                        Map<String, Object> recommendResult = new HashMap<>();
                        recommendResult.put("topGood", topGoodData);
                        
                        List<Map<String, Object>> otherGoodsData = new ArrayList<>();
                        if (recommendGoodsList.size() > 1) {
                            for (int i = 1; i < recommendGoodsList.size(); i++) {
                                Good g = recommendGoodsList.get(i);
                                Map<String, Object> goodMap = new HashMap<>();
                                goodMap.put("id", g.getId());
                                goodMap.put("name", g.getName());
                                goodMap.put("sales", g.getSales());
                                goodMap.put("price", g.getPrice());
                                goodMap.put("imgs", g.getImgs());
                                goodMap.put("image", g.getImgs());
                                goodMap.put("description", g.getDescription());
                                goodMap.put("discount", g.getDiscount());
                                goodMap.put("reviewCount", g.getReviewCount());
                                goodMap.put("goodRating", g.getGoodRating());
                                if (g.getTags() != null) {
                                    goodMap.put("tags", g.getTags());
                                }
                                otherGoodsData.add(goodMap);
                            }
                        }
                        recommendResult.put("otherGoods", otherGoodsData);
                        recommendResult.put("count", recommendGoodsList.size());
                        result = recommendResult;
                        
                        // 构建AI回复消息，包含所有商品ID，方便后续"第x个"功能使用
                        StringBuilder recommendResponse = new StringBuilder();
                        recommendResponse.append("根据您的喜好，为您推荐").append(recommendGoodsList.size()).append("个商品：");
                        for (int i = 0; i < recommendGoodsList.size() && i < 10; i++) {
                            Good g = recommendGoodsList.get(i);
                            recommendResponse.append("\n").append(i + 1).append(". ").append(g.getName());
                            recommendResponse.append(" - 商品ID:").append(g.getId());
                        }
                        recommendResponse.append("\n请查看推荐商品，回复\"第x个\"可查看详情。");
                        aiResponse = recommendResponse.toString();
                    } else {
                        result = new HashMap<>();
                        ((Map<String, Object>) result).put("topGood", null);
                        ((Map<String, Object>) result).put("otherGoods", new ArrayList<>());
                        ((Map<String, Object>) result).put("count", 0);
                        aiResponse = "抱歉，暂时没有为您推荐的商品。";
                    }
                    break;
                    
                case "specific_recommend":
                    // 具体品类推荐：使用独立的推荐函数，支持价格限定、规格限定和排序方式
                    String category = (String) parameters.get("requirement");
                    Double minPrice = parameters != null ? (Double) parameters.get("minPrice") : null;
                    Double maxPrice = parameters != null ? (Double) parameters.get("maxPrice") : null;
                    String standard = (String) parameters.get("standard");
                    String sortBy = parameters != null ? (String) parameters.get("sortBy") : "default";
                    
                    log.info("具体品类推荐参数 - 品类：{}, 价格范围：{}-{}, 规格：{}, 排序：{}", 
                             category, minPrice, maxPrice, standard, sortBy);
                    
                    Map<String, Object> recommendResult = specificCategoryRecommend(
                        userId, category, minPrice, maxPrice, standard, sortBy);
                    
                    // 确保topGood为Map格式
                    if (recommendResult != null && recommendResult.get("topGood") != null) {
                        Object topGoodObj = recommendResult.get("topGood");
                        Map<String, Object> topGoodData;
                        
                        if (topGoodObj instanceof Good) {
                            Good topGood = (Good) topGoodObj;
                            loadFirstReview(topGood);
                            topGoodData = new HashMap<>();
                            topGoodData.put("id", topGood.getId());
                            topGoodData.put("name", topGood.getName());
                            topGoodData.put("sales", topGood.getSales());
                            topGoodData.put("price", topGood.getPrice());
                            topGoodData.put("imgs", topGood.getImgs());
                            topGoodData.put("image", topGood.getImgs());
                            topGoodData.put("description", topGood.getDescription());
                            topGoodData.put("discount", topGood.getDiscount());
                            topGoodData.put("reviewCount", topGood.getReviewCount());
                            topGoodData.put("goodRating", topGood.getGoodRating());
                            if (topGood.getTags() != null) {
                                topGoodData.put("tags", topGood.getTags());
                            }
                            if (topGood.getFirstReview() != null) {
                                topGoodData.put("firstReview", topGood.getFirstReview());
                            }
                        } else if (topGoodObj instanceof Map) {
                            topGoodData = new HashMap<>((Map<String, Object>) topGoodObj);
                            Long topGoodId = null;
                            Object idObj = topGoodData.get("id");
                            if (idObj instanceof Long) {
                                topGoodId = (Long) idObj;
                            } else if (idObj instanceof Integer) {
                                topGoodId = ((Integer) idObj).longValue();
                            }
                            if (topGoodId != null && !topGoodData.containsKey("firstReview")) {
                                try {
                                    Review firstReview = reviewMapper.selectFirstFiveStarReview(topGoodId);
                                    if (firstReview != null) {
                                        topGoodData.put("firstReview", firstReview);
                                    }
                                } catch (Exception e) {
                                    log.warn("加载具体品类推荐topGood的第一条评论失败：{}", e.getMessage());
                                }
                            }
                        } else {
                            topGoodData = new HashMap<>();
                        }
                        
                        recommendResult.put("topGood", topGoodData);
                        
                        // 构建AI回复消息，包含所有商品ID，方便后续"第x个"功能使用
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> otherGoodsList = (List<Map<String, Object>>) recommendResult.get("otherGoods");
                        int totalCount = otherGoodsList != null ? otherGoodsList.size() + 1 : 1;
                        
                        StringBuilder specificResponse = new StringBuilder();
                        specificResponse.append("已为您找到").append(totalCount).append("个").append(category).append("相关商品：");
                        specificResponse.append("\n1. ").append(recommendResult.get("topGood") instanceof Map ? 
                            ((Map<String, Object>) recommendResult.get("topGood")).get("name") : "首推商品");
                        specificResponse.append(" - 商品ID:").append(recommendResult.get("topGood") instanceof Map ? 
                            ((Map<String, Object>) recommendResult.get("topGood")).get("id") : "");
                        
                        if (otherGoodsList != null) {
                            for (int i = 0; i < otherGoodsList.size() && i < 9; i++) {
                                Map<String, Object> g = otherGoodsList.get(i);
                                specificResponse.append("\n").append(i + 2).append(". ").append(g.get("name"));
                                specificResponse.append(" - 商品ID:").append(g.get("id"));
                            }
                        }
                        specificResponse.append("\n请查看推荐商品，回复\"第x个\"可查看详情。");
                        aiResponse = specificResponse.toString();
                    } else {
                        // 没有找到商品的情况
                        aiResponse = "抱歉，没有找到" + category + "相关的商品。";
                    }
                    
                    // 返回完整结果（包含 topGood、otherGoods、thinkingSteps 等）
                    result = recommendResult;
                    break;
                    
                case "track_order":
                    String orderId = parameters != null ? (String) parameters.get("orderId") : null;
                    result = trackOrderStatus(orderId != null ? orderId : "最新订单", userId);
                    aiResponse = "这是您的订单物流信息。";
                    break;
                    
                case "analyze_orders":
                    result = analyzeOrderHistory(userId);
                    aiResponse = "这是您的订单分析报告。";
                    break;
                    
                case "analyze_sentiment":
                    Long sentimentGoodId = parameters != null ? (Long) parameters.get("goodId") : null;
                    // 如果没有 goodId，但有 index，从上下文中获取商品 ID
                    if (sentimentGoodId == null && parameters != null && parameters.containsKey("index")) {
                        int index = (Integer) parameters.get("index");
                        sentimentGoodId = getGoodIdFromContextByIndex(userId, index);
                        if (sentimentGoodId != null) {
                            log.info("从上下文中获取第 {} 个商品的 ID: {}", index, sentimentGoodId);
                        }
                    }
                    result = analyzeReviewSentiment(sentimentGoodId);
                    aiResponse = sentimentGoodId != null ? 
                        "已为您分析该商品的评论舆情，请查看分析结果。" : 
                        "已为您分析所有商品的评论舆情，请查看分析结果。";
                    break;
                    
                case "analyze_sales":
                    Long salesGoodId = parameters != null ? (Long) parameters.get("goodId") : null;
                    // 如果没有 goodId，但有 index，从上下文中获取商品 ID
                    if (salesGoodId == null && parameters != null && parameters.containsKey("index")) {
                        int index = (Integer) parameters.get("index");
                        salesGoodId = getGoodIdFromContextByIndex(userId, index);
                        if (salesGoodId != null) {
                            log.info("从上下文中获取第 {} 个商品的 ID: {}", index, salesGoodId);
                        }
                    }
                    Integer days = parameters != null ? (Integer) parameters.get("days") : 30;
                    result = generateSalesReport(salesGoodId, days);
                    aiResponse = salesGoodId != null ? 
                        "已为您生成该商品的销售报告，请查看分析结果。" : 
                        "已为您生成销售数据报告，请查看分析结果。";
                    break;
                    
                default:
                    aiResponse = "抱歉，我暂时无法处理该请求。";
            }
            
            // 构建响应
            com.rabbiter.em.ai.entity.ChatResponse response = new com.rabbiter.em.ai.entity.ChatResponse();
            response.setSuccess(true);
            response.setMessage(aiResponse);
            response.setAction(intent);
            
            // 设置业务数据
            if (result != null) {
                java.util.Map<String, Object> actionData = new java.util.HashMap<>();
                
                // 添加后端返回的aiResponse消息
                actionData.put("message", aiResponse);
                
                // 如果是具体品类推荐的结果（包含 topGood、otherGoods 等），直接合并到 actionData
                if (result instanceof java.util.Map) {
                    java.util.Map<?, ?> resultMap = (java.util.Map<?, ?>) result;
                    // 检查是否包含 topGood 字段（新格式）、wordCloudData 字段（舆情分析）、orders 字段（订单分析）、salesTrend 字段（销售报告）、path 字段（导航）或 goodId 字段（快速下单）
                    if (resultMap.containsKey("topGood") || resultMap.containsKey("wordCloudData") || resultMap.containsKey("orders") || resultMap.containsKey("salesTrend") || resultMap.containsKey("path") || resultMap.containsKey("goodId")) {
                        // 直接复制所有字段到 actionData
                        for (java.util.Map.Entry<?, ?> entry : resultMap.entrySet()) {
                            actionData.put(String.valueOf(entry.getKey()), entry.getValue());
                        }
                    } else {
                        // 旧格式，保持原有逻辑
                        actionData.put("items", result);
                        actionData.put("recommendations", result);
                    }
                } else {
                    // 其他类型结果，保持原有逻辑
                    actionData.put("items", result);
                    actionData.put("recommendations", result);
                }
                
                // 对于搜索场景，添加 goods 字段（前端期望的字段名）
                if ("search_goods".equals(intent)) {
                    actionData.put("goods", result);
                    // 添加 keyword 字段
                    if (parameters != null) {
                        String keyword = (String) parameters.get("keyword");
                        if (keyword != null && !keyword.isEmpty()) {
                            actionData.put("keyword", keyword);
                        }
                    }
                }
                // 对于推荐场景，从 topGood/otherGoods 构建正确的 List<Good>
                if ("recommend_goods".equals(intent) || "specific_recommend".equals(intent)) {
                    java.util.List<Object> goodsList = new java.util.ArrayList<>();
                    Object topGood = actionData.get("topGood");
                    if (topGood != null) {
                        goodsList.add(topGood);
                    }
                    Object otherGoods = actionData.get("otherGoods");
                    if (otherGoods instanceof java.util.List) {
                        goodsList.addAll((java.util.List<?>) otherGoods);
                    }
                    if (!goodsList.isEmpty()) {
                        actionData.put("goods", goodsList);
                    }
                }
                
                response.setActionData(actionData);
            }
            
            return response;
            
        } catch (Exception e) {
            log.error("执行业务操作失败：{}", e.getMessage(), e);
            com.rabbiter.em.ai.entity.ChatResponse errorResponse = new com.rabbiter.em.ai.entity.ChatResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("抱歉，处理您的请求时出现错误");
            errorResponse.setAction("error");
            return errorResponse;
        }
    }
    
    /**
     * 评论舆情AI分析 - 分析商品评论的情感倾向、热门标签、评分分布
     * @param goodId 商品ID（可选，不传则分析所有商品）
     * @return 舆情分析结果
     */
    public Map<String, Object> analyzeReviewSentiment(Long goodId) {
        log.info("评论舆情分析，商品ID: {}", goodId);
        
        try {
            Map<String, Object> result = new HashMap<>();
            
            // 1. 获取评论统计
            List<Map<String, Object>> ratingStats;
            if (goodId != null) {
                ratingStats = reviewMapper.selectRatingDistribution(goodId);
            } else {
                // 所有商品的评论统计
                ratingStats = new ArrayList<>();
            }
            
            // 2. 获取热门评论标签
            List<String> hotTags = new ArrayList<>();
            if (goodId != null) {
                // 获取该商品的热门标签
                Good good = goodMapper.selectById(goodId);
                if (good != null && good.getTags() != null) {
                    hotTags = Arrays.asList(good.getTags().split(","));
                }
            }
            
            // 3. 获取前50条评论用于词云分析和情感计算
            List<Review> reviewsForWordCloud = new ArrayList<>();
            List<Map<String, Object>> wordCloudData = new ArrayList<>();
            if (goodId != null) {
                reviewsForWordCloud = reviewMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Review>()
                        .eq("good_id", goodId)
                        .eq("status", 1)
                        .orderByDesc("create_time")
                        .last("LIMIT 50")
                );
                
                log.info("获取到 {} 条评论用于词云分析", reviewsForWordCloud != null ? reviewsForWordCloud.size() : 0);
                
                // 调用Python脚本生成词云数据
                if (reviewsForWordCloud != null && !reviewsForWordCloud.isEmpty()) {
                    wordCloudData = generateWordCloudFromPython(reviewsForWordCloud);
                    log.info("词云数据生成完成，共 {} 个词", wordCloudData.size());
                } else {
                    log.warn("没有评论数据，无法生成词云");
                }
            }
            
            // 4. 获取评论情感分布（基于实际评分动态计算）
            Map<String, Integer> sentimentDistribution = new HashMap<>();
            if (goodId != null && reviewsForWordCloud != null && !reviewsForWordCloud.isEmpty()) {
                // 根据评论的评分计算情感分布
                int positive = 0;
                int neutral = 0;
                int negative = 0;
                
                for (Review review : reviewsForWordCloud) {
                    if (review.getRating() != null) {
                        if (review.getRating() >= 4) {
                            positive++;
                        } else if (review.getRating() == 3) {
                            neutral++;
                        } else {
                            negative++;
                        }
                    }
                }
                
                // 计算百分比
                int total = positive + neutral + negative;
                if (total > 0) {
                    sentimentDistribution.put("positive", Math.round((float) positive / total * 100));
                    sentimentDistribution.put("neutral", Math.round((float) neutral / total * 100));
                    sentimentDistribution.put("negative", Math.round((float) negative / total * 100));
                } else {
                    sentimentDistribution.put("positive", 0);
                    sentimentDistribution.put("neutral", 0);
                    sentimentDistribution.put("negative", 0);
                }
            } else {
                sentimentDistribution.put("positive", 0);
                sentimentDistribution.put("neutral", 0);
                sentimentDistribution.put("negative", 0);
            }
            
            // 5. 获取最新评论
            List<Review> latestReviews = new ArrayList<>();
            if (goodId != null) {
                latestReviews = reviewMapper.selectLatestReviewsWithUser(goodId, 1, 10);
            }
            
            result.put("ratingStats", ratingStats);
            result.put("hotTags", hotTags);
            result.put("sentimentDistribution", sentimentDistribution);
            result.put("latestReviews", latestReviews);
            result.put("wordCloudData", wordCloudData);
            result.put("totalReviews", goodId != null ? 
                reviewMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Review>()
                    .eq("good_id", goodId).eq("status", 1)) : 0);
            
            log.info("舆情分析结果 - 评分统计: {}, 热门标签: {}, 情感分布: {}, 最新评论: {}, 词云数据: {}, 总评论数: {}",
                ratingStats != null ? ratingStats.size() : 0,
                hotTags != null ? hotTags.size() : 0,
                sentimentDistribution,
                latestReviews != null ? latestReviews.size() : 0,
                wordCloudData != null ? wordCloudData.size() : 0,
                goodId != null ? reviewMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Review>()
                    .eq("good_id", goodId).eq("status", 1)) : 0);
            
            return result;
        } catch (Exception e) {
            log.error("评论舆情分析失败：{}", e.getMessage());
            return new HashMap<>();
        }
    }
    
    /**
     * 商品销售数据报告 - 生成销售分析报告和走势图表数据
     * @param goodId 商品ID（可选）
     * @param days 天数（默认30天）
     * @return 销售报告
     */
    public Map<String, Object> generateSalesReport(Long goodId, Integer days) {
        log.info("生成销售报告，商品ID: {}, 天数: {}", goodId, days);
        
        try {
            if (days == null) days = 30;
            
            Map<String, Object> result = new HashMap<>();
            
            // 1. 获取销售统计数据 - 无论是否指定商品，都获取热销商品用于排行
            List<Good> allGoods = goodMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Good>()
                    .eq("is_delete", false)
                    .orderByDesc("sales")
                    .last("LIMIT 20")
            );
            
            // 如果指定了商品ID，获取该商品的详细信息
            Good targetGood = null;
            BigDecimal targetPrice = null;
            if (goodId != null) {
                targetGood = goodMapper.selectById(goodId);
                
                // 获取商品的第一个规格价格作为单价
                if (targetGood != null) {
                    com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Standard> standardQuery = 
                        new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
                    standardQuery.eq("good_id", goodId);
                    standardQuery.last("LIMIT 1");
                    Standard firstStandard = standardMapper.selectOne(standardQuery);
                    if (firstStandard != null) {
                        targetPrice = firstStandard.getPrice();
                    }
                }
            }
            
            // 2. 构建销售趋势数据
            List<Map<String, Object>> salesTrend = new ArrayList<>();
            LocalDate endDate = LocalDate.now();
            
            // 如果指定了商品，使用真实的订单数据构建趋势
            if (targetGood != null) {
                // 查询该商品的真实销售记录（基于订单数据）
                List<Map<String, Object>> realSalesData = orderMapper.selectSalesTrendByGoodId(
                    goodId, endDate.minusDays(days), endDate);
                
                // 如果有真实数据，则使用真实数据；否则基于商品的总销量生成合理数据
                if (realSalesData != null && !realSalesData.isEmpty()) {
                    // 使用真实订单数据
                    for (Map<String, Object> record : realSalesData) {
                        Map<String, Object> dayData = new HashMap<>();
                        dayData.put("date", record.get("date"));
                        dayData.put("sales", record.get("sales"));
                        dayData.put("revenue", record.get("revenue"));
                        salesTrend.add(dayData);
                    }
                } else {
                    // 没有订单数据，基于商品总销量分配到各天
                    int totalSales = targetGood.getSales() > 0 ? targetGood.getSales() : 0;
                    BigDecimal price = targetPrice != null ? targetPrice : new BigDecimal("100");
                    // 应用折扣到单价
                    if (targetGood.getDiscount() != null) {
                        price = price.multiply(new BigDecimal(targetGood.getDiscount()));
                    }
                    
                    for (int i = 0; i < days; i++) {
                        LocalDate date = endDate.minusDays(days - 1 - i); // 从最早的日期开始
                        Map<String, Object> dayData = new HashMap<>();
                        dayData.put("date", date.toString());
                        
                        // 均匀分配销量到每天
                        int dailySales = totalSales > 0 ? Math.max(1, totalSales / days) : 0;
                        if (i == days - 1 && totalSales > 0) {
                            // 最后一天补足剩余销量
                            int distributedSales = (totalSales / days) * (days - 1);
                            dailySales = totalSales - distributedSales;
                        }
                        
                        dayData.put("sales", dailySales);
                        dayData.put("revenue", new BigDecimal(dailySales).multiply(price).doubleValue());
                        salesTrend.add(dayData);
                    }
                }
            } else {
                // 没有指定商品，获取最近30天的总体销售趋势
                List<Map<String, Object>> realSalesData = orderMapper.selectAllSalesTrend(
                    endDate.minusDays(days), endDate);
                
                if (realSalesData != null && !realSalesData.isEmpty()) {
                    for (Map<String, Object> record : realSalesData) {
                        Map<String, Object> dayData = new HashMap<>();
                        dayData.put("date", record.get("date"));
                        dayData.put("sales", record.get("sales"));
                        dayData.put("revenue", record.get("revenue"));
                        salesTrend.add(dayData);
                    }
                } else {
                    // 没有订单数据，使用基础数据
                    for (int i = 0; i < days; i++) {
                        LocalDate date = endDate.minusDays(days - 1 - i);
                        Map<String, Object> dayData = new HashMap<>();
                        dayData.put("date", date.toString());
                        dayData.put("sales", 0);
                        dayData.put("revenue", 0.0);
                        salesTrend.add(dayData);
                    }
                }
            }
            
            // 3. 计算总体统计 - 从salesTrend中计算真实的销售额
            double totalSalesFromTrend = salesTrend.stream()
                .mapToDouble(d -> ((Number) d.get("sales")).doubleValue())
                .sum();
            double totalRevenueFromTrend = salesTrend.stream()
                .mapToDouble(d -> ((Number) d.get("revenue")).doubleValue())
                .sum();
            
            // 总销售额始终从趋势数据计算（已包含折扣），更准确
            double totalSales = targetGood != null ? totalSalesFromTrend : allGoods.stream().mapToInt(Good::getSales).sum();
            double totalRevenue = totalRevenueFromTrend;
            
            // 4. 商品销售排行 - 始终使用所有商品的数据
            List<Map<String, Object>> salesRank = allGoods.stream()
                .sorted((a, b) -> Integer.compare(b.getSales(), a.getSales()))
                .limit(10)
                .map(g -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", g.getId());
                    item.put("name", g.getName());
                    item.put("sales", g.getSales());
                    BigDecimal saleMoney = g.getSaleMoney() != null ? g.getSaleMoney() : BigDecimal.ZERO;
                    if (g.getDiscount() != null && g.getDiscount() > 0) {
                        saleMoney = saleMoney.multiply(new BigDecimal(g.getDiscount()));
                    }
                    item.put("revenue", saleMoney);
                    item.put("image", g.getImgs());
                    return item;
                })
                .collect(Collectors.toList());
            
            result.put("salesTrend", salesTrend);
            result.put("totalSales", totalSales);
            result.put("totalRevenue", totalRevenue);
            result.put("salesRank", salesRank);
            result.put("goodsCount", allGoods.size());
            result.put("days", days);
            
            // 如果指定了商品，添加该商品的详细信息（包括评论和词云）
            if (targetGood != null) {
                Map<String, Object> goodInfo = new HashMap<>();
                goodInfo.put("id", targetGood.getId());
                goodInfo.put("name", targetGood.getName());
                goodInfo.put("sales", targetGood.getSales());
                BigDecimal displayPrice = targetPrice;
                if (displayPrice != null && targetGood.getDiscount() != null && targetGood.getDiscount() > 0) {
                    displayPrice = displayPrice.multiply(new BigDecimal(targetGood.getDiscount()));
                }
                goodInfo.put("price", displayPrice);
                goodInfo.put("image", targetGood.getImgs());
                goodInfo.put("description", targetGood.getDescription());
                goodInfo.put("discount", targetGood.getDiscount());
                goodInfo.put("reviewCount", targetGood.getReviewCount());
                goodInfo.put("goodRating", targetGood.getGoodRating());
                goodInfo.put("rating5Count", targetGood.getRating5Count());
                goodInfo.put("rating4Count", targetGood.getRating4Count());
                goodInfo.put("rating3Count", targetGood.getRating3Count());
                goodInfo.put("rating2Count", targetGood.getRating2Count());
                goodInfo.put("rating1Count", targetGood.getRating1Count());
                goodInfo.put("tags", targetGood.getTags());
                
                // 添加评分分布数据（用于词云图）
                List<Map<String, Object>> ratingDistribution = new ArrayList<>();
                if (targetGood.getRating5Count() != null) {
                    ratingDistribution.add(createRatingItem("5星", targetGood.getRating5Count()));
                }
                if (targetGood.getRating4Count() != null) {
                    ratingDistribution.add(createRatingItem("4星", targetGood.getRating4Count()));
                }
                if (targetGood.getRating3Count() != null) {
                    ratingDistribution.add(createRatingItem("3星", targetGood.getRating3Count()));
                }
                if (targetGood.getRating2Count() != null) {
                    ratingDistribution.add(createRatingItem("2星", targetGood.getRating2Count()));
                }
                if (targetGood.getRating1Count() != null) {
                    ratingDistribution.add(createRatingItem("1星", targetGood.getRating1Count()));
                }
                goodInfo.put("ratingDistribution", ratingDistribution);
                
                // 添加词云数据（从评论中生成）- 同时放在targetGood和顶层
                List<Map<String, Object>> wordCloudData = new ArrayList<>();
                if (goodId != null) {
                    // 获取该商品的评论用于词云分析
                    List<Review> reviewsForWordCloud = reviewMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Review>()
                            .eq("good_id", goodId)
                            .eq("status", 1)
                            .orderByDesc("create_time")
                            .last("LIMIT 50")
                    );
                    
                    if (reviewsForWordCloud != null && !reviewsForWordCloud.isEmpty()) {
                        wordCloudData = generateWordCloudFromPython(reviewsForWordCloud);
                        log.info("销售报告词云数据生成完成，共 {} 个词", wordCloudData.size());
                    }
                }
                goodInfo.put("wordCloudData", wordCloudData);
                
                // 同时将词云数据放在顶层，方便前端直接使用
                result.put("wordCloudData", wordCloudData);
                
                result.put("targetGood", goodInfo);
            }
            
            return result;
        } catch (Exception e) {
            log.error("生成销售报告失败：{}", e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    /**
     * 创建评分分布项
     */
    private Map<String, Object> createRatingItem(String name, Integer count) {
        Map<String, Object> item = new HashMap<>();
        item.put("name", name);
        item.put("count", count);
        return item;
    }
    
    /**
     * 用户个人销售数据分析 - 分析用户的购买行为、偏好和消费趋势
     * @param userId 用户ID
     * @param days 分析天数（默认30天）
     * @return 用户销售数据分析结果
     */
    public Map<String, Object> analyzeUserSalesData(Long userId, Integer days) {
        log.info("分析用户个人销售数据，用户ID: {}, 天数: {}", userId, days);
        
        try {
            if (days == null) days = 30;
            
            Map<String, Object> result = new HashMap<>();
            
            // 1. 获取用户订单数据
            List<com.rabbiter.em.entity.Order> orders = orderMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.rabbiter.em.entity.Order>()
                    .eq("user_id", userId)
                    .orderByDesc("create_time")
                    .last("LIMIT 100")
            );
            
            log.info("获取到用户 {} 的 {} 个订单", userId, orders != null ? orders.size() : 0);
            
            // 2. 统计用户消费数据
            double totalSpent = 0;
            int totalOrders = orders != null ? orders.size() : 0;
            Map<String, Integer> categoryStats = new HashMap<>();
            List<Map<String, Object>> monthlySpending = new ArrayList<>();
            
            if (orders != null && !orders.isEmpty()) {
                for (com.rabbiter.em.entity.Order order : orders) {
                    if (order.getTotalPrice() != null) {
                        totalSpent += order.getTotalPrice().doubleValue();
                    }
                    
                    // 统计品类偏好（从订单商品中获取）
                    // 这里简化处理，实际应该从订单项中获取商品品类
                }
                
                // 按月统计消费
                Map<String, Double> monthlyMap = new HashMap<>();
                for (com.rabbiter.em.entity.Order order : orders) {
                    if (order.getCreateTime() != null && order.getTotalPrice() != null) {
                        String month = order.getCreateTime().toString().substring(0, 7); // yyyy-MM
                        monthlyMap.put(month, monthlyMap.getOrDefault(month, 0.0) + order.getTotalPrice().doubleValue());
                    }
                }
                
                for (Map.Entry<String, Double> entry : monthlyMap.entrySet()) {
                    Map<String, Object> monthData = new HashMap<>();
                    monthData.put("month", entry.getKey());
                    monthData.put("amount", entry.getValue());
                    monthlySpending.add(monthData);
                }
                
                // 按月份排序
                monthlySpending.sort((a, b) -> String.valueOf(a.get("month")).compareTo(String.valueOf(b.get("month"))));
            }
            
            // 3. 获取用户购买的商品品类分布
            List<Map<String, Object>> categoryDistribution = new ArrayList<>();
            // 这里简化处理，返回模拟数据
            categoryDistribution.add(createCategoryItem("电子产品", 35));
            categoryDistribution.add(createCategoryItem("服装鞋帽", 25));
            categoryDistribution.add(createCategoryItem("食品饮料", 20));
            categoryDistribution.add(createCategoryItem("家居用品", 15));
            categoryDistribution.add(createCategoryItem("其他", 5));
            
            // 4. 构建消费趋势数据
            List<Map<String, Object>> spendingTrend = new ArrayList<>();
            LocalDate endDate = LocalDate.now();
            Random random = new Random();
            
            for (int i = days - 1; i >= 0; i--) {
                LocalDate date = endDate.minusDays(i);
                Map<String, Object> dayData = new HashMap<>();
                dayData.put("date", date.toString());
                // 模拟每日消费数据
                dayData.put("amount", random.nextDouble() * 200);
                spendingTrend.add(dayData);
            }
            
            // 5. 用户消费画像
            Map<String, Object> userProfile = new HashMap<>();
            userProfile.put("totalSpent", Math.round(totalSpent * 100) / 100.0);
            userProfile.put("totalOrders", totalOrders);
            userProfile.put("avgOrderValue", totalOrders > 0 ? Math.round((totalSpent / totalOrders) * 100) / 100.0 : 0);
            userProfile.put("monthlySpending", monthlySpending);
            userProfile.put("categoryDistribution", categoryDistribution);
            userProfile.put("spendingTrend", spendingTrend);
            
            // 6. 消费等级评估
            String consumptionLevel = "普通消费者";
            if (totalSpent > 10000) {
                consumptionLevel = "VIP消费者";
            } else if (totalSpent > 5000) {
                consumptionLevel = "高级消费者";
            } else if (totalSpent > 1000) {
                consumptionLevel = "中级消费者";
            }
            userProfile.put("consumptionLevel", consumptionLevel);
            
            result.put("userProfile", userProfile);
            result.put("userId", userId);
            result.put("analysisDays", days);
            
            log.info("用户个人销售数据分析完成，总消费: {}, 订单数: {}", totalSpent, totalOrders);
            
            return result;
        } catch (Exception e) {
            log.error("分析用户个人销售数据失败：{}", e.getMessage());
            return new HashMap<>();
        }
    }
    
    /**
     * 创建品类统计项
     */
    private Map<String, Object> createCategoryItem(String name, int percentage) {
        Map<String, Object> item = new HashMap<>();
        item.put("name", name);
        item.put("percentage", percentage);
        return item;
    }
    
    /**
     * 调用Python脚本生成词云数据
     * @param reviews 评论列表
     * @return 词云数据列表
     */
    private List<Map<String, Object>> generateWordCloudFromPython(List<Review> reviews) {
        List<Map<String, Object>> wordCloudData = new ArrayList<>();
        
        try {
            log.info("=== 词云生成开始 ===");
            log.info("输入评论数量: {}", reviews != null ? reviews.size() : 0);
            
            if (reviews == null || reviews.isEmpty()) {
                log.warn("评论列表为空");
                return wordCloudData;
            }
            
            // 构建评论JSON数据
            List<Map<String, String>> commentsJson = new ArrayList<>();
            int emptyContentCount = 0;
            for (Review review : reviews) {
                if (review.getContent() != null && !review.getContent().trim().isEmpty()) {
                    Map<String, String> comment = new HashMap<>();
                    comment.put("content", review.getContent());
                    commentsJson.add(comment);
                } else {
                    emptyContentCount++;
                }
            }
            
            log.info("有效评论数量: {}, 空内容数量: {}", commentsJson.size(), emptyContentCount);
            
            if (commentsJson.isEmpty()) {
                log.warn("没有有效的评论内容，无法生成词云");
                return wordCloudData;
            }
            
            // 转换为JSON字符串
            String commentsJsonStr = cn.hutool.json.JSONUtil.toJsonStr(commentsJson);
            log.info("评论JSON字符串长度: {}", commentsJsonStr.length());
            log.info("评论JSON示例(前200字符): {}", commentsJsonStr.length() > 200 ? commentsJsonStr.substring(0, 200) + "..." : commentsJsonStr);
            
            // 获取Python脚本路径 - 使用固定的绝对路径
            String scriptPath = "/Users/a123/Downloads/项目/graduate_design/generate_wordcloud.py";
            java.io.File scriptFile = new java.io.File(scriptPath);
            log.info("检查脚本文件: {}", scriptPath);
            log.info("脚本文件存在: {}", scriptFile.exists());
            log.info("脚本文件可读: {}", scriptFile.canRead());
            
            if (!scriptFile.exists()) {
                log.error("词云脚本不存在: {}", scriptPath);
                return wordCloudData;
            }
            
            log.info("开始执行Python词云脚本...");
            
            // 调用Python脚本
            ProcessBuilder processBuilder = new ProcessBuilder(
                "python3", scriptPath, commentsJsonStr
            );
            processBuilder.redirectErrorStream(true);
            
            Process process = processBuilder.start();
            
            // 读取Python脚本输出
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream(), "UTF-8")
            );
            
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            int exitCode = process.waitFor();
            String fullOutput = output.toString().trim();
            
            log.info("Python脚本退出码: {}", exitCode);
            log.info("Python脚本输出总长度: {}", fullOutput.length());
            
            if (exitCode != 0) {
                log.error("Python脚本执行失败，退出码: {}", exitCode);
                log.error("Python脚本输出: {}", fullOutput.length() > 1000 ? fullOutput.substring(0, 1000) : fullOutput);
                return wordCloudData;
            }
            
            if (fullOutput.isEmpty()) {
                log.warn("Python脚本无输出");
                return wordCloudData;
            }
            
            // 查找JSON输出（可能包含jieba的日志）
            String jsonOutput = fullOutput;
            int jsonStart = fullOutput.indexOf("[");
            int jsonEnd = fullOutput.lastIndexOf("]");
            
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                jsonOutput = fullOutput.substring(jsonStart, jsonEnd + 1);
                log.info("提取的JSON长度: {}", jsonOutput.length());
            } else {
                log.error("未找到有效的JSON输出，完整输出: {}", fullOutput.length() > 500 ? fullOutput.substring(0, 500) : fullOutput);
                return wordCloudData;
            }
            
            log.info("词云JSON数据(前300字符): {}", jsonOutput.length() > 300 ? jsonOutput.substring(0, 300) + "..." : jsonOutput);
            
            // 解析JSON输出
            cn.hutool.json.JSONArray jsonArray = cn.hutool.json.JSONUtil.parseArray(jsonOutput);
            log.info("解析到词云数据条数: {}", jsonArray.size());
            
            for (int i = 0; i < jsonArray.size(); i++) {
                cn.hutool.json.JSONObject jsonObj = jsonArray.getJSONObject(i);
                Map<String, Object> wordItem = new HashMap<>();
                wordItem.put("name", jsonObj.getStr("name"));
                wordItem.put("value", jsonObj.getInt("value"));
                wordCloudData.add(wordItem);
            }
            
            log.info("=== 词云生成完成，共 {} 个词 ===", wordCloudData.size());
            
        } catch (Exception e) {
            log.error("调用Python词云脚本失败：{}", e.getMessage(), e);
        }
        
        return wordCloudData;
    }
    
    /**
     * 获取品类的同义词列表
     * 用于模糊匹配，提高搜索命中率
     */
    private List<String> getCategorySynonyms(String category) {
        List<String> synonyms = new ArrayList<>();
        if (category == null || category.isEmpty()) {
            return synonyms;
        }
        
        // 鞋子相关
        if (category.contains("鞋")) {
            synonyms.add("鞋");
            synonyms.add("运动鞋");
            synonyms.add("休闲鞋");
            synonyms.add("跑鞋");
            synonyms.add("靴子");
            synonyms.add("板鞋");
            synonyms.add("帆布鞋");
            synonyms.add("皮鞋");
            synonyms.add("高跟鞋");
            synonyms.add("凉鞋");
            synonyms.add("拖鞋");
            synonyms.add("马丁靴");
            synonyms.add("雪地靴");
            synonyms.add("Nike");
            synonyms.add("Adidas");
            synonyms.add("New Balance");
            synonyms.add("Converse");
        }
        // 手机相关
        else if (category.contains("手机") || category.contains("电话")) {
            synonyms.add("手机");
            synonyms.add("智能手机");
            synonyms.add("iPhone");
            synonyms.add("苹果手机");
            synonyms.add("华为");
            synonyms.add("Huawei");
            synonyms.add("小米");
            synonyms.add("Xiaomi");
            synonyms.add("OPPO");
            synonyms.add("vivo");
            synonyms.add("VIVO");
            synonyms.add("荣耀");
            synonyms.add("Honor");
            synonyms.add("三星");
            synonyms.add("Samsung");
            synonyms.add("一加");
            synonyms.add("OnePlus");
            synonyms.add("realme");
            synonyms.add("魅族");
            synonyms.add("Meizu");
            synonyms.add("红米");
            synonyms.add("Redmi");
            synonyms.add("游戏手机");
            synonyms.add("拍照手机");
        }
        // 电脑相关
        else if (category.contains("电脑") || category.contains("笔记本")) {
            synonyms.add("电脑");
            synonyms.add("笔记本");
            synonyms.add("笔记本电脑");
            synonyms.add("平板电脑");
            synonyms.add("游戏本");
            synonyms.add("台式机");
            synonyms.add("MacBook");
            synonyms.add("联想");
            synonyms.add("Lenovo");
            synonyms.add("ThinkPad");
            synonyms.add("戴尔");
            synonyms.add("Dell");
            synonyms.add("惠普");
            synonyms.add("HP");
            synonyms.add("华硕");
            synonyms.add("ASUS");
            synonyms.add("ROG");
            synonyms.add("微星");
            synonyms.add("MSI");
            synonyms.add("雷神");
            synonyms.add("机械革命");
            synonyms.add("Surface");
            synonyms.add("iPad");
        }
        // 衣服相关
        else if (category.contains("衣") || category.contains("服") || category.contains("装")) {
            synonyms.add("衣");
            synonyms.add("卫衣");
            synonyms.add("T恤");
            synonyms.add("衬衫");
            synonyms.add("外套");
            synonyms.add("夹克");
            synonyms.add("连衣裙");
            synonyms.add("毛衣");
            synonyms.add("牛仔裤");
            synonyms.add("休闲裤");
            synonyms.add("羽绒服");
            synonyms.add("棉服");
            synonyms.add("风衣");
            synonyms.add("西装");
            synonyms.add("运动装");
            synonyms.add("休闲装");
            synonyms.add("正装");
            synonyms.add("短裙");
            synonyms.add("半身裙");
            synonyms.add("打底裤");
            synonyms.add("运动裤");
            synonyms.add("短裤");
            synonyms.add("背心");
            synonyms.add("POLO衫");
            synonyms.add("针织衫");
            synonyms.add("雪纺衫");
        }
        // 耳机相关
        else if (category.contains("耳机")) {
            synonyms.add("耳机");
            synonyms.add("蓝牙耳机");
            synonyms.add("无线耳机");
            synonyms.add("头戴式耳机");
            synonyms.add("入耳式耳机");
            synonyms.add("AirPods");
            synonyms.add("降噪耳机");
            synonyms.add("运动耳机");
            synonyms.add("游戏耳机");
            synonyms.add("索尼");
            synonyms.add("Sony");
            synonyms.add("Bose");
            synonyms.add("森海塞尔");
            synonyms.add("铁三角");
        }
        // 手表相关
        else if (category.contains("手表")) {
            synonyms.add("手表");
            synonyms.add("智能手表");
            synonyms.add("电子表");
            synonyms.add("机械表");
            synonyms.add("Apple Watch");
            synonyms.add("运动手表");
            synonyms.add("电子手表");
            synonyms.add("石英表");
            synonyms.add("卡西欧");
            synonyms.add("Casio");
            synonyms.add("劳力士");
            synonyms.add("Omega");
            synonyms.add("欧米茄");
        }
        // 零食相关
        else if (category.contains("零食") || category.contains("吃")) {
            synonyms.add("零食");
            synonyms.add("薯片");
            synonyms.add("饼干");
            synonyms.add("巧克力");
            synonyms.add("糖果");
            synonyms.add("坚果");
            synonyms.add("果干");
            synonyms.add("肉脯");
            synonyms.add("辣条");
            synonyms.add("膨化食品");
            synonyms.add("糕点");
            synonyms.add("蛋糕");
            synonyms.add("面包");
            synonyms.add("曲奇");
            synonyms.add("威化");
            synonyms.add("果冻");
            synonyms.add("布丁");
        }
        // 饮料相关
        else if (category.contains("饮料") || category.contains("水") || category.contains("茶")) {
            synonyms.add("饮料");
            synonyms.add("咖啡");
            synonyms.add("茶叶");
            synonyms.add("奶茶");
            synonyms.add("果汁");
            synonyms.add("碳酸饮料");
            synonyms.add("矿泉水");
            synonyms.add("功能饮料");
            synonyms.add("可乐");
            synonyms.add("雪碧");
            synonyms.add("红茶");
            synonyms.add("绿茶");
            synonyms.add("乌龙茶");
            synonyms.add("气泡水");
            synonyms.add("酸奶");
            synonyms.add("牛奶");
            synonyms.add("豆浆");
        }
        // 美妆相关
        else if (category.contains("妆") || category.contains("护肤") || category.contains("口红")) {
            synonyms.add("护肤");
            synonyms.add("口红");
            synonyms.add("面膜");
            synonyms.add("洗面奶");
            synonyms.add("精华");
            synonyms.add("面霜");
            synonyms.add("粉底");
            synonyms.add("眼影");
            synonyms.add("睫毛膏");
            synonyms.add("腮红");
            synonyms.add("卸妆");
            synonyms.add("防晒");
            synonyms.add("隔离");
            synonyms.add("BB霜");
            synonyms.add("气垫");
            synonyms.add("兰蔻");
            synonyms.add("Lancome");
            synonyms.add("雅诗兰黛");
            synonyms.add("Estee Lauder");
            synonyms.add("SK-II");
            synonyms.add("迪奥");
            synonyms.add("Dior");
            synonyms.add("香奈儿");
            synonyms.add("Chanel");
            synonyms.add("完美日记");
            synonyms.add("花西子");
        }
        // 书相关
        else if (category.contains("书") || category.contains("图书")) {
            synonyms.add("书");
            synonyms.add("图书");
            synonyms.add("教材");
            synonyms.add("小说");
            synonyms.add("杂志");
            synonyms.add("漫画");
            synonyms.add("绘本");
            synonyms.add("教辅");
            synonyms.add("工具书");
            synonyms.add("电子书");
        }
        // 包相关
        else if (category.contains("包")) {
            synonyms.add("包");
            synonyms.add("背包");
            synonyms.add("双肩包");
            synonyms.add("手提包");
            synonyms.add("钱包");
            synonyms.add("登山包");
            synonyms.add("单肩包");
            synonyms.add("斜挎包");
            synonyms.add("公文包");
            synonyms.add("旅行包");
            synonyms.add("腰包");
            synonyms.add("胸包");
        }
        // 运动相关
        else if (category.contains("运动") || category.contains("健身")) {
            synonyms.add("运动");
            synonyms.add("健身");
            synonyms.add("瑜伽");
            synonyms.add("跑步");
            synonyms.add("训练");
            synonyms.add("哑铃");
            synonyms.add("跑步机");
            synonyms.add("动感单车");
            synonyms.add("瑜伽垫");
            synonyms.add("拉力器");
            synonyms.add("跳绳");
            synonyms.add("健腹轮");
            synonyms.add("引体向上器");
        }
        // 宠物相关
        else if (category.contains("宠物") || category.contains("猫") || category.contains("狗")) {
            synonyms.add("宠物");
            synonyms.add("猫粮");
            synonyms.add("狗粮");
            synonyms.add("宠物玩具");
            synonyms.add("猫砂");
            synonyms.add("宠物窝");
            synonyms.add("宠物零食");
            synonyms.add("猫罐头");
            synonyms.add("狗罐头");
            synonyms.add("宠物衣服");
            synonyms.add("猫爬架");
            synonyms.add("狗绳");
            synonyms.add("猫粮");
            synonyms.add("鱼饲料");
        }
        // 家居相关
        else if (category.contains("家居") || category.contains("家具")) {
            synonyms.add("家居");
            synonyms.add("家具");
            synonyms.add("桌椅");
            synonyms.add("沙发");
            synonyms.add("床");
            synonyms.add("柜子");
            synonyms.add("衣柜");
            synonyms.add("餐桌");
            synonyms.add("书桌");
            synonyms.add("椅子");
            synonyms.add("床头柜");
            synonyms.add("鞋柜");
            synonyms.add("电视柜");
            synonyms.add("四件套");
            synonyms.add("被子");
            synonyms.add("枕头");
            synonyms.add("床垫");
        }
        // 办公相关
        else if (category.contains("办公") || category.contains("文具")) {
            synonyms.add("办公");
            synonyms.add("文具");
            synonyms.add("笔");
            synonyms.add("笔记本");
            synonyms.add("文件夹");
            synonyms.add("打印机");
            synonyms.add("复印机");
            synonyms.add("碎纸机");
            synonyms.add("订书机");
            synonyms.add("剪刀");
            synonyms.add("胶水");
            synonyms.add("胶带");
            synonyms.add("便签");
        }
        // 食品相关
        else if (category.contains("食品") || category.contains("食")) {
            synonyms.add("食品");
            synonyms.add("零食");
            synonyms.add("饮料");
            synonyms.add("茶叶");
            synonyms.add("咖啡");
            synonyms.add("坚果");
            synonyms.add("饼干");
            synonyms.add("巧克力");
        }
        // 酒相关
        else if (category.contains("酒")) {
            synonyms.add("酒");
            synonyms.add("白酒");
            synonyms.add("红酒");
            synonyms.add("啤酒");
            synonyms.add("威士忌");
            synonyms.add("葡萄酒");
            synonyms.add("洋酒");
            synonyms.add("清酒");
            synonyms.add("黄酒");
            synonyms.add("果酒");
        }
        // 母婴相关
        else if (category.contains("母婴") || category.contains("婴儿") || category.contains("儿童")) {
            synonyms.add("母婴");
            synonyms.add("婴儿");
            synonyms.add("儿童");
            synonyms.add("推车");
            synonyms.add("奶瓶");
            synonyms.add("尿不湿");
            synonyms.add("纸尿裤");
            synonyms.add("奶粉");
            synonyms.add("安全座椅");
            synonyms.add("婴儿床");
            synonyms.add("辅食");
            synonyms.add("童装");
            synonyms.add("玩具");
            synonyms.add("积木");
        }
        // 数码相关
        else if (category.contains("数码") || category.contains("电子")) {
            synonyms.add("数码");
            synonyms.add("电子");
            synonyms.add("智能");
            synonyms.add("鼠标");
            synonyms.add("键盘");
            synonyms.add("音箱");
            synonyms.add("移动电源");
            synonyms.add("充电宝");
            synonyms.add("U盘");
            synonyms.add("硬盘");
            synonyms.add("内存条");
            synonyms.add("显卡");
            synonyms.add("摄像头");
            synonyms.add("路由器");
            synonyms.add("交换机");
        }
        // 家用电器相关
        else if (category.contains("电器") || category.contains("家电")) {
            synonyms.add("电器");
            synonyms.add("家电");
            synonyms.add("冰箱");
            synonyms.add("洗衣机");
            synonyms.add("空调");
            synonyms.add("电视");
            synonyms.add("微波炉");
            synonyms.add("电饭煲");
            synonyms.add("吸尘器");
            synonyms.add("扫地机器人");
            synonyms.add("空气净化器");
            synonyms.add("加湿器");
            synonyms.add("电风扇");
            synonyms.add("暖气");
            synonyms.add("烤箱");
            synonyms.add("咖啡机");
            synonyms.add("榨汁机");
            synonyms.add("电磁炉");
        }
        
        return synonyms;
    }
    
    /**
     * 根据品类名称获取分类ID
     */
    private Long getCategoryByName(String categoryName) {
        if (categoryName == null || categoryName.isEmpty()) {
            return null;
        }
        
        try {
            // 尝试精确匹配
            com.rabbiter.em.entity.Category category = categoryMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.rabbiter.em.entity.Category>()
                    .eq("name", categoryName)
            );
            if (category != null) {
                return category.getId();
            }
            
            // 尝试模糊匹配
            List<com.rabbiter.em.entity.Category> categories = categoryMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.rabbiter.em.entity.Category>()
                    .like("name", categoryName)
            );
            if (categories != null && !categories.isEmpty()) {
                return categories.get(0).getId();
            }
            
            // 关键词匹配
            if (categoryName.contains("鞋")) {
                // 返回运动鞋或休闲鞋分类
                categories = categoryMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.rabbiter.em.entity.Category>()
                        .in("name", "运动鞋", "休闲鞋", "靴子")
                );
                if (categories != null && !categories.isEmpty()) {
                    return categories.get(0).getId();
                }
            } else if (categoryName.contains("手机")) {
                categories = categoryMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.rabbiter.em.entity.Category>()
                        .eq("name", "手机")
                );
                if (categories != null && !categories.isEmpty()) {
                    return categories.get(0).getId();
                }
            } else if (categoryName.contains("电脑") || categoryName.contains("笔记本")) {
                categories = categoryMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.rabbiter.em.entity.Category>()
                        .in("name", "笔记本", "平板电脑")
                );
                if (categories != null && !categories.isEmpty()) {
                    return categories.get(0).getId();
                }
            } else if (categoryName.contains("衣") || categoryName.contains("服")) {
                categories = categoryMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.rabbiter.em.entity.Category>()
                        .in("name", "女装", "男装")
                );
                if (categories != null && !categories.isEmpty()) {
                    return categories.get(0).getId();
                }
            }
        } catch (Exception e) {
            log.warn("获取分类ID失败：{}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 商品排序
     */
    private void sortGoods(List<Good> goods, String sortBy) {
        if (goods == null || goods.isEmpty()) {
            return;
        }
        
        if ("sales".equals(sortBy)) {
            // 按销量排序
            goods.sort((a, b) -> {
                int salesA = a.getSales() != null ? a.getSales() : 0;
                int salesB = b.getSales() != null ? b.getSales() : 0;
                return Integer.compare(salesB, salesA); // 降序
            });
        } else if ("rating".equals(sortBy)) {
            // 按好评排序：70%评分 + 30%评论数
            goods.sort((a, b) -> {
                double ratingA = a.getGoodRating() != null ? a.getGoodRating().doubleValue() : 5.0;
                double ratingB = b.getGoodRating() != null ? b.getGoodRating().doubleValue() : 5.0;
                int reviewCountA = a.getReviewCount() != null ? a.getReviewCount() : 0;
                int reviewCountB = b.getReviewCount() != null ? b.getReviewCount() : 0;
                
                // 计算综合得分：70%评分 + 30%评论数（归一化）
                double scoreA = ratingA * 0.7 + (Math.min(reviewCountA, 100) / 100.0) * 5.0 * 0.3;
                double scoreB = ratingB * 0.7 + (Math.min(reviewCountB, 100) / 100.0) * 5.0 * 0.3;
                
                return Double.compare(scoreB, scoreA); // 降序
            });
        } else if ("price".equals(sortBy)) {
            // 按价格排序（从低到高）
            goods.sort((a, b) -> {
                BigDecimal priceA = a.getPrice() != null ? a.getPrice() : BigDecimal.ZERO;
                BigDecimal priceB = b.getPrice() != null ? b.getPrice() : BigDecimal.ZERO;
                return priceA.compareTo(priceB); // 升序
            });
        } else {
            // 综合排序：销量、好评、价格各占1/3
            goods.sort((a, b) -> {
                int salesA = a.getSales() != null ? a.getSales() : 0;
                int salesB = b.getSales() != null ? b.getSales() : 0;
                double ratingA = a.getGoodRating() != null ? a.getGoodRating().doubleValue() : 5.0;
                double ratingB = b.getGoodRating() != null ? b.getGoodRating().doubleValue() : 5.0;
                BigDecimal priceA = a.getPrice() != null ? a.getPrice() : BigDecimal.ZERO;
                BigDecimal priceB = b.getPrice() != null ? b.getPrice() : BigDecimal.ZERO;
                
                // 归一化处理
                double salesScore = Math.min(salesA, 10000) / 10000.0;
                double salesScoreB = Math.min(salesB, 10000) / 10000.0;
                double ratingScore = ratingA / 5.0;
                double ratingScoreB = ratingB / 5.0;
                double priceScore = 1.0 - (Math.min(priceA.doubleValue(), 10000) / 10000.0);
                double priceScoreB = 1.0 - (Math.min(priceB.doubleValue(), 10000) / 10000.0);
                
                // 综合得分
                double totalA = (salesScore + ratingScore + priceScore) / 3.0;
                double totalB = (salesScoreB + ratingScoreB + priceScoreB) / 3.0;
                
                return Double.compare(totalB, totalA); // 降序
            });
        }
    }
}
