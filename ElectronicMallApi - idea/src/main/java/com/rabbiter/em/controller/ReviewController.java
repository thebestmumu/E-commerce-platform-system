package com.rabbiter.em.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rabbiter.em.common.Result;
import com.rabbiter.em.constants.Constants;
import com.rabbiter.em.entity.Review;
import com.rabbiter.em.entity.ReviewReply;
import com.rabbiter.em.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 评论控制器
 */
@RestController
@RequestMapping("/api/review")
@CrossOrigin
public class ReviewController {
    
    @Autowired
    private ReviewService reviewService;
    
    /**
     * 查询商品评论列表
     */
    @GetMapping("/list/{goodId}")
    public Result getReviewList(
            @PathVariable Long goodId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(defaultValue = "1") Integer status) {
        
        Page<Review> page = reviewService.getReviewListByGoodId(goodId, status, pageNum, pageSize);
        return Result.success(page);
    }
    
    /**
     * 查询商品评论列表（兼容路径）
     */
    @GetMapping("/good/{goodId}")
    public Result getReviewListCompat(
            @PathVariable Long goodId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(defaultValue = "1") Integer status) {
        
        Page<Review> page = reviewService.getReviewListByGoodId(goodId, status, pageNum, pageSize);
        return Result.success(page);
    }
    
    /**
     * 查询商品的第一条五星评论
     */
    @GetMapping("/first/{goodId}")
    public Result getFirstFiveStarReview(@PathVariable Long goodId) {
        Review review = reviewService.getFirstFiveStarReview(goodId);
        return Result.success(review);
    }
    
    /**
     * 查询热门评论
     */
    @GetMapping("/hot/{goodId}")
    public Result getHotReviews(
            @PathVariable Long goodId,
            @RequestParam(defaultValue = "3") Integer limit) {
        
        List<Review> reviews = reviewService.getHotReviews(goodId, 1, limit);
        return Result.success(reviews);
    }
    
    /**
     * 查询有图片的评论
     */
    @GetMapping("/with-images/{goodId}")
    public Result getReviewsWithImages(
            @PathVariable Long goodId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        
        Page<Review> page = reviewService.getReviewsWithImages(goodId, 1, pageNum, pageSize);
        return Result.success(page);
    }
    
    /**
     * 查询商品评论统计
     */
    @GetMapping("/statistics/{goodId}")
    public Result getReviewStatistics(@PathVariable Long goodId) {
        Map<String, Object> stats = reviewService.getReviewStatistics(goodId);
        return Result.success(stats);
    }
    
    /**
     * 发布评论
     */
    @PostMapping("/publish")
    public Result publishReview(@RequestBody Review review) {
        Review created = reviewService.publishReview(review);
        if (created != null) {
            return Result.success(created);
        }
        return Result.error(Constants.CODE_500, "发布评论失败");
    }
    
    /**
     * 回复评论
     */
    @PostMapping("/reply")
    public Result replyReview(@RequestBody ReviewReply reply) {
        ReviewReply created = reviewService.replyReview(reply);
        if (created != null) {
            return Result.success(created);
        }
        return Result.error(Constants.CODE_500, "回复评论失败");
    }
    
    /**
     * 点赞评论
     */
    @PostMapping("/like/{reviewId}")
    public Result likeReview(@PathVariable Long reviewId) {
        reviewService.likeReview(reviewId);
        return Result.success(null);
    }
    
    /**
     * 点踩评论
     */
    @PostMapping("/dislike/{reviewId}")
    public Result dislikeReview(@PathVariable Long reviewId) {
        reviewService.dislikeReview(reviewId);
        return Result.success(null);
    }
    
    /**
     * 删除评论
     */
    @DeleteMapping("/{reviewId}")
    public Result deleteReview(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
        return Result.success(null);
    }
    
    /**
     * 审核评论
     */
    @PutMapping("/audit/{reviewId}")
    public Result auditReview(
            @PathVariable Long reviewId,
            @RequestParam Integer status) {
        reviewService.auditReview(reviewId, status);
        return Result.success(null);
    }
}
