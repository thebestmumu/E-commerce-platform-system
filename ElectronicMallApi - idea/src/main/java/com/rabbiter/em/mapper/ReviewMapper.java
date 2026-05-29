package com.rabbiter.em.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rabbiter.em.entity.Review;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

/**
 * 评论 Mapper 接口
 */
@Mapper
public interface ReviewMapper extends BaseMapper<Review> {
    
    /**
     * 查询商品评论列表（带用户信息）
     */
    List<Review> selectReviewListWithUser(@Param("goodId") Long goodId, 
                                          @Param("status") Integer status,
                                          @Param("offset") Integer offset,
                                          @Param("limit") Integer limit);
    
    /**
     * 查询用户的评论列表
     */
    List<Review> selectUserReviewList(@Param("userId") Long userId,
                                      @Param("status") Integer status,
                                      @Param("offset") Integer offset,
                                      @Param("limit") Integer limit);
    
    /**
     * 统计商品评论数
     */
    Integer countByGoodId(@Param("goodId") Long goodId, @Param("status") Integer status);
    
    /**
     * 统计各星级评论数
     */
    Map<String, Object> countRatingByGoodId(@Param("goodId") Long goodId, @Param("status") Integer status);
    
    /**
     * 查询商品的第一条五星评论
     */
    Review selectFirstFiveStarReview(@Param("goodId") Long goodId);
    
    /**
     * 查询商品的平均评分
     */
    java.math.BigDecimal selectAvgRating(@Param("goodId") Long goodId);
    
    /**
     * 查询热门评论（点赞数高的）
     */
    List<Review> selectHotReviews(@Param("goodId") Long goodId,
                                  @Param("status") Integer status,
                                  @Param("limit") Integer limit);
    
    /**
     * 查询有图片的评论
     */
    List<Review> selectReviewsWithImages(@Param("goodId") Long goodId,
                                         @Param("status") Integer status,
                                         @Param("offset") Integer offset,
                                         @Param("limit") Integer limit);
    
    /**
     * 更新评论统计数据
     */
    void updateReviewStatistics(@Param("goodId") Long goodId);
    
    /**
     * 批量插入评论
     */
    Integer insertBatch(@Param("list") List<Review> reviews);
    
    /**
     * 查询最新评论（带用户信息）
     */
    List<Review> selectLatestReviewsWithUser(@Param("goodId") Long goodId,
                                              @Param("status") Integer status,
                                              @Param("limit") Integer limit);
    
    /**
     * 查询评分分布
     */
    List<Map<String, Object>> selectRatingDistribution(@Param("goodId") Long goodId);
}
