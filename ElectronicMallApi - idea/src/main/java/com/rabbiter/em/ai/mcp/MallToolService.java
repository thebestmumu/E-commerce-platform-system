package com.rabbiter.em.ai.mcp;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.rabbiter.em.entity.Cart;
import com.rabbiter.em.entity.Category;
import com.rabbiter.em.entity.Good;
import com.rabbiter.em.entity.Order;
import com.rabbiter.em.entity.Review;
import com.rabbiter.em.entity.Address;
import com.rabbiter.em.entity.User;
import com.rabbiter.em.entity.dto.GoodDTO;
import com.rabbiter.em.mapper.ReviewMapper;
import com.rabbiter.em.service.CartService;
import com.rabbiter.em.service.CategoryService;
import com.rabbiter.em.service.GoodService;
import com.rabbiter.em.service.OrderService;
import com.rabbiter.em.service.AddressService;
import com.rabbiter.em.service.UserService;
import com.rabbiter.em.ai.core.SseEmitterContext;
import com.rabbiter.em.utils.TokenUtils;
import com.rabbiter.em.ai.service.AiBusinessService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MCP (Model Context Protocol) 工具服务 - AI 的"手脚"
 * 
 * ===== 什么是 MCP 工具？ =====
 * AI 本身只是一个语言模型，不能直接操作数据库或调用业务接口。
 * MCP 工具就是给 AI 提供"手脚"，让 AI 能够：
 * - 查询数据库（查商品、查订单）
 * - 执行业务操作（加购物车、创建订单）
 * - 调用外部 API（查航班、查酒店）
 * 
 * ===== 前端类比 =====
 * 就像 Vue 组件的 methods，AI 可以根据需要调用这些方法。
 * 每个 @Tool 注解的方法，AI 都能看到它的描述和参数，然后决定何时调用。
 * 
 * ===== 工作原理 =====
 * 1. 应用启动时，LangChain4j 扫描所有 @Tool 方法
 * 2. 生成工具描述（方法名 + @Tool 描述 + @P 参数描述）
 * 3. 调用 AI 时，把工具列表一起发送给 AI
 * 4. AI 根据用户问题，决定调用哪个工具
 * 5. 框架反射调用对应的 Java 方法
 * 6. 把方法返回结果给 AI，AI 基于结果生成回答
 */
@Component
public class MallToolService {

    private static final Logger log = LoggerFactory.getLogger(MallToolService.class);

    // 注入商城的各种业务 Service（就像 Vue 中 import 各种 API 函数）
    @Resource
    private GoodService goodService;
    @Resource
    private CategoryService categoryService;
    @Resource
    private ReviewMapper reviewMapper;
    @Resource
    private CartService cartService;
    @Resource
    private OrderService orderService;
    @Resource
    private AiBusinessService aiBusinessService;

    @Resource
    private FliggyMcpService fliggyMcpService;  // 飞猪 MCP（查航班/酒店）
    @Resource
    private AddressService addressService;
    @Resource
    private UserService userService;

    /**
     * 搜索商品工具
     * 
     * @Tool 注解说明：
     * - 括号内的字符串是工具描述，AI 会看到这个描述来决定是否调用
     * - 类似函数的 JSDoc 注释，告诉 AI 这个工具是干什么的
     * 
     * @P 注解说明：
     * - 参数描述，AI 会看到这个描述来理解参数含义
     * - 类似 TypeScript 的 @param 注释
     * 
     * 返回值：必须是字符串（JSON 格式），AI 能理解的内容
     */
    @Tool("搜索商城商品，根据关键词查找商品列表。用户想买什么东西时调用此工具。返回商品ID、名称、价格、图片、销量、评分等信息")
    public String searchProducts(
            @P("搜索关键词，比如商品名称或类型，如'手机'、'运动鞋'、'笔记本电脑'") String keyword
    ) {
        log.info("[工具调用] searchProducts: keyword={}", keyword);
        
        // 发送"思考中"事件给前端（前端会显示 loading 动画）
        SseEmitterContext.sendThinking("🔍 正在从系统商品库搜索：「" + keyword + "」...");
        
        try {
            // 调用商城服务搜索商品（分页查询，每页 10 条）
            List<GoodDTO> goods = goodService.findPage(1, 10, keyword, null, null).getRecords();
            
            SseEmitterContext.sendThinking("📊 搜索到 " + goods.size() + " 条商品，正在为您整理...");
            
            // 精简返回字段（只返回 AI 和前端需要的字段）
            List<Map<String, Object>> results = goods.stream().map(g -> {
                Map<String, Object> item = new HashMap<>();
                item.put("id", g.getId());
                item.put("name", g.getName());
                item.put("price", g.getPrice());
                item.put("imgs", g.getImgs());
                return item;
            }).collect(Collectors.toList());

            // 发送 action 事件，前端收到后会展示商品卡片组件
            // 类比：就像前端 emit('action', { type: 'recommend_goods', data: ... })
            SseEmitterContext.sendAction("recommend_goods", Map.of(
                    "goods", results,
                    "reason", "为您找到 " + results.size() + " 条相关商品"
            ));

            // 返回 JSON 字符串给 AI（AI 会解析这个 JSON 来理解搜索结果）
            return JSON.toJSONString(Map.of("success", true, "goods", results, "total", results.size()));
        } catch (Exception e) {
            log.error("搜索商品失败", e);
            return JSON.toJSONString(Map.of("success", false, "error", "搜索失败: " + e.getMessage()));
        }
    }

    @Tool("获取商品的详细信息，包括名称、价格、规格、描述、销量、评分等。当用户想了解某个商品详情时调用")
    public String getProductDetail(
            @P("商品ID，从搜索结果中获取") Long productId
    ) {
        log.info("[工具调用] getProductDetail: productId={}", productId);
        SseEmitterContext.sendThinking("📄 正在查询商品详情...");
        try {
            Good good = goodService.getGoodById(productId);
            SseEmitterContext.sendThinking("📋 已获取商品信息，正在为您提炼重点...");
            Map<String, Object> result = new HashMap<>();
            result.put("id", good.getId());
            result.put("name", good.getName());
            result.put("description", good.getDescription());
            result.put("price", good.getPrice());
            result.put("discount", good.getDiscount());
            result.put("sales", good.getSales());
            result.put("imgs", good.getImgs());
            result.put("standardList", good.getStandardList());
            result.put("goodRating", good.getGoodRating());
            result.put("reviewCount", good.getReviewCount());
            return JSON.toJSONString(result);
        } catch (Exception e) {
            return "{\"error\":\"获取商品详情失败：" + e.getMessage() + "\"}";
        }
    }

    @Tool("获取商品分类列表。用户想了解商城有哪些商品分类时调用")
    public String getCategories() {
        log.info("[工具调用] getCategories");
        SseEmitterContext.sendThinking("📂 正在获取商品分类...");
        try {
            List<Category> categories = categoryService.list();
            List<Map<String, Object>> results = categories.stream().map(c -> {
                Map<String, Object> item = new HashMap<>();
                item.put("id", c.getId());
                item.put("name", c.getName());
                return item;
            }).collect(Collectors.toList());
            return JSON.toJSONString(Map.of("categories", results));
        } catch (Exception e) {
            return "{\"error\":\"获取分类失败：" + e.getMessage() + "\"}";
        }
    }

    @Tool("获取热销推荐商品列表。用户想看推荐商品、热销商品或不知道买什么时调用")
    public String getRecommendedProducts() {
        log.info("[工具调用] getRecommendedProducts");
        SseEmitterContext.sendThinking("🌟 正在获取热销推荐商品...");
        try {
            List<GoodDTO> goods = goodService.findFrontGoods();
            SseEmitterContext.sendThinking("✨ 已获取热销数据，正在为您精选推荐...");
            List<Map<String, Object>> results = goods.stream().map(g -> {
                Map<String, Object> item = new HashMap<>();
                item.put("id", g.getId());
                item.put("name", g.getName());
                item.put("price", g.getPrice());
                item.put("imgs", g.getImgs());
                return item;
            }).collect(Collectors.toList());

            // 发送 action 事件，前端展示商品卡片
            SseEmitterContext.sendAction("recommend_goods", Map.of(
                    "goods", results,
                    "reason", "为您精选 " + results.size() + " 款热销推荐商品"
            ));

            return JSON.toJSONString(Map.of("goods", results));
        } catch (Exception e) {
            return "{\"error\":\"获取推荐商品失败：" + e.getMessage() + "\"}";
        }
    }

    @Tool("获取指定商品的用户评价。用户想看商品评价、口碑时调用")
    public String getProductReviews(
            @P("商品ID") Long productId
    ) {
        log.info("[工具调用] getProductReviews: productId={}", productId);
        SseEmitterContext.sendThinking("⭐ 正在查询商品评价...");
        try {
            List<Review> reviews = reviewMapper.selectReviewListWithUser(productId, 1, 0, 5);
            SseEmitterContext.sendThinking("📊 获取到评价数据，正在为您分析口碑...");
            List<Map<String, Object>> results = reviews.stream().map(r -> {
                Map<String, Object> item = new HashMap<>();
                item.put("rating", r.getRating());
                item.put("content", r.getContent());
                item.put("tags", r.getTags());
                item.put("createTime", r.getCreateTime());
                return item;
            }).collect(Collectors.toList());
            Map<String, Object> stats = reviewMapper.countRatingByGoodId(productId, 1);
            Map<String, Object> result = new HashMap<>();
            result.put("reviews", results);
            result.put("statistics", stats);
            return JSON.toJSONString(result);
        } catch (Exception e) {
            return "{\"error\":\"获取评价失败：" + e.getMessage() + "\"}";
        }
    }

    @Tool("将商品添加到用户购物车。用户想购买某个商品时调用。需要提供用户ID、商品ID、数量和规格")
    public String addToCart(
            @P("用户ID") Long userId,
            @P("商品ID") Long goodId,
            @P("购买数量，默认为1") Integer count,
            @P("商品规格，如'128GB 黑色'、'白色'等，如果不确定可传'默认'") String standard
    ) {
        log.info("[工具调用] addToCart: userId={}, goodId={}, count={}", userId, goodId, count);
        SseEmitterContext.sendThinking("🛒 正在加入购物车...");
        try {
            if (userId == null || goodId == null) {
                return "{\"error\":\"用户ID和商品ID不能为空\"}";
            }
            if (count == null || count <= 0) {
                count = 1;
            }
            if (standard == null || standard.isEmpty() || "默认".equals(standard)) {
                String firstStandard = cartService.getFirstStandard(goodId);
                if (firstStandard != null && !firstStandard.isEmpty()) {
                    standard = firstStandard;
                } else {
                    standard = "默认";
                }
            }

            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setGoodId(goodId);
            cart.setCount(count);
            cart.setStandard(standard);
            cart.setCreateTime(DateUtil.now());

            boolean success = cartService.save(cart);
            SseEmitterContext.sendThinking(success ? "✅ 已成功加入购物车，正在确认..." : "❌ 加入购物车失败，请重试");
            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("cartId", cart.getId());
            result.put("goodId", goodId);
            result.put("count", count);
            result.put("standard", standard);
            result.put("message", success ? "添加成功" : "添加失败");
            return JSON.toJSONString(result);
        } catch (Exception e) {
            return "{\"error\":\"添加到购物车失败：" + e.getMessage() + "\"}";
        }
    }

    @Tool("查看用户购物车中的商品列表。用户想了解自己购物车里有什么时调用")
    public String viewCart(
            @P("用户ID") Long userId
    ) {
        log.info("[工具调用] viewCart: userId={}", userId);
        SseEmitterContext.sendThinking("🛍️ 正在查看购物车...");
        try {
            if (userId == null) {
                return "{\"error\":\"用户ID不能为空\"}";
            }
            List<Map<String, Object>> cartItems = cartService.selectByUserId(userId);
            SseEmitterContext.sendThinking("📋 已获取购物车数据，正在为您整理...");
            Map<String, Object> result = new HashMap<>();
            result.put("total", cartItems.size());
            result.put("items", cartItems);
            return JSON.toJSONString(result);
        } catch (Exception e) {
            return "{\"error\":\"查看购物车失败：" + e.getMessage() + "\"}";
        }
    }

    @Tool("创建订单并下单。用户确认要购买商品时调用，会直接从购物车结算。需要提供用户ID、商品列表(JSON格式)、联系人、电话和地址")
    public String createOrder(
            @P("用户ID") Long userId,
            @P("商品列表JSON，格式：[{\"id\":商品ID,\"num\":数量,\"standard\":\"规格\"}]") String goodsJson,
            @P("联系人姓名") String linkUser,
            @P("联系电话") String linkPhone,
            @P("送货地址") String linkAddress
    ) {
        log.info("[工具调用] createOrder: userId={}, linkUser={}", userId, linkUser);
        SseEmitterContext.sendThinking("📋 正在创建订单...");
        try {
            if (userId == null || goodsJson == null || goodsJson.isEmpty()) {
                return "{\"error\":\"用户ID和商品信息不能为空\"}";
            }
            if (linkUser == null || linkPhone == null || linkAddress == null) {
                return "{\"error\":\"联系人、电话和地址不能为空\"}";
            }

            Order order = new Order();
            order.setUserId(userId.intValue());
            order.setLinkUser(linkUser);
            order.setLinkPhone(linkPhone);
            order.setLinkAddress(linkAddress);
            order.setGoods(goodsJson);
            order.setState("待付款");

            String orderNo = orderService.saveOrder(order);
            SseEmitterContext.sendThinking("📝 订单已创建，正在为您确认订单信息...");
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("orderNo", orderNo);
            result.put("message", "订单创建成功，请完成支付");
            return JSON.toJSONString(result);
        } catch (Exception e) {
            return "{\"error\":\"创建订单失败：" + e.getMessage() + "\"}";
        }
    }

    @Tool("查看用户的订单列表。用户想查看自己的历史订单时调用")
    public String viewOrders(
            @P("用户ID") Long userId
    ) {
        log.info("[工具调用] viewOrders: userId={}", userId);
        SseEmitterContext.sendThinking("📦 正在查询订单列表...");
        try {
            if (userId == null) {
                return "{\"error\":\"用户ID不能为空\"}";
            }
            List<Map<String, Object>> orders = orderService.selectByUserId(userId.intValue());
            Map<String, Object> result = new HashMap<>();
            result.put("total", orders.size());
            result.put("orders", orders);
            return JSON.toJSONString(result);
        } catch (Exception e) {
            return "{\"error\":\"查看订单失败：" + e.getMessage() + "\"}";
        }
    }

    @Tool("搜索机票，查询两个城市之间的航班信息。用户想买机票时调用此工具。返回航班号、价格、时间等信息")
    public String searchFlights(
            @P("出发城市，如'北京'、'上海'、'广州'") String departureCity,
            @P("到达城市，如'深圳'、'成都'、'杭州'") String arrivalCity,
            @P("出发日期，格式为yyyy-MM-dd，如'2025-05-10'") String departureDate
    ) {
        log.info("[工具调用] searchFlights: {}→{}, date={}", departureCity, arrivalCity, departureDate);
        SseEmitterContext.sendThinking("✈️ 正在搜索「" + departureCity + "→" + arrivalCity + "」的航班...");
        try {
            if (fliggyMcpService != null) {
                SseEmitterContext.sendThinking("🔎 正在对比各航班价格和时间，为您筛选最优方案...");
                return fliggyMcpService.searchFlights(departureCity, arrivalCity, departureDate);
            }
            return "{\"error\":\"飞猪MCP未初始化\"}";
        } catch (Exception e) {
            return "{\"error\":\"搜索机票失败：" + e.getMessage() + "\"}";
        }
    }

    @Tool("搜索酒店，查询某个城市的酒店列表。用户想预订酒店时调用此工具。返回酒店名称、价格、评分、地址等信息")
    public String searchHotels(
            @P("城市名称，如'北京'、'上海'、'三亚'") String city,
            @P("入住日期，格式为yyyy-MM-dd，如'2025-05-10'") String checkInDate,
            @P("退房日期，格式为yyyy-MM-dd，如'2025-05-12'") String checkOutDate
    ) {
        log.info("[工具调用] searchHotels: city={}, checkIn={}, checkOut={}", city, checkInDate, checkOutDate);
        SseEmitterContext.sendThinking("🏨 正在搜索「" + city + "」的酒店...");
        try {
            if (fliggyMcpService != null) {
                SseEmitterContext.sendThinking("🔎 正在筛选酒店价格、评分和位置...");
                return fliggyMcpService.searchHotels(city, checkInDate, checkOutDate);
            }
            return "{\"error\":\"飞猪MCP未初始化\"}";
        } catch (Exception e) {
            return "{\"error\":\"搜索酒店失败：" + e.getMessage() + "\"}";
        }
    }

    @Tool("获取用户订单详情。用户想查看某个订单的详细信息时调用")
    public String getOrderDetail(
            @P("订单号") String orderNo
    ) {
        log.info("[工具调用] getOrderDetail: orderNo={}", orderNo);
        SseEmitterContext.sendThinking("📄 正在查询订单详情...");
        try {
            Order order = orderService.getOrderByNo(orderNo);
            if (order == null) {
                return "{\"error\":\"订单不存在\"}";
            }
            Map<String, Object> result = new HashMap<>();
            result.put("orderNo", order.getOrderNo());
            result.put("state", order.getState());
            result.put("totalPrice", order.getTotalPrice());
            result.put("linkUser", order.getLinkUser());
            result.put("linkPhone", order.getLinkPhone());
            result.put("linkAddress", order.getLinkAddress());
            result.put("createTime", order.getCreateTime());
            result.put("goods", order.getGoods());
            return JSON.toJSONString(result);
        } catch (Exception e) {
            return "{\"error\":\"获取订单详情失败：" + e.getMessage() + "\"}";
        }
    }

    @Tool("取消订单。用户想取消已创建的订单时调用。需要提供订单号")
    public String cancelOrder(
            @P("订单号") String orderNo
    ) {
        log.info("[工具调用] cancelOrder: orderNo={}", orderNo);
        SseEmitterContext.sendThinking("❌ 正在取消订单...");
        try {
            boolean success = orderService.cancelOrder(orderNo);
            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("orderNo", orderNo);
            result.put("message", success ? "订单已取消" : "取消失败，请检查订单状态");
            return JSON.toJSONString(result);
        } catch (Exception e) {
            return "{\"error\":\"取消订单失败：" + e.getMessage() + "\"}";
        }
    }

    @Tool("按价格区间筛选商品。用户想找某个价格范围内的商品时调用")
    public String searchProductsByPriceRange(
            @P("搜索关键词，如'手机'、'电脑'") String keyword,
            @P("最低价格，如0") Double minPrice,
            @P("最高价格，如5000") Double maxPrice
    ) {
        log.info("[工具调用] searchProductsByPriceRange: keyword={}, price=[{}~{}]", keyword, minPrice, maxPrice);
        SseEmitterContext.sendThinking("💰 正在按价格筛选「" + keyword + "」...");
        try {
            List<Good> goods = goodService.searchByPriceRange(keyword, minPrice, maxPrice);
            SseEmitterContext.sendThinking("📋 筛选完成，正在为您整理结果...");
            List<Map<String, Object>> results = goods.stream().map(g -> {
                Map<String, Object> item = new HashMap<>();
                item.put("id", g.getId());
                item.put("name", g.getName());
                item.put("price", g.getPrice());
                item.put("imgs", g.getImgs());
                item.put("sales", g.getSales());
                return item;
            }).collect(Collectors.toList());
            return JSON.toJSONString(Map.of("goods", results, "total", results.size()));
        } catch (Exception e) {
            return "{\"error\":\"价格筛选失败：" + e.getMessage() + "\"}";
        }
    }

    @Tool("获取多个商品进行对比。用户想比较几个商品时调用。需要提供商品ID列表")
    public String compareProducts(
            @P("商品ID列表，用逗号分隔，如'1,2,3'") String productIds
    ) {
        log.info("[工具调用] compareProducts: ids={}", productIds);
        SseEmitterContext.sendThinking("⚖️ 正在对比商品...");
        try {
            String[] ids = productIds.split(",");
            List<Map<String, Object>> products = new java.util.ArrayList<>();
            for (String id : ids) {
                Good good = goodService.getGoodById(Long.parseLong(id.trim()));
                if (good != null) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", good.getId());
                    item.put("name", good.getName());
                    item.put("price", good.getPrice());
                    item.put("discount", good.getDiscount());
                    item.put("description", good.getDescription());
                    item.put("sales", good.getSales());
                    item.put("goodRating", good.getGoodRating());
                    products.add(item);
                }
            }
            SseEmitterContext.sendThinking("📊 对比完成，正在为您分析各项差异...");
            return JSON.toJSONString(Map.of("products", products, "count", products.size()));
        } catch (Exception e) {
            return "{\"error\":\"商品对比失败：" + e.getMessage() + "\"}";
        }
    }

    @Tool("获取用户的个性化商品推荐。用户想'给我推荐一些商品'、'有什么好东西推荐'、'推荐适合我的商品'时调用。基于用户的历史记录做个性化推荐")
    public String getPersonalizedRecommendations(
            @P("用户ID") Long userId
    ) {
        log.info("[工具调用] getPersonalizedRecommendations: userId={}", userId);
        SseEmitterContext.sendThinking("🎯 正在为您生成个性化推荐...");
        try {
            Object recommendResult = aiBusinessService.getPersonalizedRecommendation(userId);
            SseEmitterContext.sendThinking("✨ 个性化推荐已生成，正在为您整理...");
            return JSON.toJSONString(Map.of("success", true, "recommendations", recommendResult, "type", "personalized"));
        } catch (Exception e) {
            return "{\"error\":\"个性化推荐失败：" + e.getMessage() + "\"}";
        }
    }

    @Tool("获取用户的订单统计数据。用户想查看'我的订单统计'、'买了多少钱'、'订单分析'时调用。返回订单总数、总金额、各状态订单数量等")
    public String getOrderStatistics(
            @P("用户ID") Long userId
    ) {
        log.info("[工具调用] getOrderStatistics: userId={}", userId);
        SseEmitterContext.sendThinking("📊 正在统计订单数据...");
        try {
            Object stats = aiBusinessService.orderStatistics(userId);
            return JSON.toJSONString(Map.of("success", true, "statistics", stats));
        } catch (Exception e) {
            return "{\"error\":\"获取订单统计失败：" + e.getMessage() + "\"}";
        }
    }

    @Tool("查看商品库存信息。用户想'还有货吗'、'库存多少'、'有没有库存'时调用。返回商品的当前库存数量")
    public String checkStock(
            @P("商品ID") Long productId
    ) {
        log.info("[工具调用] checkStock: productId={}", productId);
        SseEmitterContext.sendThinking("📦 正在查询库存...");
        try {
            Object stock = aiBusinessService.checkStock(productId);
            return JSON.toJSONString(Map.of("success", true, "stock", stock, "productId", productId));
        } catch (Exception e) {
            return "{\"error\":\"查询库存失败：" + e.getMessage() + "\"}";
        }
    }

    @Tool("查询用户的收货地址列表。用户想查看'我的地址'、'收货地址'、'地址管理'时调用。需要用户ID")
    public String queryAddresses(
            @P("用户ID") Long userId
    ) {
        log.info("[工具调用] queryAddresses: userId={}", userId);
        SseEmitterContext.sendThinking("📫 正在查询您的收货地址...");
        try {
            List<Address> addresses = addressService.findAllById(userId);
            SseEmitterContext.sendThinking("📬 已获取 " + addresses.size() + " 条地址记录");
            return JSON.toJSONString(Map.of("success", true, "addresses", addresses, "total", addresses.size()));
        } catch (Exception e) {
            log.error("查询地址失败", e);
            return JSON.toJSONString(Map.of("success", false, "error", "查询地址失败: " + e.getMessage()));
        }
    }

    @Tool("查询用户的基本信息。用户想查看'我的信息'、'个人信息'、'我的账号'时调用。需要用户ID。返回用户名、昵称、手机号、邮箱等")
    public String getUserInfo(
            @P("用户ID") Long userId
    ) {
        log.info("[工具调用] getUserInfo: userId={}", userId);
        SseEmitterContext.sendThinking("👤 正在查询用户信息...");
        try {
            User user = userService.getById(userId);
            if (user == null) {
                return JSON.toJSONString(Map.of("success", false, "error", "用户不存在"));
            }
            Map<String, Object> info = new HashMap<>();
            info.put("id", user.getId());
            info.put("username", user.getUsername());
            info.put("nickname", user.getNickname());
            info.put("phone", user.getPhone());
            info.put("email", user.getEmail());
            info.put("avatarUrl", user.getAvatarUrl());
            SseEmitterContext.sendThinking("✅ 已获取用户「" + user.getNickname() + "」的信息");
            return JSON.toJSONString(Map.of("success", true, "userInfo", info));
        } catch (Exception e) {
            log.error("查询用户信息失败", e);
            return JSON.toJSONString(Map.of("success", false, "error", "查询用户信息失败: " + e.getMessage()));
        }
    }

    // ==================== 以下为新增：智能帮助模式功能 MCP 化工具 ====================

    @Tool("分析用户的订单历史，统计订单数量、消费金额、状态分布、月度消费趋势、品类偏好等。用户想'分析我的订单'、'统计我的消费'、'我的消费报告'时调用。返回详细的订单分析报告")
    public String analyzeOrders(
            @P("用户ID") Long userId,
            @P("分析最近多少天的订单，默认0表示分析全部") Integer days
    ) {
        log.info("[工具调用] analyzeOrders: userId={}, days={}", userId, days);
        SseEmitterContext.sendThinking("📊 正在分析您的订单历史...");
        try {
            if (userId == null) {
                return "{\"error\":\"用户ID不能为空\"}";
            }
            Map<String, Object> analysis = aiBusinessService.analyzeOrderHistory(userId);
            SseEmitterContext.sendThinking("📈 已生成订单分析报告，包含消费趋势、品类偏好等");
            return JSON.toJSONString(Map.of("success", true, "analysis", analysis));
        } catch (Exception e) {
            log.error("分析订单历史失败", e);
            return JSON.toJSONString(Map.of("success", false, "error", "分析订单失败: " + e.getMessage()));
        }
    }

    @Tool("分析某个商品的评论舆情，包括评分分布、情感分布（正面/中性/负面）、热门评论标签、词云数据等。用户想'看看大家对这个商品的评价'、'分析一下评论'、'商品口碑怎么样'时调用。返回评论分析报告")
    public String analyzeReviews(
            @P("商品ID") Long goodId
    ) {
        log.info("[工具调用] analyzeReviews: goodId={}", goodId);
        SseEmitterContext.sendThinking("💬 正在分析商品评论...");
        try {
            if (goodId == null) {
                return "{\"error\":\"商品ID不能为空\"}";
            }
            Map<String, Object> analysis = aiBusinessService.analyzeReviewSentiment(goodId);
            SseEmitterContext.sendThinking("📝 已生成评论舆情分析报告");
            return JSON.toJSONString(Map.of("success", true, "analysis", analysis));
        } catch (Exception e) {
            log.error("分析评论失败", e);
            return JSON.toJSONString(Map.of("success", false, "error", "分析评论失败: " + e.getMessage()));
        }
    }

    @Tool("分析用户的个人销售/消费数据，包括消费总额、订单数量、品类偏好、月度消费趋势等。用户想'分析我的消费情况'、'我的购买统计'、'我的消费报告'时调用。返回个人消费分析报告")
    public String analyzeSales(
            @P("用户ID") Long userId,
            @P("分析最近多少天的数据，默认30天") Integer days
    ) {
        log.info("[工具调用] analyzeSales: userId={}, days={}", userId, days);
        SseEmitterContext.sendThinking("📈 正在分析您的消费数据...");
        try {
            if (userId == null) {
                return "{\"error\":\"用户ID不能为空\"}";
            }
            if (days == null) days = 30;
            Map<String, Object> analysis = aiBusinessService.analyzeUserSalesData(userId, days);
            SseEmitterContext.sendThinking("📊 已生成个人消费分析报告");
            return JSON.toJSONString(Map.of("success", true, "analysis", analysis));
        } catch (Exception e) {
            log.error("分析销售数据失败", e);
            return JSON.toJSONString(Map.of("success", false, "error", "分析消费数据失败: " + e.getMessage()));
        }
    }

    @Tool("追踪订单物流和状态进度。用户想'我的订单到哪了'、'查物流'、'订单发货了吗'、'跟踪订单'时调用。可以查询指定订单号，也可以查询最新订单。返回订单状态和物流进度")
    public String trackOrder(
            @P("订单号，如果用户提供了具体订单号就传入，否则传null表示查询最新订单") String orderId,
            @P("用户ID") Long userId
    ) {
        log.info("[工具调用] trackOrder: orderId={}, userId={}", orderId, userId);
        SseEmitterContext.sendThinking("🚚 正在查询订单物流状态...");
        try {
            if (userId == null) {
                return "{\"error\":\"用户ID不能为空\"}";
            }
            Object result = aiBusinessService.trackOrderStatus(orderId, userId);
            SseEmitterContext.sendThinking("📦 已获取订单物流信息");
            return JSON.toJSONString(Map.of("success", true, "tracking", result));
        } catch (Exception e) {
            log.error("追踪订单状态失败", e);
            return JSON.toJSONString(Map.of("success", false, "error", "查询物流失败: " + e.getMessage()));
        }
    }

    @Tool("批量添加多个商品到购物车。用户想'把这些都加入购物车'、'一起加入购物车'时调用。需要提供用户ID和商品列表（每个商品包含goodId、count、standard）。返回批量添加结果")
    public String batchAddToCart(
            @P("用户ID") Long userId,
            @P("商品列表，JSON数组格式，每个元素包含goodId(商品ID)、count(数量)、standard(规格)，如[{\"goodId\":1,\"count\":2,\"standard\":\"黑色\"},{\"goodId\":2,\"count\":1,\"standard\":\"默认\"}]") String itemsJson
    ) {
        log.info("[工具调用] batchAddToCart: userId={}, itemsJson={}", userId, itemsJson);
        SseEmitterContext.sendThinking("🛒 正在批量添加商品到购物车...");
        try {
            if (userId == null) {
                return "{\"error\":\"用户ID不能为空\"}";
            }
            if (itemsJson == null || itemsJson.isEmpty()) {
                return "{\"error\":\"商品列表不能为空\"}";
            }
            // 解析 JSON 数组
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) (List<?>) JSON.parseArray(itemsJson, Map.class);
            if (items == null || items.isEmpty()) {
                return "{\"error\":\"商品列表格式不正确\"}";
            }
            // 为每个商品添加 userId
            for (Map<String, Object> item : items) {
                item.put("userId", userId);
            }
            Object result = aiBusinessService.batchAddCart(items);
            SseEmitterContext.sendThinking("✅ 已批量添加 " + items.size() + " 个商品到购物车");
            return JSON.toJSONString(Map.of("success", true, "result", result, "count", items.size()));
        } catch (Exception e) {
            log.error("批量添加购物车失败", e);
            return JSON.toJSONString(Map.of("success", false, "error", "批量添加失败: " + e.getMessage()));
        }
    }
}
