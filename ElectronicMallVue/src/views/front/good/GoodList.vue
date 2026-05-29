<template>
  <div class="taobao-goodlist">
    <search @search="handleSearch"></search>
    
    <div class="main-content">
      <!-- 面包屑导航 -->
      <div class="breadcrumb">
        <router-link to="/" class="breadcrumb-item">首页</router-link>
        <span class="breadcrumb-separator">></span>
        <span class="breadcrumb-item current">{{ currentCategoryName || '商品列表' }}</span>
      </div>

      <!-- 筛选栏 -->
      <div class="filter-bar">
        <div class="filter-tabs">
          <span 
            v-for="(tab, index) in filterTabs" 
            :key="index"
            :class="['filter-tab', { active: currentTab === tab.value }]"
            @click="changeTab(tab.value)"
          >
            {{ tab.label }}
          </span>
        </div>
      </div>

      <!-- 分类侧边栏 -->
      <div class="list-layout">
        <div class="category-sidebar">
          <div class="sidebar-header">商品分类</div>
          <ul class="sidebar-list">
            <li v-for="(item, index) in icons" :key="index" class="sidebar-item">
              <div class="sidebar-item-header">
                <i class="sidebar-icon" v-html="item.value"></i>
                <span>{{ item.categories && item.categories[0] ? item.categories[0].name : '' }}</span>
              </div>
              <div class="sidebar-item-links">
                <a
                  v-for="(category, idx) in item.categories"
                  :key="idx"
                  href="#"
                  @click.prevent="load(category.id)"
                  :class="['sidebar-link', { active: categoryId == category.id }]"
                >
                  {{ category.name }}
                </a>
              </div>
            </li>
          </ul>
        </div>

        <!-- 商品列表 -->
        <div class="product-list-wrapper">
          <div class="product-grid">
            <router-link 
              v-for="item in goods" 
              :key="item.id"
              :to="'goodView/' + item.id"
              class="product-card"
            >
              <div class="product-image-wrapper">
                <div 
                  v-if="isEmojiImage(item.imgs)"
                  class="emoji-placeholder"
                >
                  {{ getEmoji(item.imgs) }}
                </div>
                <img
                  v-else
                  :src="baseApi + item.imgs"
                  class="product-image"
                  @error="handleImageError"
                />
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
                  <span v-if="item.reviewCount && item.reviewCount > 100" class="tag tag-review">{{ item.reviewCount }}条评价</span>
                </div>
              </div>
            </router-link>
          </div>

          <!-- 空状态 -->
          <div v-if="goods.length === 0" class="empty-state">
            <div class="empty-icon">📦</div>
            <div class="empty-text">暂无商品</div>
          </div>

          <!-- 分页 -->
          <div class="pagination-wrapper">
            <el-pagination
              background
              :hide-on-single-page="false"
              :current-page="currentPage"
              :page-size="pageSize"
              layout="total, prev, pager, next, jumper"
              :total="total"
              @current-change="handleCurrentPage"
            >
            </el-pagination>
          </div>
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
import search from "../../../components/Search";

export default {
  name: "GoodList",
  data() {
    return {
      icons: [],
      total: 0,
      pageSize: 20,
      currentPage: 1,
      categoryId: null,
      searchText: "",
      goods: [],
      baseApi: this.$store.state.baseApi,
      currentCategoryName: '',
      currentTab: 'default',
      filterTabs: [
        { label: '综合', value: 'default' },
        { label: '销量', value: 'sales' },
        { label: '价格', value: 'price' },
        { label: '好评榜', value: 'rating' },
      ],
      showBackToTop: false,
    };
  },
  components: {
    search,
  },
  created() {
    // 支持两种参数名：searchText 和 keyword
    this.searchText = this.$route.query.searchText || this.$route.query.keyword || '';
    this.categoryId = this.$route.query.categoryId;

    console.log('GoodList created - 路由参数:', this.$route.query);
    console.log('GoodList created - searchText:', this.searchText);
    console.log('GoodList created - categoryId:', this.categoryId);

    this.loadCategories();
    this.load();
    
    window.addEventListener('scroll', this.handleScroll);
  },
  watch: {
    '$route.query': {
      handler(newQuery) {
        console.log('路由参数变化:', newQuery);
        this.searchText = newQuery.searchText || newQuery.keyword || '';
        this.categoryId = newQuery.categoryId;
        this.currentPage = 1;
        this.load();
      },
      deep: true
    }
  },
  beforeDestroy() {
    window.removeEventListener('scroll', this.handleScroll);
  },
  methods: {
    loadCategories() {
      this.request.get("/api/icon").then((res) => {
        if (res.code === "200") {
          this.icons = res.data;
        }
      });
    },
    handleCurrentPage(currentPage) {
      this.currentPage = currentPage;
      this.load();
      window.scrollTo({ top: 0, behavior: 'smooth' });
    },
    handleSearch(text) {
      this.searchText = text;
      this.currentPage = 1;
      this.load();
    },
    changeTab(tab) {
      this.currentTab = tab;
      this.load();
    },
    load(categoryId) {
      console.log('load() 被调用 - searchText:', this.searchText);
      console.log('load() 被调用 - categoryId:', this.categoryId);
      
      if (categoryId != undefined) {
        this.categoryId = categoryId;
        this.currentPage = 1;

        this.$router.push({
          path: "/goodList",
          query: { categoryId: this.categoryId, keyword: this.searchText },
        });
        
        const category = this.findCategoryName(categoryId);
        this.currentCategoryName = category;
      }
      
      const requestParams = {
        pageNum: this.currentPage,
        pageSize: this.pageSize,
        searchText: this.searchText,
        categoryId: this.categoryId,
        sortBy: this.currentTab,
      };
      
      console.log('发送搜索请求，参数:', requestParams);
      
      this.request
        .get("/api/good/page", {
          params: requestParams,
        })
        .then((res) => {
          console.log('搜索结果:', res);
          if (res.code === "200") {
            this.total = res.data.total;
            this.goods = res.data.records;
            console.log('商品数量:', this.goods.length);
          }
        });
    },
    findCategoryName(categoryId) {
      for (const item of this.icons) {
        for (const category of item.categories) {
          if (category.id == categoryId) {
            return category.name;
          }
        }
      }
      return '';
    },
    isEmojiImage(imgs) {
      return imgs && imgs.startsWith('emoji:');
    },
    getEmoji(imgs) {
      if (!imgs || !imgs.startsWith('emoji:')) return '📦';
      const parts = imgs.split(':');
      return parts.length >= 2 ? parts[1] : '📦';
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
.taobao-goodlist {
  min-height: 100vh;
  background: #f4f4f4;
}

.main-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 16px;
}

/* 面包屑导航 */
.breadcrumb {
  background: white;
  padding: 12px 16px;
  border-radius: 8px;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.breadcrumb-item {
  color: #666;
  text-decoration: none;
  transition: color 0.3s ease;
}

.breadcrumb-item:hover {
  color: #ff5000;
}

.breadcrumb-item.current {
  color: #333;
  font-weight: 600;
}

.breadcrumb-separator {
  color: #999;
}

/* 筛选栏 */
.filter-bar {
  background: white;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.filter-tabs {
  display: flex;
  gap: 24px;
}

.filter-tab {
  font-size: 16px;
  color: #666;
  cursor: pointer;
  padding: 8px 16px;
  border-radius: 20px;
  transition: all 0.3s ease;
}

.filter-tab:hover {
  color: #ff5000;
  background: #fff5f0;
}

.filter-tab.active {
  color: white;
  background: linear-gradient(90deg, #ff9000 0%, #ff5000 100%);
  font-weight: 600;
}

/* 布局 */
.list-layout {
  display: grid;
  grid-template-columns: 200px 1fr;
  gap: 16px;
}

/* 分类侧边栏 */
.category-sidebar {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  height: fit-content;
  position: sticky;
  top: 16px;
}

.sidebar-header {
  background: linear-gradient(90deg, #ff9000 0%, #ff5000 100%);
  padding: 12px 16px;
  color: white;
  font-size: 14px;
  font-weight: 600;
}

.sidebar-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.sidebar-item {
  padding: 12px 16px;
  border-bottom: 1px solid #f5f5f5;
}

.sidebar-item-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.sidebar-icon {
  font-size: 16px;
}

.sidebar-item-links {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.sidebar-link {
  font-size: 12px;
  color: #666;
  text-decoration: none;
  padding: 4px 8px;
  border-radius: 12px;
  background: #f5f5f5;
  transition: all 0.3s ease;
}

.sidebar-link:hover,
.sidebar-link.active {
  background: #ff5000;
  color: white;
}

/* 商品列表 */
.product-list-wrapper {
  background: white;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}

.product-card {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  text-decoration: none;
  color: inherit;
  transition: all 0.3s ease;
  border: 1px solid #f5f5f5;
}

.product-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  border-color: #ff5000;
}

.product-image-wrapper {
  position: relative;
  width: 100%;
  aspect-ratio: 1;
  overflow: hidden;
  background: #f5f5f5;
}

.product-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.product-card:hover .product-image {
  transform: scale(1.05);
}

.emoji-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 56px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.discount-tag {
  position: absolute;
  top: 8px;
  left: 8px;
  background: linear-gradient(90deg, #ff9000 0%, #ff5000 100%);
  color: white;
  padding: 4px 8px;
  border-radius: 12px;
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
  height: 40px;
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
  align-items: baseline;
  margin-bottom: 8px;
}

.product-price {
  color: #ff5000;
  font-size: 18px;
  font-weight: 700;
}

.product-price::before {
  content: "¥";
  font-size: 12px;
  margin-right: 2px;
}

.product-sales {
  font-size: 12px;
  color: #999;
}

.product-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.tag {
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 500;
}

.tag-hot {
  background: #fff0e5;
  color: #ff5000;
}

.tag-good {
  background: #e8f5e9;
  color: #4caf50;
}

.tag-review {
  background: #e3f2fd;
  color: #2196f3;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.empty-text {
  font-size: 16px;
}

/* 分页 */
.pagination-wrapper {
  margin-top: 32px;
  display: flex;
  justify-content: center;
}

::v-deep .el-pagination {
  padding: 20px 0;
}

::v-deep .el-pagination.is-background .el-pager li:not(.disabled).active {
  background-color: #ff5000;
  border-radius: 4px;
}

::v-deep .el-pagination.is-background .el-pager li:not(.disabled):hover {
  color: #ff5000;
}

/* 返回顶部 */
.back-to-top {
  position: fixed;
  bottom: 80px;
  right: 20px;
  width: 48px;
  height: 48px;
  background: white;
  border-radius: 50%;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  z-index: 999;
  font-size: 20px;
  color: #666;
}

.back-to-top:hover {
  background: #ff5000;
  color: white;
  transform: translateY(-4px);
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .list-layout {
    grid-template-columns: 1fr;
  }
  
  .category-sidebar {
    display: none;
  }
  
  .product-grid {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  }
}
</style>
