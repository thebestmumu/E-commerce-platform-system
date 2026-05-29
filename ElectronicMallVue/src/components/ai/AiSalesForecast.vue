<template>
  <div class="ai-sales-forecast-container">
    <div class="tool-header">
      <span class="tool-icon">📦</span>
      <span class="tool-title">库存销量预测</span>
    </div>
    
    <div class="tool-form">
      <div class="form-group">
        <label>商品名称</label>
        <el-input v-model="form.productName" placeholder="请输入商品名称" class="form-input"></el-input>
      </div>
      
      <div class="form-group">
        <label>当前库存（可选）</label>
        <el-input-number v-model="form.currentStock" :min="0" placeholder="请输入当前库存" class="form-input"></el-input-number>
      </div>
      
      <el-button type="primary" class="forecast-btn" @click="handleForecast" :loading="loading">
        <i class="el-icon-data-line"></i>
        开始预测
      </el-button>
    </div>
    
    <div v-if="result" class="result-section">
      <div class="result-header">
        <span class="result-icon">🔮</span>
        <span>预测结果</span>
      </div>
      <div class="result-content" v-html="formatResult(result)"></div>
    </div>
    
    <div v-if="salesData && Object.keys(salesData).length > 0" class="sales-data-section">
      <div class="data-header">
        <span class="data-icon">📊</span>
        <span>历史销售数据</span>
      </div>
      <div class="sales-stats">
        <div class="stat-item">
          <span class="stat-label">总销量</span>
          <span class="stat-value">{{ salesData.totalSales || 0 }} 件</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">月均销量</span>
          <span class="stat-value">{{ salesData.monthlySales || 0 }} 件</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">日均销量</span>
          <span class="stat-value">{{ salesData.dailySales || 0 }} 件</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'AiSalesForecast',
  data() {
    return {
      form: {
        productName: '',
        currentStock: null
      },
      loading: false,
      result: '',
      salesData: {}
    }
  },
  methods: {
    async handleForecast() {
      if (!this.form.productName) {
        this.$message.warning('请输入商品名称')
        return
      }
      
      this.loading = true
      try {
        const res = await this.request.post('/api/ai/admin/sales-forecast', this.form)
        if (res.success) {
          this.result = res.data
          this.salesData = res.salesData || {}
          this.$message.success('预测完成')
        } else {
          this.$message.error(res.message || '预测失败')
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
.ai-sales-forecast-container {
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

.forecast-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  background: linear-gradient(90deg, #ff9000, #ff5000);
  border: none;
  border-radius: 22px;
  margin-top: 8px;
}

.forecast-btn:hover {
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

.sales-data-section {
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

.sales-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px;
  background: #fafafa;
  border-radius: 8px;
}

.stat-label {
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 16px;
  font-weight: 600;
  color: #ff5000;
}
</style>
