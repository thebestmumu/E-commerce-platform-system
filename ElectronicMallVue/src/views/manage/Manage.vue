<template>
  <div class="taobao-admin-layout">
    <el-container class="admin-container">
      <el-aside :width="sideWidth + 'px'" class="admin-aside">
        <Aside :is-collapse="isCollapse"></Aside>
      </el-aside>

      <el-container class="admin-main">
        <el-header class="admin-header">
          <Header
            :collapse-icon="collapseIcon"
            :collapse-title="collapseTitle"
            @collapse="handleCollapse"
            :user="user"
          ></Header>
        </el-header>

        <el-main class="admin-content">
          <router-view @refresh="getUser" />
        </el-main>
      </el-container>
    </el-container>

    <AiChat v-if="isAdmin" />
  </div>
</template>

<style scoped>
@import '@/assets/taobao-style.css';

.taobao-admin-layout {
  height: 100vh;
  overflow: hidden;
  background: var(--taobao-bg);
}

.admin-container {
  height: 100%;
}

.admin-aside {
  transition: width 0.3s ease;
  overflow: hidden;
  background: white;
}

.admin-main {
  display: flex;
  flex-direction: column;
  background: var(--taobao-bg);
}

.admin-header {
  background: white;
  height: 60px;
  padding: 0;
  border-bottom: 1px solid var(--taobao-border);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.admin-content {
  background: var(--taobao-bg);
  padding: 20px;
  overflow-y: auto;
}

.admin-content::-webkit-scrollbar {
  width: 6px;
}

.admin-content::-webkit-scrollbar-track {
  background: #f5f5f5;
  border-radius: 3px;
}

.admin-content::-webkit-scrollbar-thumb {
  background: linear-gradient(135deg, #ff9000, #ff5000);
  border-radius: 3px;
}

.admin-content::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(135deg, #ff5000, #ff3000);
}
</style>

<script>
import Aside from "@/components/Aside";
import Header from "@/components/Header";
import AiChat from "@/components/ai/AiChat";

export default {
  data() {
    return {
      user: {},
      isCollapse: false,
      sideWidth: 250,
      collapseIcon: "el-icon-s-fold",
      collapseTitle: "收缩",
      isAdmin: false,
    };
  },

  components: {
    Aside,
    Header,
    AiChat,
  },
  methods: {
    handleCollapse() {
      this.isCollapse = !this.isCollapse;
      if (this.isCollapse) {
        this.sideWidth = 64;
        this.collapseIcon = "el-icon-s-unfold";
        this.collapseTitle = "展开";
      } else {
        this.sideWidth = 250;
        this.collapseIcon = "el-icon-s-fold";
        this.collapseTitle = "收缩";
      }
    },
    getUser() {
      let username = localStorage.getItem("user")
        ? JSON.parse(localStorage.getItem("user")).username
        : "";
      if (username) {
        this.request.get("/userinfo/" + username).then((res) => {
          this.user = res.data;
          console.log(this.user.role);
          this.checkAdminRole();
        });
      }
    },
    checkAdminRole() {
      this.request.post("/role").then((res) => {
        if (res.code === "200") {
          this.isAdmin = res.data === "admin";
          console.log("管理员身份验证:", this.isAdmin);
        }
      });
    },
  },
  created() {
    this.getUser();
  },
};
</script>
