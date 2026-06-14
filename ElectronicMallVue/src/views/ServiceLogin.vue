<template>
  <div id="bk" class="wrapper">
    <!-- 装饰性背景元素 -->
    <div class="bg-decoration">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
      <div class="circle circle-3"></div>
    </div>

    <div class="login-container">
      <div class="login-box fade-in">
        <!-- Logo 和标题 -->
        <div class="login-header">
          <div class="logo-wrapper">
            <div class="logo-icon">
              <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M3 9L12 2L21 9V20C21 20.5304 20.7893 21.0391 20.4142 21.4142C20.0391 21.7893 19.5304 22 19 22H5C4.46957 22 3.96086 21.7893 3.58579 21.4142C3.21071 21.0391 3 20.5304 3 20V9Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M9 22V12H15V22" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
          </div>
          <h1 class="login-title">客服登录</h1>
          <p class="login-subtitle">登录客服系统，处理用户工单</p>
        </div>

        <!-- 登录表单 -->
        <div class="login-form">
          <el-form @submit.native.prevent="onSubmit">
            <el-form-item>
              <div class="input-wrapper">
                <span class="input-icon">
                  <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M20 21V19C20 17.8954 19.1046 17 18 17H6C4.89543 17 4 17.8954 4 19V21" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                    <circle cx="12" cy="7" r="4" stroke="currentColor" stroke-width="2"/>
                  </svg>
                </span>
                <el-input
                  v-model.trim="user.username"
                  placeholder="请输入客服账号"
                  size="large"
                ></el-input>
              </div>
            </el-form-item>

            <el-form-item>
              <div class="input-wrapper">
                <span class="input-icon">
                  <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <rect x="3" y="11" width="18" height="11" rx="2" stroke="currentColor" stroke-width="2"/>
                    <path d="M7 11V7C7 4.23858 9.23858 2 12 2C14.7614 2 17 4.23858 17 7V11" stroke="currentColor" stroke-width="2"/>
                    <circle cx="12" cy="16" r="1" fill="currentColor"/>
                  </svg>
                </span>
                <el-input
                  v-model.trim="user.password"
                  placeholder="请输入密码"
                  show-password
                  size="large"
                  @keyup.enter.native="onSubmit"
                ></el-input>
              </div>
            </el-form-item>

            <el-form-item class="submit-btn">
              <el-button
                type="primary"
                size="large"
                @click="onSubmit"
                class="login-btn"
                :loading="loading"
              >
                <span v-if="!loading">登 录</span>
              </el-button>
            </el-form-item>

            <div class="back-link">
              <a @click="$router.push('/login')" class="link-text">返回普通登录</a>
            </div>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: "ServiceLogin",
  data() {
    return {
      user: {},
      loading: false,
    };
  },
  methods: {
    onSubmit() {
      if (this.user.username === "" || this.user.password === "") {
        this.$message.error("账号或密码不能为空");
        return false;
      }
      
      this.loading = true;
      
      this.request
        .post("/api/service/login", this.user)
        .then((res) => {
          if (res.code === "200") {
            this.$message.success({
              message: "登录成功，欢迎客服！",
              showClose: true,
            });
            localStorage.setItem("serviceUser", JSON.stringify(res.data));
            setTimeout(() => {
              this.$router.push('/service/ticket');
            }, 300);
          } else {
            this.$message.error(res.msg);
          }
        })
        .catch((e) => {
          console.log(e);
          if (e.response == undefined || e.response.data == undefined) {
            this.$message({
              showClose: true,
              message: e,
              type: "error",
              duration: 5000,
            });
          } else {
            this.$message({
              showClose: true,
              message: e.response.data,
              type: "error",
              duration: 5000,
            });
          }
        })
        .finally(() => {
          this.loading = false;
        });
    },
  },
};
</script>

<style scoped>
.wrapper {
  height: 100vh;
  overflow: hidden;
  position: relative;
}

#bk {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #ff9000 0%, #ff5000 100%);
  overflow: hidden;
}

/* 背景装饰 */
.bg-decoration {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
  pointer-events: none;
}

.circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  animation: float 20s infinite ease-in-out;
}

.circle-1 {
  width: 200px;
  height: 200px;
  top: -50px;
  left: -50px;
  animation-delay: 0s;
}

.circle-2 {
  width: 300px;
  height: 300px;
  bottom: -100px;
  right: -100px;
  animation-delay: 5s;
}

.circle-3 {
  width: 150px;
  height: 150px;
  top: 50%;
  right: 10%;
  animation-delay: 10s;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-20px) rotate(180deg);
  }
}

.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  padding: 20px;
}

.login-box {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  padding: 40px;
  width: 100%;
  max-width: 420px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.logo-wrapper {
  display: flex;
  justify-content: center;
  margin-bottom: 20px;
}

.logo-icon {
  width: 64px;
  height: 64px;
  color: #ff5000;
}

.login-title {
  font-size: 28px;
  font-weight: 600;
  color: #ff5000;
  margin: 0 0 8px 0;
}

.login-subtitle {
  font-size: 14px;
  color: #666;
  margin: 0;
}

.login-form {
  margin-top: 30px;
}

.input-wrapper {
  display: flex;
  align-items: center;
  position: relative;
}

.input-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: #999;
  z-index: 1;
}

.input-icon svg {
  width: 20px;
  height: 20px;
}

:deep(.el-input__inner) {
  padding-left: 45px !important;
  height: 50px;
  border-radius: 10px;
  font-size: 15px;
}

.submit-btn {
  margin-top: 30px;
  margin-bottom: 20px;
}

.login-btn {
  width: 100%;
  height: 50px;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #ff9000, #ff5000);
  border: none;
  transition: all 0.3s ease;
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(255, 80, 0, 0.3);
}

.back-link {
  text-align: center;
  font-size: 14px;
  color: #666;
}

.link-text {
  color: #ff5000;
  cursor: pointer;
  text-decoration: none;
  font-weight: 500;
}

.link-text:hover {
  text-decoration: underline;
}

.fade-in {
  animation: fadeIn 0.5s ease-in;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
