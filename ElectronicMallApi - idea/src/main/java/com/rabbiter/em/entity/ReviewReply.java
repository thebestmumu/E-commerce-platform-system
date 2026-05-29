package com.rabbiter.em.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论回复实体类
 */
@TableName("review_reply")
public class ReviewReply extends Model<ReviewReply> {
    
    /**
     * 回复 ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 评论 ID
     */
    @TableField("review_id")
    private Long reviewId;
    
    /**
     * 回复用户 ID
     */
    @TableField("user_id")
    private Long userId;
    
    /**
     * 父回复 ID（用于楼中楼）
     */
    @TableField("parent_id")
    private Long parentId;
    
    /**
     * 被回复用户 ID
     */
    @TableField("to_user_id")
    private Long toUserId;
    
    /**
     * 回复内容
     */
    @TableField("content")
    private String content;
    
    /**
     * 点赞数量
     */
    @TableField("like_count")
    private Integer likeCount;
    
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
     * 回复用户信息
     */
    @TableField(exist = false)
    private User user;
    
    /**
     * 被回复用户信息
     */
    @TableField(exist = false)
    private User toUser;
    
    /**
     * 父回复信息
     */
    @TableField(exist = false)
    private ReviewReply parent;
    
    /**
     * 子回复列表
     */
    @TableField(exist = false)
    private List<ReviewReply> children;
    
    // Getters and Setters
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getReviewId() {
        return reviewId;
    }
    
    public void setReviewId(Long reviewId) {
        this.reviewId = reviewId;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Long getParentId() {
        return parentId;
    }
    
    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }
    
    public Long getToUserId() {
        return toUserId;
    }
    
    public void setToUserId(Long toUserId) {
        this.toUserId = toUserId;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public Integer getLikeCount() {
        return likeCount;
    }
    
    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
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
    
    public User getToUser() {
        return toUser;
    }
    
    public void setToUser(User toUser) {
        this.toUser = toUser;
    }
    
    public ReviewReply getParent() {
        return parent;
    }
    
    public void setParent(ReviewReply parent) {
        this.parent = parent;
    }
    
    public List<ReviewReply> getChildren() {
        return children;
    }
    
    public void setChildren(List<ReviewReply> children) {
        this.children = children;
    }
}
