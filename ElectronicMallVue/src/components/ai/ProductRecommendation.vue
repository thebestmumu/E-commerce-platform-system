<template>
  <div class="product-recommendation-container">
    <!-- 首推商品（淘宝问问风格） -->
    <div v-if="topGood" class="taobao-top-recommendation">
      <div class="taobao-recommendation-header">
        <span class="taobao-recommendation-text">问问将会为你推荐三款非常受欢迎的宝贝：</span>
      </div>
      <div class="taobao-top-item" @click="$emit('view-detail', topGood)">
        <div class="taobao-top-img-wrapper">
          <img 
            v-if="topGood.imgs && !isEmojiImage(topGood.imgs)" 
            :src="getImageUrl(topGood.imgs)" 
            :alt="topGood.name"
            class="taobao-top-img"
            @error="handleImageError"
          />
          <div 
            v-else-if="isEmojiImage(topGood.imgs)"
            class="taobao-top-emoji-placeholder"
            :style="{ backgroundColor: getEmojiBgColor(topGood.imgs) }"
          >
            <span class="taobao-top-emoji-icon">{{ getEmoji(topGood.imgs) }}</span>
          </div>
          <div v-else class="taobao-top-img-placeholder">📦</div>
        </div>
        <div class="taobao-top-info">
          <div class="taobao-top-name">{{ topGood.name }}</div>
          
          <!-- 商品标签 -->
          <div v-if="topGood.tags" class="taobao-top-tags">
            <span 
              v-for="(tag, tagIndex) in topGood.tags.split(',').slice(0, 3)" 
              :key="tagIndex"
              class="taobao-tag-item"
            >
              {{ tag.trim() }}
            </span>
          </div>
          
          <div class="taobao-top-price">
            <span class="price-symbol">¥</span>
            <span class="price-value">{{ getGoodPrice(topGood) }}</span>
            <span class="taobao-top-sales">{{ topGood.sales || 0 }}+人付款</span>
          </div>
          
          <!-- 评分和评论数 -->
          <div class="taobao-top-rating">
            <div class="rating-stars">
              <span v-for="i in 5" :key="i" class="star-icon" :class="{ filled: i <= Math.round(topGood.goodRating || 5) }">
                {{ i <= Math.round(topGood.goodRating || 5) ? '⭐' : '☆' }}
              </span>
            </div>
            <span class="rating-score">{{ topGood.goodRating || 5.0 }}分</span>
            <span class="review-count">{{ topGood.reviewCount || 0 }}条评价</span>
          </div>
          
          <!-- 第一条五星评论 -->
          <div v-if="topGood.firstReview && topGood.firstReview.content" class="taobao-first-review">
            <div class="review-user">
              <span class="user-avatar">👤</span>
              <span class="user-name">{{ topGood.firstReview.user && topGood.firstReview.user.nickname ? topGood.firstReview.user.nickname : '匿名用户' }}</span>
              <span class="review-rating">
                <span v-for="i in 5" :key="i" :class="{ filled: i <= topGood.firstReview.rating }">⭐</span>
              </span>
            </div>
            <div class="review-content">
              {{ truncateText(topGood.firstReview.content, 60) }}
            </div>
            <div v-if="topGood.firstReview.tags" class="review-tags">
              <span 
                v-for="(tag, tagIndex) in topGood.firstReview.tags.split(',').slice(0, 3)" 
                :key="tagIndex"
                class="review-tag-item"
              >
                {{ tag.trim() }}
              </span>
            </div>
          </div>
          
          <div class="taobao-top-actions">
            <button 
              class="taobao-cart-btn"
              @click.stop="$emit('add-to-cart', topGood)"
              title="加入购物车"
            >
              <span class="btn-icon">🛒</span>
            </button>
            <button 
              class="taobao-buy-btn"
              @click.stop="$emit('buy-now', topGood)"
              title="立即购买"
            >
              <span class="btn-icon">⚡</span>
            </button>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 其他商品（淘宝问问风格 - 两列卡片） -->
    <div v-if="otherGoods && otherGoods.length > 0" class="taobao-other-recommendations">
      <div class="taobao-other-header">
        <span class="taobao-other-text">你还可能感兴趣以下商品：</span>
        <!-- 排序按钮 -->
        <div class="sort-buttons">
          <button 
            :class="['sort-btn', { active: currentSortBy === 'sales' }]"
            @click="handleSort('sales')"
          >
            🔥 销量
          </button>
          <button 
            :class="['sort-btn', { active: currentSortBy === 'price' }]"
            @click="handleSort('price')"
          >
            💰 价格
          </button>
          <button 
            :class="['sort-btn', { active: currentSortBy === 'rating' }]"
            @click="handleSort('rating')"
          >
            ⭐ 好评
          </button>
        </div>
      </div>
      <div class="taobao-other-grid">
        <div 
          v-for="(good, goodIndex) in sortedGoods.slice(0, displayCount)" 
          :key="goodIndex"
          class="taobao-other-item"
          @click="$emit('view-detail', good)"
        >
          <div class="taobao-other-img-wrapper">
            <img 
              v-if="good.imgs && !isEmojiImage(good.imgs)" 
              :src="getImageUrl(good.imgs)" 
              :alt="good.name"
              class="taobao-other-img"
              @error="handleImageError"
            />
            <div 
              v-else-if="isEmojiImage(good.imgs)"
              class="taobao-other-emoji-placeholder"
              :style="{ backgroundColor: getEmojiBgColor(good.imgs) }"
            >
              <span class="taobao-other-emoji-icon">{{ getEmoji(good.imgs) }}</span>
            </div>
            <div v-else class="taobao-other-img-placeholder">📦</div>
          </div>
          <div class="taobao-other-info">
            <div class="taobao-other-name">{{ good.name }}</div>
            
            <!-- 商品标签（如果有） -->
            <div v-if="good.tags" class="taobao-other-tags">
              <span 
                v-for="(tag, tagIndex) in good.tags.split(',').slice(0, 2)" 
                :key="tagIndex"
                class="taobao-other-tag-item"
              >
                {{ tag.trim() }}
              </span>
            </div>
            
            <div class="taobao-other-bottom">
              <div class="taobao-other-price-row">
                <span class="other-price-symbol">¥</span>
                <span class="other-price-value">{{ getGoodPrice(good) }}</span>
              </div>
              <div class="taobao-other-meta">
                <span class="taobao-other-sales">{{ good.sales || 0 }}人付款</span>
                <span class="taobao-other-rating">{{ good.goodRating || 5.0 }}分</span>
              </div>
            </div>
            <div class="taobao-other-actions">
              <button 
                class="taobao-other-cart-btn"
                @click.stop="$emit('add-to-cart', good)"
                title="加入购物车"
              >
                <span class="btn-icon">🛒</span>
              </button>
              <button 
                class="taobao-other-buy-btn"
                @click.stop="$emit('buy-now', good)"
                title="立即购买"
              >
                <span class="btn-icon">⚡</span>
              </button>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 加载更多按钮 -->
      <div v-if="displayCount < sortedGoods.length" class="load-more-container">
        <button class="load-more-btn" @click="loadMore">
          展开更多商品（还有 {{ sortedGoods.length - displayCount }} 个）
        </button>
      </div>
    </div>
    
    <!-- 旧格式兼容（数组格式） -->
    <div v-if="recommendations && recommendations.length > 0 && !topGood" class="recommendations-grid">
      <div 
        v-for="(good, goodIndex) in recommendations.slice(0, 12)" 
        :key="goodIndex"
        class="recommendation-item"
      >
        <div class="recommendation-img-container" @click="$emit('view-detail', good)">
          <img 
            v-if="good.imgs && !isEmojiImage(good.imgs)" 
            :src="getImageUrl(good.imgs)" 
            :alt="good.name"
            class="recommendation-img"
            @error="handleImageError"
          />
          <div 
            v-else-if="isEmojiImage(good.imgs)"
            class="recommendation-emoji-placeholder"
            :style="{ backgroundColor: getEmojiBgColor(good.imgs) }"
          >
            <span class="recommendation-emoji-icon">{{ getEmoji(good.imgs) }}</span>
          </div>
          <div v-else class="recommendation-img-placeholder">📦</div>
        </div>
        <div class="recommendation-info">
          <div class="recommendation-name" @click="$emit('view-detail', good)">{{ good.name }}</div>
          <div class="recommendation-price">¥{{ getGoodPrice(good) }}</div>
          <div class="recommendation-action">
            <button 
              class="recommendation-cart-btn"
              @click.stop="$emit('add-to-cart', good)"
            >
              <span class="btn-icon">🛒</span>
              <span>加购</span>
            </button>
            <button 
              class="recommendation-buy-btn"
              @click.stop="$emit('buy-now', good)"
            >
              <span class="btn-icon"></span>
              <span>购买</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ProductRecommendation',
  props: {
    topGood: {
      type: Object,
      default: null
    },
    otherGoods: {
      type: Array,
      default: () => []
    },
    recommendations: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      currentSortBy: 'sales',
      displayCount: 6
    }
  },
  computed: {
    sortedGoods() {
      const goods = [...this.otherGoods]
      switch (this.currentSortBy) {
        case 'sales':
          return goods.sort((a, b) => (b.sales || 0) - (a.sales || 0))
        case 'price':
          return goods.sort((a, b) => {
            const priceA = a.price != null ? a.price : (a.saleMoney != null ? a.saleMoney / 100 : 0)
            const priceB = b.price != null ? b.price : (b.saleMoney != null ? b.saleMoney / 100 : 0)
            return priceA - priceB
          })
        case 'rating':
          return goods.sort((a, b) => (b.goodRating || 0) - (a.goodRating || 0))
        default:
          return goods
      }
    }
  },
  methods: {
    handleSort(sortBy) {
      this.currentSortBy = sortBy
    },
    
    loadMore() {
      this.displayCount += 6
    },
    
    getImageUrl(image) {
      if (!image) return ''
      if (image.startsWith('http://') || image.startsWith('https://')) {
        return image
      }
      return 'http://localhost:9191' + image
    },
    
    handleImageError(e) {
      e.target.style.display = 'none'
      const placeholder = e.target.nextElementSibling
      if (placeholder) {
        placeholder.style.display = 'flex'
      }
    },
    
    isEmojiImage(image) {
      return image && (image.startsWith('emoji:') || image.includes('😀') || image.includes('😊'))
    },
    
    getEmoji(image) {
      const emojis = ['📱', '💻', '👕', '👟', '🎮', '📚', '🏠', '🎁']
      let hash = 0
      for (let i = 0; i < (image || '').length; i++) {
        hash = image.charCodeAt(i) + ((hash << 5) - hash)
      }
      return emojis[Math.abs(hash) % emojis.length]
    },
    
    getEmojiBgColor(image) {
      const colors = ['#e3f2fd', '#f3e5f5', '#e8f5e9', '#fff3e0', '#fce4ec']
      let hash = 0
      for (let i = 0; i < (image || '').length; i++) {
        hash = image.charCodeAt(i) + ((hash << 5) - hash)
      }
      return colors[Math.abs(hash) % colors.length]
    },
    
    getGoodPrice(good) {
      if (good.price != null) return good.price
      if (good.saleMoney != null) return (good.saleMoney / 100).toFixed(2)
      return '0.00'
    },
    
    truncateText(text, maxLength) {
      if (!text) return ''
      if (text.length <= maxLength) return text
      return text.substring(0, maxLength) + '...'
    }
  }
}
</script>

<style scoped>
.product-recommendation-container {
  margin-top: 16px;
}

.taobao-top-recommendation {
  margin-bottom: 16px;
}

.taobao-recommendation-header {
  margin-bottom: 12px;
}

.taobao-recommendation-text {
  font-size: 13px;
  color: #606266;
}

.taobao-top-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: linear-gradient(135deg, #fff5f0 0%, #ffffff 100%);
  border-radius: 12px;
  border: 1px solid #ffe0cc;
  cursor: pointer;
  transition: all 0.3s ease;
}

.taobao-top-item:hover {
  box-shadow: 0 4px 12px rgba(255, 140, 0, 0.15);
  transform: translateY(-2px);
}

.taobao-top-img-wrapper {
  flex-shrink: 0;
}

.taobao-top-img {
  width: 120px;
  height: 120px;
  object-fit: cover;
  border-radius: 8px;
}

.taobao-top-emoji-placeholder,
.taobao-top-img-placeholder {
  width: 120px;
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f0f0f0;
  border-radius: 8px;
  font-size: 48px;
}

.taobao-top-info {
  flex: 1;
  position: relative;
}

.taobao-top-name {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
  margin-bottom: 8px;
  line-height: 1.4;
}

.taobao-top-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 8px;
}

.taobao-tag-item {
  padding: 2px 6px;
  background-color: #fff0e6;
  color: #ff7700;
  font-size: 10px;
  border-radius: 8px;
}

.taobao-top-price {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin-bottom: 8px;
}

.price-symbol {
  font-size: 12px;
  color: #ff4400;
}

.price-value {
  font-size: 20px;
  font-weight: bold;
  color: #ff4400;
}

.taobao-top-sales {
  font-size: 11px;
  color: #909399;
  margin-left: 8px;
}

.taobao-top-rating {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.rating-stars {
  display: flex;
  gap: 2px;
}

.star-icon {
  font-size: 12px;
  color: #ddd;
}

.star-icon.filled {
  color: #ff9000;
}

.rating-score {
  font-size: 12px;
  color: #ff9000;
  font-weight: bold;
}

.review-count {
  font-size: 11px;
  color: #909399;
}

.taobao-first-review {
  padding: 8px;
  background-color: #fff;
  border-radius: 6px;
  margin-bottom: 8px;
}

.review-user {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
  font-size: 11px;
  color: #606266;
}

.user-avatar {
  font-size: 12px;
}

.review-rating {
  display: flex;
  gap: 2px;
}

.review-rating .filled {
  color: #ff9000;
}

.review-content {
  font-size: 12px;
  color: #303133;
  line-height: 1.4;
  margin-bottom: 4px;
}

.review-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.review-tag-item {
  padding: 1px 4px;
  background-color: #f5f7fa;
  color: #909399;
  font-size: 10px;
  border-radius: 6px;
}

.taobao-top-actions {
  display: flex;
  gap: 6px;
  margin-top: 10px;
}

.taobao-cart-btn {
  width: 32px;
  height: 32px;
  min-width: 32px;
  border: 1.5px solid #ff5000;
  border-radius: 16px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
  color: #ff5000;
  background: white;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}

.taobao-cart-btn:hover {
  background: #fff5f0;
  transform: translateY(-1px);
}

.taobao-buy-btn {
  width: 32px;
  height: 32px;
  min-width: 32px;
  border: none;
  border-radius: 16px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
  color: #fff;
  background: linear-gradient(90deg, #ff9000 0%, #ff5000 100%);
  box-shadow: 0 2px 8px rgba(255, 80, 0, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}

.taobao-buy-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 80, 0, 0.35);
}

.taobao-other-recommendations {
  margin-top: 16px;
}

.taobao-other-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.taobao-other-text {
  font-size: 13px;
  color: #606266;
}

.sort-buttons {
  display: flex;
  gap: 8px;
}

.sort-btn {
  padding: 4px 8px;
  background-color: #f5f7fa;
  color: #606266;
  border: 1px solid #e4e7ed;
  border-radius: 12px;
  font-size: 11px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.sort-btn:hover {
  background-color: #ff9000;
  color: #fff;
  border-color: #ff9000;
}

.sort-btn.active {
  background-color: #ff9000;
  color: #fff;
  border-color: #ff9000;
}

/* 两列网格布局 */
.taobao-other-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  max-height: 400px;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: thin;
  scrollbar-color: #ff9000 #f0f0f0;
  padding-right: 4px;
}

.taobao-other-grid::-webkit-scrollbar {
  width: 6px;
}

.taobao-other-grid::-webkit-scrollbar-track {
  background: #f0f0f0;
  border-radius: 3px;
}

.taobao-other-grid::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, #ff9000, #ff5000);
  border-radius: 3px;
}

/* 淘宝风格小卡片 */
.taobao-other-item {
  background: linear-gradient(135deg, #ffffff 0%, #fafbfc 100%);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  border: 1px solid #f0f0f0;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
  position: relative;
}

.taobao-other-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 20px rgba(255, 80, 0, 0.12);
  border-color: #ff9000;
}

.taobao-other-img-wrapper {
  width: 100%;
  height: 140px;
  overflow: hidden;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
}

.taobao-other-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.taobao-other-item:hover .taobao-other-img {
  transform: scale(1.05);
}

.taobao-other-emoji-placeholder,
.taobao-other-img-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
}

.taobao-other-info {
  padding: 10px;
  position: relative;
}

.taobao-other-name {
  font-size: 13px;
  color: #303133;
  margin-bottom: 6px;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  font-weight: 500;
}

.taobao-other-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 6px;
}

.taobao-other-tag-item {
  padding: 1px 4px;
  background-color: #fff0e6;
  color: #ff7700;
  font-size: 9px;
  border-radius: 6px;
}

.taobao-other-bottom {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.taobao-other-price-row {
  display: flex;
  align-items: baseline;
  gap: 2px;
}

.other-price-symbol {
  font-size: 11px;
  color: #ff4400;
}

.other-price-value {
  font-size: 18px;
  font-weight: bold;
  color: #ff4400;
}

.taobao-other-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 10px;
  color: #909399;
}

.taobao-other-sales {
  color: #909399;
}

.taobao-other-rating {
  color: #ff9000;
  font-weight: bold;
}

.taobao-other-actions {
  display: flex;
  gap: 6px;
  margin-top: 8px;
}

.taobao-other-cart-btn {
  width: 32px;
  height: 32px;
  min-width: 32px;
  border: 1.5px solid #ff5000;
  border-radius: 16px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
  color: #ff5000;
  background: white;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}

.taobao-other-cart-btn:hover {
  background: #fff5f0;
  transform: translateY(-1px);
}

.taobao-other-buy-btn {
  width: 32px;
  height: 32px;
  min-width: 32px;
  border: none;
  border-radius: 16px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
  color: #fff;
  background: linear-gradient(90deg, #ff9000 0%, #ff5000 100%);
  box-shadow: 0 2px 8px rgba(255, 80, 0, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}

.taobao-other-buy-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 80, 0, 0.35);
}

.load-more-container {
  margin-top: 12px;
  text-align: center;
}

.load-more-btn {
  padding: 10px 24px;
  background: linear-gradient(135deg, #ff9000, #ff5000);
  color: #fff;
  border: none;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(255, 80, 0, 0.2);
}

.load-more-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 80, 0, 0.3);
}

.load-more-btn:active {
  transform: translateY(0);
}

.recommendations-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
}

.recommendation-item {
  background-color: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.recommendation-img-container {
  cursor: pointer;
}

.recommendation-img {
  width: 100%;
  height: 140px;
  object-fit: cover;
}

.recommendation-emoji-placeholder,
.recommendation-img-placeholder {
  width: 100%;
  height: 140px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f0f0f0;
  font-size: 48px;
}

.recommendation-info {
  padding: 8px;
}

.recommendation-name {
  font-size: 13px;
  color: #303133;
  margin-bottom: 4px;
  cursor: pointer;
}

.recommendation-name:hover {
  color: #409eff;
}

.recommendation-price {
  font-size: 14px;
  color: #ff4400;
  font-weight: bold;
  margin-bottom: 8px;
}

.recommendation-action {
  display: flex;
  gap: 6px;
}

.recommendation-cart-btn,
.recommendation-buy-btn {
  flex: 1;
  height: 32px;
  min-width: 0;
  border: none;
  border-radius: 16px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
  transition: all 0.3s ease;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 3px;
  white-space: nowrap;
  overflow: hidden;
  padding: 0 8px;
}

.recommendation-cart-btn {
  background: white;
  color: #ff5000;
  border: 1.5px solid #ff5000;
}

.recommendation-cart-btn:hover {
  background: #fff5f0;
  transform: translateY(-1px);
}

.recommendation-buy-btn {
  background: linear-gradient(90deg, #ff9000 0%, #ff5000 100%);
  box-shadow: 0 2px 8px rgba(255, 80, 0, 0.25);
}

.recommendation-buy-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 80, 0, 0.35);
}
</style>
