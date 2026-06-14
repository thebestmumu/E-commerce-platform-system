package com.rabbiter.em.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rabbiter.em.constants.Constants;
import com.rabbiter.em.entity.Good;
import com.rabbiter.em.entity.Order;
import com.rabbiter.em.entity.OrderGoods;
import com.rabbiter.em.entity.OrderItem;
import com.rabbiter.em.entity.dto.OrderAnalyticsDTO;
import com.rabbiter.em.exception.ServiceException;
import com.rabbiter.em.mapper.GoodMapper;
import com.rabbiter.em.mapper.OrderGoodsMapper;
import com.rabbiter.em.mapper.OrderMapper;
import com.rabbiter.em.mapper.StandardMapper;
import com.rabbiter.em.utils.TokenUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.rabbiter.em.constants.RedisConstants.GOOD_TOKEN_KEY;

@Service
public class OrderService extends ServiceImpl<OrderMapper, Order> {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    @Resource
    private OrderMapper orderMapper;
    @Resource
    private OrderGoodsMapper orderGoodsMapper;
    @Resource
    private StandardMapper standardMapper;
    @Resource
    private GoodMapper goodMapper;
    @Resource
    private CartService cartService;
    @Resource
    private RedisTemplate<String, Good> redisTemplate;

    @Transactional
    public String saveOrder(Order order) {
        order.setUserId(TokenUtils.getCurrentUser().getId());
        String orderNo = DateUtil.format(new Date(), "yyyyMMddHHmmss") + RandomUtil.randomNumbers(6);
        order.setOrderNo(orderNo);
        order.setCreateTime(DateUtil.now());
        
        // 从商品获取发货地址，如果没有则随机生成一个全国地址
        String goods = order.getGoods();
        List<OrderItem> orderItems = JSON.parseArray(goods, OrderItem.class);
        if (orderItems != null && !orderItems.isEmpty()) {
            Long goodId = orderItems.get(0).getId();
            Good good = goodMapper.selectById(goodId);
            if (good != null && good.getDeliveryAddress() != null && !good.getDeliveryAddress().isEmpty()) {
                order.setDeliveryAddress(good.getDeliveryAddress());
            } else {
                // 随机生成全国发货地址
                order.setDeliveryAddress(generateRandomDeliveryAddress());
            }
        }
        
        orderMapper.insert(order);

        //遍历order里携带的goods数组，并用orderItem对象来接收
        for (OrderItem orderItem : orderItems) {
            OrderGoods orderGoods = new OrderGoods();
            orderGoods.setOrderId(order.getId());
            long good_id = orderItem.getId();
            String standard = orderItem.getStandard();
            int num = orderItem.getNum();
            orderGoods.setGoodId(good_id);
            orderGoods.setCount(num);
            orderGoods.setStandard(standard);
            //插入到order_good表
            orderGoodsMapper.insert(orderGoods);
        }
        // 清除购物车（支持多个cartId，逗号分隔）
        String cartIdStr = order.getCartId();
        if (cartIdStr != null && !cartIdStr.isEmpty()) {
            String[] cartIds = cartIdStr.split(",");
            for (String cartId : cartIds) {
                try {
                    cartService.removeById(Long.parseLong(cartId.trim()));
                } catch (NumberFormatException e) {
                    log.warn("无效的cartId: {}", cartId);
                }
            }
        }
        return orderNo;
    }

    //给订单付款
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(String orderNo) {
        log.info("开始处理订单支付，订单号：{}", orderNo);
        
        // 先检查订单是否存在
        LambdaQueryWrapper<Order> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(Order::getOrderNo, orderNo);
        Order existingOrder = getOne(checkWrapper);
        
        if (existingOrder == null) {
            throw new ServiceException(Constants.CODE_500, "订单不存在");
        }
        
        // 如果订单已经是"已支付"状态，直接返回（幂等性处理）
        if ("已支付".equals(existingOrder.getState())) {
            log.info("订单已支付，无需重复处理，订单号：{}", orderNo);
            return;
        }
        
        // 获取订单项
        List<Map<String, Object>> orderItems = orderMapper.selectByOrderNo(orderNo);
        
        if (orderItems == null || orderItems.isEmpty()) {
            throw new ServiceException(Constants.CODE_500, "订单项为空");
        }
        
        log.info("订单项数量：{}", orderItems.size());
        
        // 先扣减库存（如果失败会抛出异常，事务回滚，状态不会更新）
        for (Map<String, Object> orderMap : orderItems) {
            // 安全获取 count
            Object countObj = orderMap.get("count");
            int count = 0;
            if (countObj instanceof Integer) {
                count = (Integer) countObj;
            } else if (countObj instanceof Long) {
                count = ((Long) countObj).intValue();
            } else if (countObj != null) {
                try {
                    count = Integer.parseInt(countObj.toString());
                } catch (NumberFormatException e) {
                    throw new ServiceException(Constants.CODE_500, "商品数量格式错误");
                }
            }
            
            // 安全获取 goodId
            Object goodIdObj = orderMap.get("goodId");
            Long goodId = null;
            if (goodIdObj instanceof Long) {
                goodId = (Long) goodIdObj;
            } else if (goodIdObj instanceof Integer) {
                goodId = ((Integer) goodIdObj).longValue();
            } else if (goodIdObj != null) {
                try {
                    goodId = Long.parseLong(goodIdObj.toString());
                } catch (NumberFormatException e) {
                    throw new ServiceException(Constants.CODE_500, "商品ID格式错误");
                }
            }

            if (goodId == null) {
                throw new ServiceException(Constants.CODE_500, "商品ID不存在");
            }
            
            // 安全获取 standard
            String standard = (String) orderMap.get("standard");
            if (standard == null || standard.trim().isEmpty()) {
                standard = "默认";
            }
            
            log.info("扣减库存 - goodId: {}, standard: {}, count: {}", goodId, standard, count);
            
            // 检查库存
            int store = standardMapper.getStore(goodId, standard);
            if (store < count) {
                throw new ServiceException(Constants.CODE_500, "商品库存不足");
            }
            
            // 扣减库存
            standardMapper.deductStore(goodId, standard, store - count);
            log.info("库存扣减成功 - goodId: {}, 原库存: {}, 扣减后: {}", goodId, store, store - count);
        }
        
        // 库存扣减成功后，更新订单状态为已支付
        LambdaUpdateWrapper<Order> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Order::getOrderNo, orderNo)
                .set(Order::getState, "已支付");
        update(updateWrapper);
        
        log.info("订单状态已更新为已支付，订单号：{}", orderNo);
        
        // 获取订单总价，更新商品销量和销售额
        BigDecimal totalPrice = existingOrder.getTotalPrice();
        
        for (Map<String, Object> orderMap : orderItems) {
            Object countObj = orderMap.get("count");
            int count = 0;
            if (countObj instanceof Integer) {
                count = (Integer) countObj;
            } else if (countObj instanceof Long) {
                count = ((Long) countObj).intValue();
            }
            
            Object goodIdObj = orderMap.get("goodId");
            Long goodId = null;
            if (goodIdObj instanceof Long) {
                goodId = (Long) goodIdObj;
            } else if (goodIdObj instanceof Integer) {
                goodId = ((Integer) goodIdObj).longValue();
            }
            
            if (goodId == null) {
                continue;
            }
            
            // 更新商品销量和销售额
            goodMapper.saleGood(goodId, count, totalPrice);
            log.info("更新商品销量 - goodId: {}, count: {}", goodId, count);

            // redis 增销量
            try {
                String redisKey = GOOD_TOKEN_KEY + goodId;
                ValueOperations<String, Good> valueOperations = redisTemplate.opsForValue();
                Good good = valueOperations.get(redisKey);
                if (!ObjectUtils.isEmpty(good)) {
                    good.setSales(good.getSales() + count);
                    valueOperations.set(redisKey, good);
                }
            } catch (Exception e) {
                log.warn("Redis更新销量失败，goodId: {}", goodId, e);
            }
        }
        
        log.info("订单支付处理完成，订单号：{}", orderNo);
    }

    public List<Map<String, Object>> selectByUserId(int userId) {
        return orderMapper.selectByUserId(userId);
    }

    public boolean receiveOrder(String orderNo) {
        return orderMapper.receiveOrder(orderNo);
    }

    public List<Map<String, Object>> selectByOrderNo(String orderNo) {
        return orderMapper.selectByOrderNo(orderNo);
    }

    public Order getOrderByNo(String orderNo) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderNo, orderNo);
        return getOne(wrapper);
    }

    @Transactional
    public boolean cancelOrder(String orderNo) {
        Order order = getOrderByNo(orderNo);
        if (order == null) {
            return false;
        }
        String state = order.getState();
        if ("待支付".equals(state) || "已支付".equals(state)) {
            LambdaUpdateWrapper<Order> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(Order::getOrderNo, orderNo)
                    .set(Order::getState, "已取消");
            return update(wrapper);
        }
        return false;
    }

    public void delivery(String orderNo, String deliveryAddress, String expressCompany, String expressNo) {
        LambdaUpdateWrapper<Order> orderLambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        orderLambdaUpdateWrapper.eq(Order::getOrderNo, orderNo)
                .set(Order::getState, "已发货");
        if (deliveryAddress != null && !deliveryAddress.isEmpty()) {
            orderLambdaUpdateWrapper.set(Order::getDeliveryAddress, deliveryAddress);
        }
        if (expressCompany != null && !expressCompany.isEmpty()) {
            orderLambdaUpdateWrapper.set(Order::getExpressCompany, expressCompany);
        }
        if (expressNo != null && !expressNo.isEmpty()) {
            orderLambdaUpdateWrapper.set(Order::getExpressNo, expressNo);
        }
        update(orderLambdaUpdateWrapper);
    }

    /**
     * 订单退款
     * @param orderNo 订单号
     */
    @Transactional
    public void refundOrder(String orderNo) {
        LambdaUpdateWrapper<Order> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Order::getOrderNo, orderNo)
                .set(Order::getState, "已退款");
        update(updateWrapper);
    }
    
    /**
     * 获取订单分析数据
     * @param userId 用户ID
     * @param year 年份（可选）
     * @param month 月份（可选）
     * @return 分析数据
     */
    public OrderAnalyticsDTO getOrderAnalytics(Integer userId, Integer year, Integer month) {
        OrderAnalyticsDTO analytics = new OrderAnalyticsDTO();
        
        log.info("=== 开始分析订单数据 ===");
        log.info("userId: {}, year: {}, month: {}", userId, year, month);
        
        // 获取用户所有订单
        List<Map<String, Object>> orders = selectByUserId(userId);
        
        log.info("获取到订单数量：{}", orders != null ? orders.size() : 0);
        
        if (orders == null || orders.isEmpty()) {
            log.info("没有订单数据");
            return analytics;
        }
        
        // 按年份统计消费金额
        Map<Integer, BigDecimal> yearlyConsumption = new HashMap<>();
        // 按月份统计消费金额
        Map<Integer, BigDecimal> monthlyConsumption = new HashMap<>();
        
        for (Map<String, Object> order : orders) {
            try {
                // SQL返回的是驼峰命名（createTime, totalPrice）
                Object createTimeObj = order.get("createTime");
                Object totalPriceObj = order.get("totalPrice");
                
                log.info("订单数据 - createTime: {}, totalPrice: {}", createTimeObj, totalPriceObj);
                
                if (createTimeObj != null && totalPriceObj != null) {
                    // 解析日期
                    String createTimeStr = createTimeObj.toString();
                    Date createTime = null;
                    try {
                        // 尝试解析 ISO 格式日期
                        createTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(createTimeStr);
                        log.info("成功解析 ISO 格式日期：{}", createTimeStr);
                    } catch (Exception e) {
                        try {
                            // 尝试解析标准格式
                            createTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(createTimeStr);
                            log.info("成功解析标准格式日期：{}", createTimeStr);
                        } catch (Exception e2) {
                            try {
                                // 尝试解析只有日期的格式
                                createTime = new SimpleDateFormat("yyyy-MM-dd").parse(createTimeStr);
                                log.info("成功解析日期格式：{}", createTimeStr);
                            } catch (Exception e3) {
                                log.warn("日期解析失败：{}", createTimeStr);
                                continue;
                            }
                        }
                    }
                    
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(createTime);
                    
                    int orderYear = cal.get(Calendar.YEAR);
                    int orderMonth = cal.get(Calendar.MONTH) + 1; // 月份从 1 开始
                    
                    log.info("订单年份：{}, 订单月份：{}", orderYear, orderMonth);
                    
                    BigDecimal totalPrice = new BigDecimal(totalPriceObj.toString());
                    
                    // 按年份统计
                    yearlyConsumption.put(orderYear, 
                        yearlyConsumption.getOrDefault(orderYear, BigDecimal.ZERO).add(totalPrice));
                    log.info("统计年度数据：{}年，金额：{}", orderYear, totalPrice);
                    
                    // 按月份统计（考虑年份和月份筛选）
                    boolean yearMatch = (year == null || orderYear == year);
                    boolean monthMatch = (month == null || month == 0 || orderMonth == month);
                    
                    // 如果年份和月份都匹配，统计到月度数据中
                    if (yearMatch && monthMatch) {
                        monthlyConsumption.put(orderMonth, 
                            monthlyConsumption.getOrDefault(orderMonth, BigDecimal.ZERO).add(totalPrice));
                        log.info("统计月度数据：{}月，金额：{}", orderMonth, totalPrice);
                    }
                }
            } catch (Exception e) {
                log.info("订单处理异常：{}", e.getMessage());
            }
        }
        
        log.info("年度消费数据：{}", yearlyConsumption);
        log.info("月度消费数据：{}", monthlyConsumption);
        
        analytics.setYearlyConsumption(yearlyConsumption);
        analytics.setMonthlyConsumption(monthlyConsumption);
        
        return analytics;
    }
    
    /**
     * 生成随机全国发货地址
     */
    private String generateRandomDeliveryAddress() {
        String[] provinces = {
            "北京市", "上海市", "天津市", "重庆市", "河北省", "山西省", "辽宁省", 
            "吉林省", "黑龙江省", "江苏省", "浙江省", "安徽省", "福建省", "江西省", 
            "山东省", "河南省", "湖北省", "湖南省", "广东省", "海南省", "四川省", 
            "贵州省", "云南省", "陕西省", "甘肃省", "青海省", "台湾省", "内蒙古自治区", 
            "广西壮族自治区", "西藏自治区", "宁夏回族自治区", "新疆维吾尔自治区"
        };
        
        String[] cities = {
            "朝阳区", "海淀区", "浦东新区", "黄浦区", "和平区", "南开区", "和平区", 
            "南关区", "道里区", "玄武区", "西湖区", "蜀山区", "鼓楼区", "东湖区", 
            "历下区", "金水区", "武昌区", "岳麓区", "天河区", "龙华区", "锦江区", 
            "南明区", "盘龙区", "雁塔区", "城关区", "城东区", "中正区", "新城区", 
            "青秀区", "城关区", "兴庆区", "天山区"
        };
        
        String[] streets = {
            "中关村大街", "建国路", "南京路", "人民路", "解放路", "中山路", "和平路",
            "建设路", "文化路", "光明路", "幸福路", "胜利路", "发展路", "创新路",
            "科技路", "工业路", "商业路", "学府路", "花园路", "春晖路"
        };
        
        int provinceIndex = RandomUtil.randomInt(provinces.length);
        int cityIndex = RandomUtil.randomInt(cities.length);
        int streetIndex = RandomUtil.randomInt(streets.length);
        int streetNum = RandomUtil.randomInt(1, 999);
        
        return provinces[provinceIndex] + cities[cityIndex] + streets[streetIndex] + streetNum + "号";
    }
}
