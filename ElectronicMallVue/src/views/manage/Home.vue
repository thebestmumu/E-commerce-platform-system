<template>
<div class="admin-dashboard">
  <div class="stats-row">
    <div class="stat-card" v-for="(stat, idx) in stats" :key="idx">
      <div class="stat-icon" :style="{ background: stat.color }">{{ stat.icon }}</div>
      <div class="stat-info">
        <div class="stat-value">{{ stat.value }}</div>
        <div class="stat-label">{{ stat.label }}</div>
        <div class="stat-trend" :class="stat.trend > 0 ? 'up' : 'down'">
          {{ stat.trend > 0 ? '↑' : '↓' }} {{ Math.abs(stat.trend) }}%
        </div>
      </div>
    </div>
  </div>
  
  <div class="ai-tools-section">
    <div class="section-header">
      <span class="section-icon">🤖</span>
      <span class="section-title">AI 智能工具箱</span>
      <span class="section-badge">管理员专属</span>
    </div>
    <div class="ai-tools-grid">
      <div class="ai-tool-card" @click="copywritingDialog = true">
        <div class="tool-icon">✍️</div>
        <div class="tool-name">AI 文案生成</div>
        <div class="tool-desc">智能生成商品标题、卖点、回复话术</div>
        <div class="tool-tag">热门</div>
      </div>
      <div class="ai-tool-card" @click="priceDialog = true">
        <div class="tool-icon">📊</div>
        <div class="tool-name">竞品价格分析</div>
        <div class="tool-desc">竞品价格行情分析 + 定价建议</div>
        <div class="tool-tag">推荐</div>
      </div>
      <div class="ai-tool-card" @click="forecastDialog = true">
        <div class="tool-icon">📦</div>
        <div class="tool-name">库存销量预测</div>
        <div class="tool-desc">智能预测销量 + 备货预警</div>
        <div class="tool-tag">实用</div>
      </div>
      <div class="ai-tool-card" @click="$router.push('/manage/good')">
        <div class="tool-icon">🛒</div>
        <div class="tool-name">商品管理</div>
        <div class="tool-desc">管理所有商品信息</div>
      </div>
      <div class="ai-tool-card" @click="$router.push('/manage/order')">
        <div class="tool-icon">📋</div>
        <div class="tool-name">订单管理</div>
        <div class="tool-desc">查看和处理订单</div>
      </div>
      <div class="ai-tool-card" @click="$router.push('/manage/incomeChart')">
        <div class="tool-icon">💰</div>
        <div class="tool-name">营收分析</div>
        <div class="tool-desc">查看收入图表和排行</div>
      </div>
    </div>
  </div>
  
  <div class="quick-actions">
    <div class="section-header">
      <span class="section-icon">⚡</span>
      <span class="section-title">快捷操作</span>
    </div>
    <div class="action-buttons">
      <el-button type="primary" icon="el-icon-plus" @click="$router.push('/manage/goodInfo')">新增商品</el-button>
      <el-button type="success" icon="el-icon-upload2" @click="$router.push('/manage/carousel')">更新轮播图</el-button>
      <el-button type="warning" icon="el-icon-data-analysis" @click="$router.push('/manage/incomeChart')">查看报表</el-button>
      <el-button type="info" icon="el-icon-user" @click="$router.push('/manage/user')">用户管理</el-button>
    </div>
  </div>
  
  <!-- AI文案生成弹窗 -->
  <el-dialog title="✍️ AI 文案生成" :visible.sync="copywritingDialog" width="560px" class="ai-tool-dialog">
    <div class="dialog-form">
      <div class="form-group">
        <label>文案类型</label>
        <el-select v-model="copywritingForm.type" placeholder="选择文案类型" class="form-select">
          <el-option label="商品标题" value="标题"></el-option>
          <el-option label="核心卖点" value="卖点"></el-option>
          <el-option label="客服话术" value="话术"></el-option>
        </el-select>
      </div>
      <div class="form-group">
        <label>商品名称</label>
        <el-input v-model="copywritingForm.productName" placeholder="请输入商品名称" class="form-input"></el-input>
      </div>
      <div class="form-group">
        <label>品类（可选）</label>
        <el-input v-model="copywritingForm.category" placeholder="如：手机、服装、食品" class="form-input"></el-input>
      </div>
      <el-button type="primary" class="submit-btn" @click="generateCopywriting" :loading="copywritingLoading" :disabled="copywritingLoading">
        <i class="el-icon-magic-stick"></i>
        生成文案
      </el-button>
    </div>
    <div v-if="copywritingResult || copywritingStreaming" class="dialog-result">
      <div class="result-header">
        <span>✨ 生成结果</span>
        <el-button v-if="copywritingResult && !copywritingStreaming" size="mini" type="primary" @click="copyResult(copywritingResult)">复制</el-button>
        <span v-if="copywritingStreaming" class="streaming-indicator">
          <i class="el-icon-loading"></i> 生成中...
        </span>
      </div>
      <div class="result-content" v-html="formatResult(copywritingResult)"></div>
    </div>
  </el-dialog>
  
  <!-- 竞品价格分析弹窗 -->
  <el-dialog title="📊 竞品价格分析" :visible.sync="priceDialog" width="560px" class="ai-tool-dialog">
    <div class="dialog-form">
      <div class="form-group">
        <label>商品名称</label>
        <el-input v-model="priceForm.productName" placeholder="请输入商品名称" class="form-input"></el-input>
      </div>
      <div class="form-group">
        <label>当前售价（可选）</label>
        <el-input-number v-model="priceForm.currentPrice" :min="0" :precision="2" placeholder="请输入当前售价" class="form-input"></el-input-number>
      </div>
      <el-button type="primary" class="submit-btn" @click="analyzePrice" :loading="priceLoading" :disabled="priceLoading">
        <i class="el-icon-data-analysis"></i>
        开始分析
      </el-button>
    </div>
    <div v-if="priceResult || priceStreaming" class="dialog-result">
      <div class="result-header">
        <span>📈 分析结果</span>
        <span v-if="priceStreaming" class="streaming-indicator">
          <i class="el-icon-loading"></i> 分析中...
        </span>
      </div>
      <div class="result-content" v-html="formatResult(priceResult)"></div>
    </div>
    <div v-if="priceData && priceData.length > 0" class="dialog-reference">
      <div class="ref-header">同类商品价格参考</div>
      <div class="ref-list">
        <div class="ref-item" v-for="(item, idx) in priceData" :key="idx">
          <span class="ref-name">{{ item.name }}</span>
          <span class="ref-price">¥{{ item.price }}</span>
        </div>
      </div>
    </div>
  </el-dialog>
  
  <!-- 库存销量预测弹窗 -->
  <el-dialog title="📦 库存销量预测" :visible.sync="forecastDialog" width="560px" class="ai-tool-dialog">
    <div class="dialog-form">
      <div class="form-group">
        <label>商品名称</label>
        <el-input v-model="forecastForm.productName" placeholder="请输入商品名称" class="form-input"></el-input>
      </div>
      <div class="form-group">
        <label>当前库存（可选）</label>
        <el-input-number v-model="forecastForm.currentStock" :min="0" placeholder="请输入当前库存" class="form-input"></el-input-number>
      </div>
      <el-button type="primary" class="submit-btn" @click="predictStock" :loading="forecastLoading" :disabled="forecastLoading">
        <i class="el-icon-data-line"></i>
        开始预测
      </el-button>
    </div>
    <div v-if="forecastResult || forecastStreaming" class="dialog-result">
      <div class="result-header">
        <span>🔮 预测结果</span>
        <span v-if="forecastStreaming" class="streaming-indicator">
          <i class="el-icon-loading"></i> 预测中...
        </span>
      </div>
      <div class="result-content" v-html="formatResult(forecastResult)"></div>
    </div>
    <div v-if="salesData && Object.keys(salesData).length > 0" class="dialog-reference">
      <div class="ref-header">历史销售数据</div>
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
  </el-dialog>
</div>
</template>

<script>
export default {
  name: "Home",
  data() {
    return {
      stats: [
        { icon: '💰', label: '今日营收', value: '¥12,580', trend: 12.5, color: 'linear-gradient(135deg, #ff9000, #ff5000)' },
        { icon: '📦', label: '今日订单', value: '156', trend: 8.3, color: 'linear-gradient(135deg, #4caf50, #2e7d32)' },
        { icon: '👥', label: '新增用户', value: '89', trend: -3.2, color: 'linear-gradient(135deg, #2196f3, #1565c0)' },
        { icon: '🛒', label: '在售商品', value: '328', trend: 5.1, color: 'linear-gradient(135deg, #9c27b0, #6a1b9a)' },
      ],
      copywritingDialog: false,
      copywritingLoading: false,
      copywritingStreaming: false,
      copywritingForm: { type: '标题', productName: '', category: '' },
      copywritingResult: '',
      priceDialog: false,
      priceLoading: false,
      priceStreaming: false,
      priceForm: { productName: '', currentPrice: null },
      priceResult: '',
      priceData: [],
      forecastDialog: false,
      forecastLoading: false,
      forecastStreaming: false,
      forecastForm: { productName: '', currentStock: null },
      forecastResult: '',
      salesData: {}
    };
  },
  methods: {
    formatResult(text) {
      if (!text) return ''
      return text.replace(/\n/g, '<br>')
    },
    copyResult(text) {
      navigator.clipboard.writeText(text).then(() => {
        this.$message.success('已复制到剪贴板')
      })
    },
    async generateCopywriting() {
      if (!this.copywritingForm.productName) {
        this.$message.warning('请输入商品名称')
        return
      }
      this.copywritingLoading = true
      this.copywritingStreaming = true
      this.copywritingResult = ''
      try {
        const user = JSON.parse(localStorage.getItem('user'))
        const token = user ? user.token : ''
        const url = 'http://localhost:9191/api/ai/admin/copywriting/stream'
        
        const response = await fetch(url, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'token': token
          },
          body: JSON.stringify(this.copywritingForm)
        })
        
        const reader = response.body.getReader()
        const decoder = new TextDecoder('utf-8')
        let buffer = ''
        
        while (true) {
          const { done, value } = await reader.read()
          if (done) break
          
          buffer += decoder.decode(value, { stream: true })
          const lines = buffer.split('\n')
          buffer = lines.pop() || ''
          
          for (const line of lines) {
            if (line.startsWith('data: ')) {
              const data = line.substring(6)
              if (data === '[DONE]') {
                this.copywritingStreaming = false
                this.$message.success('文案生成成功')
                break
              }
              try {
                const parsed = JSON.parse(data)
                if (parsed.content) {
                  this.copywritingResult += parsed.content
                }
              } catch (e) {
                // ignore
              }
            }
          }
        }
      } catch (e) {
        this.$message.error('请求失败：' + e.message)
        this.copywritingStreaming = false
      } finally {
        this.copywritingLoading = false
      }
    },
    async analyzePrice() {
      if (!this.priceForm.productName) {
        this.$message.warning('请输入商品名称')
        return
      }
      this.priceLoading = true
      this.priceStreaming = true
      this.priceResult = ''
      this.priceData = []
      try {
        const user = JSON.parse(localStorage.getItem('user'))
        const token = user ? user.token : ''
        const url = 'http://localhost:9191/api/ai/admin/price-analysis/stream'
        
        const response = await fetch(url, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'token': token
          },
          body: JSON.stringify(this.priceForm)
        })
        
        const reader = response.body.getReader()
        const decoder = new TextDecoder('utf-8')
        let buffer = ''
        
        while (true) {
          const { done, value } = await reader.read()
          if (done) break
          
          buffer += decoder.decode(value, { stream: true })
          const lines = buffer.split('\n')
          buffer = lines.pop() || ''
          
          for (const line of lines) {
            if (line.startsWith('data: ')) {
              const data = line.substring(6)
              if (data === '[DONE]') {
                this.priceStreaming = false
                this.$message.success('分析完成')
                break
              }
              try {
                const parsed = JSON.parse(data)
                if (parsed.content) {
                  this.priceResult += parsed.content
                }
              } catch (e) {
                // ignore
              }
            }
          }
        }
      } catch (e) {
        this.$message.error('请求失败：' + e.message)
        this.priceStreaming = false
      } finally {
        this.priceLoading = false
      }
    },
    async predictStock() {
      if (!this.forecastForm.productName) {
        this.$message.warning('请输入商品名称')
        return
      }
      this.forecastLoading = true
      this.forecastStreaming = true
      this.forecastResult = ''
      this.salesData = {}
      try {
        const user = JSON.parse(localStorage.getItem('user'))
        const token = user ? user.token : ''
        const url = 'http://localhost:9191/api/ai/admin/sales-forecast/stream'
        
        const response = await fetch(url, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'token': token
          },
          body: JSON.stringify(this.forecastForm)
        })
        
        const reader = response.body.getReader()
        const decoder = new TextDecoder('utf-8')
        let buffer = ''
        
        while (true) {
          const { done, value } = await reader.read()
          if (done) break
          
          buffer += decoder.decode(value, { stream: true })
          const lines = buffer.split('\n')
          buffer = lines.pop() || ''
          
          for (const line of lines) {
            if (line.startsWith('data: ')) {
              const data = line.substring(6)
              if (data === '[DONE]') {
                this.forecastStreaming = false
                this.$message.success('预测完成')
                break
              }
              try {
                const parsed = JSON.parse(data)
                if (parsed.content) {
                  this.forecastResult += parsed.content
                }
              } catch (e) {
                // ignore
              }
            }
          }
        }
      } catch (e) {
        this.$message.error('请求失败：' + e.message)
        this.forecastStreaming = false
      } finally {
        this.forecastLoading = false
      }
    }
  }
};
</script>

<style scoped>
@import '@/assets/taobao-style.css';

.admin-dashboard {
  padding: 20px;
  background: var(--taobao-bg);
  min-height: calc(100vh - 80px);
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: var(--taobao-white);
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
  border: 1px solid var(--taobao-border);
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(255, 80, 0, 0.12);
  border-color: var(--taobao-orange);
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--taobao-text);
  margin-bottom: 4px;
}

.stat-label {
  font-size: 13px;
  color: var(--taobao-text-light);
  margin-bottom: 4px;
}

.stat-trend {
  font-size: 12px;
  font-weight: 600;
}

.stat-trend.up {
  color: #4caf50;
}

.stat-trend.down {
  color: #f44336;
}

.ai-tools-section {
  background: var(--taobao-white);
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  border: 1px solid var(--taobao-border);
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 2px solid var(--taobao-border);
}

.section-icon {
  font-size: 20px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--taobao-text);
}

.section-badge {
  background: var(--taobao-gradient);
  color: white;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 600;
}

.ai-tools-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.ai-tool-card {
  background: linear-gradient(135deg, #fafafa 0%, #f5f5f5 100%);
  border-radius: 12px;
  padding: 24px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid transparent;
  position: relative;
  overflow: hidden;
}

.ai-tool-card:hover {
  border-color: var(--taobao-orange);
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(255, 80, 0, 0.15);
}

.ai-tool-card .tool-tag {
  position: absolute;
  top: 8px;
  right: 8px;
  background: var(--taobao-gradient);
  color: white;
  padding: 2px 8px;
  border-radius: 8px;
  font-size: 10px;
  font-weight: 600;
}

.tool-icon {
  font-size: 40px;
  margin-bottom: 12px;
}

.tool-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--taobao-text);
  margin-bottom: 8px;
}

.tool-desc {
  font-size: 12px;
  color: var(--taobao-text-light);
  line-height: 1.5;
}

.quick-actions {
  background: var(--taobao-white);
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  border: 1px solid var(--taobao-border);
}

.action-buttons {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.action-buttons .el-button {
  border-radius: 20px;
  padding: 10px 24px;
  font-weight: 600;
  transition: all 0.3s ease;
}

.action-buttons .el-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.dialog-form {
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

.submit-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  background: linear-gradient(90deg, #ff9000, #ff5000);
  border: none;
  border-radius: 22px;
}

.submit-btn:hover {
  opacity: 0.9;
}

.dialog-result {
  margin-top: 16px;
  background: #fafafa;
  border-radius: 8px;
  padding: 16px;
  border-left: 4px solid #ff5000;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 600;
  color: #ff5000;
}

.streaming-indicator {
  color: #ff9000;
  font-size: 13px;
}

.result-content {
  font-size: 14px;
  line-height: 1.8;
  color: #333;
}

.dialog-reference {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.ref-header {
  font-size: 13px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
}

.ref-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ref-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #fafafa;
  border-radius: 8px;
}

.ref-name {
  font-size: 13px;
  color: #333;
  flex: 1;
}

.ref-price {
  font-size: 14px;
  font-weight: 600;
  color: #ff5000;
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

@media (max-width: 1200px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .ai-tools-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: 1fr;
  }
  .ai-tools-grid {
    grid-template-columns: 1fr;
  }
}
</style>
