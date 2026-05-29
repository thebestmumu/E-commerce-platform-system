<template>
  <div class="ai-copywriting-container">
    <div class="tool-header">
      <span class="tool-icon">✍️</span>
      <span class="tool-title">AI 文案生成</span>
    </div>
    
    <div class="tool-form">
      <div class="form-group">
        <label>文案类型</label>
        <el-select v-model="form.type" placeholder="选择文案类型" class="form-select">
          <el-option label="商品标题" value="标题"></el-option>
          <el-option label="核心卖点" value="卖点"></el-option>
          <el-option label="客服话术" value="话术"></el-option>
        </el-select>
      </div>
      
      <div class="form-group">
        <label>商品名称</label>
        <el-input v-model="form.productName" placeholder="请输入商品名称" class="form-input"></el-input>
      </div>
      
      <div class="form-group">
        <label>品类（可选）</label>
        <el-input v-model="form.category" placeholder="如：手机、服装、食品" class="form-input"></el-input>
      </div>
      
      <el-button type="primary" class="generate-btn" @click="handleGenerate" :loading="loading">
        <i class="el-icon-magic-stick"></i>
        生成文案
      </el-button>
    </div>
    
    <div v-if="result" class="result-section">
      <div class="result-header">
        <span class="result-icon">✨</span>
        <span>生成结果</span>
      </div>
      <div class="result-content" v-html="formatResult(result)"></div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'AiCopywriting',
  data() {
    return {
      form: {
        type: '标题',
        productName: '',
        category: ''
      },
      loading: false,
      result: ''
    }
  },
  methods: {
    async handleGenerate() {
      if (!this.form.productName) {
        this.$message.warning('请输入商品名称')
        return
      }
      
      this.loading = true
      try {
        const res = await this.request.post('/api/ai/admin/copywriting', this.form)
        if (res.success) {
          this.result = res.data
          this.$message.success('文案生成成功')
        } else {
          this.$message.error(res.message || '生成失败')
        }
      } catch (e) {
        this.$message.error('请求失败：' + e.message)
      } finally {
        this.loading = false
      }
    },
    formatResult(text) {
      if (!text) return ''
      return text
        .replace(/\n/g, '<br>')
        .replace(/(\d+\..*?)(?=\d+\.|$)/g, '<div class="result-item">$1</div>')
    }
  }
}
</script>

<style scoped>
.ai-copywriting-container {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.tool-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #f0f0f0;
}

.tool-icon {
  font-size: 24px;
}

.tool-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.tool-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  font-size: 13px;
  color: #666;
  font-weight: 500;
}

.form-select,
.form-input {
  width: 100%;
}

.generate-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  background: linear-gradient(90deg, #ff9000, #ff5000);
  border: none;
  border-radius: 22px;
  margin-top: 8px;
}

.generate-btn:hover {
  opacity: 0.9;
}

.result-section {
  margin-top: 20px;
  padding-top: 15px;
  border-top: 1px solid #f0f0f0;
}

.result-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 600;
  color: #ff5000;
}

.result-content {
  font-size: 14px;
  line-height: 1.8;
  color: #333;
  background: #fafafa;
  padding: 16px;
  border-radius: 8px;
}

.result-item {
  margin-bottom: 8px;
}
</style>
