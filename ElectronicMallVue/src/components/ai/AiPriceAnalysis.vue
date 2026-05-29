<template>
  <div class="ai-price-analysis-container">
    <div class="tool-header">
      <span class="tool-icon">📊</span>
      <span class="tool-title">竞品价格分析</span>
    </div>
    
    <div class="tool-form">
      <div class="form-group">
        <label>商品名称</label>
        <el-input v-model="form.productName" placeholder="请输入商品名称" class="form-input"></el-input>
      </div>
      
      <div class="form-group">
        <label>当前售价（可选）</label>
        <el-input-number v-model="form.currentPrice" :min="0" :precision="2" placeholder="请输入当前售价" class="form-input"></el-input-number>
      </div>
      
      <el-button type="primary" class="analyze-btn" @click="handleAnalyze" :loading="loading">
        <i class="el-icon-data-analysis"></i>
        开始分析
      </el-button>
    </div>
    
    <div v-if="result" class="result-section">
      <div class="result-header">
        <span class="result-icon">📈</span>
        <span>分析结果</span>
      </div>
      <div class="result-content" v-html="formatResult(result)"></div>
    </div>
    
    <div v-if="priceData && priceData.length > 0" class="price-data-section">
      <div class="data-header">
        <span class="data-icon">💰</span>
        <span>同类商品价格参考</span>
      </div>
      <div class="price-list">
        <div class="price-item" v-for="(item, idx) in priceData" :key="idx">
          <span class="price-name">{{ item.name }}</span>
          <span class="price-value">¥{{ item.price }}</span>
          <span class="price-sales">销量 {{ item.sales || 0 }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'AiPriceAnalysis',
  data() {
    return {
      form: {
        productName: '',
        currentPrice: null
      },
      loading: false,
      result: '',
      priceData: []
    }
  },
  methods: {
    async handleAnalyze() {
      if (!this.form.productName) {
        this.$message.warning('请输入商品名称')
        return
      }
      
      this.loading = true
      try {
        const res = await this.request.post('/api/ai/admin/price-analysis', this.form)
        if (res.success) {
          this.result = res.data
          this.priceData = res.priceData || []
          this.$message.success('分析完成')
        } else {
          this.$message.error(res.message || '分析失败')
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
.ai-price-analysis-container {
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

.form-input {
  width: 100%;
}

.analyze-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  background: linear-gradient(90deg, #ff9000, #ff5000);
  border: none;
  border-radius: 22px;
  margin-top: 8px;
}

.analyze-btn:hover {
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

.price-data-section {
  margin-top: 20px;
  padding-top: 15px;
  border-top: 1px solid #f0f0f0;
}

.data-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.price-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.price-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  background: #fafafa;
  border-radius: 8px;
}

.price-name {
  font-size: 13px;
  color: #333;
  flex: 1;
}

.price-value {
  font-size: 14px;
  font-weight: 600;
  color: #ff5000;
  margin: 0 12px;
}

.price-sales {
  font-size: 12px;
  color: #999;
}
</style>
