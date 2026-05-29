<template>
  <transition name="fade">
    <div v-if="visible" class="back-to-top" @click="scrollToTop">
      <div class="back-to-top-icon">↑</div>
      <div class="back-to-top-text">顶部</div>
    </div>
  </transition>
</template>

<script>
export default {
  name: 'BackToTop',
  data() {
    return {
      visible: false,
      scrollThreshold: 300
    }
  },
  mounted() {
    window.addEventListener('scroll', this.handleScroll)
  },
  beforeDestroy() {
    window.removeEventListener('scroll', this.handleScroll)
  },
  methods: {
    handleScroll() {
      this.visible = window.pageYOffset > this.scrollThreshold
    },
    scrollToTop() {
      window.scrollTo({
        top: 0,
        behavior: 'smooth'
      })
    }
  }
}
</script>

<style scoped>
.back-to-top {
  position: fixed;
  right: 20px;
  bottom: 80px;
  width: 50px;
  height: 60px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  transition: all 0.3s;
  z-index: 999;
}

.back-to-top:hover {
  background: linear-gradient(90deg, #ff9000 0%, #ff5000 100%);
  transform: translateY(-4px);
  box-shadow: 0 4px 16px rgba(255, 80, 0, 0.3);
}

.back-to-top-icon {
  font-size: 24px;
  font-weight: 700;
  color: #ff5000;
  line-height: 1;
}

.back-to-top:hover .back-to-top-icon {
  color: white;
}

.back-to-top-text {
  font-size: 12px;
  color: #666;
}

.back-to-top:hover .back-to-top-text {
  color: white;
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter, .fade-leave-to {
  opacity: 0;
}
</style>
