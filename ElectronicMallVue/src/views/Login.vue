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
          <h1 class="login-title">欢迎回来</h1>
          <p class="login-subtitle">登录在线商城，继续您的购物之旅</p>
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

            <div class="register-link">
              <span>还没有账号？</span>
              <a @click="$router.push('/register')" class="link-text">立即注册</a>
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
  name: "Login",
  data() {
    return {
      to: "/",
      user: {},
      loading: false,
    };
  },
  created() {
    this.to = this.$route.query.to ? this.$route.query.to : "/";
  },
  methods: {
    onSubmit() {
      if (this.user.username === "" || this.user.password === "") {
        this.$message.error("账号或密码不能为空");
        return false;
      }
      
      this.loading = true;
      let form = {};
      Object.assign(form, this.user);
      form.password = md5(this.user.password);
      
      this.request
        .post("/login", form)
        .then((res) => {
          if (res.code === "200") {
            this.$message.success({
              message: "登录成功，欢迎回来！",
              showClose: true,
            });
            localStorage.setItem("user", JSON.stringify(res.data));
            setTimeout(() => {
              if (res.data.role === 'admin') {
                this.$router.push('/manage/home');
              } else {
                this.$router.push(this.to);
              }
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

/* 登录容器 */
.login-container {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  padding: 20px;
}

/* 登录卡片 */
.login-box {
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
.login-header {
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

.login-title {
  font-size: 28px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 8px 0;
  background: linear-gradient(135deg, #ff9000 0%, #ff5000 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.login-subtitle {
  font-size: 14px;
  color: #64748b;
  margin: 0;
}

/* 表单 */
.login-form {
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
  border-color: #ff5000 !important;
  box-shadow: 0 0 0 4px rgba(255, 80, 0, 0.1) !important;
}

.submit-btn {
  margin-top: 32px;
  margin-bottom: 0;
}

.login-btn {
  width: 100%;
  height: 52px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px !important;
  background: linear-gradient(135deg, #ff9000 0%, #ff5000 100%) !important;
  border: none !important;
  transition: all 0.3s ease !important;
}

.login-btn:hover {
  transform: translateY(-2px) !important;
  box-shadow: 0 10px 25px rgba(255, 80, 0, 0.4) !important;
}

.login-btn:active {
  transform: translateY(0) !important;
}

/* 注册链接 */
.register-link {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: #64748b;
}

.link-text {
  color: #ff5000;
  font-weight: 600;
  cursor: pointer;
  margin-left: 8px;
  transition: color 0.3s ease;
}

.link-text:hover {
  color: #ff9000;
  text-decoration: underline;
}

/* 响应式 */
@media (max-width: 480px) {
  .login-box {
    padding: 32px 24px;
    border-radius: 20px;
  }

  .login-title {
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
