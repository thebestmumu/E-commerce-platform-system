package com.rabbiter.em.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rabbiter.em.entity.Review;
import com.rabbiter.em.entity.ReviewReply;
import com.rabbiter.em.entity.Good;
import com.rabbiter.em.mapper.ReviewMapper;
import com.rabbiter.em.mapper.ReviewReplyMapper;
import com.rabbiter.em.mapper.GoodMapper;
import com.rabbiter.em.service.ReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 评论服务实现类
 */
@Slf4j
@Service
public class ReviewServiceImpl implements ReviewService {
    
    @Autowired
    private ReviewMapper reviewMapper;
    
    @Autowired
    private ReviewReplyMapper reviewReplyMapper;
    
    @Autowired
    private GoodMapper goodMapper;
    
    @Override
    public Page<Review> getReviewListByGoodId(Long goodId, Integer status, Integer pageNum, Integer pageSize) {
        Page<Review> page = new Page<>(pageNum, pageSize);
        int offset = (pageNum - 1) * pageSize;
        
        List<Review> reviews = reviewMapper.selectReviewListWithUser(goodId, status, offset, pageSize);
        int total = reviewMapper.countByGoodId(goodId, status);
        
        page.setRecords(reviews);
        page.setTotal(total);
        
        // 加载每条评论的回复
        for (Review review : reviews) {
            List<ReviewReply> replies = reviewReplyMapper.selectNestedReplies(goodId, review.getId(), status);
            review.setReplies(replies);
        }
        
        return page;
    }
    
    @Override
    public Page<Review> getUserReviewList(Long userId, Integer status, Integer pageNum, Integer pageSize) {
        Page<Review> page = new Page<>(pageNum, pageSize);
        int offset = (pageNum - 1) * pageSize;
        
        List<Review> reviews = reviewMapper.selectUserReviewList(userId, status, offset, pageSize);
        long total = reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
                .eq(Review::getUserId, userId)
                .eq(Review::getStatus, status));
        
        page.setRecords(reviews);
        page.setTotal(total);
        
        return page;
    }
    
    @Override
    public Review getFirstFiveStarReview(Long goodId) {
        return reviewMapper.selectFirstFiveStarReview(goodId);
    }
    
    @Override
    public List<Review> getHotReviews(Long goodId, Integer status, Integer limit) {
        return reviewMapper.selectHotReviews(goodId, status, limit);
    }
    
    @Override
    public Page<Review> getReviewsWithImages(Long goodId, Integer status, Integer pageNum, Integer pageSize) {
        Page<Review> page = new Page<>(pageNum, pageSize);
        int offset = (pageNum - 1) * pageSize;
        
        List<Review> reviews = reviewMapper.selectReviewsWithImages(goodId, status, offset, pageSize);
        long total = reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
                .eq(Review::getGoodId, goodId)
                .eq(Review::getStatus, status)
                .isNotNull(Review::getImages));
        
        page.setRecords(reviews);
        page.setTotal(total);
        
        return page;
    }
    
    @Override
    public Map<String, Object> getReviewStatistics(Long goodId) {
        Map<String, Object> stats = reviewMapper.countRatingByGoodId(goodId, 1);
        
        if (stats == null) {
            stats = new HashMap<>();
            stats.put("total", 0);
            stats.put("rating_5_count", 0);
            stats.put("rating_4_count", 0);
            stats.put("rating_3_count", 0);
            stats.put("rating_2_count", 0);
            stats.put("rating_1_count", 0);
            stats.put("avg_rating", 5.00);
        }
        
        return stats;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Review publishReview(Review review) {
        review.setCreateTime(LocalDateTime.now());
        review.setUpdateTime(LocalDateTime.now());
        review.setStatus(1); // 默认直接发布，可改为 0 待审核
        
        if (reviewMapper.insert(review) > 0) {
            // 更新商品评论统计
            updateReviewStatistics(review.getGoodId());
            return review;
        }
        return null;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReviewReply replyReview(ReviewReply reply) {
        reply.setCreateTime(LocalDateTime.now());
        reply.setUpdateTime(LocalDateTime.now());
        reply.setStatus(1);
        
        if (reviewReplyMapper.insert(reply) > 0) {
            // 更新评论的回复数
            Review review = reviewMapper.selectById(reply.getReviewId());
            if (review != null) {
                review.setReplyCount(review.getReplyCount() + 1);
                reviewMapper.updateById(review);
            }
            return reply;
        }
        return null;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeReview(Long reviewId) {
        Review review = reviewMapper.selectById(reviewId);
        if (review != null) {
            review.setLikeCount(review.getLikeCount() + 1);
            reviewMapper.updateById(review);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dislikeReview(Long reviewId) {
        Review review = reviewMapper.selectById(reviewId);
        if (review != null) {
            review.setDislikeCount(review.getDislikeCount() + 1);
            reviewMapper.updateById(review);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReview(Long reviewId) {
        Review review = reviewMapper.selectById(reviewId);
        if (review != null) {
            // 软删除：将状态改为 2（已屏蔽）
            review.setStatus(2);
            reviewMapper.updateById(review);
            
            // 更新商品评论统计
            updateReviewStatistics(review.getGoodId());
        }
    }
    
    @Override
    public void auditReview(Long reviewId, Integer status) {
        Review review = reviewMapper.selectById(reviewId);
        if (review != null) {
            review.setStatus(status);
            reviewMapper.updateById(review);
            
            // 如果审核通过，更新商品评论统计
            if (status == 1) {
                updateReviewStatistics(review.getGoodId());
            }
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateReviewStatistics(Long goodId) {
        reviewMapper.updateReviewStatistics(goodId);
        
        // 同时更新商品表的字段
        Map<String, Object> stats = getReviewStatistics(goodId);
        Good good = goodMapper.selectById(goodId);
        if (good != null) {
            good.setReviewCount((Integer) stats.get("total"));
            Object ratingObj = stats.get("avg_rating");
            if (ratingObj != null) {
                good.setGoodRating(new java.math.BigDecimal(ratingObj.toString()));
            }
            good.setRating5Count((Integer) stats.get("rating_5_count"));
            good.setRating4Count((Integer) stats.get("rating_4_count"));
            good.setRating3Count((Integer) stats.get("rating_3_count"));
            good.setRating2Count((Integer) stats.get("rating_2_count"));
            good.setRating1Count((Integer) stats.get("rating_1_count"));
            goodMapper.updateById(good);
        }
    }
}
