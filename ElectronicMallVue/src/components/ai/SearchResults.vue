<template>
  <div class="search-results-container">
    <div class="search-results-header">
      <div class="search-results-title">搜索结果：</div>
      <div class="data-source-badge" v-if="dataSource && dataSource !== ''">
        <span class="badge-icon"></span>
        <span class="badge-text">{{ dataSource }}</span>
      </div>
    </div>
    <div class="search-results-grid">
      <div 
        v-for="(good, goodIndex) in searchResults" 
        :key="goodIndex"
        class="search-result-item"
        @click="$emit('view-detail', good)"
      >
        <div class="product-index">{{ goodIndex + 1 }}</div>
        <img 
          v-if="good.imgs && !isEmojiImage(good.imgs) && isValidImageUrl(good.imgs)" 
          :src="getImageUrl(good.imgs)" 
          :alt="good.name"
          class="search-result-img"
          @error="handleImageError"
        />
        <div 
          v-else-if="isEmojiImage(good.imgs)"
          class="search-result-emoji-placeholder"
          :style="{ backgroundColor: getEmojiBgColor(good.imgs) }"
        >
          <span class="search-result-emoji-icon">{{ getEmoji(good.imgs) }}</span>
        </div>
        <div v-else class="search-result-img-placeholder">
          <span class="placeholder-icon">📦</span>
          <span class="placeholder-text">暂无图片</span>
        </div>
        <div class="search-result-info">
          <div class="search-result-name">{{ good.name }}</div>
          <div class="search-result-price">¥{{ getGoodPrice(good) }}</div>
          <div class="search-result-shop" v-if="good.shopName">{{ good.shopName }}</div>
        </div>
        <div class="search-result-actions">
          <!-- 百度优选商品：只显示跳转购买按钮 -->
          <a 
            v-if="good.spuUrl"
            :href="good.spuUrl"
            target="_blank"
            class="search-result-btn btn-buy baidu-buy"
            @click.stop
          >
            <span class="btn-icon"></span>
            <span>去百度优选购买</span>
          </a>
          <!-- 本地商品：显示加入购物车和立即购买 -->
          <template v-else>
            <button 
              class="search-result-btn btn-cart"
              @click.stop="handleAddToCart(good)"
            >
              <span class="btn-icon">🛒</span>
              <span>加入购物车</span>
            </button>
            <button 
              class="search-result-btn btn-buy"
              @click.stop="handleBuyNow(good)"
            >
              <span class="btn-icon"></span>
              <span>立即购买</span>
            </button>
          </template>
        </div>
      </div>
    </div>
    <div class="search-results-hint" v-if="searchResults.length > 0">
      
    </div>

  </div>
</template>

<script>
export default {
  name: 'SearchResults',
  props: {
    searchResults: {
      type: Array,
      required: true,
      default: () => []
    },
    dataSource: {
      type: String,
      default: ''
    }
  },
  methods: {
    getImageUrl(image) {
      if (!image) return ''
      if (image.startsWith('http://') || image.startsWith('https://')) {
        return image
      }
      return 'http://localhost:9191' + image
    },
    
    isValidImageUrl(image) {
      if (!image) return false
      // 检查是否是有效的URL（不是emoji标记或其他特殊格式）
      if (image.startsWith('emoji:') || image.includes('😀') || image.includes('😊')) {
        return false
      }
      // 检查是否是有效的HTTP URL或相对路径
      return image.startsWith('http') || image.startsWith('/') || image.startsWith('data:')
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
    
    handleAddToCart(good) {
      console.log('搜索结果 - 添加到购物车:', good)
      if (!good || !good.id) {
        this.$message.error('商品信息不完整')
        return
      }
      
      const userStr = localStorage.getItem('user')
      if (!userStr) {
        this.$message.error('请先登录后再添加购物车')
        return
      }
      
      let user
      try {
        user = JSON.parse(userStr)
      } catch (e) {
        this.$message.error('登录信息异常，请重新登录')
        return
      }
      
      if (!user.id || !user.token) {
        this.$message.error('登录信息不完整，请重新登录')
        return
      }
      
      const userId = user.id
      
      // 先获取商品信息，获取第一个规格
      this.request.get('/api/good/' + good.id).then(res => {
        if (res.code === '200') {
          const goodDetail = res.data
          
          // 获取第一个规格
          let standard = '默认'
          if (goodDetail.standardList && goodDetail.standardList.length > 0) {
            standard = goodDetail.standardList[0]
          }
          
          // 调用购物车 API
          this.request.post('/api/cart', {
            goodId: good.id,
            count: 1,
            userId: userId,
            standard: standard
          }).then(res => {
            if (res.code === '200') {
              this.$message.success('商品已成功添加到购物车！')
              // 触发购物车刷新事件
              this.$root.$emit('refresh-cart')
            } else {
              this.$message.error(res.msg || '添加购物车失败')
            }
          }).catch(error => {
            console.error('添加购物车失败:', error)
            this.$message.error('添加购物车失败，请稍后重试')
          })
        }
      }).catch(error => {
        console.error('获取商品信息失败:', error)
        this.$message.error('获取商品信息失败')
      })
    },
    
    handleBuyNow(good) {
      console.log('搜索结果 - 立即购买:', good)
      if (!good || !good.id) {
        this.$message.error('商品信息不完整')
        return
      }
      
      const userStr = localStorage.getItem('user')
      if (!userStr) {
        this.$message.error('请先登录后再购买')
        return
      }
      
      let user
      try {
        user = JSON.parse(userStr)
      } catch (e) {
        this.$message.error('登录信息异常，请重新登录')
        return
      }
      
      if (!user.id || !user.token) {
        this.$message.error('登录信息不完整，请重新登录')
        return
      }
      
      // 跳转到商品详情页面 - 注意路由路径是 goodView（V大写）
      this.$router.push({
        path: `/goodView/${good.id}`
      })
    }
  }
}
</script>

<style scoped>
.search-results-container {
  margin-top: 16px;
  padding: 16px;
  background: linear-gradient(135deg, #f5f7fa 0%, #ffffff 100%);
  border-radius: 12px;
  border: 1px solid #e4e7ed;
}

.search-results-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.search-results-title {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.data-source-badge {
  display: flex;
  align-items: center;
  padding: 4px 10px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  color: #fff;
  font-size: 12px;
  font-weight: 500;
}

.badge-icon {
  margin-right: 4px;
  font-size: 12px;
}

.badge-text {
  font-size: 12px;
}

.search-results-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.search-result-item {
  display: flex;
  flex-direction: column;
  background-color: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
}

.search-result-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.product-index {
  position: absolute;
  top: 8px;
  left: 8px;
  width: 24px;
  height: 24px;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
  z-index: 1;
}

.search-result-img {
  width: 100%;
  height: 150px;
  object-fit: cover;
}

.search-result-emoji-placeholder,
.search-result-img-placeholder {
  width: 100%;
  height: 150px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background-color: #f0f0f0;
  font-size: 48px;
}

.placeholder-icon {
  font-size: 36px;
  margin-bottom: 4px;
}

.placeholder-text {
  font-size: 12px;
  color: #999;
}

.search-result-info {
  padding: 12px 12px 4px;
  flex: 1;
  min-height: 0;
}

.search-result-name {
  font-size: 13px;
  color: #303133;
  margin-bottom: 6px;
  line-height: 1.3;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  min-height: 34px;
}

.search-result-price {
  font-size: 16px;
  color: #ff4400;
  font-weight: bold;
  margin-bottom: 4px;
  white-space: nowrap;
}

.search-result-shop {
  font-size: 11px;
  color: #999;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.search-result-actions {
  display: flex;
  gap: 8px;
  padding: 8px 12px 12px;
  flex-shrink: 0;
}

.search-result-btn {
  flex: 1;
  height: 36px;
  min-width: 0;
  border: none;
  border-radius: 18px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.3s ease;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  text-decoration: none;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  padding: 0 12px;
}

.btn-icon {
  font-size: 14px;
  line-height: 1;
  flex-shrink: 0;
}

.search-result-btn span:not(.btn-icon) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.search-result-btn.btn-cart {
  background: white;
  color: #ff5000;
  border: 1.5px solid #ff5000;
}

.search-result-btn.btn-cart:hover {
  background: #fff5f0;
  transform: translateY(-1px);
}

.search-result-btn.btn-buy {
  background: linear-gradient(90deg, #ff9000 0%, #ff5000 100%);
  box-shadow: 0 2px 8px rgba(255, 80, 0, 0.25);
}

.search-result-btn.btn-buy:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 80, 0, 0.35);
}

/* 百度优选购买按钮样式 */
.search-result-btn.btn-buy.baidu-buy {
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.25);
}

.search-result-btn.btn-buy.baidu-buy:hover {
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.35);
}

.search-results-hint {
  margin-top: 12px;
  padding: 8px 12px;
  background: #f0f9ff;
  border-radius: 6px;
  font-size: 12px;
  color: #666;
  text-align: center;
}
</style>
