<template>
  <div id="app">
    <router-view/>
    <ai-chat/>
    <back-to-top/>
    <bottom-nav/>
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
  }
}
</script>

<style>
@import "./resource/css/icon.css";

#app {
  min-height: 100vh;
  background: #f4f4f4;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif;
}

body {
  margin: 0;
  padding: 0;
  background: #f4f4f4;
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


