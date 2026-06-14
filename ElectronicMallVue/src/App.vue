<template>
  <div id="app" :class="{ 'chat-page': isChatPage }">
    <router-view/>
    <ai-chat v-if="!isChatPage"/>
    <back-to-top v-if="!isChatPage"/>
    <bottom-nav v-if="!isChatPage"/>
  </div>
</template>

<script>
import AiChat from './components/ai/AiChat.vue'
import BackToTop from './components/BackToTop.vue'
import BottomNav from './components/BottomNav.vue'

export default {
  name: 'App',
  components: {
    AiChat,
    BackToTop,
    BottomNav
  },
  computed: {
    isChatPage() {
      const path = this.$route.path
      return path === '/user-chat' || path === '/service/chat' || path === '/service'
    }
  },
  watch: {
    isChatPage: {
      immediate: true,
      handler(val) {
        if (val) {
          document.body.style.overflow = 'hidden'
        } else {
          document.body.style.overflow = ''
        }
      }
    }
  }
}
</script>

<style>
@import "./resource/css/icon.css";

html, body {
  height: 100%;
  margin: 0;
  padding: 0;
}

#app {
  min-height: 100vh;
  background: #f4f4f4;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif;
}

/* 聊天页面 - 使用 BFC 防止父容器塌陷 */
#app.chat-page {
  height: 100vh;
  max-height: 100vh;
  overflow: hidden;
  background: #f5f5f5;
}

/* router-view 需要设置高度，子元素的 height: 100% 才能生效 */
#app.chat-page > div {
  height: 100%;
}

* {
  box-sizing: border-box;
}

a {
  text-decoration: none;
  color: inherit;
}

button {
  font-family: inherit;
}

::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb {
  background: linear-gradient(90deg, #ff9000 0%, #ff5000 100%);
  border-radius: 4px;
  transition: all 0.3s ease;
}

::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(90deg, #ff5000 0%, #ff9000 100%);
}
</style>


