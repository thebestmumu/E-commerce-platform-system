<template>
  <div class="order-analysis-container">
    <div class="tb-section-title">
      <span>📋 订单分析报告</span>
    </div>

    <div v-if="orderData" class="tb-stat-grid">
      <div class="tb-stat-card">
        <span class="tb-stat-icon">📦</span>
        <div class="tb-stat-body">
          <div class="tb-stat-num">{{ orderData.totalOrders || 0 }}</div>
          <div class="tb-stat-label">订单总数</div>
        </div>
      </div>
      <div class="tb-stat-card">
        <span class="tb-stat-icon">💰</span>
        <div class="tb-stat-body">
          <div class="tb-stat-num price">¥{{ formatMoney(orderData.totalAmount) }}</div>
          <div class="tb-stat-label">总消费金额</div>
        </div>
      </div>
      <div class="tb-stat-card">
        <span class="tb-stat-icon">📊</span>
        <div class="tb-stat-body">
          <div class="tb-stat-num price">¥{{ formatMoney(orderData.avgAmount) }}</div>
          <div class="tb-stat-label">平均订单金额</div>
        </div>
      </div>
      <div class="tb-stat-card">
        <span class="tb-stat-icon">🏆</span>
        <div class="tb-stat-body">
          <div class="tb-stat-num price">¥{{ formatMoney(orderData.maxAmount) }}</div>
          <div class="tb-stat-label">最高单笔</div>
        </div>
      </div>
    </div>

    <div v-if="orderData && hasStatusData" class="tb-chart-section">
      <div class="tb-chart-title">订单状态分布</div>
      <div class="tb-chart-bars">
        <div v-for="item in statusList" :key="item.key" class="tb-chart-row" v-if="item.count > 0">
          <span class="tb-chart-lbl">{{ item.label }}</span>
          <div class="tb-chart-track">
            <div class="tb-chart-fill" :class="item.barClass" :style="{ width: getPercentage(item.count, orderData.totalOrders) + '%' }"></div>
          </div>
          <span class="tb-chart-cnt">{{ item.count }}单</span>
        </div>
      </div>
    </div>

    <div v-if="orderData && orderData.monthlyConsumption && Object.keys(orderData.monthlyConsumption).length > 0" class="tb-chart-section">
      <div class="tb-chart-title">月度消费趋势</div>
      <div class="tb-chart-bars">
        <div v-for="(amount, month) in orderData.monthlyConsumption" :key="month" class="tb-chart-row">
          <span class="tb-chart-lbl" style="min-width: 44px;">{{ month }}</span>
          <div class="tb-chart-track">
            <div class="tb-chart-fill fill-orange" :style="{ width: getMonthlyPercentage(amount) + '%' }"></div>
          </div>
          <span class="tb-chart-cnt" style="min-width: 64px;">¥{{ formatMoney(amount) }}</span>
        </div>
      </div>
    </div>

    <div v-if="orderData && orderData.insights && orderData.insights.length > 0" class="tb-insight-section">
      <div class="tb-chart-title">💡 消费洞察</div>
      <div class="tb-insight-list">
        <div v-for="(insight, idx) in orderData.insights" :key="idx" class="tb-insight-item">
          <span class="tb-insight-dot"></span>
          <span class="tb-insight-text">{{ insight }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'OrderAnalysis',
  props: {
    orderData: {
      type: Object,
      default: () => ({})
    }
  },
  computed: {
    hasStatusData() {
      return this.orderData.pendingPay > 0 || this.orderData.shipped > 0 ||
             this.orderData.completed > 0 || this.orderData.cancelled > 0
    },
    statusList() {
      return [
        { key: 'pendingPay', label: '待付款', count: this.orderData.pendingPay || 0, barClass: 'fill-pending' },
        { key: 'shipped', label: '待收货', count: this.orderData.shipped || 0, barClass: 'fill-shipped' },
        { key: 'completed', label: '已完成', count: this.orderData.completed || 0, barClass: 'fill-completed' },
        { key: 'cancelled', label: '已取消', count: this.orderData.cancelled || 0, barClass: 'fill-cancelled' }
      ]
    }
  },
  methods: {
    formatMoney(val) {
      if (!val) return '0.00'
      return (typeof val === 'number' ? val : parseFloat(val)).toFixed(2)
    },
    getPercentage(count, total) {
      if (!total || total === 0) return 0
      return Math.round((count / total) * 100)
    },
    getMonthlyPercentage(amount) {
      const allAmounts = Object.values(this.orderData.monthlyConsumption || {})
      const maxAmount = Math.max(...allAmounts, 1)
      const val = typeof amount === 'string' ? parseFloat(amount) : (amount || 0)
      return Math.round((val / maxAmount) * 100)
    }
  }
}
</script>

<style scoped>
.order-analysis-container {
  margin-top: 12px;
  padding: 14px;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #eee;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}

.tb-section-title {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
  background: linear-gradient(135deg, #fff5f0 0%, #ffffff 100%);
  margin: -14px -14px 12px;
  padding: 12px 14px;
  border-radius: 12px 12px 0 0;
}

.tb-stat-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  margin-bottom: 14px;
}

.tb-stat-card {
  padding: 10px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  gap: 10px;
  transition: all 0.25s ease;
}
.tb-stat-card:hover {
  box-shadow: 0 2px 8px rgba(255,80,0,0.08);
  border-color: #ffe0cc;
  transform: translateY(-1px);
}

.tb-stat-icon {
  font-size: 20px;
}

.tb-stat-body {
  flex: 1;
}

.tb-stat-num {
  font-size: 16px;
  font-weight: 700;
  color: #303133;
}

.tb-stat-num.price {
  color: #ff4400;
}

.tb-stat-label {
  font-size: 11px;
  color: #909399;
  margin-top: 1px;
}

.tb-chart-section {
  margin-bottom: 14px;
  padding: 10px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
  transition: all 0.25s ease;
}
.tb-chart-section:hover {
  box-shadow: 0 2px 8px rgba(255,80,0,0.06);
  border-color: #ffe0cc;
}

.tb-chart-title {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 8px;
}

.tb-chart-bars {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.tb-chart-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.tb-chart-lbl {
  min-width: 48px;
  font-size: 11px;
  color: #606266;
}

.tb-chart-track {
  flex: 1;
  height: 6px;
  background: #f0f0f0;
  border-radius: 3px;
  overflow: hidden;
}

.tb-chart-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.3s ease;
}

.fill-pending   { background: linear-gradient(90deg, #f57c00, #ffb74d); }
.fill-shipped   { background: linear-gradient(90deg, #7b1fa2, #ce93d8); }
.fill-completed { background: linear-gradient(90deg, #388e3c, #81c784); }
.fill-cancelled { background: linear-gradient(90deg, #d32f2f, #e57373); }
.fill-orange    { background: linear-gradient(90deg, #ff9000, #ff5000); }

.tb-chart-cnt {
  min-width: 48px;
  font-size: 10px;
  color: #909399;
  text-align: right;
}

.tb-insight-section {
  margin-bottom: 0;
  padding: 10px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
  transition: all 0.25s ease;
}
.tb-insight-section:hover {
  box-shadow: 0 2px 8px rgba(255,80,0,0.06);
  border-color: #ffe0cc;
}

.tb-insight-list {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.tb-insight-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 8px;
  background: linear-gradient(135deg, #ffffff 0%, #fafbfc 100%);
  border-radius: 6px;
  font-size: 11px;
  color: #606266;
  line-height: 1.4;
  border: 1px solid #f0f0f0;
  transition: all 0.2s ease;
}
.tb-insight-item:hover {
  border-color: #ffe0cc;
  background: #fff5f0;
}

.tb-insight-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff9000, #ff5000);
  flex-shrink: 0;
}

.tb-insight-text {
  flex: 1;
}
</style>