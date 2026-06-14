package com.rabbiter.em.mapper;

import com.rabbiter.em.entity.Order;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface OrderMapper extends BaseMapper<Order> {
    @Update("update t_order set state = '已支付' where order_no = #{orderNo}")
    void payOrder(String orderNo);

    @Select("SELECT o.id, o.order_no as orderNo, o.total_price as totalPrice, o.user_id as userId, " +
            "o.link_user as linkUser, o.link_phone as linkPhone, o.link_address as linkAddress, " +
            "o.state, o.create_time as createTime, o.delivery_address as deliveryAddress, " +
            "o.express_company as expressCompany, o.express_no as expressNo, " +
            "og.good_id as goodId, good.name as goodName, og.count, og.standard, good.imgs, " +
            "gs.price as goodPrice, good.discount " +
            "FROM t_order o " +
            "LEFT JOIN order_goods og ON o.id = og.order_id " +
            "LEFT JOIN good ON og.good_id = good.id " +
            "LEFT JOIN good_standard gs ON gs.good_id = og.good_id AND gs.value = og.standard " +
            "WHERE o.user_id = #{userId} ORDER BY o.create_time DESC")
    List<Map<String, Object>> selectByUserId(int userId);

    @Update("update t_order set state = '已收货' where order_no = #{orderNo}")
    boolean receiveOrder(String orderNo);

    @MapKey("goodId")
    @Select("SELECT o.id, o.order_no as orderNo, o.total_price as totalPrice, o.user_id as userId, o.link_user as linkUser, o.link_phone as linkPhone, o.link_address as linkAddress, o.state as status, o.create_time as createTime, o.delivery_address as deliveryAddress, o.express_company as expressCompany, o.express_no as expressNo, og.good_id as goodId, good.name as goodName, og.count, og.standard, good.imgs, gs.price, good.discount FROM t_order o LEFT JOIN order_goods og ON o.id = og.order_id LEFT JOIN good ON og.good_id = good.id LEFT JOIN good_standard gs ON gs.good_id = og.good_id AND gs.value = og.standard WHERE o.order_no = #{orderNo}")
    List<Map<String, Object>> selectByOrderNo(String orderNo);
    
    /**
     * 查询指定商品在指定时间范围内的销售趋势
     * @param goodId 商品ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 每日销售数据列表
     */
    @Select("SELECT DATE(o.create_time) as date, SUM(og.count) as sales, SUM(og.count * gs.price * IFNULL(good.discount, 1)) as revenue " +
            "FROM t_order o " +
            "LEFT JOIN order_goods og ON o.id = og.order_id " +
            "LEFT JOIN good_standard gs ON gs.good_id = og.good_id AND gs.value = og.standard " +
            "LEFT JOIN good ON og.good_id = good.id " +
            "WHERE og.good_id = #{goodId} " +
            "AND o.state = '已支付' " +
            "AND DATE(o.create_time) BETWEEN #{startDate} AND #{endDate} " +
            "GROUP BY DATE(o.create_time) " +
            "ORDER BY date ASC")
    List<Map<String, Object>> selectSalesTrendByGoodId(@Param("goodId") Long goodId, 
                                                       @Param("startDate") LocalDate startDate, 
                                                       @Param("endDate") LocalDate endDate);
    
    /**
     * 查询所有商品在指定时间范围内的销售趋势
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 每日销售数据列表
     */
    @Select("SELECT DATE(o.create_time) as date, SUM(og.count) as sales, SUM(og.count * gs.price * IFNULL(good.discount, 1)) as revenue " +
            "FROM t_order o " +
            "LEFT JOIN order_goods og ON o.id = og.order_id " +
            "LEFT JOIN good_standard gs ON gs.good_id = og.good_id AND gs.value = og.standard " +
            "LEFT JOIN good ON og.good_id = good.id " +
            "WHERE o.state = '已支付' " +
            "AND DATE(o.create_time) BETWEEN #{startDate} AND #{endDate} " +
            "GROUP BY DATE(o.create_time) " +
            "ORDER BY date ASC")
    List<Map<String, Object>> selectAllSalesTrend(@Param("startDate") LocalDate startDate, 
                                                  @Param("endDate") LocalDate endDate);
}
