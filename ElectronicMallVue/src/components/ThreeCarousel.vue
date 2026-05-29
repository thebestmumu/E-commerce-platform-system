<template>
  <div class="modern-carousel">
    <div class="carousel-container">
      <div 
        class="carousel-slides" 
        :style="{ transform: `translateX(-${currentIndex * 100}%)` }"
      >
        <div 
          v-for="(product, index) in displayProducts" 
          :key="product.id || index"
          class="carousel-slide"
        >
          <div class="slide-image-wrapper">
            <img 
              :src="baseApi + product.imgs" 
              :alt="product.name"
              class="slide-image"
              @error="handleImageError"
            />
            <div class="slide-gradient"></div>
          </div>
          <div class="slide-content">
            <h3 class="slide-title">{{ product.name }}</h3>
            <div class="slide-price-row">
              <span class="slide-price">¥{{ product.price }}</span>
              <span class="slide-sales">{{ product.sales || 0 }}人付款</span>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 导航按钮 -->
    <button class="nav-btn nav-prev" @click="prevSlide">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
        <path d="M15 18l-6-6 6-6"/>
      </svg>
    </button>
    <button class="nav-btn nav-next" @click="nextSlide">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
        <path d="M9 18l6-6-6-6"/>
      </svg>
    </button>
    
    <!-- 指示器 -->
    <div class="carousel-dots">
      <span 
        v-for="(item, index) in displayProducts" 
        :key="index"
        :class="['dot', { active: index === currentIndex }]"
        @click="currentIndex = index"
      ></span>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ModernCarousel',
  props: {
    products: {
      type: Array,
      default: () => []
    },
    baseApi: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      currentIndex: 0,
      autoplayTimer: null
    };
  },
  computed: {
    displayProducts() {
      if (this.products.length === 0) return [];
      return this.products.slice(0, 10);
    }
  },
  watch: {
    products: {
      handler() {
        if (this.products.length > 0) {
          this.startAutoplay();
        }
      },
      immediate: true
    }
  },
  mounted() {
    if (this.products.length > 0) {
      this.startAutoplay();
    }
  },
  beforeDestroy() {
    this.stopAutoplay();
  },
  methods: {
    handleImageError(event) {
      event.target.style.display = 'none';
    },
    
    goToSlide(index) {
      this.currentIndex = index;
      this.$emit('change', index);
      this.resetAutoplay();
    },
    
    prevSlide() {
      if (this.displayProducts.length === 0) return;
      this.currentIndex = (this.currentIndex - 1 + this.displayProducts.length) % this.displayProducts.length;
      this.$emit('change', this.currentIndex);
      this.resetAutoplay();
    },
    
    nextSlide() {
      if (this.displayProducts.length === 0) return;
      this.currentIndex = (this.currentIndex + 1) % this.displayProducts.length;
      this.$emit('change', this.currentIndex);
      this.resetAutoplay();
    },
    
    startAutoplay() {
      this.stopAutoplay();
      this.autoplayTimer = setInterval(() => {
        this.nextSlide();
      }, 5000);
    },
    
    stopAutoplay() {
      if (this.autoplayTimer) {
        clearInterval(this.autoplayTimer);
        this.autoplayTimer = null;
      }
    },
    
    resetAutoplay() {
      this.stopAutoplay();
      this.startAutoplay();
    }
  }
};
</script>

<style scoped>
.modern-carousel {
  position: relative;
  width: 100%;
  height: 420px;
  border-radius: 12px;
  overflow: hidden;
  background: #1a1a2e;
}

.carousel-container {
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.carousel-slides {
  display: flex;
  height: 100%;
  transition: transform 0.5s ease;
}

.carousel-slide {
  flex: 0 0 100%;
  height: 100%;
  position: relative;
}

.slide-image-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
}

.slide-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.slide-gradient {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 60%;
  background: linear-gradient(to top, rgba(0,0,0,0.7), transparent);
  pointer-events: none;
}

.slide-content {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 30px 40px;
  color: white;
}

.slide-title {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 12px 0;
  text-shadow: 0 2px 4px rgba(0,0,0,0.3);
}

.slide-price-row {
  display: flex;
  align-items: baseline;
  gap: 16px;
}

.slide-price {
  font-size: 32px;
  font-weight: 700;
  color: #ff6b35;
}

.slide-sales {
  font-size: 14px;
  color: rgba(255,255,255,0.8);
}

/* 导航按钮 */
.nav-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: rgba(255,255,255,0.9);
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #333;
  transition: all 0.3s ease;
  z-index: 10;
  box-shadow: 0 2px 8px rgba(0,0,0,0.2);
}

.nav-btn:hover {
  background: white;
  box-shadow: 0 4px 12px rgba(0,0,0,0.3);
  transform: translateY(-50%) scale(1.1);
}

.nav-btn svg {
  width: 20px;
  height: 20px;
}

.nav-prev {
  left: 20px;
}

.nav-next {
  right: 20px;
}

/* 指示器 */
.carousel-dots {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 8px;
  z-index: 10;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(255,255,255,0.5);
  cursor: pointer;
  transition: all 0.3s ease;
}

.dot.active {
  background: white;
  width: 24px;
  border-radius: 4px;
}

/* 响应式 */
@media (max-width: 768px) {
  .modern-carousel {
    height: 300px;
  }
  
  .slide-title {
    font-size: 18px;
  }
  
  .slide-price {
    font-size: 24px;
  }
  
  .slide-content {
    padding: 20px;
  }
}
</style>
