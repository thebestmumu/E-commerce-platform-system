<template>
  <div id="bk" class="wrapper">
    <!-- 装饰性背景元素 -->
    <div class="bg-decoration">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
      <div class="circle circle-3"></div>
    </div>

    <div class="register-container">
      <div class="register-box fade-in">
        <!-- Logo 和标题 -->
        <div class="register-header">
          <div class="logo-wrapper">
            <div class="logo-icon">
              <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M16 21V19C16 17.8954 15.1046 17 14 17H5C3.89543 17 3 17.8954 3 19V21" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                <circle cx="8.5" cy="7" r="4" stroke="currentColor" stroke-width="2"/>
                <path d="M20 8V14" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                <path d="M23 11H17" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              </svg>
            </div>
          </div>
          <h1 class="register-title">创建账号</h1>
          <p class="register-subtitle">注册在线商城，开启您的购物之旅</p>
        </div>

        <!-- 注册表单 -->
        <div class="register-form">
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
                  placeholder="请输入用户名"
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
                ></el-input>
              </div>
            </el-form-item>

            <el-form-item>
              <div class="input-wrapper">
                <span class="input-icon">
                  <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M12 22C17.5228 22 22 17.5228 22 12C22 6.47715 17.5228 2 12 2C6.47715 2 2 6.47715 2 12C2 17.5228 6.47715 22 12 22Z" stroke="currentColor" stroke-width="2"/>
                    <path d="M12 16C14.2091 16 16 14.2091 16 12C16 9.79086 14.2091 8 12 8C9.79086 8 8 9.79086 8 12C8 14.2091 9.79086 16 12 16Z" stroke="currentColor" stroke-width="2"/>
                    <path d="M2 12H22" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                  </svg>
                </span>
                <el-input
                  v-model.trim="user.confirmPassword"
                  placeholder="请再次输入密码"
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
                class="register-btn"
                :loading="loading"
              >
                <span v-if="!loading">注 册</span>
              </el-button>
            </el-form-item>

            <div class="login-link">
              <span>已有账号？</span>
              <a @click="$router.push('/login')" class="link-text">立即登录</a>
            </div>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import md5 from "js-md5";

export default {
  name: "Register",
  data() {
    return {
      user: {},
      loading: false,
    };
  },
  methods: {
    onSubmit() {
      if (
        this.user.username === "" ||
        this.user.password === "" ||
        this.user.confirmPassword === ""
      ) {
        this.$message.error("账号或密码不能为空");
        return false;
      }
      if (this.user.password !== this.user.confirmPassword) {
        this.$message.error("两次密码不一致");
        return false;
      }
      
      this.loading = true;
      const registerData = {
        username: this.user.username,
        password: md5(this.user.password),
      };
      
      this.request
        .post("/register", registerData)
        .then((res) => {
          if (res.code === "200") {
            this.$message.success({
              message: "注册成功，请登录！",
              showClose: true,
            });
            setTimeout(() => {
              this.$router.push("/login");
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
  width: 300px;
  height: 300px;
  top: -100px;
  left: -100px;
  animation-delay: 0s;
}

.circle-2 {
  width: 200px;
  height: 200px;
  bottom: -50px;
  right: 10%;
  animation-delay: 5s;
}

.circle-3 {
  width: 150px;
  height: 150px;
  top: 20%;
  right: -50px;
  animation-delay: 10s;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  33% {
    transform: translateY(-30px) rotate(120deg);
  }
  66% {
    transform: translateY(30px) rotate(240deg);
  }
}

/* 注册容器 */
.register-container {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  padding: 20px;
}

/* 注册卡片 */
.register-box {
  width: 100%;
  max-width: 420px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 24px;
  padding: 48px 40px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  border: 1px solid rgba(255, 255, 255, 0.3);
}

/* 头部 */
.register-header {
  text-align: center;
  margin-bottom: 40px;
}

.logo-wrapper {
  display: inline-flex;
  justify-content: center;
  align-items: center;
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #ff9000 0%, #ff5000 100%);
  border-radius: 20px;
  margin-bottom: 24px;
  box-shadow: 0 10px 25px rgba(255, 80, 0, 0.3);
}

.logo-icon {
  width: 40px;
  height: 40px;
  color: white;
}

.register-title {
  font-size: 28px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 8px 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.register-subtitle {
  font-size: 14px;
  color: #64748b;
  margin: 0;
}

/* 表单 */
.register-form {
  margin-top: 32px;
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 16px;
  width: 20px;
  height: 20px;
  color: #94a3b8;
  pointer-events: none;
  z-index: 1;
}

.input-wrapper ::v-deep .el-input__inner {
  padding-left: 48px !important;
  height: 52px !important;
  border-radius: 12px !important;
  border: 2px solid #e2e8f0 !important;
  font-size: 15px !important;
  transition: all 0.3s ease !important;
}

.input-wrapper ::v-deep .el-input__inner:focus {
  border-color: #667eea !important;
  box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.1) !important;
}

.submit-btn {
  margin-top: 32px;
  margin-bottom: 0;
}

.register-btn {
  width: 100%;
  height: 52px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px !important;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%) !important;
  border: none !important;
  transition: all 0.3s ease !important;
}

.register-btn:hover {
  transform: translateY(-2px) !important;
  box-shadow: 0 10px 25px rgba(102, 126, 234, 0.4) !important;
}

.register-btn:active {
  transform: translateY(0) !important;
}

/* 登录链接 */
.login-link {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: #64748b;
}

.link-text {
  color: #667eea;
  font-weight: 600;
  cursor: pointer;
  margin-left: 8px;
  transition: color 0.3s ease;
}

.link-text:hover {
  color: #764ba2;
  text-decoration: underline;
}

/* 响应式 */
@media (max-width: 480px) {
  .register-box {
    padding: 32px 24px;
    border-radius: 20px;
  }

  .register-title {
    font-size: 24px;
  }

  .logo-wrapper {
    width: 64px;
    height: 64px;
  }

  .logo-icon {
    width: 32px;
    height: 32px;
  }
}
</style>
