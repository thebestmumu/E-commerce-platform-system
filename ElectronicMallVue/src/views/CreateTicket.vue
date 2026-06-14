<template>
  <div class="create-ticket-page">
    <!-- 顶部导航栏 -->
    <div class="page-header">
      <div class="header-back" @click="goBack">
        <span class="icon"></span>
      </div>
      <h2 class="header-title">联系在线客服</h2>
      <div class="header-placeholder"></div>
    </div>

    <!-- 工单表单 -->
    <div class="form-container">
      <div class="form-card">
        <div class="card-header">
          <div class="header-icon"></div>
          <div class="header-text">
            <h3>请描述您的问题</h3>
            <p>客服将根据您的描述尽快为您处理</p>
          </div>
        </div>

        <el-form :model="ticketForm" class="ticket-form" label-position="top">
          <el-form-item label="问题类型" required class="form-item">
            <el-select 
              v-model="ticketForm.category" 
              placeholder="请选择问题类型" 
              class="form-select"
              popper-class="orange-select-dropdown"
            >
              <el-option 
                v-for="cat in ticketCategories" 
                :key="cat.value" 
                :label="cat.label" 
                :value="cat.value">
              </el-option>
            </el-select>
          </el-form-item>

          <el-form-item label="问题描述" required class="form-item">
            <el-input 
              v-model="ticketForm.description" 
              type="textarea" 
              :rows="8"
              placeholder="请详细描述您遇到的问题，以便客服更快解决&#10;例如：订单号、商品名称、具体问题等"
              maxlength="500"
              show-word-limit
              class="form-textarea"
            >
            </el-input>
          </el-form-item>

          <el-form-item class="form-item submit-item">
            <el-button 
              class="submit-btn" 
              @click="createTicket"
              :loading="loading"
              :disabled="!canSubmit"
              round
            >
              <span v-if="!loading">提交并连接客服</span>
              <span v-else>提交中...</span>
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 温馨提示 -->
      <div class="tips-card">
        <div class="tips-header">
          <span class="tips-icon"></span>
          <span class="tips-title">温馨提示</span>
        </div>
        <ul class="tips-list">
          <li>提交后，您可以进入在线客服实时沟通</li>
          <li>客服会按排队顺序依次接入</li>
          <li>您也可以拨打客服热线：<strong>400-800-8888</strong></li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'CreateTicket',
  data() {
    return {
      ticketForm: {
        category: 'other',
        description: ''
      },
      ticketCategories: [
        { value: 'order', label: '订单问题' },
        { value: 'product', label: '商品问题' },
        { value: 'refund', label: '退款/售后' },
        { value: 'logistics', label: '物流问题' },
        { value: 'complaint', label: '投诉建议' },
        { value: 'other', label: '其他问题' }
      ],
      loading: false
    }
  },
  computed: {
    canSubmit() {
      return this.ticketForm.description.trim().length >= 5
    }
  },
  methods: {
    goBack() {
      this.$router.back()
    },
    async createTicket() {
      if (!this.canSubmit) {
        this.$message.warning('请至少输入5个字符描述问题')
        return
      }

      this.loading = true
      try {
        const userStr = localStorage.getItem('user')
        const user = userStr ? JSON.parse(userStr) : null

        const response = await this.request.post('/api/ticket', {
          category: this.ticketForm.category,
          subject: this.ticketForm.description.substring(0, 50),
          description: this.ticketForm.description
        })

        if (response.code === '200') {
          const ticketId = response.data?.id || response.data
          const ticketNo = response.data?.ticketNo

          this.$message.success('提交成功，正在为您连接客服...')

          // 跳转到聊天页面，携带工单信息
          setTimeout(() => {
            this.$router.replace({
              path: '/user-chat',
              query: {
                ticketId: ticketId,
                ticketNo: ticketNo
              }
            })
          }, 500)
        } else {
          this.$message.error(response.msg || '提交失败')
        }
      } catch (e) {
        console.error('创建工单失败:', e)
        this.$message.error('提交失败，请稍后重试')
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.create-ticket-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 60px;
}

/* 顶部导航栏 */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 50px;
  padding: 0 16px;
  background: linear-gradient(135deg, #ff9000 0%, #ff5000 100%);
  color: #fff;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-back {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-radius: 50%;
}

.header-back:hover {
  background: rgba(255, 255, 255, 0.2);
}

.header-back .icon {
  font-size: 14px;
}

.header-title {
  font-size: 17px;
  font-weight: 600;
  margin: 0;
}

.header-placeholder {
  width: 32px;
}

/* 表单容器 */
.form-container {
  padding: 16px;
}

.form-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.header-icon {
  font-size: 32px;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #fff5f0 0%, #ffe8d6 100%);
  border-radius: 12px;
}

.header-text h3 {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0 0 4px;
}

.header-text p {
  font-size: 12px;
  color: #999;
  margin: 0;
}

/* 表单样式 */
.ticket-form {
  padding: 0;
}

.form-item {
  margin-bottom: 20px;
}

.form-item >>> .el-form-item__label {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  padding-bottom: 8px;
  line-height: 1.4;
}

.form-item >>> .el-form-item__label::before {
  color: #ff5000;
}

.form-select,
.form-textarea {
  width: 100%;
}

.form-select >>> .el-input__inner {
  border-radius: 8px;
  border: 1px solid #e8e8e8;
  height: 44px;
  font-size: 14px;
}

.form-select >>> .el-input__inner:focus {
  border-color: #ff9000;
}

.form-textarea >>> .el-textarea__inner {
  border-radius: 8px;
  border: 1px solid #e8e8e8;
  font-size: 14px;
  line-height: 1.6;
}

.form-textarea >>> .el-textarea__inner:focus {
  border-color: #ff9000;
}

.form-item >>> .el-input__count {
  background: transparent;
  color: #999;
  font-size: 12px;
}

/* 提交按钮 */
.submit-item {
  margin-bottom: 0;
  margin-top: 8px;
}

.submit-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #ff9000 0%, #ff5000 100%);
  border: none;
  border-radius: 24px;
  letter-spacing: 1px;
  color: #fff;
}

.submit-btn:hover {
  background: linear-gradient(135deg, #ff5000 0%, #e04000 100%);
}

.submit-btn:disabled {
  background: #e8e8e8;
  color: #999;
}

/* 温馨提示 */
.tips-card {
  margin-top: 16px;
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.tips-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.tips-icon {
  font-size: 16px;
}

.tips-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.tips-list {
  margin: 0;
  padding-left: 20px;
}

.tips-list li {
  font-size: 13px;
  color: #666;
  line-height: 1.8;
}

.tips-list li strong {
  color: #ff5000;
  font-weight: 600;
}

/* 下拉框样式覆盖 */
::v-deep .orange-select-dropdown .el-select-dropdown__item.hover,
::v-deep .orange-select-dropdown .el-select-dropdown__item:hover {
  background-color: #fff5f0;
  color: #ff5000;
}

::v-deep .orange-select-dropdown .el-select-dropdown__item.selected {
  color: #ff5000;
  font-weight: 600;
}
</style>
