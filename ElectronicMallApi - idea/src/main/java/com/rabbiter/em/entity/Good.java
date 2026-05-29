package com.rabbiter.em.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.baomidou.mybatisplus.annotation.TableId;

import java.math.BigDecimal;
import java.util.List;

@TableName("good")
public class Good extends Model<Good> {
    /**
      * 主键
      */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
      * 商品名称 
      */
    private String name;

    /**
      * 商品描述 
      */
    private String description;



    /**
      * 折扣 
      */
    private Double discount;


    /**
      * 销量 
      */
    private Integer sales;

    /*
    *销售额
    */
    private BigDecimal saleMoney;

    /**
      * 分类id 
      */
    private Long categoryId;

    /**
      * 商品图片 
      */
    private String imgs;

    /**
      * 创建时间 
      */
    private String createTime;

    /**
      * 是否推荐：0不推荐，1推荐 
      */
    private Boolean recommend;


    /**
     * 是否删除
     */
    private Boolean isDelete;
    
    /**
     * 商品发货地址
     */
    private String deliveryAddress;
    
    /**
     * 评论总数
     */
    @TableField("review_count")
    private Integer reviewCount;
    
    /**
     * 商品评分（1-5 分）
     */
    @TableField("good_rating")
    private BigDecimal goodRating;
    
    /**
     * 5 星评价数量
     */
    @TableField("rating_5_count")
    private Integer rating5Count;
    
    /**
     * 4 星评价数量
     */
    @TableField("rating_4_count")
    private Integer rating4Count;
    
    /**
     * 3 星评价数量
     */
    @TableField("rating_3_count")
    private Integer rating3Count;
    
    /**
     * 2 星评价数量
     */
    @TableField("rating_2_count")
    private Integer rating2Count;
    
    /**
     * 1 星评价数量
     */
    @TableField("rating_1_count")
    private Integer rating1Count;
    
    /**
     * 商品标签（从评论中提取的热门标签）
     */
    @TableField("tags")
    private String tags;
    
    /**
     * 原价
     */
    @TableField(exist = false)
    private BigDecimal price;
    
    /**
     * 规格列表
     */
    @TableField(exist = false)
    private List<String> standardList;
    
    /**
     * 第一条五星评论
     */
    @TableField(exist = false)
    private Review firstReview;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getDiscount() {
        return discount;
    }

    public void setDiscount(Double discount) {
        this.discount = discount;
    }

    public Integer getSales() {
        return sales;
    }

    public void setSales(Integer sales) {
        this.sales = sales;
    }

    public BigDecimal getSaleMoney() {
        return saleMoney;
    }

    public void setSaleMoney(BigDecimal saleMoney) {
        this.saleMoney = saleMoney;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getImgs() {
        return imgs;
    }

    public void setImgs(String imgs) {
        this.imgs = imgs;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public Boolean getRecommend() {
        return recommend;
    }

    public void setRecommend(Boolean recommend) {
        this.recommend = recommend;
    }

    public Boolean getIsDelete() {
        return isDelete;
    }

    public void setIsDelete(Boolean delete) {
        isDelete = delete;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    
    public List<String> getStandardList() {
        return standardList;
    }
    
    public void setStandardList(List<String> standardList) {
        this.standardList = standardList;
    }
    
    public Integer getReviewCount() {
        return reviewCount;
    }
    
    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }
    
    public BigDecimal getGoodRating() {
        return goodRating;
    }
    
    public void setGoodRating(BigDecimal goodRating) {
        this.goodRating = goodRating;
    }
    
    public Integer getRating5Count() {
        return rating5Count;
    }
    
    public void setRating5Count(Integer rating5Count) {
        this.rating5Count = rating5Count;
    }
    
    public Integer getRating4Count() {
        return rating4Count;
    }
    
    public void setRating4Count(Integer rating4Count) {
        this.rating4Count = rating4Count;
    }
    
    public Integer getRating3Count() {
        return rating3Count;
    }
    
    public void setRating3Count(Integer rating3Count) {
        this.rating3Count = rating3Count;
    }
    
    public Integer getRating2Count() {
        return rating2Count;
    }
    
    public void setRating2Count(Integer rating2Count) {
        this.rating2Count = rating2Count;
    }
    
    public Integer getRating1Count() {
        return rating1Count;
    }
    
    public void setRating1Count(Integer rating1Count) {
        this.rating1Count = rating1Count;
    }
    
    public String getTags() {
        return tags;
    }
    
    public void setTags(String tags) {
        this.tags = tags;
    }
    
    public Review getFirstReview() {
        return firstReview;
    }
    
    public void setFirstReview(Review firstReview) {
        this.firstReview = firstReview;
    }

    @Override
    public String toString() {
        return "Good{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", discount=" + discount +
                ", sales=" + sales +
                ", saleMoney=" + saleMoney +
                ", categoryId=" + categoryId +
                ", imgs='" + imgs + '\'' +
                ", createTime='" + createTime + '\'' +
                ", recommend=" + recommend +
                ", isDelete=" + isDelete +
                ", price=" + price +
                '}';
    }


}