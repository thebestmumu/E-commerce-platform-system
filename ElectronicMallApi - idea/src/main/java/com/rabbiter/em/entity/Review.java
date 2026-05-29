package com.rabbiter.em.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品评论实体类
 */
@TableName("review")
public class Review extends Model<Review> {
    
    /**
     * 评论 ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 用户 ID
     */
    @TableField("user_id")
    private Long userId;
    
    /**
     * 商品 ID
     */
    @TableField("good_id")
    private Long goodId;
    
    /**
     * 订单 ID（可选，验证购买）
     */
    @TableField("order_id")
    private Long orderId;
    
    /**
     * 评分：1-5 星
     */
    @TableField("rating")
    private Integer rating;
    
    /**
     * 评论内容
     */
    @TableField("content")
    private String content;
    
    /**
     * 评论图片（多张图片用逗号分隔）
     */
    @TableField("images")
    private String images;
    
    /**
     * 评论标签（如：质量好、物流快、包装精美）
     */
    @TableField("tags")
    private String tags;
    
    /**
     * 回复数量
     */
    @TableField("reply_count")
    private Integer replyCount;
    
    /**
     * 点赞数量
     */
    @TableField("like_count")
    private Integer likeCount;
    
    /**
     * 点踩数量
     */
    @TableField("dislike_count")
    private Integer dislikeCount;
    
    /**
     * 是否匿名：0-否，1-是
     */
    @TableField("is_anonymous")
    private Boolean isAnonymous;
    
    /**
     * 状态：0-待审核，1-已发布，2-已屏蔽
     */
    @TableField("status")
    private Integer status;
    
    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;
    
    // ===== 扩展字段（非数据库字段） =====
    
    /**
     * 用户信息
     */
    @TableField(exist = false)
    private User user;
    
    /**
     * 商品信息
     */
    @TableField(exist = false)
    private Good good;
    
    /**
     * 回复列表
     */
    @TableField(exist = false)
    private List<ReviewReply> replies;
    
    // Getters and Setters
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Long getGoodId() {
        return goodId;
    }
    
    public void setGoodId(Long goodId) {
        this.goodId = goodId;
    }
    
    public Long getOrderId() {
        return orderId;
    }
    
    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
    
    public Integer getRating() {
        return rating;
    }
    
    public void setRating(Integer rating) {
        this.rating = rating;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getImages() {
        return images;
    }
    
    public void setImages(String images) {
        this.images = images;
    }
    
    public String getTags() {
        return tags;
    }
    
    public void setTags(String tags) {
        this.tags = tags;
    }
    
    public Integer getReplyCount() {
        return replyCount;
    }
    
    public void setReplyCount(Integer replyCount) {
        this.replyCount = replyCount;
    }
    
    public Integer getLikeCount() {
        return likeCount;
    }
    
    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }
    
    public Integer getDislikeCount() {
        return dislikeCount;
    }
    
    public void setDislikeCount(Integer dislikeCount) {
        this.dislikeCount = dislikeCount;
    }
    
    public Boolean getIsAnonymous() {
        return isAnonymous;
    }
    
    public void setIsAnonymous(Boolean isAnonymous) {
        this.isAnonymous = isAnonymous;
    }
    
    public Integer getStatus() {
        return status;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
    }
    
    public LocalDateTime getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
    
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }
    
    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public Good getGood() {
        return good;
    }
    
    public void setGood(Good good) {
        this.good = good;
    }
    
    public List<ReviewReply> getReplies() {
        return replies;
    }
    
    public void setReplies(List<ReviewReply> replies) {
        this.replies = replies;
    }
}
