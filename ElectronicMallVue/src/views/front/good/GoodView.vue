<template>
    <div class="good-view-container">
        <!-- 面包屑导航 -->
        <div class="breadcrumb-bar">
            <div class="breadcrumb-content">
                <router-link to="/" class="breadcrumb-link">首页</router-link>
                <span class="breadcrumb-separator">&gt;</span>
                <router-link :to="{ path: '/goodlist', query: { categoryId: good.categoryId } }" class="breadcrumb-link">
                    {{ good.categoryName || '商品分类' }}
                </router-link>
                <span class="breadcrumb-separator">&gt;</span>
                <span class="breadcrumb-current">{{ good.name }}</span>
            </div>
        </div>

        <!-- 商品主信息区 -->
        <div class="product-main-section">
            <!-- 左侧图片区 -->
            <div class="product-gallery">
                <div class="main-image-wrapper">
                    <img 
                        v-if="!isEmojiImage(good.imgs)" 
                        :src="baseApi + good.imgs" 
                        alt="商品图片" 
                        class="main-image"
                        @error="handleImageError"
                    />
                    <div v-else class="emoji-main-placeholder">
                        <span class="emoji-main-icon">{{ getEmoji(good.imgs) }}</span>
                    </div>
                    <div v-if="good.discount && good.discount < 1" class="discount-badge">
                        {{ (good.discount * 10).toFixed(1) }}折
                    </div>
                </div>
            </div>

            <!-- 右侧商品信息区 -->
            <div class="product-info-panel">
                <!-- 商品标题 -->
                <div class="product-title-section">
                    <h1 class="product-title">{{ good.name }}</h1>
                    <p class="product-subtitle">{{ good.description }}</p>
                    <div class="product-tags">
                        <span v-if="good.recommend" class="tag tag-hot">热卖</span>
                        <span v-if="good.goodRating && good.goodRating >= 4.5" class="tag tag-good">好评如潮</span>
                        <span class="tag tag-sales">月销 {{ good.sales || 0 }}+</span>
                    </div>
                </div>

                <!-- 价格区域 -->
                <div class="price-section">
                    <div class="price-box-taobao">
                        <div class="price-row">
                            <span class="price-label">价格</span>
                            <div class="price-value">
                                <span class="price-symbol">¥</span>
                                <span class="price-number">{{ price }}</span>
                            </div>
                        </div>
                        <div v-if="good.discount && good.discount < 1" class="discount-row">
                            <span class="discount-label">优惠</span>
                            <span class="discount-value">{{ (good.discount * 10).toFixed(1) }}折</span>
                            <span class="discount-price">折后价 ¥{{ realPrice }}</span>
                        </div>
                    </div>
                </div>

                <!-- 销量信息 -->
                <div class="sales-info-section">
                    <div class="info-item">
                        <span class="info-label">月销量</span>
                        <span class="info-value">{{ good.sales || 0 }}</span>
                    </div>
                    <div class="info-item">
                        <span class="info-label">累计评价</span>
                        <span class="info-value">{{ good.reviewCount || 0 }}</span>
                    </div>
                    <div v-if="showStore" class="info-item">
                        <span class="info-label">库存</span>
                        <span class="info-value">{{ store }}件</span>
                    </div>
                </div>

                <!-- 规格选择 -->
                <div v-if="standards.length > 0" class="spec-section">
                    <div class="spec-header">
                        <span class="spec-label">规格</span>
                    </div>
                    <div class="spec-options">
                        <div 
                            v-for="(standard, index) in standards" 
                            :key="index"
                            :class="['spec-option', { active: checkedStandard === standard.value }]"
                            @click="selectStandard(standard)"
                        >
                            {{ standard.value }}
                            <span class="spec-price">¥{{ standard.price }}</span>
                        </div>
                    </div>
                </div>

                <!-- 数量选择 -->
                <div class="quantity-section">
                    <span class="quantity-label">数量</span>
                    <div class="quantity-control">
                        <button class="qty-btn" @click="decreaseCount" :disabled="count <= 1">-</button>
                        <input type="number" v-model.number="count" class="qty-input" :min="1" :max="store" />
                        <button class="qty-btn" @click="increaseCount" :disabled="count >= store">+</button>
                        <span class="qty-stock" v-if="showStore">(库存 {{ store }} 件)</span>
                    </div>
                </div>

                <!-- 操作按钮 -->
                <div class="action-buttons">
                    <button class="btn-buy-now" @click="goToOrder">立即购买</button>
                    <button class="btn-add-cart" @click="addToCart">
                        <span class="cart-icon">🛒</span>
                        加入购物车
                    </button>
                    <button class="btn-favorite" @click="toggleFavorite">
                        <span :class="['heart-icon', { active: isFavorited }]">❤</span>
                        收藏
                    </button>
                </div>

                <!-- 服务保障 -->
                <div class="service-guarantee">
                    <div class="guarantee-title">服务保障</div>
                    <div class="guarantee-items">
                        <span class="guarantee-item">✓ 正品保证</span>
                        <span class="guarantee-item">✓ 七天无理由退换</span>
                        <span class="guarantee-item">✓ 极速退款</span>
                        <span class="guarantee-item">✓ 运费险</span>
                    </div>
                </div>
            </div>
        </div>

        <!-- 商品详情标签页 -->
        <div class="product-detail-tabs">
            <div class="tabs-header">
                <div 
                    v-for="tab in detailTabs" 
                    :key="tab.value"
                    :class="['tab-item', { active: activeTab === tab.value }]"
                    @click="activeTab = tab.value"
                >
                    {{ tab.label }}
                </div>
            </div>

            <!-- 商品详情 -->
            <div v-show="activeTab === 'detail'" class="tab-content detail-content">
                <div class="detail-placeholder">
                    <p>商品详情展示区</p>
                    <p class="detail-hint">{{ good.description }}</p>
                </div>
            </div>

            <!-- 评价列表 -->
            <div v-show="activeTab === 'review'" class="tab-content review-content">
                <!-- 好评率概览 -->
                <div class="rating-summary-overview">
                    <div class="rating-main-score">
                        <div class="score-number">{{ good.goodRating || 5.0 }}</div>
                        <div class="score-stars">
                            <span v-for="i in 5" :key="i" :class="['star', { filled: i <= Math.round(good.goodRating || 5) }]">★</span>
                        </div>
                        <div class="score-label">综合评分</div>
                    </div>
                    <div class="rating-stats">
                        <div class="stat-item">
                            <div class="stat-value">{{ good.reviewCount || 0 }}</div>
                            <div class="stat-label">累计评价</div>
                        </div>
                        <div class="stat-item">
                            <div class="stat-value">{{ getGoodReviewRate() }}%</div>
                            <div class="stat-label">好评率</div>
                        </div>
                        <div class="stat-item">
                            <div class="stat-value">{{ good.sales || 0 }}</div>
                            <div class="stat-label">月销量</div>
                        </div>
                    </div>
                </div>

                <!-- 评分分布 -->
                <div class="rating-distribution-section">
                    <div class="section-title">评分分布</div>
                    <div class="rating-bars">
                        <div v-for="star in [5, 4, 3, 2, 1]" :key="star" class="rating-bar-row">
                            <span class="bar-star-label">{{ star }}星</span>
                            <div class="bar-track">
                                <div class="bar-progress" :style="{ width: getRatingPercent(star) + '%' }"></div>
                            </div>
                            <span class="bar-count-text">{{ getRatingCount(star) }}</span>
                        </div>
                    </div>
                </div>

                <!-- 评价列表 -->
                <div class="reviews-section">
                    <div class="section-title">用户评价 ({{ reviewTotal }})</div>
                    <div class="review-list">
                        <div v-for="review in reviews" :key="review.id" class="review-item">
                            <div class="review-header-info">
                                <div class="user-avatar">{{ review.user && review.user.nickname ? review.user.nickname.charAt(0) : '匿' }}</div>
                                <div class="user-details">
                                    <div class="user-name">{{ review.user && review.user.nickname ? review.user.nickname : '匿名用户' }}</div>
                                    <div class="review-time">{{ formatTime(review.createTime) }}</div>
                                </div>
                                <div class="review-rating-small">
                                    <span v-for="i in 5" :key="i" :class="['star-small', { filled: i <= review.rating }]">★</span>
                                </div>
                            </div>
                            <div class="review-text">{{ review.content }}</div>
                            <div v-if="review.tags" class="review-tags">
                                <span v-for="(tag, index) in review.tags.split(',')" :key="index" class="review-tag">
                                    {{ tag.trim() }}
                                </span>
                            </div>
                            <div class="review-footer">
                                <span class="like-btn" @click="likeReview(review)">
                                    👍 {{ review.likeCount || 0 }}
                                </span>
                            </div>
                        </div>
                        <div v-if="reviews.length === 0" class="no-reviews">
                            暂无评价，快来抢沙发~
                        </div>
                    </div>
                    
                    <!-- 分页组件 -->
                    <div v-if="reviewTotal > reviewPageSize" class="review-pagination">
                        <button 
                            class="page-btn" 
                            :disabled="reviewPage === 1" 
                            @click="handleReviewPageChange(reviewPage - 1)"
                        >
                            上一页
                        </button>
                        <span class="page-info">
                            第 {{ reviewPage }} 页 / 共 {{ Math.ceil(reviewTotal / reviewPageSize) }} 页 ({{ reviewTotal }} 条)
                        </span>
                        <button 
                            class="page-btn" 
                            :disabled="reviewPage >= Math.ceil(reviewTotal / reviewPageSize)" 
                            @click="handleReviewPageChange(reviewPage + 1)"
                        >
                            下一页
                        </button>
                    </div>
                </div>
            </div>
        </div>

        <!-- 底部固定操作栏 -->
        <div class="fixed-bottom-bar">
            <div class="bottom-actions">
                <div class="bottom-action-item" @click="$router.push('/')">
                    <span class="action-icon">🏠</span>
                    <span>首页</span>
                </div>
                <div class="bottom-action-item" @click="$router.push('/cart')">
                    <span class="action-icon">🛒</span>
                    <span>购物车</span>
                </div>
                <div class="bottom-action-item" @click="toggleFavorite">
                    <span :class="['action-icon', { active: isFavorited }]">❤</span>
                    <span>收藏</span>
                </div>
                <button class="btn-add-cart-bottom" @click="addToCart">加入购物车</button>
                <button class="btn-buy-now-bottom" @click="goToOrder">立即购买</button>
            </div>
        </div>
    </div>
</template>

<script>
import API from "@/utils/request";

export default {
    name: "GoodView",
    data() {
        return {
            baseApi: this.$store.state.baseApi,
            good: {},
            goodId: null,
            price: -1,
            isDiscount: false,
            discount: "",
            standards: [],
            checkedStandard: "",
            store: 0,
            showStore: false,
            count: 1,
            reviews: [],
            reviewPage: 1,
            reviewPageSize: 10,
            reviewTotal: 0,
            activeTab: 'review',
            isFavorited: false,
            detailTabs: [
                { label: '商品详情', value: 'detail' },
                { label: '商品评价', value: 'review' }
            ]
        };
    },
    methods: {
        getPriceRange(standards) {
            let arr = standards.map((item) => {
                return item.price;
            });
            for (let i = 0; i < arr.length; i++) {
                let min = i;
                for (let j = i + 1; j < arr.length; j++) {
                    if (arr[j] < arr[min]) {
                        min = j;
                    }
                }
                [arr[i], arr[min]] = [arr[min], arr[i]];
            }
            if (arr[0] === arr[arr.length - 1]) {
                return arr[0];
            } else {
                return arr[0] + "元 ~ " + arr[arr.length - 1];
            }
        },
        selectStandard(standard) {
            this.checkedStandard = standard.value;
            this.showStore = true;
            this.price = standard.price;
            this.store = standard.store;
        },
        decreaseCount() {
            if (this.count > 1) {
                this.count--;
            }
        },
        increaseCount() {
            if (this.count < this.store) {
                this.count++;
            }
        },
        goToOrder() {
            if (this.standards.length !== 0) {
                if (this.checkedStandard === "") {
                    this.$message.warning("请选择规格");
                    return false;
                }
            }
            this.$router.push({
                name: "preOrder",
                query: {
                    good: JSON.stringify(this.good),
                    realPrice: this.realPrice,
                    num: this.count,
                    standard: this.checkedStandard,
                },
            });
        },
        addToCart() {
            if (!localStorage.getItem("user")) {
                this.$router.push("/login");
            }
            if (!this.checkedStandard) {
                this.$message.error("请选择规格");
                return false;
            }
            this.request.get("/userid").then((res) => {
                let userId = res;
                let cart = {
                    userId: userId,
                    goodId: this.goodId,
                    standard: this.checkedStandard,
                    count: this.count,
                };
                this.request.post("/api/cart", cart).then((res) => {
                    if (res.code === "200") {
                        this.$message.success("成功添加购物车");
                    }
                });
            });
        },
        toggleFavorite() {
            this.isFavorited = !this.isFavorited;
            this.$message.success(this.isFavorited ? "已收藏" : "已取消收藏");
        },
        likeReview(review) {
            review.likeCount = (review.likeCount || 0) + 1;
        },
        getRatingPercent(star) {
            const total = this.good.reviewCount || 0;
            if (total === 0) return 0;
            const count = this.getRatingCount(star);
            return (count / total) * 100;
        },
        getRatingCount(star) {
            const fieldName = `rating${star}Count`;
            return this.good[fieldName] || 0;
        },
        getGoodReviewRate() {
            const total = this.good.reviewCount || 0;
            if (total === 0) return 100;
            const goodCount = (this.good.rating5Count || 0) + (this.good.rating4Count || 0);
            return Math.round((goodCount / total) * 100);
        },
        handleImageError(e) {
            console.log('图片加载失败:', e.target.src);
        },
        loadReviews() {
            this.request.get(`/api/review/good/${this.goodId}?pageNum=${this.reviewPage}&pageSize=${this.reviewPageSize}`)
                .then((res) => {
                    if (res.code === "200") {
                        if (res.data && res.data.records) {
                            this.reviews = res.data.records;
                            this.reviewTotal = res.data.total || 0;
                        } else if (Array.isArray(res.data)) {
                            this.reviews = res.data;
                            this.reviewTotal = res.data.length;
                        } else {
                            this.reviews = [];
                            this.reviewTotal = 0;
                        }
                    }
                })
                .catch((error) => {
                    console.error("加载评论失败:", error);
                });
        },
        handleReviewPageChange(page) {
            this.reviewPage = page;
            this.loadReviews();
            // 滚动到评论区域
            this.$nextTick(() => {
                const reviewSection = document.querySelector('.reviews-section');
                if (reviewSection) {
                    reviewSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
                }
            });
        },
        formatTime(timeStr) {
            if (!timeStr) return '';
            const date = new Date(timeStr);
            const now = new Date();
            const diff = now - date;
            const minutes = Math.floor(diff / 60000);
            const hours = Math.floor(diff / 3600000);
            const days = Math.floor(diff / 86400000);
            
            if (minutes < 1) return '刚刚';
            if (minutes < 60) return `${minutes}分钟前`;
            if (hours < 24) return `${hours}小时前`;
            if (days < 30) return `${days}天前`;
            return date.toLocaleDateString('zh-CN');
        },
        isEmojiImage(imgs) {
            return imgs && imgs.startsWith('emoji:');
        },
        getEmoji(imgs) {
            if (!imgs || !imgs.startsWith('emoji:')) return '📦';
            const parts = imgs.split(':');
            return parts.length >= 2 ? parts[1] : '📦';
        },
    },

    created() {
        this.goodId = this.$route.params.goodId;
        
        // 确保 goodId 有效后再加载数据
        if (!this.goodId) {
            this.$message.error('商品ID无效');
            this.$router.push('/');
            return;
        }
        
        this.request
            .get("/api/good/" + this.goodId)
            .then((res) => {
                if (res.code === "200") {
                    this.good = res.data;
                    let discount = this.good.discount;
                    if (discount < 1) {
                        this.isDiscount = true;
                        this.discount = discount * 10 + "折";
                    }
                } else {
                    this.$router.go(0);
                }
            })
            .catch((error) => {
                this.$message.error(error.response.data);
                console.log(error);
            });
        this.request
            .get("/api/good/standard/" + this.goodId)
            .then((res) => {
                if (res.code === "200") {
                    let standards = JSON.parse(res.data);
                    this.standards = standards;
                    this.price = this.getPriceRange(standards);
                } else {
                    this.price = this.good.price;
                    this.store = this.good.store;
                    this.showStore = true;
                }
            })
            .catch((error) => {
                this.$message.error(error.response.data);
                console.log(error);
            });
        
        // 加载评论
        this.loadReviews();
    },
    computed: {
        realPrice: function () {
            if (this.good.discount < 1) {
                if (isNaN(this.price)) {
                    let down =
                        this.price.substring(0, this.price.indexOf("元")) *
                        this.good.discount;
                    let up =
                        this.price.substring(this.price.lastIndexOf(" ")) *
                        this.good.discount;
                    return down.toFixed(2) + "元 ~ " + up.toFixed(2);
                } else {
                    return (this.price * this.good.discount).toFixed(2);
                }
            }
            return this.price;
        },
    },
};
</script>

<style scoped>
.good-view-container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 20px;
    background: #f4f4f4;
    min-height: 100vh;
}

.breadcrumb-bar {
    background: white;
    padding: 12px 20px;
    border-radius: 8px;
    margin-bottom: 20px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.breadcrumb-content {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
}

.breadcrumb-link {
    color: #666;
    text-decoration: none;
    transition: color 0.3s;
}

.breadcrumb-link:hover {
    color: #ff5000;
}

.breadcrumb-separator {
    color: #999;
}

.breadcrumb-current {
    color: #333;
    font-weight: 500;
}

.product-main-section {
    display: flex;
    gap: 20px;
    background: white;
    border-radius: 12px;
    padding: 20px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.product-gallery {
    flex: 0 0 480px;
}

.main-image-wrapper {
    width: 480px;
    height: 480px;
    border-radius: 8px;
    overflow: hidden;
    position: relative;
    background: #f8f8f8;
}

.main-image {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.emoji-main-placeholder {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, #ff9000 0%, #ff5000 100%);
}

.emoji-main-icon {
    font-size: 160px;
    filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.2));
}

.discount-badge {
    position: absolute;
    top: 16px;
    right: 16px;
    background: linear-gradient(90deg, #ff9000 0%, #ff5000 100%);
    color: white;
    padding: 8px 16px;
    border-radius: 20px;
    font-size: 16px;
    font-weight: 600;
    box-shadow: 0 4px 12px rgba(255, 80, 0, 0.3);
}

.product-info-panel {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 20px;
}

.product-title-section {
    padding-bottom: 16px;
    border-bottom: 1px solid #f0f0f0;
}

.product-title {
    font-size: 24px;
    font-weight: 700;
    color: #333;
    margin: 0 0 8px 0;
    line-height: 1.4;
}

.product-subtitle {
    font-size: 14px;
    color: #666;
    margin: 0 0 12px 0;
    line-height: 1.6;
}

.product-tags {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
}

.tag {
    padding: 4px 12px;
    border-radius: 12px;
    font-size: 12px;
    font-weight: 500;
}

.tag-hot {
    background: #fff0e5;
    color: #ff5000;
    border: 1px solid #ffd4b8;
}

.tag-good {
    background: #e8f5e9;
    color: #4caf50;
    border: 1px solid #c8e6c9;
}

.tag-sales {
    background: #f5f5f5;
    color: #666;
    border: 1px solid #e0e0e0;
}

.price-section {
    background: linear-gradient(90deg, #fff9f5 0%, #fff5f0 100%);
    border-radius: 8px;
    padding: 16px;
}

.price-box-taobao {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.price-row {
    display: flex;
    align-items: baseline;
    gap: 12px;
}

.price-label {
    font-size: 14px;
    color: #666;
    min-width: 60px;
}

.price-value {
    display: flex;
    align-items: baseline;
}

.price-symbol {
    font-size: 20px;
    color: #ff5000;
    font-weight: 600;
}

.price-number {
    font-size: 36px;
    color: #ff5000;
    font-weight: 700;
    line-height: 1;
}

.discount-row {
    display: flex;
    align-items: center;
    gap: 12px;
    padding-top: 12px;
    border-top: 1px dashed #ffd4b8;
}

.discount-label {
    font-size: 14px;
    color: #666;
    min-width: 60px;
}

.discount-value {
    background: linear-gradient(90deg, #ff9000 0%, #ff5000 100%);
    color: white;
    padding: 4px 12px;
    border-radius: 12px;
    font-size: 14px;
    font-weight: 600;
}

.discount-price {
    font-size: 18px;
    color: #ff5000;
    font-weight: 600;
}

.sales-info-section {
    display: flex;
    gap: 24px;
    padding: 16px 0;
    border-top: 1px solid #f0f0f0;
    border-bottom: 1px solid #f0f0f0;
}

.info-item {
    display: flex;
    flex-direction: column;
    gap: 4px;
}

.info-label {
    font-size: 12px;
    color: #999;
}

.info-value {
    font-size: 18px;
    color: #333;
    font-weight: 600;
}

.spec-section {
    padding: 16px 0;
}

.spec-header {
    margin-bottom: 12px;
}

.spec-label {
    font-size: 14px;
    color: #666;
    font-weight: 500;
}

.spec-options {
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
}

.spec-option {
    padding: 10px 20px;
    border: 2px solid #e5e5e5;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.3s;
    display: flex;
    flex-direction: column;
    gap: 4px;
    background: white;
}

.spec-option:hover {
    border-color: #ff5000;
    color: #ff5000;
}

.spec-option.active {
    border-color: #ff5000;
    background: #fff5f0;
    color: #ff5000;
}

.spec-price {
    font-size: 12px;
    color: #999;
}

.spec-option.active .spec-price {
    color: #ff5000;
}

.quantity-section {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 16px 0;
}

.quantity-label {
    font-size: 14px;
    color: #666;
    min-width: 60px;
}

.quantity-control {
    display: flex;
    align-items: center;
    gap: 0;
    border: 1px solid #e5e5e5;
    border-radius: 8px;
    overflow: hidden;
}

.qty-btn {
    width: 40px;
    height: 40px;
    border: none;
    background: #f5f5f5;
    font-size: 20px;
    color: #666;
    cursor: pointer;
    transition: all 0.3s;
}

.qty-btn:hover:not(:disabled) {
    background: #ff5000;
    color: white;
}

.qty-btn:disabled {
    opacity: 0.4;
    cursor: not-allowed;
}

.qty-input {
    width: 60px;
    height: 40px;
    border: none;
    border-left: 1px solid #e5e5e5;
    border-right: 1px solid #e5e5e5;
    text-align: center;
    font-size: 16px;
    color: #333;
}

.qty-stock {
    font-size: 12px;
    color: #999;
    margin-left: 8px;
}

.action-buttons {
    display: flex;
    gap: 12px;
    padding: 20px 0;
}

.btn-buy-now {
    flex: 1;
    height: 48px;
    background: linear-gradient(90deg, #ff9000 0%, #ff5000 100%);
    color: white;
    border: none;
    border-radius: 24px;
    font-size: 18px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s;
    box-shadow: 0 4px 12px rgba(255, 80, 0, 0.3);
}

.btn-buy-now:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 16px rgba(255, 80, 0, 0.4);
}

.btn-add-cart {
    flex: 1;
    height: 48px;
    background: white;
    color: #ff5000;
    border: 2px solid #ff5000;
    border-radius: 24px;
    font-size: 18px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
}

.btn-add-cart:hover {
    background: #fff5f0;
}

.cart-icon {
    font-size: 20px;
}

.btn-favorite {
    width: 48px;
    height: 48px;
    border: 2px solid #e5e5e5;
    border-radius: 24px;
    background: white;
    cursor: pointer;
    transition: all 0.3s;
    display: flex;
    align-items: center;
    justify-content: center;
}

.btn-favorite:hover {
    border-color: #ff0036;
}

.heart-icon {
    font-size: 24px;
    color: #999;
    transition: all 0.3s;
}

.heart-icon.active {
    color: #ff0036;
}

.service-guarantee {
    padding: 16px;
    background: #f9f9f9;
    border-radius: 8px;
}

.guarantee-title {
    font-size: 14px;
    color: #666;
    margin-bottom: 12px;
    font-weight: 500;
}

.guarantee-items {
    display: flex;
    gap: 16px;
    flex-wrap: wrap;
}

.guarantee-item {
    font-size: 12px;
    color: #4caf50;
    display: flex;
    align-items: center;
    gap: 4px;
}

.product-detail-tabs {
    background: white;
    border-radius: 12px;
    margin-top: 20px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
    overflow: hidden;
}

.tabs-header {
    display: flex;
    border-bottom: 2px solid #f0f0f0;
}

.tab-item {
    flex: 1;
    padding: 16px;
    text-align: center;
    font-size: 16px;
    font-weight: 500;
    color: #666;
    cursor: pointer;
    transition: all 0.3s;
    position: relative;
}

.tab-item:hover {
    color: #ff5000;
}

.tab-item.active {
    color: #ff5000;
}

.tab-item.active::after {
    content: '';
    position: absolute;
    bottom: -2px;
    left: 50%;
    transform: translateX(-50%);
    width: 60px;
    height: 3px;
    background: linear-gradient(90deg, #ff9000 0%, #ff5000 100%);
    border-radius: 2px;
}

.tab-content {
    padding: 24px;
    min-height: 400px;
}

.detail-placeholder {
    text-align: center;
    padding: 60px 20px;
    color: #999;
}

.detail-placeholder p {
    margin: 8px 0;
}

.detail-hint {
    font-size: 14px;
    color: #bbb;
}

/* 好评率概览 */
.rating-summary-overview {
    display: flex;
    gap: 40px;
    padding: 24px;
    background: linear-gradient(135deg, #fff5f0 0%, #fff 100%);
    border-radius: 12px;
    margin-bottom: 24px;
    border: 1px solid #ffe0d0;
}

.rating-main-score {
    text-align: center;
    min-width: 150px;
    padding: 20px;
    background: white;
    border-radius: 12px;
    box-shadow: 0 4px 12px rgba(255, 80, 0, 0.1);
}

.score-number {
    font-size: 56px;
    font-weight: 700;
    color: #ff5000;
    line-height: 1;
}

.score-stars {
    margin-top: 12px;
    font-size: 24px;
    letter-spacing: 4px;
}

.score-stars .star {
    color: #ddd;
}

.score-stars .star.filled {
    color: #ff9000;
}

.score-label {
    margin-top: 12px;
    font-size: 14px;
    color: #666;
    font-weight: 500;
}

.rating-stats {
    flex: 1;
    display: flex;
    justify-content: space-around;
    align-items: center;
}

.stat-item {
    text-align: center;
    padding: 20px;
}

.stat-value {
    font-size: 32px;
    font-weight: 700;
    color: #ff5000;
    line-height: 1;
}

.stat-label {
    margin-top: 8px;
    font-size: 14px;
    color: #666;
}

/* 评分分布 */
.rating-distribution-section {
    padding: 20px 24px;
    background: #f9f9f9;
    border-radius: 12px;
    margin-bottom: 24px;
}

.section-title {
    font-size: 16px;
    font-weight: 600;
    color: #333;
    margin-bottom: 16px;
    padding-bottom: 12px;
    border-bottom: 2px solid #f0f0f0;
}

.rating-bars {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.rating-bar-row {
    display: flex;
    align-items: center;
    gap: 12px;
}

.bar-star-label {
    font-size: 14px;
    color: #666;
    min-width: 40px;
}

.bar-track {
    flex: 1;
    height: 12px;
    background: #e5e5e5;
    border-radius: 6px;
    overflow: hidden;
}

.bar-progress {
    height: 100%;
    background: linear-gradient(90deg, #ff9000 0%, #ff5000 100%);
    border-radius: 6px;
    transition: width 0.5s ease;
}

.bar-count-text {
    font-size: 14px;
    color: #999;
    min-width: 40px;
    text-align: right;
}

/* 评价列表 */
.reviews-section {
    padding: 0 24px;
}

.reviews-section .section-title {
    padding: 16px 0;
}

.review-list {
    display: flex;
    flex-direction: column;
    gap: 20px;
}

.review-item {
    padding: 20px;
    border-bottom: 1px solid #f0f0f0;
}

.review-header-info {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 12px;
}

.user-avatar {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    background: linear-gradient(135deg, #ff9000 0%, #ff5000 100%);
    color: white;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 18px;
    font-weight: 600;
}

.user-details {
    flex: 1;
}

.user-name {
    font-size: 14px;
    color: #333;
    font-weight: 500;
}

.review-time {
    font-size: 12px;
    color: #999;
    margin-top: 4px;
}

.review-rating-small {
    font-size: 14px;
    letter-spacing: 2px;
}

.star-small {
    color: #ddd;
}

.star-small.filled {
    color: #ff9000;
}

.review-text {
    font-size: 14px;
    color: #333;
    line-height: 1.6;
    margin-bottom: 12px;
}

.review-tags {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
    margin-bottom: 12px;
}

.review-tag {
    padding: 4px 12px;
    background: #f5f5f5;
    border-radius: 12px;
    font-size: 12px;
    color: #666;
}

.review-footer {
    display: flex;
    gap: 16px;
}

.like-btn {
    font-size: 14px;
    color: #999;
    cursor: pointer;
    transition: color 0.3s;
}

.like-btn:hover {
    color: #ff5000;
}

.no-reviews {
    text-align: center;
    padding: 60px 20px;
    color: #999;
    font-size: 16px;
}

.review-pagination {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 16px;
    padding: 24px 0;
    margin-top: 16px;
    border-top: 1px solid #f0f0f0;
}

.page-btn {
    padding: 8px 20px;
    border: 1px solid #ddd;
    border-radius: 4px;
    background: white;
    color: #333;
    font-size: 14px;
    cursor: pointer;
    transition: all 0.3s;
}

.page-btn:hover:not(:disabled) {
    border-color: #ff5000;
    color: #ff5000;
}

.page-btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
}

.page-info {
    font-size: 14px;
    color: #666;
}

.fixed-bottom-bar {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    background: white;
    box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.08);
    padding: 12px 20px;
    z-index: 1000;
}

.bottom-actions {
    max-width: 1200px;
    margin: 0 auto;
    display: flex;
    align-items: center;
    gap: 16px;
}

.bottom-action-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    cursor: pointer;
    transition: all 0.3s;
    padding: 8px 16px;
    border-radius: 8px;
}

.bottom-action-item:hover {
    background: #f5f5f5;
}

.action-icon {
    font-size: 20px;
    color: #666;
}

.action-icon.active {
    color: #ff0036;
}

.bottom-action-item span:last-child {
    font-size: 12px;
    color: #666;
}

.btn-add-cart-bottom {
    flex: 1;
    height: 44px;
    background: linear-gradient(90deg, #ffcc00 0%, #ff9000 100%);
    color: white;
    border: none;
    border-radius: 22px;
    font-size: 16px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s;
}

.btn-add-cart-bottom:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(255, 144, 0, 0.3);
}

.btn-buy-now-bottom {
    flex: 1;
    height: 44px;
    background: linear-gradient(90deg, #ff9000 0%, #ff5000 100%);
    color: white;
    border: none;
    border-radius: 22px;
    font-size: 16px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s;
}

.btn-buy-now-bottom:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(255, 80, 0, 0.3);
}
</style>