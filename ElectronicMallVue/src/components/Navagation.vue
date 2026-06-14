<!--
 * @Description: 
 * @Author: Rabbiter
 * @Date: 2023-03-26 15:27:05
-->
<template>
  <div class="navagation">
    <el-row>
      <el-col :span="3">
        <div style="font-size: 20px; font-weight: bold; text-align: center">
          <a href="/"><i class="iconfont icon-r-home" style="font-size: 24px;"></i> 在线商城</a>
        </div>
      </el-col>
      <el-col :span="17">
        <el-menu
          :default-active="activeIndex"
          class="el-menu-demo"
          mode="horizontal"
          router
        >
          <el-menu-item index="/" class="menu-item">商城首页</el-menu-item>
          
          <el-menu-item index="/goodList" class="menu-item"
            >商品分类</el-menu-item
          >
          <el-menu-item index="/cart" class="menu-item"
            >我的购物车</el-menu-item
          >
          <el-menu-item index="/orderlist" class="menu-item"
            >我的订单</el-menu-item
          >
          <el-menu-item index="/myTickets" class="menu-item"
            >我的工单</el-menu-item
          >
          <el-menu-item
            index="/manage"
            class="menu-item"
            v-if="role === 'admin'"
            >后台管理</el-menu-item
          >
        </el-menu>
      </el-col>
      <el-col :span="4">
        <!--         右上角个人信息-->
        <div style="display: flex; align-items: center; justify-content: flex-end; height: 60px; margin-right: 20px; gap: 8px; flex-wrap: nowrap;">
          <!-- 在线客服按钮 -->
          <div class="customer-service-btn" @click="openCustomerService">
            <i class="el-icon-service"></i>
            <span>在线客服</span>
          </div>
          <!-- 智能客服按钮 -->
          <div class="customer-service-btn" @click="openAIAssistant">
            <i class="el-icon-chat-dot-round"></i>
            <span>智能客服</span>
          </div>
          <!-- 用户下拉菜单 -->
          <el-dropdown style="cursor: pointer;">
            <span class="el-dropdown-link">
              <div style="display: inline-block">
                <img
                  v-if="user.avatarUrl != null"
                  :src="baseApi + user.avatarUrl"
                  class="avatar"
                />
                {{ user.nickname }}
                <i
                  class="el-icon-arrow-down el-icon--right"
                  style="margin-right: 5px"
                ></i>
              </div>
            </span>
            <!--          下拉菜单-->
            <el-dropdown-menu slot="dropdown" style="text-align: center">
              <el-dropdown-item>
                <!--              传给前端，登录后跳转页面的path为 "/"-->
                <div
                  @click="$router.push({ path: '/login', query: { to: '/' } })"
                  v-show="!loginStatus"
                >
                  登录
                </div>
              </el-dropdown-item>
              <el-dropdown-item v-show="loginStatus">
                <div @click="$router.push('/person')">个人信息</div>
              </el-dropdown-item>
              <el-dropdown-item v-show="loginStatus">
                <div @click="logout">退出</div>
              </el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>
      </el-col>
    </el-row>
  </div>
</template>


<script>
import eventBus from '../utils/eventBus'

export default {
  name: "Navagation",
  props: {
    user: Object,
    loginStatus: Boolean,
    role: String,
  },
  data() {
    return {
      activeIndex: this.$route.path,
      activeIndex2: "1",
      baseApi: this.$store.state.baseApi,
    };
  },
  watch: {
    $route(to) {
      this.activeIndex = to.path;
    }
  },
  methods: {
    logout() {
      localStorage.removeItem("user");
      this.$router.go(0);
      this.$message.success("退出成功");
    },
    openCustomerService() {
      // 打开人工客服聊天页面
      if (this.loginStatus) {
        this.$router.push('/user-chat')
      } else {
        this.$message.warning('请先登录')
        this.$router.push({ path: '/login', query: { to: '/user-chat' } })
      }
    },
    openAIAssistant() {
      // 打开智能客服
      eventBus.$emit('open-customer-service')
    }
  },
};
</script>
<style>
a {
  text-decoration: none;
}
.navagation {
  width: 100%;
  height: 60px;
  line-height: 60px;
  background-color: white;
  overflow: hidden;
}
.avatar {
  width: 45px;
  border-radius: 5px;
  position: relative;
  top: 10px;
  right: 5px;
}
.menu-item {
  padding-left: 50px;
  padding-right: 50px;
}
.customer-service-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border-radius: 20px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
  color: #ff5000;
  background: linear-gradient(135deg, #fff5f0, #fff0e6);
  border: 1px solid #ffd6c0;
  transition: all 0.3s ease;
  white-space: nowrap;
  flex-shrink: 0;
}
.customer-service-btn:hover {
  color: white;
  background: linear-gradient(135deg, #ff5000, #ff6a00);
  border-color: #ff5000;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 80, 0, 0.3);
}
.customer-service-btn i {
  font-size: 14px;
}
.customer-service-btn span {
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>