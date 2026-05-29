<template>
  <div class="sentiment-analysis-container">
    <div class="analysis-header">
      <h4>📊 评论舆情分析报告</h4>
      <div class="total-reviews">共 {{ sentimentData.totalReviews || 0 }} 条评价</div>
    </div>
    
    <!-- 评分分布 -->
    <div class="rating-distribution">
      <h5>评分分布</h5>
      <div class="rating-bars">
        <div 
          v-for="stat in sentimentData.ratingStats" 
          :key="stat.rating"
          class="rating-bar-item"
        >
          <div class="rating-label">{{ stat.rating }}星</div>
          <div class="rating-bar-wrapper">
            <div class="rating-bar" :style="{ width: stat.percentage + '%' }"></div>
          </div>
          <div class="rating-count">{{ stat.count }}条 ({{ stat.percentage }}%)</div>
        </div>
      </div>
    </div>
    
    <!-- 情感分布 -->
    <div class="sentiment-distribution">
      <h5>情感分布</h5>
      <div class="sentiment-pie">
        <div class="sentiment-item positive">
          <div class="sentiment-color" style="background-color: #52c41a;"></div>
          <span>正面</span>
          <span class="sentiment-value">{{ (sentimentData.sentimentDistribution && sentimentData.sentimentDistribution.positive) || 0 }}%</span>
        </div>
        <div class="sentiment-item neutral">
          <div class="sentiment-color" style="background-color: #faad14;"></div>
          <span>中性</span>
          <span class="sentiment-value">{{ (sentimentData.sentimentDistribution && sentimentData.sentimentDistribution.neutral) || 0 }}%</span>
        </div>
        <div class="sentiment-item negative">
          <div class="sentiment-color" style="background-color: #ff4d4f;"></div>
          <span>负面</span>
          <span class="sentiment-value">{{ (sentimentData.sentimentDistribution && sentimentData.sentimentDistribution.negative) || 0 }}%</span>
        </div>
      </div>
    </div>
    
    <!-- 热门标签 -->
    <div v-if="sentimentData.hotTags && sentimentData.hotTags.length > 0" class="hot-tags">
      <h5>热门标签</h5>
      <div class="tags-cloud">
        <span 
          v-for="(tag, index) in sentimentData.hotTags.slice(0, 15)" 
          :key="index"
          class="tag-item"
          :style="{ fontSize: (12 + Math.random() * 8) + 'px' }"
        >
          {{ tag.trim() }}
        </span>
      </div>
    </div>
    
    <!-- 最新评论 -->
    <div v-if="sentimentData.latestReviews && sentimentData.latestReviews.length > 0" class="latest-reviews">
      <h5>最新评论</h5>
      <div class="reviews-list">
        <div 
          v-for="review in sentimentData.latestReviews.slice(0, 5)" 
          :key="review.id"
          class="review-item"
        >
          <div class="review-header">
            <span class="review-user">👤 {{ (review.user && review.user.nickname) || '匿名用户' }}</span>
            <span class="review-rating">
              <span v-for="i in 5" :key="i" :class="{ filled: i <= review.rating }">⭐</span>
            </span>
          </div>
          <div class="review-content">{{ review.content }}</div>
          <div v-if="review.tags" class="review-tags">
            <span 
              v-for="(tag, tagIndex) in review.tags.split(',').slice(0, 3)" 
              :key="tagIndex"
              class="review-tag"
            >
              {{ tag.trim() }}
            </span>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 词云图 -->
    <div v-if="sentimentData.wordCloudData && sentimentData.wordCloudData.length > 0" class="word-cloud-section">
      <h5>评论词云</h5>
      <div class="word-cloud-container">
        <div class="word-cloud">
          <span 
            v-for="(word, index) in sentimentData.wordCloudData.slice(0, 30)" 
            :key="index"
            class="word-item"
            :style="{
              fontSize: getWordFontSize(word.value) + 'px',
              color: getWordColor(index),
              opacity: getWordOpacity(word.value)
            }"
          >
            {{ word.name }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'SentimentAnalysis',
  props: {
    sentimentData: {
      type: Object,
      required: true,
      default: () => ({})
    }
  },
  methods: {
    getWordFontSize(value) {
      const wordCloudData = this.sentimentData.wordCloudData
      if (!wordCloudData || wordCloudData.length === 0) return 12
      const maxValue = Math.max(...wordCloudData.map(w => w.value))
      const minValue = Math.min(...wordCloudData.map(w => w.value))
      const range = maxValue - minValue || 1
      const normalized = (value - minValue) / range
      return 12 + normalized * 18
    },
    
    getWordColor(index) {
      const colors = [
        '#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399',
        '#1abc9c', '#3498db', '#9b59b6', '#e74c3c', '#2ecc71',
        '#f39c12', '#16a085', '#2980b9', '#8e44ad', '#c0392b'
      ]
      return colors[index % colors.length]
    },
    
    getWordOpacity(value) {
      const wordCloudData = this.sentimentData.wordCloudData
      if (!wordCloudData || wordCloudData.length === 0) return 0.6
      const maxValue = Math.max(...wordCloudData.map(w => w.value))
      const minValue = Math.min(...wordCloudData.map(w => w.value))
      const range = maxValue - minValue || 1
      const normalized = (value - minValue) / range
      return 0.6 + normalized * 0.4
    }
  }
}
</script>

<style scoped>
.sentiment-analysis-container {
  margin-top: 16px;
  padding: 16px;
  background: linear-gradient(135deg, #f5f7fa 0%, #ffffff 100%);
  border-radius: 12px;
  border: 1px solid #e4e7ed;
}

.analysis-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.analysis-header h4 {
  margin: 0;
  font-size: 16px;
  color: #303133;
}

.total-reviews {
  font-size: 12px;
  color: #909399;
}

.rating-distribution {
  margin-bottom: 16px;
}

.rating-distribution h5 {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #606266;
}

.rating-bars {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.rating-bar-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rating-label {
  min-width: 40px;
  font-size: 12px;
  color: #606266;
}

.rating-bar-wrapper {
  flex: 1;
  height: 8px;
  background-color: #f0f0f0;
  border-radius: 4px;
  overflow: hidden;
}

.rating-bar {
  height: 100%;
  background: linear-gradient(90deg, #ff9000, #ff5000);
  border-radius: 4px;
  transition: width 0.3s ease;
}

.rating-count {
  min-width: 80px;
  font-size: 11px;
  color: #909399;
  text-align: right;
}

.sentiment-distribution {
  margin-bottom: 16px;
}

.sentiment-distribution h5 {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #606266;
}

.sentiment-pie {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.sentiment-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #606266;
}

.sentiment-color {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.sentiment-value {
  font-weight: bold;
  color: #303133;
}

.hot-tags {
  margin-bottom: 16px;
}

.hot-tags h5 {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #606266;
}

.tags-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 12px;
  background-color: #fff;
  border-radius: 8px;
}

.tag-item {
  padding: 4px 8px;
  background-color: #fff5f0;
  color: #ff5000;
  border-radius: 12px;
  cursor: default;
  transition: all 0.2s ease;
}

.tag-item:hover {
  background-color: #ff5000;
  color: #fff;
}

.latest-reviews {
  margin-top: 16px;
}

.latest-reviews h5 {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #606266;
}

.reviews-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 210px;
  overflow-y: auto;
  padding-right: 8px;
}

.reviews-list::-webkit-scrollbar {
  width: 6px;
}

.reviews-list::-webkit-scrollbar-track {
  background: #f5f5f5;
  border-radius: 3px;
}

.reviews-list::-webkit-scrollbar-thumb {
  background: linear-gradient(135deg, #ff9000, #ff5000);
  border-radius: 3px;
}

.reviews-list::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(135deg, #ff5000, #ff3000);
}

.review-item {
  padding: 12px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.review-user {
  font-size: 12px;
  color: #606266;
}

.review-rating {
  display: flex;
  gap: 2px;
}

.review-rating .filled {
  color: #ff9000;
}

.review-content {
  font-size: 13px;
  color: #303133;
  line-height: 1.5;
  margin-bottom: 8px;
}

.review-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.review-tag {
  padding: 2px 8px;
  background-color: #f5f7fa;
  color: #909399;
  font-size: 11px;
  border-radius: 10px;
}

.word-cloud-section {
  margin-top: 16px;
}

.word-cloud-section h5 {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #606266;
}

.word-cloud-container {
  width: 100%;
  min-height: 150px;
  max-height: 200px;
  padding: 16px;
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
  border-radius: 8px;
  border: 1px solid #e4e7ed;
  overflow-y: auto;
}

.word-cloud-container::-webkit-scrollbar {
  width: 6px;
}

.word-cloud-container::-webkit-scrollbar-track {
  background: #f5f5f5;
  border-radius: 3px;
}

.word-cloud-container::-webkit-scrollbar-thumb {
  background: linear-gradient(135deg, #ff9000, #ff5000);
  border-radius: 3px;
}

.word-cloud-container::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(135deg, #ff5000, #ff3000);
}

.word-cloud {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  align-items: center;
  gap: 8px;
  min-height: 150px;
}

.word-item {
  display: inline-block;
  padding: 4px 8px;
  cursor: default;
  transition: all 0.3s ease;
  font-weight: 500;
  white-space: nowrap;
}

.word-item:hover {
  transform: scale(1.1);
  filter: brightness(1.2);
}
</style>
