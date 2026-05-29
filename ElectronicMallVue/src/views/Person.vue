<template>
  <div class="person-container">
    <div class="person-header">
      <div class="user-profile">
        <div class="avatar-wrapper">
          <el-upload
            class="avatar-uploader"
            :action="baseApi + '/avatar'"
            :headers="token"
            :show-file-list="false"
            :on-success="handleAvatarSuccess"
          >
            <img
              v-if="form.avatarUrl"
              :src="baseApi + form.avatarUrl"
              class="avatar"
            />
            <div v-else class="avatar-placeholder">
              <span class="placeholder-icon">+</span>
              <span class="placeholder-text">上传头像</span>
            </div>
          </el-upload>
        </div>
        <div class="user-info">
          <h2 class="user-name">{{ form.nickname || '未设置昵称' }}</h2>
          <p class="user-desc">完善个人信息，享受更好的购物体验</p>
        </div>
      </div>
    </div>

    <div class="person-content">
      <div class="info-card">
        <div class="card-header">
          <span class="card-icon">👤</span>
          <span class="card-title">个人信息</span>
        </div>
        
        <el-form label-position="top" class="info-form">
          <el-form-item label="昵称">
            <el-input v-model="form.nickname" placeholder="请输入昵称">
              <i slot="prefix" class="input-icon">😊</i>
            </el-input>
          </el-form-item>
          
          <el-form-item label="联系电话">
            <el-input v-model="form.phone" placeholder="请输入联系电话">
              <i slot="prefix" class="input-icon">📱</i>
            </el-input>
          </el-form-item>
          
          <el-form-item label="电子邮箱">
            <el-input v-model="form.email" placeholder="请输入电子邮箱">
              <i slot="prefix" class="input-icon">📧</i>
            </el-input>
          </el-form-item>
          
          <el-form-item label="收货地址">
            <el-input v-model="form.address" placeholder="请输入收货地址">
              <i slot="prefix" class="input-icon">📍</i>
            </el-input>
          </el-form-item>
          
          <div class="form-actions">
            <button class="save-btn" @click="save">
              <span class="btn-icon">✓</span>
              保存修改
            </button>
          </div>
        </el-form>
      </div>

      <div class="security-card">
        <div class="card-header">
          <span class="card-icon">🔒</span>
          <span class="card-title">安全设置</span>
        </div>
        
        <div class="security-content">
          <div class="password-section">
            <div class="password-header">
              <span class="password-label">登录密码</span>
              <button class="change-password-btn" @click="showPasswordDialog = true">
                修改密码
              </button>
            </div>
            <p class="password-hint">定期修改密码可以保护您的账户安全</p>
          </div>
        </div>
      </div>
    </div>

    <el-dialog 
      title="修改密码" 
      :visible.sync="showPasswordDialog" 
      width="450px"
      class="password-dialog"
    >
      <el-form label-position="top">
        <el-form-item label="新密码">
          <el-input
            type="password"
            v-model="resetPsw.newPassword"
            placeholder="请输入新密码"
            show-password
          ></el-input>
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input
            type="password"
            v-model="resetPsw.confirmPassword"
            placeholder="请再次输入新密码"
            show-password
          ></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <button class="dialog-btn cancel-btn" @click="showPasswordDialog = false">取消</button>
        <button class="dialog-btn confirm-btn" @click="toResetPassword">确定</button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import md5 from "js-md5";

export default {
  name: "Person",
  data() {
    return {
      form: {},
      baseApi: this.$store.state.baseApi,
      user: localStorage.getItem("user")
        ? JSON.parse(localStorage.getItem("user"))
        : {},
      resetPsw: {
        newPassword: "",
        confirmPassword: "",
      },
      showPasswordDialog: false,
    };
  },
  methods: {
    toResetPassword() {
      if (this.resetPsw.newPassword.trim() == "") {
        this.$message.error("新密码不能为空");
        return;
      }
      if (this.resetPsw.confirmPassword != this.resetPsw.newPassword) {
        this.$message.error("两次密码不一致");
        return;
      }
      this.request
        .get(
          "/user/resetPassword?id=" +
            this.user.id +
            "&newPassword=" +
            md5(this.resetPsw.newPassword)
        )
        .then((res) => {
          if (res.code === "200") {
            this.$message.success("修改成功");
            this.showPasswordDialog = false;
            this.resetPsw = {
              newPassword: "",
              confirmPassword: "",
            };
          } else {
            this.$message.error(res.msg);
          }
        });
    },
    handleAvatarSuccess(res) {
      this.imageUrl = res.data;
      this.form.avatarUrl = this.imageUrl;
    },
    save() {
      this.request.post("/user", this.form).then((res) => {
        if (res.code === "200") {
          this.$message.success("保存成功");
          for (let key in this.form) {
            this.user[key] = this.form[key];
          }
          localStorage.setItem("user", JSON.stringify(this.user));
          this.$emit("refresh");
          this.$router.go(0);
        } else {
          this.$message.error(res.msg);
        }
      });
    },
  },
  created() {
    this.request.get("/userinfo/" + this.user.username).then((res) => {
      if (res.code === "200") {
        this.form = res.data;
      } else {
        this.$message.error(res.msg);
      }
    });
  },
  computed: {
    token() {
      return { token: this.user.token };
    },
  },
};
</script>

<style scoped>
.person-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  background: #f4f4f4;
  min-height: 100vh;
}

.person-header {
  background: white;
  border-radius: 12px;
  padding: 32px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 24px;
}

.avatar-wrapper {
  flex-shrink: 0;
}

.avatar-uploader {
  display: block;
}

.avatar-uploader .el-upload {
  border: none;
  cursor: pointer;
  display: block;
}

.avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid #ff5000;
  transition: all 0.3s;
}

.avatar:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(255, 80, 0, 0.3);
}

.avatar-placeholder {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff9000 0%, #ff5000 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.avatar-placeholder:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(255, 80, 0, 0.3);
}

.placeholder-icon {
  font-size: 32px;
  color: white;
  font-weight: 300;
}

.placeholder-text {
  font-size: 12px;
  color: white;
}

.user-info {
  flex: 1;
}

.user-name {
  font-size: 28px;
  font-weight: 700;
  color: #333;
  margin: 0 0 8px 0;
}

.user-desc {
  font-size: 14px;
  color: #999;
  margin: 0;
}

.person-content {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.info-card,
.security-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 2px solid #f0f0f0;
}

.card-icon {
  font-size: 24px;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.info-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

::v-deep .el-form-item__label {
  font-weight: 500;
  color: #666;
  padding-bottom: 8px;
}

::v-deep .el-input__inner {
  border-radius: 8px;
  border: 2px solid #e5e5e5;
  height: 44px;
  transition: all 0.3s;
  padding-left: 40px;
}

::v-deep .el-input__inner:focus {
  border-color: #ff5000;
  box-shadow: 0 0 0 3px rgba(255, 80, 0, 0.1);
}

.input-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  font-style: normal;
  font-size: 16px;
}

.form-actions {
  margin-top: 12px;
}

.save-btn {
  width: 100%;
  height: 48px;
  background: linear-gradient(90deg, #ff9000 0%, #ff5000 100%);
  color: white;
  border: none;
  border-radius: 24px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  box-shadow: 0 4px 12px rgba(255, 80, 0, 0.3);
}

.save-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(255, 80, 0, 0.4);
}

.btn-icon {
  font-size: 18px;
}

.security-content {
  padding: 8px 0;
}

.password-section {
  padding: 20px;
  background: #f9f9f9;
  border-radius: 8px;
}

.password-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.password-label {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.change-password-btn {
  padding: 8px 20px;
  background: white;
  border: 1px solid #ff5000;
  border-radius: 20px;
  color: #ff5000;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.change-password-btn:hover {
  background: #fff5f0;
}

.password-hint {
  font-size: 13px;
  color: #999;
  margin: 0;
}

::v-deep .password-dialog {
  border-radius: 12px;
}

::v-deep .el-dialog__header {
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
}

::v-deep .el-dialog__title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

::v-deep .el-dialog__body {
  padding: 24px;
}

.dialog-footer {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  padding: 16px 24px;
  border-top: 1px solid #f0f0f0;
}

.dialog-btn {
  padding: 10px 24px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  border: none;
}

.cancel-btn {
  background: #f5f5f5;
  color: #666;
}

.cancel-btn:hover {
  background: #e5e5e5;
}

.confirm-btn {
  background: linear-gradient(90deg, #ff9000 0%, #ff5000 100%);
  color: white;
}

.confirm-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 80, 0, 0.3);
}

@media (max-width: 768px) {
  .person-content {
    grid-template-columns: 1fr;
  }
  
  .user-profile {
    flex-direction: column;
    text-align: center;
  }
  
  .user-name {
    font-size: 24px;
  }
}
</style>
