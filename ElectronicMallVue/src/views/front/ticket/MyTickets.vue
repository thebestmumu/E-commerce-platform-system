<template>
  <div class="my-tickets-page">
    <!-- 顶部标题 -->
    <div class="page-header">
      <div class="header-back" @click="goBack">
        <span class="icon"></span>
      </div>
      <h2 class="header-title">我的工单</h2>
      <div class="header-placeholder"></div>
    </div>

    <!-- Tab 切换 -->
    <div class="tabs-container">
      <div 
        v-for="tab in tabs" 
        :key="tab.key"
        class="tab-item"
        :class="{ active: activeTab === tab.key }"
        @click="activeTab = tab.key; loadTickets()"
      >
        {{ tab.label }}
        <span class="tab-count" v-if="tab.count > 0">{{ tab.count }}</span>
      </div>
    </div>

    <!-- 工单列表 -->
    <div class="ticket-list" v-loading="loading">
      <div v-if="filteredTickets.length === 0 && !loading" class="empty-state">
        <div class="empty-icon"></div>
        <p class="empty-text">暂无工单</p>
      </div>

      <div 
        v-for="ticket in filteredTickets" 
        :key="ticket.id"
        class="ticket-card"
        @click="handleTicketClick(ticket)"
      >
        <div class="ticket-header">
          <span class="ticket-no">{{ ticket.ticketNo }}</span>
          <span class="ticket-status" :class="'status-' + getTicketStatus(ticket)">{{ getStatusText(ticket) }}</span>
        </div>
        
        <div class="ticket-body">
          <h4 class="ticket-subject">{{ ticket.subject }}</h4>
          <p class="ticket-desc">{{ ticket.description }}</p>
        </div>

        <div class="ticket-footer">
          <span class="ticket-time">{{ formatTime(ticket.createdAt) }}</span>
          <div class="ticket-actions">
            <!-- 进行中的工单显示"继续对话"按钮 -->
            <el-button 
              v-if="getTicketStatus(ticket) === 'processing'"
              size="mini" 
              type="primary"
              class="chat-btn"
              @click.stop="goToChat(ticket)"
            >
              继续对话
            </el-button>
            <!-- 未评价的工单显示"去评价"按钮 -->
            <el-button 
              v-if="getTicketStatus(ticket) === 'unrated'"
              size="mini" 
              class="rate-btn"
              @click.stop="goToRate(ticket)"
            >
              去评价
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'MyTickets',
  data() {
    return {
      activeTab: 'all',
      tabs: [
        { key: 'all', label: '全部', count: 0 },
        { key: 'processing', label: '进行中', count: 0 },
        { key: 'unrated', label: '未评价', count: 0 },
        { key: 'rated', label: '已评价', count: 0 }
      ],
      tickets: [],
      loading: false
    }
  },
  computed: {
    filteredTickets() {
      if (this.activeTab === 'all') return this.tickets
      return this.tickets.filter(t => this.getTicketStatus(t) === this.activeTab)
    }
  },
  created() {
    this.loadTickets()
  },
  methods: {
    goBack() {
      this.$router.back()
    },
    async loadTickets() {
      this.loading = true
      try {
        const userStr = localStorage.getItem('user')
        const user = userStr ? JSON.parse(userStr) : null
        
        const response = await request.get('/api/ticket/list', {
          headers: { userId: user?.id }
        })
        
        if (response.code === '200') {
          this.tickets = response.data || []
          this.updateTabCounts()
        }
      } catch (e) {
        console.error('加载工单列表失败:', e)
        this.$message.error('加载工单列表失败')
      } finally {
        this.loading = false
      }
    },
    updateTabCounts() {
      this.tabs[0].count = this.tickets.length
      this.tabs[1].count = this.tickets.filter(t => this.getTicketStatus(t) === 'processing').length
      this.tabs[2].count = this.tickets.filter(t => this.getTicketStatus(t) === 'unrated').length
      this.tabs[3].count = this.tickets.filter(t => this.getTicketStatus(t) === 'rated').length
    },
    getTicketStatus(ticket) {
      // 已评价
      if (ticket.satisfactionScore) return 'rated'
      // 进行中（待处理、处理中、已分配）
      if (['pending', 'processing', 'assigned'].includes(ticket.status)) return 'processing'
      // 未评价（已解决或已关闭但未评价）
      if (['resolved', 'closed'].includes(ticket.status)) return 'unrated'
      return 'processing'
    },
    getStatusText(ticket) {
      const status = this.getTicketStatus(ticket)
      const map = {
        'rated': '已评价',
        'processing': '进行中',
        'unrated': '待评价'
      }
      return map[status] || ticket.status
    },
    formatTime(time) {
      if (!time) return ''
      const date = new Date(time)
      const now = new Date()
      const diff = now - date
      const days = Math.floor(diff / (1000 * 60 * 60 * 24))
      
      if (days === 0) {
        const hours = String(date.getHours()).padStart(2, '0')
        const minutes = String(date.getMinutes()).padStart(2, '0')
        return `今天 ${hours}:${minutes}`
      } else if (days === 1) {
        return '昨天'
      } else if (days < 7) {
        return `${days}天前`
      } else {
        const month = date.getMonth() + 1
        const day = date.getDate()
        return `${month}月${day}日`
      }
    },
    handleTicketClick(ticket) {
      // 点击卡片默认行为
      if (this.getTicketStatus(ticket) === 'processing') {
        this.goToChat(ticket)
      } else if (this.getTicketStatus(ticket) === 'unrated') {
        this.goToRate(ticket)
      }
    },
    goToChat(ticket) {
      this.$router.push({
        path: '/user-chat',
        query: {
          ticketId: ticket.id,
          ticketNo: ticket.ticketNo
        }
      })
    },
    goToRate(ticket) {
      this.$router.push({
        path: '/user-chat',
        query: {
          ticketId: ticket.id,
          ticketNo: ticket.ticketNo,
          showRating: 'true'
        }
      })
    }
  }
}
</script>

<style scoped>
.my-tickets-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 60px;
}

/* 顶部导航 */
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

/* Tab 切换 */
.tabs-container {
  display: flex;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  position: sticky;
  top: 50px;
  z-index: 99;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 14px 0;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  position: relative;
  transition: all 0.2s;
}

.tab-item.active {
  color: #ff5000;
  font-weight: 600;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 24px;
  height: 3px;
  background: linear-gradient(90deg, #ff9000, #ff5000);
  border-radius: 2px;
}

.tab-count {
  display: inline-block;
  min-width: 18px;
  height: 18px;
  line-height: 18px;
  padding: 0 5px;
  font-size: 11px;
  color: #fff;
  background: #ff5000;
  border-radius: 9px;
  margin-left: 4px;
}

.tab-item.active .tab-count {
  background: linear-gradient(135deg, #ff9000, #ff5000);
}

/* 工单列表 */
.ticket-list {
  padding: 12px;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
}

.empty-icon {
  width: 120px;
  height: 120px;
  margin: 0 auto 16px;
  background: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 120 120'%3E%3Ccircle cx='60' cy='60' r='50' fill='%23f5f5f5'/%3E%3Ctext x='60' y='70' text-anchor='middle' font-size='40'%3E📋%3C/text%3E%3C/svg%3E") center/contain no-repeat;
}

.empty-text {
  font-size: 14px;
  color: #999;
}

/* 工单卡片 */
.ticket-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: all 0.2s;
}

.ticket-card:hover {
  box-shadow: 0 4px 16px rgba(255, 80, 0, 0.12);
  transform: translateY(-1px);
}

.ticket-card:active {
  transform: scale(0.99);
}

.ticket-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.ticket-no {
  font-size: 12px;
  color: #999;
}

.ticket-status {
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 10px;
}

.status-processing {
  color: #ff5000;
  background: #fff5f0;
}

.status-unrated {
  color: #e6a23c;
  background: #fdf6ec;
}

.status-rated {
  color: #67c23a;
  background: #f0f9eb;
}

.ticket-body {
  margin-bottom: 12px;
}

.ticket-subject {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ticket-desc {
  font-size: 13px;
  color: #666;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.5;
}

.ticket-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f5f5f5;
}

.ticket-time {
  font-size: 12px;
  color: #999;
}

.ticket-actions {
  display: flex;
  gap: 8px;
}

.chat-btn {
  border-radius: 16px !important;
  padding: 6px 16px !important;
  font-size: 12px !important;
  background: linear-gradient(135deg, #ff9000 0%, #ff5000 100%) !important;
  border: none !important;
}

.rate-btn {
  border-radius: 16px !important;
  padding: 6px 16px !important;
  font-size: 12px !important;
  border: 1px solid #ff9000 !important;
  color: #ff5000 !important;
  background: #fff !important;
}
</style>
