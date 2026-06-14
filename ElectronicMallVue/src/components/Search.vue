<template>
  <div class="search-wrapper">
    <div class="search-container">
      <div class="search-logo">商城</div>
      <div class="search-box">
        <input 
          @keydown.enter="$emit('search', searchText)" 
          type="text" 
          :placeholder="placeholder" 
          v-model="searchText"
          class="search-input"
        />
        <button @click="$emit('search', searchText)" class="search-btn">
          <i class="iconfont icon-search">🔍</i>
          <span>搜索</span>
        </button>
      </div>
      <div class="search-cart" @click="goToCart">
        <span class="cart-icon">🛒</span>
        <span class="cart-text">购物车</span>
        <span v-if="cartCount > 0" class="cart-badge">{{ cartCount > 99 ? '99+' : cartCount }}</span>
      </div>
      <div class="search-nav-item" @click="goToTickets">
        <span class="nav-item-icon">📋</span>
        <span class="nav-item-text">我的工单</span>
      </div>
    </div>
    <div class="search-hot-words">
      <span class="hot-label">热搜：</span>
      <span 
        v-for="(word, index) in hotWords" 
        :key="index" 
        class="hot-word"
        @click="$emit('search', word)"
      >
        {{ word }}
      </span>
    </div>
  </div>
</template>

<script>
export default {
  name: "Search",
  data() {
    return {
      searchText: '',
      placeholder: '搜索商品、品牌或店铺',
      hotWords: ['手机', '电脑', '耳机', '运动鞋', '零食'],
      cartCount: 0
    }
  },
  mounted() {
    this.loadCartCount()
  },
  methods: {
    goToCart() {
      this.$router.push('/cart')
    },
    goToTickets() {
      this.$router.push('/myTickets')
    },
    loadCartCount() {
      const userStr = localStorage.getItem('user')
      if (userStr) {
        try {
          const user = JSON.parse(userStr)
          this.request.get('/api/cart/count/' + user.id).then(res => {
            if (res.code === '200') {
              this.cartCount = res.data
            }
          }).catch(() => {
            this.cartCount = 0
          })
        } catch (e) {
          this.cartCount = 0
        }
      }
    }
  }
}
</script>

<style scoped>
.search-wrapper {
  background: linear-gradient(90deg, #ff9000 0%, #ff5000 100%);
  padding: 12px 0 8px;
  box-shadow: 0 2px 8px rgba(255, 80, 0, 0.2);
}

.search-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 16px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.search-logo {
  font-size: 28px;
  font-weight: 700;
  color: white;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  cursor: pointer;
  white-space: nowrap;
}

.search-box {
  flex: 1;
  display: flex;
  background: white;
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.search-input {
  flex: 1;
  padding: 10px 20px;
  border: none;
  outline: none;
  font-size: 14px;
  color: #333;
}

.search-input::placeholder {
  color: #999;
}

.search-btn {
  padding: 10px 24px;
  background: linear-gradient(90deg, #ff5000 0%, #ff0036 100%);
  color: white;
  border: none;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 4px;
  transition: all 0.3s ease;
}

.search-btn:hover {
  opacity: 0.9;
}

.search-cart {
  position: relative;
  background: rgba(255, 255, 255, 0.2);
  padding: 8px 16px;
  border-radius: 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  color: white;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.search-cart:hover {
  background: rgba(255, 255, 255, 0.3);
}

.cart-icon {
  font-size: 20px;
}

.cart-text {
  font-size: 14px;
  font-weight: 500;
}

.cart-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  background: #ff0036;
  color: white;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 10px;
  min-width: 16px;
  text-align: center;
  font-weight: 600;
}

.search-nav-item {
  position: relative;
  background: rgba(255, 255, 255, 0.2);
  padding: 8px 16px;
  border-radius: 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  color: white;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.search-nav-item:hover {
  background: rgba(255, 255, 255, 0.3);
}

.nav-item-icon {
  font-size: 18px;
}

.nav-item-text {
  font-size: 14px;
  font-weight: 500;
}

.search-hot-words {
  max-width: 1200px;
  margin: 8px auto 0;
  padding: 0 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
}

.hot-label {
  color: rgba(255, 255, 255, 0.8);
}

.hot-word {
  color: white;
  cursor: pointer;
  transition: all 0.3s ease;
  padding: 2px 8px;
  border-radius: 12px;
}

.hot-word:hover {
  background: rgba(255, 255, 255, 0.2);
}
</style>
