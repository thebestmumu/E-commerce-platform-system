<template>
    <div class="taobao-home">
        <!-- 搜索栏 -->
        <search @search="handleSearch"></search>

        <!-- 主内容区 -->
        <div class="main-content">
            <!-- 3D轮播图区域 -->
            <div class="carousel-3d-section">
                <three-carousel 
                    :products="showcaseProducts" 
                    :baseApi="baseApi"
                    @change="handleCarouselChange"
                    @goToDetail="goToDetail"
                ></three-carousel>
            </div>

            <!-- 第二行内容：分类 + 信息区 -->
            <div class="second-row-section">
                <!-- 左侧分类导航 -->
                <div class="category-nav">
                    <div class="nav-header">
                        <span class="nav-title">商品分类</span>
                    </div>
                    <ul class="nav-list">
                        <li v-for="(item, index) in icons" :key="index" class="nav-item">
                            <div class="nav-item-header">
                                <i class="nav-icon" v-html="item.value"></i>
                                <span class="nav-item-name">{{ item.categories && item.categories[0] ? item.categories[0].name : '' }}</span>
                            </div>
                            <div class="nav-item-links">
                                <router-link
                                    v-for="(category, idx) in item.categories.slice(0, 3)"
                                    :key="idx"
                                    :to="{ path: '/goodlist', query: { categoryId: category.id } }"
                                    class="nav-link"
                                >
                                    {{ category.name }}
                                </router-link>
                            </div>
                        </li>
                    </ul>
                </div>

                <!-- 中间内容区 -->
                <div class="middle-content">
                    <!-- 限时秒杀 -->
                    <div class="flash-sale-section">
                        <div class="flash-sale-header">
                            <div class="flash-sale-title">
                                <span class="flash-icon">⚡</span>
                                <span>限时秒杀</span>
                                <span class="flash-timer" v-if="flashTimeLeft">{{ flashTimeLeft }}</span>
                            </div>
                            <span class="flash-more" @click="$router.push('/goodList')">更多 ></span>
                        </div>
                        <div class="flash-sale-list">
                            <div 
                                class="flash-item" 
                                v-for="(item, idx) in flashGoods" 
                                :key="idx"
                                @click="$router.push('/goodView/' + item.id)"
                            >
                                <div class="flash-item-img-wrapper">
                                    <img v-if="item.imgs && !isEmojiImage(item.imgs)" :src="baseApi + item.imgs" class="flash-item-img" @error="handleImageError" />
                                    <div v-else class="flash-item-img-placeholder">{{ getEmoji(item.imgs) }}</div>
                                    <div class="flash-discount" v-if="item.discount && item.discount < 1">{{ (item.discount * 10).toFixed(1) }}折</div>
                                </div>
                                <div class="flash-item-info">
                                    <div class="flash-item-name">{{ item.name }}</div>
                                    <div class="flash-item-price">
                                        <span class="flash-current-price">¥{{ item.price }}</span>
                                        <span class="flash-original-price" v-if="item.originalPrice">¥{{ item.originalPrice }}</span>
                                    </div>
                                    <div class="flash-progress">
                                        <div class="flash-progress-bar">
                                            <div class="flash-progress-fill" :style="{ width: (item.flashProgress || 60) + '%' }"></div>
                                        </div>
                                        <span class="flash-progress-text">已抢{{ item.flashProgress || 60 }}%</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- 品类推荐 -->
                    <div class="category-recommend-section">
                        <div class="category-recommend-header">
                            <span class="category-icon">🎯</span>
                            <span>品类推荐</span>
                        </div>
                        <div class="category-recommend-grid">
                            <div 
                                class="category-recommend-item" 
                                v-for="(cat, idx) in recommendCategories" 
                                :key="idx"
                                @click="$router.push({ path: '/goodList', query: { categoryId: cat.id } })"
                            >
                                <span class="category-recommend-icon">{{ cat.icon }}</span>
                                <span class="category-recommend-name">{{ cat.name }}</span>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- 右侧信息区 -->
                <div class="info-section">
                    <!-- 热门榜单 -->
                    <div class="info-card hot-list-card">
                        <div class="info-card-header">
                            <span class="info-icon">🏆</span>
                            <span class="info-title">热销榜单</span>
                            <span class="hot-list-more" @click="$router.push('/goodList')">更多 ></span>
                        </div>
                        <div class="hot-list">
                            <div 
                                class="hot-item" 
                                v-for="(item, idx) in hotGoods.slice(0, 6)" 
                                :key="idx"
                                @click="$router.push('/goodView/' + item.id)"
                            >
                                <span class="hot-rank" :class="{ 'top3': idx < 3 }">{{ idx + 1 }}</span>
                                <img v-if="item.imgs && !isEmojiImage(item.imgs)" :src="baseApi + item.imgs" class="hot-item-img" @error="handleImageError" />
                                <div v-else class="hot-item-img-placeholder">{{ getEmoji(item.imgs) }}</div>
                                <div class="hot-item-info">
                                    <div class="hot-item-name">{{ item.name }}</div>
                                    <div class="hot-item-bottom">
                                        <span class="hot-item-price">¥{{ item.price }}</span>
                                        <span class="hot-item-sales">{{ item.sales || 0 }}人付款</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                    
                    <!-- 快捷入口 -->
                    <div class="info-card quick-entry-card">
                        <div class="info-card-header">
                            <span class="info-icon"></span>
                            <span class="info-title">快捷入口</span>
                        </div>
                        <div class="quick-entry-grid">
                            <div class="quick-entry-item" @click="$router.push('/goodList')">
                                <span class="entry-icon">🛍️</span>
                                <span>全部商品</span>
                            </div>
                            <div class="quick-entry-item" @click="$router.push('/cart')">
                                <span class="entry-icon">🛒</span>
                                <span>购物车</span>
                            </div>
                            <div class="quick-entry-item" @click="$router.push('/orderlist')">
                                <span class="entry-icon">📦</span>
                                <span>我的订单</span>
                            </div>
                            <div class="quick-entry-item" @click="$router.push('/myTickets')">
                                <span class="entry-icon">📋</span>
                                <span>我的工单</span>
                            </div>
                            <div class="quick-entry-item" @click="$router.push('/person')">
                                <span class="entry-icon">👤</span>
                                <span>个人中心</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- 推荐商品区域 -->
            <div class="recommend-section">
                <div class="section-header">
                    <h2 class="section-title">
                        <span class="title-icon">🔥</span>
                        猜你喜欢
                    </h2>
                    <div class="section-more" @click="loadMore">查看更多 ></div>
                </div>

                <!-- 商品网格 -->
                <div class="product-grid">
                    <router-link 
                        v-for="item in goods" 
                        :key="item.id" 
                        :to="'goodView/' + item.id"
                        class="product-card"
                    >
                        <div class="product-image-wrapper">
                            <img
                                :src="baseApi + item.imgs"
                                class="product-image"
                                @error="handleProductImageError($event, item)"
                                @load="handleProductImageLoad($event, item)"
                            />
                            <div class="product-image-fallback" :data-good-id="item.id">
                                <span class="fallback-text">{{ item.name }}</span>
                            </div>
                            <div v-if="item.discount && item.discount < 1" class="discount-tag">
                                {{ (item.discount * 10).toFixed(1) }}折
                            </div>
                        </div>
                        <div class="product-info">
                            <div class="product-title">{{ item.name }}</div>
                            <div class="product-price-row">
                                <span class="product-price">{{ item.price }}</span>
                                <span class="product-sales">{{ item.sales || 0 }}人付款</span>
                            </div>
                            <div class="product-tags">
                                <span v-if="item.recommend" class="tag tag-hot">热卖</span>
                                <span v-if="item.goodRating && item.goodRating >= 4.5" class="tag tag-good">好评</span>
                            </div>
                        </div>
                    </router-link>
                </div>
            </div>
        </div>

        <!-- 返回顶部 -->
        <div v-if="showBackToTop" class="back-to-top" @click="scrollToTop">
            <span>↑</span>
        </div>
    </div>
</template>

<script>
import search from "../../components/Search";
import ThreeCarousel from "../../components/ThreeCarousel";

export default {
    name: "TopView",
    data() {
        return {
            carousels: [],
            goods: [],
            baseApi: this.$store.state.baseApi,
            icons: [],
            searchText: "",
            currentUser: null,
            defaultAvatar: "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'%3E%3Ccircle cx='50' cy='50' r='50' fill='%23f5f5f5'/%3E%3Ccircle cx='50' cy='40' r='20' fill='%23ddd'/%3E%3Cellipse cx='50' cy='85' rx='35' ry='25' fill='%23ddd'/%3E%3C/svg%3E",
            showBackToTop: false,
            // 3D商品展示
            showcaseProducts: [],
            showcaseIndex: 0,
            showcaseTimer: null,
            // 热门商品
            hotGoods: [],
            // 限时秒杀
            flashGoods: [],
            flashTimeLeft: '',
            // 品类推荐
            recommendCategories: [
                { id: 1, name: '手机数码', icon: '' },
                { id: 2, name: '电脑办公', icon: '💻' },
                { id: 3, name: '服饰鞋包', icon: '👗' },
                { id: 4, name: '美妆护肤', icon: '' },
                { id: 5, name: '家居家装', icon: '🏠' },
                { id: 6, name: '运动户外', icon: '' },
                { id: 7, name: '食品生鲜', icon: '🍎' },
                { id: 8, name: '图书文娱', icon: '' },
                { id: 9, name: '家用电器', icon: '' },
                { id: 10, name: '汽车用品', icon: '' },
                { id: 11, name: '母婴用品', icon: '' },
                { id: 12, name: '宠物用品', icon: '' },
            ],
            // 公告
            notices: [
                { type: 'new', tag: '新品', text: 'iPhone 16 Pro Max 全新上市' },
                { type: 'hot', tag: '热卖', text: '夏季清仓大促 全场5折起' },
                { type: 'notice', tag: '公告', text: '618年中大促即将开始' },
                { type: 'new', tag: '新品', text: '华为Mate 70 预约开启' },
            ]
        };
    },
    computed: {
        currentProduct() {
            return this.showcaseProducts[this.showcaseIndex] || {};
        }
    },
    components: {
        search,
        ThreeCarousel,
    },
    created() {
        this.loadUser();
        this.loadGoods();
        this.loadCategories();
        this.loadCarousels();
        this.loadShowcaseProducts();
        this.loadHotGoods();
        
        window.addEventListener('scroll', this.handleScroll);
    },
    beforeDestroy() {
        window.removeEventListener('scroll', this.handleScroll);
        if (this.showcaseTimer) {
            clearInterval(this.showcaseTimer);
        }
    },
    methods: {
        loadUser() {
            const userStr = localStorage.getItem('user');
            if (userStr) {
                try {
                    this.currentUser = JSON.parse(userStr);
                } catch (e) {
                    console.error('解析用户信息失败:', e);
                }
            }
        },
        
        loadGoods() {
            const userStr = localStorage.getItem('user');
            let userId = null;
            if (userStr) {
                try {
                    const user = JSON.parse(userStr);
                    userId = user.id;
                } catch (e) {}
            }
            
            if (userId) {
                this.request.post("/api/ai/recommend", { userId: userId })
                    .then((res) => {
                        if (res.code === 0 && res.actionData && res.actionData.items) {
                            this.goods = res.actionData.items;
                        } else {
                            this.loadHotGoodsForGrid();
                        }
                    })
                    .catch(() => {
                        this.loadHotGoodsForGrid();
                    });
            } else {
                this.loadHotGoodsForGrid();
            }
        },
        
        loadHotGoods() {
            this.request.get("/api/good/rank?num=10")
                .then((res) => {
                    if (res.code === "200") {
                        this.hotGoods = res.data.slice(0, 10);
                    }
                })
                .catch((e) => {
                    console.log(e);
                });
        },
        
        loadHotGoodsForGrid() {
            this.request.get("/api/good/rank?num=50")
                .then((res) => {
                    if (res.code === "200") {
                        this.goods = res.data;
                    }
                })
                .catch((e) => {
                    console.log(e);
                });
        },
        
        loadCategories() {
            this.request.get("/api/icon")
                .then((res) => {
                    if (res.code === "200") {
                        this.icons = res.data;
                        if (this.icons.length > 8) {
                            this.icons = this.icons.slice(0, 8);
                        }
                    }
                })
                .catch((e) => {
                    console.log(e);
                });
        },
        
        loadCarousels() {
            this.request.get("/api/carousel")
                .then((res) => {
                    if (res.code === "200") {
                        this.carousels = res.data;
                    }
                })
                .catch((e) => {
                    console.log(e);
                });
        },
        
        loadShowcaseProducts() {
            this.request.get("/api/good/rank?num=10")
                .then((res) => {
                    if (res.code === "200") {
                        this.showcaseProducts = res.data.slice(0, 10);
                        this.hotGoods = res.data.slice(0, 10);
                        this.flashGoods = res.data.slice(0, 4).map(item => ({
                            ...item,
                            flashProgress: Math.floor(Math.random() * 40) + 50
                        }));
                        this.startAutoPlay();
                        this.startFlashTimer();
                    }
                })
                .catch((e) => {
                    console.log(e);
                });
        },
        
        startFlashTimer() {
            const updateTimer = () => {
                const now = new Date();
                const endOfDay = new Date();
                endOfDay.setHours(23, 59, 59, 999);
                const diff = endOfDay - now;
                const hours = Math.floor(diff / (1000 * 60 * 60));
                const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
                const seconds = Math.floor((diff % (1000 * 60)) / 1000);
                this.flashTimeLeft = `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
            };
            updateTimer();
            setInterval(updateTimer, 1000);
        },
        
        startAutoPlay() {
            if (this.showcaseTimer) {
                clearInterval(this.showcaseTimer);
            }
            this.showcaseTimer = setInterval(() => {
                this.nextProduct();
            }, 4000);
        },
        
        handleCarouselChange(index) {
            this.showcaseIndex = index;
        },
        
        nextProduct() {
            this.showcaseIndex = (this.showcaseIndex + 1) % this.showcaseProducts.length;
        },
        
        prevProduct() {
            this.showcaseIndex = this.showcaseIndex > 0 ? this.showcaseIndex - 1 : this.showcaseProducts.length - 1;
        },
        
        goToDetail() {
            if (this.currentProduct.id) {
                this.$router.push('/goodView/' + this.currentProduct.id);
            }
        },
        
        getVisibleProducts() {
            const len = this.showcaseProducts.length;
            if (len === 0) return [];
            
            const center = this.showcaseIndex;
            const left2 = (center - 2 + len) % len;
            const left1 = (center - 1 + len) % len;
            const right1 = (center + 1) % len;
            const right2 = (center + 2) % len;
            
            return [
                this.showcaseProducts[left2],
                this.showcaseProducts[left1],
                this.showcaseProducts[center],
                this.showcaseProducts[right1],
                this.showcaseProducts[right2]
            ];
        },
        
        getCardClass(idx) {
            if (idx === 2) return 'card-center';
            if (idx === 1) return 'card-left1';
            if (idx === 0) return 'card-left2';
            if (idx === 3) return 'card-right1';
            return 'card-right2';
        },
        
        isCenterCard(idx) {
            return idx === 2;
        },
        
        handleCardClick(idx) {
            if (idx < 2) {
                this.prevProduct();
            } else if (idx > 2) {
                this.nextProduct();
            }
        },
        
        addToCart(product) {
            this.$message.success('已加入购物车');
        },
        
        handleSearch(text) {
            this.searchText = text;
            this.$router.push({
                path: "/goodList",
                query: { keyword: this.searchText },
            });
        },
        
        loadMore() {
            this.$router.push('/goodList');
        },
        
        isEmojiImage(imgPath) {
            return imgPath && imgPath.startsWith('emoji:');
        },
        
        getEmoji(imgPath) {
            if (!imgPath || !imgPath.startsWith('emoji:')) return '';
            const parts = imgPath.split(':');
            return parts.length >= 2 ? parts[1] : '';
        },
        
        handleProductImageError(event, item) {
            event.target.style.display = 'none';
            const fallback = event.target.parentNode.querySelector('.product-image-fallback');
            if (fallback) {
                fallback.style.display = 'flex';
            }
        },
        
        handleProductImageLoad(event, item) {
            const fallback = event.target.parentNode.querySelector('.product-image-fallback');
            if (fallback) {
                fallback.style.display = 'none';
            }
        },
        
        handleImageError(event) {
            event.target.style.display = 'none';
            const placeholder = event.target.parentNode.querySelector('.emoji-placeholder');
            if (placeholder) {
                placeholder.style.display = 'flex';
            }
        },
        
        handleScroll() {
            this.showBackToTop = window.scrollY > 300;
        },
        
        scrollToTop() {
            window.scrollTo({ top: 0, behavior: 'smooth' });
        },
    },
};
</script>

<style scoped>
.taobao-home {
    min-height: 100vh;
    background: #f4f4f4;
}

.main-content {
    max-width: 1200px;
    margin: 0 auto;
    padding: 16px;
}

/* 3D轮播图区域 */
.carousel-3d-section {
    margin-bottom: 24px;
}

/* 第二行内容区域 */

.control-back::after {
    background: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%231a1a2e' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z'/%3E%3C/svg%3E") no-repeat center;
    background-size: contain;
}

.control-next::after {
    background: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%231a1a2e' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolygon points='5 3 19 12 5 21 5 3'/%3E%3C/svg%3E") no-repeat center;
    background-size: contain;
}

.control-center {
    padding: 14px 56px;
    min-width: 160px;
}

/* 指示器 */
.carousel-indicators {
    position: absolute;
    bottom: 24px;
    left: 50%;
    transform: translateX(-50%);
    display: flex;
    gap: 10px;
    z-index: 10;
}

.indicator-dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.25);
    cursor: pointer;
    transition: all 0.3s ease;
}

.indicator-dot.active {
    width: 28px;
    border-radius: 5px;
    background: #fff;
}

/* 第二行内容区域 */
.second-row-section {
    display: grid;
    grid-template-columns: 200px 1fr 240px;
    gap: 16px;
    margin-bottom: 24px;
    align-items: stretch;
}

/* 左侧分类导航 */
.category-nav {
    background: white;
    border-radius: 12px;
    overflow: hidden;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.nav-header {
    background: linear-gradient(90deg, #ff9000 0%, #ff5000 100%);
    padding: 12px 16px;
    color: white;
}

.nav-title {
    font-size: 16px;
    font-weight: 600;
}

.nav-list {
    list-style: none;
    padding: 0;
    margin: 0;
}

.nav-item {
    padding: 12px 16px;
    border-bottom: 1px solid #f5f5f5;
    transition: all 0.3s ease;
    cursor: pointer;
}

.nav-item:hover {
    background: #fff5f0;
}

.nav-item-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;
}

.nav-icon {
    font-size: 18px;
}

.nav-item-name {
    font-size: 14px;
    font-weight: 600;
    color: #333;
}

.nav-item-links {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}

.nav-link {
    font-size: 12px;
    color: #666;
    text-decoration: none;
    padding: 2px 8px;
    border-radius: 12px;
    background: #f5f5f5;
    transition: all 0.3s ease;
}

.nav-link:hover {
    background: #ff5000;
    color: white;
}

/* 中间内容区 */
.middle-content {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

/* 限时秒杀 */
.flash-sale-section {
    background: white;
    border-radius: 12px;
    padding: 41px 16px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.flash-sale-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
}

.flash-sale-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 18px;
    font-weight: 600;
    color: #333;
}

.flash-icon {
    font-size: 20px;
}

.flash-timer {
    background: #ff5000;
    color: white;
    padding: 2px 8px;
    border-radius: 4px;
    font-size: 14px;
    font-weight: 600;
}

.flash-more {
    color: #ff5000;
    cursor: pointer;
    font-size: 14px;
}

.flash-sale-list {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 12px;
}

.flash-item {
    cursor: pointer;
    transition: all 0.3s ease;
}

.flash-item:hover {
    transform: translateY(-4px);
}

.flash-item-img-wrapper {
    position: relative;
    width: 100%;
    height: 140px;
    border-radius: 8px;
    overflow: hidden;
    margin-bottom: 8px;
}

.flash-item-img {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.flash-item-img-placeholder {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 48px;
    background: #f5f5f5;
}

.flash-discount {
    position: absolute;
    top: 8px;
    left: 8px;
    background: #ff5000;
    color: white;
    padding: 2px 8px;
    border-radius: 4px;
    font-size: 12px;
    font-weight: 600;
}

.flash-item-info {
    padding: 0 4px;
}

.flash-item-name {
    font-size: 13px;
    color: #333;
    line-height: 1.4;
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    margin-bottom: 8px;
}

.flash-item-price {
    display: flex;
    align-items: baseline;
    gap: 8px;
    margin-bottom: 8px;
}

.flash-current-price {
    color: #ff5000;
    font-size: 18px;
    font-weight: 700;
}

.flash-original-price {
    color: #999;
    font-size: 12px;
    text-decoration: line-through;
}

.flash-progress {
    display: flex;
    align-items: center;
    gap: 8px;
}

.flash-progress-bar {
    flex: 1;
    height: 16px;
    background: #ffe5d9;
    border-radius: 8px;
    overflow: hidden;
}

.flash-progress-fill {
    height: 100%;
    background: linear-gradient(90deg, #ff9000 0%, #ff5000 100%);
    border-radius: 8px;
    transition: width 0.3s ease;
}

.flash-progress-text {
    font-size: 12px;
    color: #ff5000;
    white-space: nowrap;
}

/* 品类推荐 */
.category-recommend-section {
    background: white;
    border-radius: 12px;
    padding: 16px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    margin-bottom: 0;
}

.category-recommend-header {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 16px;
    font-weight: 600;
    color: #333;
    margin-bottom: 16px;
}

.category-icon {
    font-size: 20px;
}

.category-recommend-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 8px;
}

.category-recommend-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 6px;
    padding: 10px 8px;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.3s ease;
}

.category-recommend-item:hover {
    background: #fff5f0;
}

.category-recommend-icon {
    font-size: 28px;
}

.category-recommend-name {
    font-size: 12px;
    color: #666;
}

/* 右侧信息区 */
.info-section {
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.info-card {
    background: white;
    border-radius: 12px;
    overflow: hidden;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.info-card-header {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px 16px;
    border-bottom: 1px solid #f5f5f5;
}

.info-icon {
    font-size: 18px;
}

.info-title {
    font-size: 14px;
    font-weight: 600;
    color: #333;
}

.hot-list-more {
    margin-left: auto;
    color: #ff5000;
    font-size: 12px;
    cursor: pointer;
}

/* 公告栏 */
.notice-list {
    padding: 12px 16px;
}

.notice-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 0;
    border-bottom: 1px solid #f5f5f5;
}

.notice-item:last-child {
    border-bottom: none;
}

.notice-tag {
    padding: 2px 8px;
    border-radius: 4px;
    font-size: 12px;
    font-weight: 600;
    white-space: nowrap;
}

.notice-tag.new {
    background: #e6f7ff;
    color: #1890ff;
}

.notice-tag.hot {
    background: #fff2e8;
    color: #ff5000;
}

.notice-tag.notice {
    background: #f6ffed;
    color: #52c41a;
}

.notice-text {
    font-size: 13px;
    color: #666;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

/* 热门榜单 */
.hot-list {
    padding: 12px 16px;
}

.hot-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 8px 0;
    border-bottom: 1px solid #f5f5f5;
    cursor: pointer;
    transition: all 0.3s ease;
}

.hot-item:last-child {
    border-bottom: none;
}

.hot-item:hover {
    background: #fff5f0;
    margin: 0 -16px;
    padding: 8px 16px;
}

.hot-rank {
    width: 20px;
    height: 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    font-weight: 600;
    color: #999;
    background: #f5f5f5;
    border-radius: 4px;
}

.hot-rank.top3 {
    background: #ff5000;
    color: white;
}

.hot-item-img {
    width: 40px;
    height: 40px;
    border-radius: 4px;
    object-fit: cover;
}

.hot-item-img-placeholder {
    width: 40px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
    background: #f5f5f5;
    border-radius: 4px;
}

.hot-item-info {
    flex: 1;
    min-width: 0;
}

.hot-item-name {
    font-size: 13px;
    color: #333;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    margin-bottom: 4px;
}

.hot-item-bottom {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.hot-item-price {
    color: #ff5000;
    font-size: 14px;
    font-weight: 600;
}

.hot-item-sales {
    font-size: 12px;
    color: #999;
}

/* 快捷入口 */
.quick-entry-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
    padding: 16px;
}

.quick-entry-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    padding: 12px;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.3s ease;
}

.quick-entry-item:hover {
    background: #fff5f0;
}

.entry-icon {
    font-size: 24px;
}

.quick-entry-item span:last-child {
    font-size: 12px;
    color: #666;
}

/* 推荐商品区域 */
.recommend-section {
    margin-bottom: 24px;
}

.section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
}

.section-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 20px;
    font-weight: 600;
    color: #333;
    margin: 0;
}

.title-icon {
    font-size: 24px;
}

.section-more {
    color: #ff5000;
    cursor: pointer;
    font-size: 14px;
}

.product-grid {
    display: grid;
    grid-template-columns: repeat(5, 1fr);
    gap: 16px;
}

.product-card {
    background: white;
    border-radius: 12px;
    overflow: hidden;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    transition: all 0.3s ease;
    text-decoration: none;
    color: inherit;
}

.product-card:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.product-image-wrapper {
    position: relative;
    width: 100%;
    height: 200px;
    overflow: hidden;
}

.product-image {
    width: 100%;
    height: 100%;
    object-fit: contain;
    background: #f5f5f5;
}

.product-image-fallback {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    display: none;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, #fff5f0 0%, #ffe8d6 100%);
    padding: 12px;
    box-sizing: border-box;
}

.fallback-text {
    font-size: 14px;
    font-weight: 700;
    color: #ff6b35;
    text-align: center;
    line-height: 1.4;
    word-break: break-all;
    overflow: hidden;
    display: -webkit-box;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
}

.emoji-placeholder {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 28px;
    font-weight: 700;
    color: #ff6b35;
    background: linear-gradient(135deg, #fff5f0 0%, #ffe8d6 100%);
    text-align: center;
    padding: 10px;
    line-height: 1.3;
    word-break: break-all;
}

.discount-tag {
    position: absolute;
    top: 8px;
    left: 8px;
    background: #ff5000;
    color: white;
    padding: 2px 8px;
    border-radius: 4px;
    font-size: 12px;
    font-weight: 600;
}

.product-info {
    padding: 12px;
}

.product-title {
    font-size: 14px;
    color: #333;
    line-height: 1.4;
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    margin-bottom: 8px;
}

.product-price-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
}

.product-price {
    color: #ff5000;
    font-size: 18px;
    font-weight: 700;
}

.product-sales {
    font-size: 12px;
    color: #999;
}

.product-tags {
    display: flex;
    gap: 8px;
}

.tag {
    padding: 2px 8px;
    border-radius: 4px;
    font-size: 12px;
    font-weight: 600;
}

.tag-hot {
    background: #fff2e8;
    color: #ff5000;
}

.tag-good {
    background: #f6ffed;
    color: #52c41a;
}

/* 返回顶部 */
.back-to-top {
    position: fixed;
    bottom: 40px;
    right: 40px;
    width: 48px;
    height: 48px;
    background: white;
    border-radius: 50%;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: all 0.3s ease;
    z-index: 100;
}

.back-to-top:hover {
    background: #ff5000;
    color: white;
}

.back-to-top span {
    font-size: 20px;
}
</style>
