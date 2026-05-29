package com.rabbiter.em.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 商品分类图标实体
 */
@TableName("category_icon")
public class CategoryIcon {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 分类名称
     */
    private String categoryName;
    
    /**
     * 图标 URL
     */
    private String iconUrl;
    
    /**
     * 图标 Emoji
     */
    private String iconEmoji;
    
    /**
     * 背景颜色
     */
    private String bgColor;
    
    /**
     * 排序
     */
    private Integer sortOrder;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public String getIconEmoji() {
        return iconEmoji;
    }

    public void setIconEmoji(String iconEmoji) {
        this.iconEmoji = iconEmoji;
    }

    public String getBgColor() {
        return bgColor;
    }

    public void setBgColor(String bgColor) {
        this.bgColor = bgColor;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
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

    @Override
    public String toString() {
        return "CategoryIcon{" +
                "id=" + id +
                ", categoryName='" + categoryName + '\'' +
                ", iconUrl='" + iconUrl + '\'' +
                ", iconEmoji='" + iconEmoji + '\'' +
                ", bgColor='" + bgColor + '\'' +
                ", sortOrder=" + sortOrder +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }
}
