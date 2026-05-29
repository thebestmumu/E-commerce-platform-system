<template>
  <div class="taobao-admin-header">
    <div class="header-left">
      <span class="collapse-btn" :class="collapseIcon" @click="$emit('collapse')" :title="collapseTitle"></span>
      <span class="back-btn iconfont icon-r-left" @click="back" title="返回"></span>
      <el-breadcrumb class="header-breadcrumb">
        <el-breadcrumb-item>{{routePath}}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>
    
    <div class="header-right">
      <el-dropdown class="user-dropdown">
        <span class="el-dropdown-link">
          <div class="user-info">
            <img :src="baseApi + user.avatarUrl" class="user-avatar">
            <span class="user-nickname">{{user.nickname }}</span>
            <i class="el-icon-arrow-down"></i>
          </div>
        </span>
        <el-dropdown-menu slot="dropdown" class="user-dropdown-menu">
          <el-dropdown-item @click.native="$router.push('/manage/person')">
            <span class="dropdown-icon">👤</span>
            <span>个人信息</span>
          </el-dropdown-item>
          <el-dropdown-item @click.native="logout" class="logout-item">
            <span class="dropdown-icon">🚪</span>
            <span>退出登录</span>
          </el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
    </div>
  </div>
</template>

<script>
export default {
  name: "Header",
  props: {
    collapseIcon: String,
    collapseTitle: String,
    user: Object
  },
  methods: {
    logout() {
      localStorage.removeItem("user");
      this.$router.push('/login');
      this.$message.success("退出成功");
    },
    back() {
      this.$router.go(-1)
    }
  },
  data() {
    return {
      routePath: '',
      baseApi: this.$store.state.baseApi,
    }
  },
  watch: {
    '$route': function() {
      this.routePath = this.$route.meta.path
    },
  },
  created() {
    this.routePath = this.$route.meta.path;
  }
}
</script>

<style scoped>
.taobao-admin-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 60px;
  padding: 0 24px;
  background: white;
  border-bottom: 1px solid #f0f0f0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  color: #666;
  transition: color 0.3s;
}

.collapse-btn:hover {
  color: #ff5000;
}

.back-btn {
  font-size: 20px;
  cursor: pointer;
  color: #666;
  transition: color 0.3s;
}

.back-btn:hover {
  color: #ff5000;
}

.header-breadcrumb {
  font-size: 14px;
}

.header-breadcrumb ::v-deep .el-breadcrumb__item:last-child .el-breadcrumb__inner {
  color: #ff5000;
  font-weight: 500;
}

.header-breadcrumb ::v-deep .el-breadcrumb__inner {
  color: #666;
}

.header-breadcrumb ::v-deep .el-breadcrumb__separator {
  color: #ccc;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-dropdown {
  cursor: pointer;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #333;
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 2px solid #fff5f0;
  box-shadow: 0 2px 8px rgba(255, 80, 0, 0.15);
}

.user-nickname {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.user-dropdown-menu ::v-deep .el-dropdown-menu__item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
}

.dropdown-icon {
  font-size: 16px;
}

.logout-item {
  color: #ff5000;
}

.logout-item:hover {
  background: #fff5f0;
}
</style>
