<template>
  <div class="service-layout">
    <router-view />
  </div>
</template>

<script>
export default {
  name: 'Service',
  created() {
    // 路由守卫已经验证了客服权限，这里只需要检查是否已登录
    const serviceUserStr = localStorage.getItem('serviceUser')
    if (!serviceUserStr) {
      this.$message.warning('请先登录客服账号')
      this.$router.push('/service-login')
    }
  }
}
</script>

<style scoped>
.service-layout {
  height: 100%;
  width: 100%;
  overflow: hidden;
}
</style>
