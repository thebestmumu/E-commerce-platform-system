package com.rabbiter.em.ai.mcp;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.rabbiter.em.entity.Cart;
import com.rabbiter.em.entity.Category;
import com.rabbiter.em.entity.Good;
import com.rabbiter.em.entity.Order;
import com.rabbiter.em.entity.Review;
import com.rabbiter.em.entity.dto.GoodDTO;
import com.rabbiter.em.mapper.ReviewMapper;
import com.rabbiter.em.service.CartService;
import com.rabbiter.em.service.CategoryService;
import com.rabbiter.em.service.GoodService;
import com.rabbiter.em.service.OrderService;
import com.rabbiter.em.utils.TokenUtils;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MCP 工具服务 - 封装商城查询功能为 LLM 可调用的工具
 * 所有方法均基于现有 Service，不影响原有功能
 */
@Component
public class MallToolService {

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
    private BaiduYouxuanMcpService baiduYouxuanMcpService;
    @Resource
    private FliggyMcpService fliggyMcpService;

    @Tool("搜索商城商品，根据关键词查找商品列表。用户想买什么东西时调用此工具。返回商品ID、名称、价格、图片等信息")
    public String searchProducts(
            @P("搜索关键词，比如商品名称或类型，如'手机'、'运动鞋'、'笔记本电脑'") String keyword
    ) {
        try {
            // 使用百度优选MCP搜索真实商品
            if (baiduYouxuanMcpService != null) {
                return baiduYouxuanMcpService.searchYouxuanProducts(keyword);
            }
            return "{\"error\":\"百度优选MCP服务未初始化\"}";
        } catch (Exception e) {
            return "{\"error\":\"搜索失败：" + e.getMessage() + "\"}";
        }
    }

    @Tool("获取商品的详细信息，包括名称、价格、规格、描述、销量、评分等。当用户想了解某个商品详情时调用")
    public String getProductDetail(
            @P("商品ID，从搜索结果中获取") Long productId
    ) {
        try {
            Good good = goodService.getGoodById(productId);
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
        try {
            List<GoodDTO> goods = goodService.findFrontGoods();
            List<Map<String, Object>> results = goods.stream().map(g -> {
                Map<String, Object> item = new HashMap<>();
                item.put("id", g.getId());
                item.put("name", g.getName());
                item.put("price", g.getPrice());
                item.put("imgs", g.getImgs());
                return item;
            }).collect(Collectors.toList());
            return JSON.toJSONString(Map.of("goods", results));
        } catch (Exception e) {
            return "{\"error\":\"获取推荐商品失败：" + e.getMessage() + "\"}";
        }
    }

    @Tool("获取指定商品的用户评价。用户想看商品评价、口碑时调用")
    public String getProductReviews(
            @P("商品ID") Long productId
    ) {
        try {
            List<Review> reviews = reviewMapper.selectReviewListWithUser(productId, 1, 0, 5);
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
        try {
            if (userId == null) {
                return "{\"error\":\"用户ID不能为空\"}";
            }
            List<Map<String, Object>> cartItems = cartService.selectByUserId(userId);
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
        try {
            if (fliggyMcpService != null) {
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
        try {
            if (fliggyMcpService != null) {
                return fliggyMcpService.searchHotels(city, checkInDate, checkOutDate);
            }
            return "{\"error\":\"飞猪MCP未初始化\"}";
        } catch (Exception e) {
            return "{\"error\":\"搜索酒店失败：" + e.getMessage() + "\"}";
        }
    }
}
