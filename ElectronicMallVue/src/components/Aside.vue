<template>
  <el-menu 
    :default-openeds="['2', 'good']" 
    class="taobao-admin-menu"
    :collapse-transition="false"
    :collapse="isCollapse"
    router
  >
    <div class="menu-logo">
      <router-link to="/manage/home" class="logo-link">
        <img src="../resource/logo.png" class="logo-img">
        <span class="logo-text" v-show="!isCollapse">商城后台</span>
      </router-link>
    </div>

    <el-menu-item index="/manage/home" class="menu-item home-item">
      <i class="iconfont icon-r-home menu-icon"></i>
      <span slot="title">工作台</span>
    </el-menu-item>

    <el-submenu index="2" class="menu-submenu">
      <template slot="title">
        <i class="iconfont icon-r-setting menu-icon"></i>
        <span slot="title">系统管理</span>
      </template>
      <el-submenu v-show="userGroup" index="user" class="submenu-item">
        <template slot="title">
          <i class="iconfont icon-r-user2 submenu-icon"></i>
          用户管理
        </template>
        <el-menu-item index="/manage/user" v-if="menuFlags.userMenu" class="submenu-child-item">
          用户管理
        </el-menu-item>
      </el-submenu>
      <el-submenu v-if="fileGroup" index="file" class="submenu-item">
        <template slot="title">
          <i class="iconfont icon-r-paper submenu-icon"></i>
          文件管理
        </template>
        <el-menu-item index="/manage/file" v-if="menuFlags.fileMenu" class="submenu-child-item">
          文件管理
        </el-menu-item>
        <el-menu-item index="/manage/avatar" v-if="menuFlags.avatarMenu" class="submenu-child-item">
          头像管理
        </el-menu-item>
      </el-submenu>
      <el-submenu v-if="GoodGroup" index="good" class="submenu-item">
        <template slot="title">
          <i class="iconfont icon-r-find submenu-icon"></i>
          商品管理
        </template>
        <el-menu-item index="/manage/category" v-if="menuFlags.categoryMenu" class="submenu-child-item">
          商品分类管理
        </el-menu-item>
        <el-menu-item index="/manage/carousel" v-if="menuFlags.carouselMenu" class="submenu-child-item">
          轮播图管理
        </el-menu-item>
        <el-menu-item index="/manage/good" v-if="menuFlags.goodMenu" class="submenu-child-item">
          商品管理
        </el-menu-item>
        <el-menu-item index="/manage/order" v-if="menuFlags.orderMenu" class="submenu-child-item">
          订单管理
        </el-menu-item>
      </el-submenu>
      <el-submenu v-if="incomeGroup" index="income" class="submenu-item">
        <template slot="title">
          <i class="iconfont icon-r-shield submenu-icon"></i>
          营收管理
        </template>
        <el-menu-item index="/manage/incomeChart" v-if="menuFlags.incomeChartMenu" class="submenu-child-item">
          图表分析
        </el-menu-item>
        <el-menu-item index="/manage/incomeRank" v-if="menuFlags.incomeRankMenu" class="submenu-child-item">
          收入排行榜
        </el-menu-item>
      </el-submenu>
    </el-submenu>
  </el-menu>
</template>

<script>
import request from "@/utils/request";

export default {
  name: "Aside",
  props: {
    isCollapse: Boolean,
  },
  data() {
    return {
      role: 'user',
      menuFlags: {
        userMenu: false,
        fileMenu: false,
        avatarMenu: false,
        goodMenu: false,
        carouselMenu: false,
        orderMenu: false,
        categoryMenu: false,
        incomeChartMenu: false,
        incomeRankMenu: false,
      }
    }
  },
  computed: {
    userGroup: function() {
      return this.menuFlags.userMenu
    },
    fileGroup: function() {
      return this.menuFlags.fileMenu || this.menuFlags.avatarMenu
    },
    GoodGroup: function() {
      return this.menuFlags.goodMenu || this.menuFlags.orderMenu || this.menuFlags.categoryMenu || this.menuFlags.carouselMenu
    },
    incomeGroup: function() {
      return this.menuFlags.incomeChartMenu || this.menuFlags.incomeRankMenu
    }
  },
  created() {
    request.post("http://localhost:9191/role").then(res => {
      if (res.code === '200') {
        this.role = res.data;
        console.log("asider，role：" + this.role)
        if (this.role === 'admin') {
          this.menuFlags.userMenu = true
          this.menuFlags.fileMenu = true
          this.menuFlags.avatarMenu = true
          this.menuFlags.categoryMenu = true
          this.menuFlags.goodMenu = true
          this.menuFlags.carouselMenu = true
          this.menuFlags.orderMenu = true
          this.menuFlags.incomeChartMenu = true
          this.menuFlags.incomeRankMenu = true
        }
        console.log(this.menuFlags)
      }
    })
  }
}
</script>

<style scoped>
.taobao-admin-menu {
  height: 100%;
  background: linear-gradient(180deg, #ffffff 0%, #fafafa 100%);
  border-right: 1px solid #f0f0f0;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.04);
}

.menu-logo {
  height: 60px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  border-bottom: 1px solid #f0f0f0;
  background: linear-gradient(90deg, #ff9000 0%, #ff5000 100%);
}

.logo-link {
  display: flex;
  align-items: center;
  text-decoration: none;
  gap: 10px;
}

.logo-img {
  width: 32px;
  height: 32px;
  filter: brightness(0) invert(1);
}

.logo-text {
  color: white;
  font-size: 18px;
  font-weight: 600;
  white-space: nowrap;
}

.menu-item {
  margin: 8px 12px;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.menu-item:hover {
  background: #fff5f0 !important;
}

.home-item {
  background: linear-gradient(90deg, rgba(255, 144, 0, 0.1) 0%, rgba(255, 80, 0, 0.05) 100%);
  color: #ff5000 !important;
}

.front-item {
  color: #666 !important;
}

.menu-icon {
  font-size: 20px;
  color: #ff5000;
  margin-right: 8px;
}

.menu-submenu {
  margin: 8px 12px;
}

.submenu-item {
  background: transparent;
}

.submenu-icon {
  font-size: 18px;
  color: #666;
}

.submenu-child-item {
  padding-left: 48px !important;
  color: #666;
  transition: all 0.3s ease;
}

.submenu-child-item:hover {
  background: #fff5f0 !important;
  color: #ff5000 !important;
}

.el-menu-item.is-active {
  background: linear-gradient(90deg, rgba(255, 144, 0, 0.15) 0%, rgba(255, 80, 0, 0.08) 100%) !important;
  color: #ff5000 !important;
  border-right: 3px solid #ff5000;
}
</style>
