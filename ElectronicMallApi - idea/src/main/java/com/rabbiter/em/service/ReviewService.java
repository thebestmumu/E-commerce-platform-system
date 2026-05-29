package com.rabbiter.em.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rabbiter.em.entity.Review;
import com.rabbiter.em.entity.ReviewReply;
import java.util.List;
import java.util.Map;

/**
 * 评论服务接口
 */
public interface ReviewService {
    
    /**
     * 查询商品评论列表
     */
    Page<Review> getReviewListByGoodId(Long goodId, Integer status, Integer pageNum, Integer pageSize);
    
    /**
     * 查询用户的评论列表
     */
    Page<Review> getUserReviewList(Long userId, Integer status, Integer pageNum, Integer pageSize);
    
    /**
     * 查询商品的第一条五星评论
     */
    Review getFirstFiveStarReview(Long goodId);
    
    /**
     * 查询热门评论
     */
    List<Review> getHotReviews(Long goodId, Integer status, Integer limit);
    
    /**
     * 查询有图片的评论
     */
    Page<Review> getReviewsWithImages(Long goodId, Integer status, Integer pageNum, Integer pageSize);
    
    /**
     * 统计商品评论数据
     */
    Map<String, Object> getReviewStatistics(Long goodId);
    
    /**
     * 发布评论
     */
    Review publishReview(Review review);
    
    /**
     * 回复评论
     */
    ReviewReply replyReview(ReviewReply reply);
    
    /**
     * 点赞评论
     */
    void likeReview(Long reviewId);
    
    /**
     * 点踩评论
     */
    void dislikeReview(Long reviewId);
    
    /**
     * 删除评论
     */
    void deleteReview(Long reviewId);
    
    /**
     * 审核评论
     */
    void auditReview(Long reviewId, Integer status);
    
    /**
     * 更新商品评论统计
     */
    void updateReviewStatistics(Long goodId);
}
