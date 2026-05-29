<template>
  <div class="sales-report-container">
    <div class="report-header">
      <h4>📈 销售数据报告</h4>
      <div class="report-period">统计周期：{{ salesData.days || 30 }}天</div>
    </div>
    
    <!-- 目标商品信息（如果有） -->
    <div v-if="salesData.targetGood" class="target-good-info">
      <div class="target-good-header">
        <img 
          v-if="salesData.targetGood.image" 
          :src="getImageUrl(salesData.targetGood.image)" 
          :alt="salesData.targetGood.name"
          class="target-good-img"
          @error="handleImageError"
        />
        <div v-else class="target-good-img-placeholder">📦</div>
        <div class="target-good-details">
          <h5>{{ salesData.targetGood.name }}</h5>
          <div class="target-good-stats">
            <span class="stat-item">销量：{{ salesData.targetGood.sales || 0 }}件</span>
            <span class="stat-item">价格：¥{{ (salesData.targetGood.price || 0).toFixed(2) }}</span>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 总体统计 -->
    <div class="total-stats-wrapper">
      <div class="total-stats">
        <div class="stat-card">
          <div class="stat-icon">💰</div>
          <div class="stat-info">
            <div class="stat-label">总销售额</div>
            <div class="stat-value">¥{{ (salesData.totalRevenue || 0).toFixed(2) }}</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">📦</div>
          <div class="stat-info">
            <div class="stat-label">总销量</div>
            <div class="stat-value">{{ salesData.totalSales || 0 }}件</div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 销售趋势 -->
    <div v-if="salesData.salesTrend && salesData.salesTrend.length > 0" class="sales-trend">
      <h5>销售趋势</h5>
      <div class="trend-chart" ref="salesTrendChart"></div>
    </div>
    
    <!-- 销售排行 -->
    <div v-if="salesData.salesRank && salesData.salesRank.length > 0" class="sales-rank">
      <h5>商品销售排行 TOP 10</h5>
      <div class="rank-list">
        <div 
          v-for="(item, index) in salesData.salesRank" 
          :key="item.id"
          class="rank-item"
        >
          <div class="rank-number" :class="{ 'top3': index < 3 }">{{ index + 1 }}</div>
          <div class="rank-info">
            <div class="rank-name">{{ item.name }}</div>
            <div class="rank-stats">
              <span>销量：{{ item.sales }}件</span>
              <span>销售额：¥{{ (item.revenue || 0).toFixed(2) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 评论舆情分析 -->
    <div v-if="salesData.sentimentDistribution || salesData.wordCloudData || salesData.ratingStats" class="sentiment-analysis">
      <h5> 评论舆情分析</h5>
      
      <!-- 评论统计 -->
      <div v-if="salesData.totalReviews" class="review-stats">
        <div class="stat-card">
          <div class="stat-icon">💬</div>
          <div class="stat-info">
            <div class="stat-label">总评论数</div>
            <div class="stat-value">{{ salesData.totalReviews }}</div>
          </div>
        </div>
        <div v-if="salesData.targetGood && salesData.targetGood.goodRating" class="stat-card">
          <div class="stat-icon">⭐</div>
          <div class="stat-info">
            <div class="stat-label">平均评分</div>
            <div class="stat-value">{{ salesData.targetGood.goodRating }}</div>
          </div>
        </div>
      </div>
      
      <!-- 情感分布 -->
      <div v-if="salesData.sentimentDistribution" class="sentiment-distribution">
        <h6>情感分布</h6>
        <div class="sentiment-bars">
          <div class="sentiment-bar-item">
            <span class="sentiment-label">😊 正面</span>
            <div class="sentiment-bar-bg">
              <div class="sentiment-bar-fill positive" :style="{ width: (salesData.sentimentDistribution.positive || 0) + '%' }"></div>
            </div>
            <span class="sentiment-value">{{ salesData.sentimentDistribution.positive || 0 }}%</span>
          </div>
          <div class="sentiment-bar-item">
            <span class="sentiment-label">😐 中性</span>
            <div class="sentiment-bar-bg">
              <div class="sentiment-bar-fill neutral" :style="{ width: (salesData.sentimentDistribution.neutral || 0) + '%' }"></div>
            </div>
            <span class="sentiment-value">{{ salesData.sentimentDistribution.neutral || 0 }}%</span>
          </div>
          <div class="sentiment-bar-item">
            <span class="sentiment-label">😞 负面</span>
            <div class="sentiment-bar-bg">
              <div class="sentiment-bar-fill negative" :style="{ width: (salesData.sentimentDistribution.negative || 0) + '%' }"></div>
            </div>
            <span class="sentiment-value">{{ salesData.sentimentDistribution.negative || 0 }}%</span>
          </div>
        </div>
      </div>
      
      <!-- 评分分布 -->
      <div v-if="salesData.ratingStats && salesData.ratingStats.length > 0" class="rating-distribution">
        <h6>评分分布</h6>
        <div class="rating-bars">
          <div v-for="item in salesData.ratingStats" :key="item.rating" class="rating-bar-item">
            <span class="rating-label">{{ item.rating }}星</span>
            <div class="rating-bar-bg">
              <div class="rating-bar-fill" :style="{ width: getRatingPercentage(item.count) + '%' }"></div>
            </div>
            <span class="rating-value">{{ item.count }}</span>
          </div>
        </div>
      </div>
      
      <!-- 最新评论 -->
      <div v-if="salesData.latestReviews && salesData.latestReviews.length > 0" class="latest-reviews">
        <h6>最新评论</h6>
        <div class="reviews-list">
          <div v-for="review in salesData.latestReviews.slice(0, 5)" :key="review.id" class="review-item">
            <div class="review-header">
              <span class="review-user">👤 {{ (review.user && review.user.nickname) || '匿名用户' }}</span>
              <span class="review-rating">
                <span v-for="i in 5" :key="i" :class="{ filled: i <= review.rating }">⭐</span>
              </span>
            </div>
            <div class="review-content">{{ review.content }}</div>
            <div v-if="review.tags" class="review-tags">
              <span v-for="(tag, tagIndex) in review.tags.split(',').slice(0, 3)" :key="tagIndex" class="review-tag">
                {{ tag.trim() }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'SalesReport',
  props: {
    salesData: {
      type: Object,
      required: true,
      default: () => ({})
    }
  },
  mounted() {
    console.log('===== SalesReport mounted =====')
    console.log('salesData:', JSON.stringify(this.salesData, null, 2))
    console.log('wordCloudData:', this.salesData.wordCloudData)
    console.log('sentimentDistribution:', this.salesData.sentimentDistribution)
    console.log('ratingStats:', this.salesData.ratingStats)
    this.$nextTick(() => {
      this.drawSalesTrendChart(this.salesData.salesTrend)
    })
  },
  watch: {
    salesData: {
      handler(newData) {
        console.log('===== SalesData changed =====')
        console.log('newData:', JSON.stringify(newData, null, 2))
        console.log('wordCloudData:', newData.wordCloudData)
        console.log('sentimentDistribution:', newData.sentimentDistribution)
        console.log('ratingStats:', newData.ratingStats)
        this.$nextTick(() => {
          this.drawSalesTrendChart(newData.salesTrend)
        })
      },
      deep: true
    }
  },
  methods: {
    getImageUrl(image) {
      if (!image) return ''
      if (image.startsWith('http://') || image.startsWith('https://')) {
        return image
      }
      return 'http://localhost:9191' + image
    },
    
    handleImageError(e) {
      e.target.style.display = 'none'
      const placeholder = e.target.nextElementSibling
      if (placeholder) {
        placeholder.style.display = 'flex'
      }
    },
    
    getRatingPercentage(count) {
      if (!this.salesData.ratingStats || this.salesData.ratingStats.length === 0) return 0
      const total = this.salesData.ratingStats.reduce((sum, item) => sum + (item.count || 0), 0)
      if (total === 0) return 0
      return Math.round((count / total) * 100)
    },

    drawSalesTrendChart(trendData) {
      if (!trendData || trendData.length === 0) return
      
      const chartContainer = this.$refs.salesTrendChart
      if (!chartContainer) return
      
      chartContainer.innerHTML = ''
      
      const width = chartContainer.clientWidth - 24
      const height = 176
      const padding = { top: 20, right: 20, bottom: 30, left: 50 }
      const chartWidth = width - padding.left - padding.right
      const chartHeight = height - padding.top - padding.bottom
      
      const salesValues = trendData.map(d => d.sales || 0)
      const maxSales = Math.max(...salesValues)
      const minSales = Math.min(...salesValues)
      const range = maxSales - minSales
      
      const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg')
      svg.setAttribute('width', width)
      svg.setAttribute('height', height)
      svg.style.width = '100%'
      svg.style.height = '100%'
      
      // 添加Y轴刻度
      if (range > 0) {
        const yTickCount = 4
        for (let i = 0; i <= yTickCount; i++) {
          const value = minSales + (range * i / yTickCount)
          const y = padding.top + chartHeight - (i / yTickCount) * chartHeight
          
          const text = document.createElementNS('http://www.w3.org/2000/svg', 'text')
          text.setAttribute('x', padding.left - 8)
          text.setAttribute('y', y + 3)
          text.setAttribute('text-anchor', 'end')
          text.setAttribute('font-size', '9')
          text.setAttribute('fill', '#909399')
          text.textContent = Math.round(value)
          svg.appendChild(text)
          
          // 网格线
          const line = document.createElementNS('http://www.w3.org/2000/svg', 'line')
          line.setAttribute('x1', padding.left)
          line.setAttribute('y1', y)
          line.setAttribute('x2', padding.left + chartWidth)
          line.setAttribute('y2', y)
          line.setAttribute('stroke', '#e4e7ed')
          line.setAttribute('stroke-width', '1')
          line.setAttribute('stroke-dasharray', '3,3')
          svg.appendChild(line)
        }
      }
      
      let pathData = ''
      trendData.forEach((d, i) => {
        const x = trendData.length > 1 
          ? padding.left + (i / (trendData.length - 1)) * chartWidth 
          : padding.left + chartWidth / 2
        const y = range > 0
          ? padding.top + chartHeight - ((d.sales - minSales) / range) * chartHeight
          : padding.top + chartHeight / 2
        pathData += (i === 0 ? 'M' : 'L') + `${x},${y}`
      })
      
      const path = document.createElementNS('http://www.w3.org/2000/svg', 'path')
      path.setAttribute('d', pathData)
      path.setAttribute('fill', 'none')
      path.setAttribute('stroke', '#409eff')
      path.setAttribute('stroke-width', '2')
      svg.appendChild(path)
      
      trendData.forEach((d, i) => {
        const x = trendData.length > 1 
          ? padding.left + (i / (trendData.length - 1)) * chartWidth 
          : padding.left + chartWidth / 2
        const y = range > 0
          ? padding.top + chartHeight - ((d.sales - minSales) / range) * chartHeight
          : padding.top + chartHeight / 2
        
        const circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle')
        circle.setAttribute('cx', x)
        circle.setAttribute('cy', y)
        circle.setAttribute('r', '3')
        circle.setAttribute('fill', '#409eff')
        svg.appendChild(circle)
      })
      
      // 只显示首尾日期标签，避免重叠
      if (trendData.length > 0) {
        // 第一个日期
        const firstDate = trendData[0].date || ''
        const firstX = padding.left
        const firstText = document.createElementNS('http://www.w3.org/2000/svg', 'text')
        firstText.setAttribute('x', firstX)
        firstText.setAttribute('y', height - 5)
        firstText.setAttribute('text-anchor', 'start')
        firstText.setAttribute('font-size', '10')
        firstText.setAttribute('fill', '#909399')
        firstText.textContent = firstDate.length >= 10 ? firstDate.substring(5) : firstDate
        svg.appendChild(firstText)
        
        // 最后一个日期
        if (trendData.length > 1) {
          const lastDate = trendData[trendData.length - 1].date || ''
          const lastX = padding.left + chartWidth
          const lastText = document.createElementNS('http://www.w3.org/2000/svg', 'text')
          lastText.setAttribute('x', lastX)
          lastText.setAttribute('y', height - 5)
          lastText.setAttribute('text-anchor', 'end')
          lastText.setAttribute('font-size', '10')
          lastText.setAttribute('fill', '#909399')
          lastText.textContent = lastDate.length >= 10 ? lastDate.substring(5) : lastDate
          svg.appendChild(lastText)
        }
      }
      
      chartContainer.appendChild(svg)
    }
  }
}
</script>

<style scoped>
.sales-report-container {
  margin-top: 16px;
  padding: 16px;
  background: linear-gradient(135deg, #f5f7fa 0%, #ffffff 100%);
  border-radius: 12px;
  border: 1px solid #e4e7ed;
}

.report-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.report-header h4 {
  margin: 0;
  font-size: 16px;
  color: #303133;
}

.report-period {
  font-size: 12px;
  color: #909399;
}

.target-good-info {
  margin-bottom: 16px;
  padding: 12px;
  background-color: #fff;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}

.target-good-header {
  display: flex;
  gap: 12px;
  align-items: center;
}

.target-good-img {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 8px;
}

.target-good-img-placeholder {
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f0f0f0;
  border-radius: 8px;
  font-size: 24px;
}

.target-good-details h5 {
  margin: 0 0 8px 0;
  font-size: 14px;
  color: #303133;
}

.target-good-stats {
  display: flex;
  gap: 16px;
}

.stat-item {
  font-size: 12px;
  color: #606266;
}

/* 总体统计 - 左右滑动 */
.total-stats-wrapper {
  margin-bottom: 16px;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: thin;
  scrollbar-color: #ff9000 #f0f0f0;
}

.total-stats-wrapper::-webkit-scrollbar {
  height: 6px;
}

.total-stats-wrapper::-webkit-scrollbar-track {
  background: #f0f0f0;
  border-radius: 3px;
}

.total-stats-wrapper::-webkit-scrollbar-thumb {
  background: linear-gradient(90deg, #ff9000, #ff5000);
  border-radius: 3px;
}

.total-stats {
  display: flex;
  gap: 12px;
  min-width: max-content;
  padding-bottom: 8px;
}

.stat-card {
  min-width: 140px;
  padding: 16px;
  background: linear-gradient(135deg, #fff5f0 0%, #ffffff 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: 0 2px 8px rgba(255, 80, 0, 0.08);
  border: 1px solid #ffe8d6;
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 80, 0, 0.15);
}

.stat-icon {
  font-size: 28px;
}

.stat-info {
  flex: 1;
}

.stat-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 18px;
  font-weight: bold;
  color: #ff5000;
}

.sales-trend {
  margin-bottom: 16px;
}

.sales-trend h5 {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #606266;
}

.trend-chart {
  width: 100%;
  height: 200px;
  padding: 12px;
  background-color: #fff;
  border-radius: 8px;
}

.sales-rank {
  margin-top: 16px;
  display: none;
}

.sales-rank h5 {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #606266;
}

/* 销售排行 - 上下滑动 */
.rank-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 320px;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: thin;
  scrollbar-color: #ff9000 #f0f0f0;
  padding-right: 4px;
}

.rank-list::-webkit-scrollbar {
  width: 6px;
}

.rank-list::-webkit-scrollbar-track {
  background: #f0f0f0;
  border-radius: 3px;
}

.rank-list::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, #ff9000, #ff5000);
  border-radius: 3px;
}

.rank-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  background: linear-gradient(135deg, #ffffff 0%, #fafbfc 100%);
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  border: 1px solid #f0f0f0;
  transition: all 0.3s ease;
}

.rank-item:hover {
  transform: translateX(4px);
  box-shadow: 0 4px 12px rgba(255, 80, 0, 0.12);
  border-color: #ff9000;
}

.rank-number {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f0f0f0;
  color: #606266;
  font-weight: bold;
  border-radius: 50%;
  font-size: 14px;
  flex-shrink: 0;
}

.rank-number.top3 {
  background: linear-gradient(135deg, #ff9000, #ff5000);
  color: #fff;
  box-shadow: 0 2px 8px rgba(255, 80, 0, 0.3);
}

.rank-info {
  flex: 1;
  min-width: 0;
}

.rank-name {
  font-size: 14px;
  color: #303133;
  margin-bottom: 4px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-stats {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #909399;
}

/* 评论舆情分析样式 */
.sentiment-analysis {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e4e7ed;
}

.sentiment-analysis h5 {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #303133;
}

.sentiment-analysis h6 {
  margin: 12px 0 8px 0;
  font-size: 13px;
  color: #606266;
}

.review-stats {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.sentiment-bars,
.rating-bars {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.sentiment-bar-item,
.rating-bar-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sentiment-label,
.rating-label {
  width: 60px;
  font-size: 12px;
  color: #606266;
  flex-shrink: 0;
}

.sentiment-bar-bg,
.rating-bar-bg {
  flex: 1;
  height: 8px;
  background-color: #f0f0f0;
  border-radius: 4px;
  overflow: hidden;
}

.sentiment-bar-fill,
.rating-bar-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.3s ease;
}

.sentiment-bar-fill.positive {
  background: linear-gradient(90deg, #67c23a, #85ce61);
}

.sentiment-bar-fill.neutral {
  background: linear-gradient(90deg, #e6a23c, #ebb563);
}

.sentiment-bar-fill.negative {
  background: linear-gradient(90deg, #f56c6c, #f78989);
}

.rating-bar-fill {
  background: linear-gradient(90deg, #409eff, #66b1ff);
}

.sentiment-value,
.rating-value {
  width: 40px;
  text-align: right;
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
}

/* 词云图样式 */
.word-cloud {
  margin-top: 16px;
}

.word-cloud-container {
  min-height: 200px;
  padding: 16px;
  background-color: #fff;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.word-cloud-item {
  display: inline-block;
  padding: 4px 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-weight: 500;
}

.word-cloud-item:hover {
  transform: scale(1.1);
  opacity: 0.8;
}

/* 最新评论样式 */
.latest-reviews {
  margin-top: 16px;
}

.reviews-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.review-item {
  padding: 8px;
  background: #fff;
  border-radius: 6px;
  border: 1px solid #e4e7ed;
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.review-user {
  font-size: 12px;
  color: #606266;
  font-weight: 500;
}

.review-rating span {
  font-size: 12px;
}

.review-content {
  font-size: 12px;
  color: #303133;
  line-height: 1.5;
}

.review-tags {
  display: flex;
  gap: 4px;
  margin-top: 4px;
}

.review-tag {
  font-size: 10px;
  padding: 1px 6px;
  background: #f0f2f5;
  border-radius: 8px;
  color: #909399;
}
</style>
