<template>
  <div class="service-container">
    <!-- 侧边栏 -->
    <div class="sidebar">
      <div class="sidebar-header">
        <h2>客服工作台</h2>
      </div>
      <div class="sidebar-menu">
        <div 
          class="menu-item active" 
          @click="currentView = 'tickets'"
        >
          <span class="menu-icon">📋</span>
          <span>工单处理</span>
        </div>
        <div 
          class="menu-item" 
          @click="$router.push('/service/chat')"
        >
          <span class="menu-icon">💬</span>
          <span>在线客服</span>
        </div>
        <div 
          class="menu-item" 
          @click="currentView = 'history'"
        >
          <span class="menu-icon">📜</span>
          <span>历史记录</span>
        </div>
        <div 
          class="menu-item" 
          @click="currentView = 'stats'"
        >
          <span class="menu-icon">📊</span>
          <span>数据统计</span>
        </div>
      </div>
      <div class="sidebar-footer">
        <div class="service-info">
          <div class="service-avatar">{{ (serviceUser.nickname || serviceUser.username || '客').charAt(0) }}</div>
          <div class="service-name">{{ serviceUser.nickname || serviceUser.username }}</div>
        </div>
        <button class="logout-btn" @click="logout">退出登录</button>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="main-content">
      <!-- 工单处理 -->
      <div v-if="currentView === 'tickets'" class="tickets-view">
        <div class="page-header">
          <h1>工单处理</h1>
          <div class="header-actions">
            <el-button 
              :type="currentTab === 'pending' ? 'primary' : 'info'" 
              @click="currentTab = 'pending'"
            >
              待处理 ({{ pendingCount }})
            </el-button>
            <el-button 
              :type="currentTab === 'assigned' ? 'primary' : 'info'"
              @click="currentTab = 'assigned'"
            >
              已分配 ({{ assignedCount }})
            </el-button>
          </div>
        </div>

        <!-- 工单列表 -->
        <div class="ticket-list">
          <div 
            v-for="ticket in currentTickets" 
            :key="ticket.id"
            class="ticket-item"
            @click="viewTicketDetail(ticket)"
          >
            <div class="ticket-header">
              <div class="ticket-info">
                <h3 class="ticket-title">{{ ticket.subject }}</h3>
                <p class="ticket-desc">{{ ticket.description }}</p>
              </div>
              <div class="ticket-meta">
                <el-tag :type="getStatusType(ticket.status)" size="small">
                  {{ getStatusText(ticket.status) }}
                </el-tag>
                <span class="ticket-time">{{ formatTime(ticket.createdAt) }}</span>
              </div>
            </div>
            <div class="ticket-footer">
              <span class="ticket-no">工单号：{{ ticket.ticketNo }}</span>
              <span class="ticket-category">{{ getCategoryText(ticket.category) }}</span>
            </div>
          </div>

          <div v-if="currentTickets.length === 0" class="empty-state">
            <div class="empty-icon">📭</div>
            <p class="empty-text">
              {{ currentTab === 'pending' ? '暂无待处理工单' : '暂无已分配工单' }}
            </p>
          </div>
        </div>
      </div>

      <!-- 工单详情 -->
      <div v-if="currentView === 'detail'" class="ticket-detail-view">
        <div class="detail-header">
          <el-button icon="el-icon-arrow-left" @click="currentView = 'tickets'">返回</el-button>
          <h1>工单详情</h1>
        </div>

        <div v-if="currentTicket" class="detail-content">
          <div class="detail-section">
            <h3>工单信息</h3>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="工单编号">{{ currentTicket.ticketNo }}</el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag :type="getStatusType(currentTicket.status)">
                  {{ getStatusText(currentTicket.status) }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="分类">{{ getCategoryText(currentTicket.category) }}</el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ formatTime(currentTicket.createdAt) }}</el-descriptions-item>
              <el-descriptions-item label="用户 ID">{{ currentTicket.userId }}</el-descriptions-item>
              <el-descriptions-item label="处理客服">
                {{ currentTicket.serviceId ? '客服 #' + currentTicket.serviceId : '未分配' }}
              </el-descriptions-item>
            </el-descriptions>
          </div>

          <div class="detail-section">
            <h3>问题描述</h3>
            <div class="description-content">
              {{ currentTicket.description }}
            </div>
          </div>

          <div class="detail-section">
            <h3>处理操作</h3>
            <div class="action-buttons">
              <el-button 
                v-if="currentTicket.status === 'pending'"
                type="primary" 
                @click="claimTicket(currentTicket.id)"
              >
                抢单处理
              </el-button>
              <el-button 
                v-if="currentTicket.status === 'assigned' && currentTicket.serviceId === serviceUser.userId"
                type="success" 
                @click="startProcessing(currentTicket.id)"
              >
                开始处理
              </el-button>
              <el-button 
                v-if="currentTicket.status === 'processing'"
                type="success" 
                @click="completeTicket(currentTicket.id)"
              >
                完成处理
              </el-button>
              <el-button @click="currentView = 'tickets'">返回列表</el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 历史记录 -->
      <div v-if="currentView === 'history'" class="history-view">
        <div class="page-header">
          <h1>历史记录</h1>
        </div>
        <div class="ticket-list">
          <div 
            v-for="ticket in historyTickets" 
            :key="ticket.id"
            class="ticket-item"
            @click="viewTicketDetail(ticket)"
          >
            <div class="ticket-header">
              <div class="ticket-info">
                <h3 class="ticket-title">{{ ticket.subject }}</h3>
                <p class="ticket-desc">{{ ticket.description }}</p>
              </div>
              <div class="ticket-meta">
                <el-tag :type="getStatusType(ticket.status)" size="small">
                  {{ getStatusText(ticket.status) }}
                </el-tag>
                <span class="ticket-time">{{ formatTime(ticket.completedAt || ticket.updatedAt) }}</span>
              </div>
            </div>
            <div class="ticket-footer">
              <span class="ticket-no">工单号：{{ ticket.ticketNo }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 数据统计 -->
      <div v-if="currentView === 'stats'" class="stats-view">
        <div class="page-header">
          <h1>数据统计</h1>
        </div>
        <div class="stats-cards">
          <div class="stat-card">
            <div class="stat-value">{{ pendingCount }}</div>
            <div class="stat-label">待处理工单</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ assignedCount }}</div>
            <div class="stat-label">已分配工单</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ historyCount }}</div>
            <div class="stat-label">已完成工单</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ServiceTicket',
  data() {
    return {
      serviceUser: {},
      currentView: 'tickets',
      currentTab: 'pending',
      currentTicket: null,
      pendingTickets: [],
      assignedTickets: [],
      historyTickets: [],
      loading: false,
      pollingTimer: null,  // 轮询定时器
      pollingInterval: 5000  // 轮询间隔：5 秒
    }
  },
  computed: {
    currentTickets() {
      return this.currentTab === 'pending' ? this.pendingTickets : this.assignedTickets
    },
    pendingCount() {
      return this.pendingTickets.length
    },
    assignedCount() {
      return this.assignedTickets.length
    },
    historyCount() {
      return this.historyTickets.length
    }
  },
  created() {
    this.checkLogin()
    this.loadTickets()
    this.startPolling()  // 启动轮询
  },
  beforeDestroy() {
    // 组件销毁时清除定时器
    this.stopPolling()
  },
  methods: {
    checkLogin() {
      const serviceUserStr = localStorage.getItem('serviceUser')
      if (!serviceUserStr) {
        this.$message.error('请先登录客服账号')
        this.$router.push('/service-login')
        return
      }
      
      try {
        this.serviceUser = JSON.parse(serviceUserStr)
        console.log('当前客服:', this.serviceUser)
      } catch (e) {
        console.error('解析客服信息失败:', e)
        this.$message.error('登录信息无效')
        this.$router.push('/service-login')
      }
    },
    
    async loadTickets() {
      this.loading = true
      try {
        const serviceId = this.serviceUser.userId
        
        // 加载待处理工单
        const pendingRes = await this.request.get('/api/service/ticket/pending', {
          headers: {
            'serviceId': serviceId
          }
        })
        if (pendingRes.code === '200') {
          this.pendingTickets = pendingRes.data || []
        }
        
        // 加载已分配工单
        const assignedRes = await this.request.get('/api/service/ticket/assigned', {
          headers: {
            'serviceId': serviceId
          }
        })
        if (assignedRes.code === '200') {
          this.assignedTickets = assignedRes.data || []
        }
        
        // 加载历史记录
        const historyRes = await this.request.get('/api/service/ticket/history', {
          headers: {
            'serviceId': serviceId
          }
        })
        if (historyRes.code === '200') {
          this.historyTickets = historyRes.data || []
        }
      } catch (e) {
        // 静默失败，不显示错误提示，避免频繁弹窗
        console.error('加载工单失败:', e)
      } finally {
        this.loading = false
      }
    },
    
    // 启动轮询
    startPolling() {
      if (this.pollingTimer) {
        clearInterval(this.pollingTimer)
      }
      this.pollingTimer = setInterval(() => {
        console.log('自动刷新工单列表...')
        this.loadTickets()
      }, this.pollingInterval)
    },
    
    // 停止轮询
    stopPolling() {
      if (this.pollingTimer) {
        clearInterval(this.pollingTimer)
        this.pollingTimer = null
      }
    },
    
    viewTicketDetail(ticket) {
      this.currentTicket = ticket
      this.currentView = 'detail'
    },
    
    async claimTicket(ticketId) {
      try {
        const serviceId = this.serviceUser.userId
        const res = await this.request.post(`/api/service/ticket/claim/${ticketId}`, null, {
          headers: {
            'serviceId': serviceId
          }
        })
        if (res.code === '200') {
          this.$message.success('抢单成功')
          this.loadTickets()
          this.currentView = 'tickets'
        } else {
          this.$message.error(res.msg || '抢单失败')
        }
      } catch (e) {
        console.error('抢单失败:', e)
        this.$message.error('抢单失败')
      }
    },
    
    async startProcessing(ticketId) {
      try {
        const serviceId = this.serviceUser.userId
        const res = await this.request.post(`/api/service/ticket/process/${ticketId}`, null, {
          headers: {
            'serviceId': serviceId
          }
        })
        if (res.code === '200') {
          this.$message.success('开始处理')
          this.loadTickets()
        } else {
          this.$message.error(res.msg || '操作失败')
        }
      } catch (e) {
        console.error('开始处理失败:', e)
        this.$message.error('操作失败')
      }
    },
    
    async completeTicket(ticketId) {
      try {
        const serviceId = this.serviceUser.userId
        const res = await this.request.post(`/api/service/ticket/complete/${ticketId}`, null, {
          headers: {
            'serviceId': serviceId
          }
        })
        if (res.code === '200') {
          this.$message.success('工单已完成')
          this.loadTickets()
          this.currentView = 'tickets'
        } else {
          this.$message.error(res.msg || '操作失败')
        }
      } catch (e) {
        console.error('完成工单失败:', e)
        this.$message.error('操作失败')
      }
    },
    
    getStatusType(status) {
      const types = {
        'pending': 'warning',
        'assigned': 'primary',
        'processing': 'success',
        'completed': 'info'
      }
      return types[status] || 'info'
    },
    
    getStatusText(status) {
      const texts = {
        'pending': '待处理',
        'assigned': '已分配',
        'processing': '处理中',
        'completed': '已完成'
      }
      return texts[status] || status
    },
    
    getCategoryText(category) {
      const texts = {
        'technical': '技术问题',
        'billing': '账单问题',
        'product': '商品问题',
        'complaint': '投诉建议',
        'other': '其他'
      }
      return texts[category] || category
    },
    
    formatTime(timeStr) {
      if (!timeStr) return '-'
      const date = new Date(timeStr)
      return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      })
    },
    
    logout() {
      this.stopPolling()  // 停止轮询
      this.$confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        localStorage.removeItem('serviceUser')
        this.$router.push('/service-login')
      }).catch(() => {})
    }
  },
  // 监听标签页切换
  watch: {
    currentTab() {
      // 切换标签页时重新加载工单
      this.loadTickets()
    },
    currentView() {
      // 切换视图时控制轮询
      if (this.currentView === 'tickets') {
        this.startPolling()
      } else {
        this.stopPolling()
      }
    }
  }
}
</script>

<style scoped>
.service-container {
  display: flex;
  height: 100vh;
  background: #f5f7fa;
}

/* 侧边栏 - 淘宝橙色主题 */
.sidebar {
  width: 240px;
  background: linear-gradient(180deg, #ff9000 0%, #ff5000 100%);
  color: white;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 24px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.sidebar-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.sidebar-menu {
  flex: 1;
  padding: 20px 0;
}

.menu-item {
  padding: 12px 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  transition: all 0.3s ease;
}

.menu-item:hover {
  background: rgba(255, 255, 255, 0.1);
}

.menu-item.active {
  background: rgba(255, 255, 255, 0.2);
  border-left: 3px solid #fff;
}

.menu-icon {
  margin-right: 12px;
  font-size: 18px;
}

.sidebar-footer {
  padding: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.service-info {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}

.service-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  font-weight: 600;
}

.service-name {
  font-size: 14px;
}

.logout-btn {
  width: 100%;
  padding: 8px;
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: white;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.logout-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

/* 主内容区 */
.main-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0;
  font-size: 24px;
  color: #333;
}

.header-actions {
  display: flex;
  gap: 12px;
}

/* 工单列表 */
.ticket-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.ticket-item {
  background: white;
  border-radius: 8px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.ticket-item:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.ticket-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;
}

.ticket-title {
  margin: 0 0 8px 0;
  font-size: 16px;
  color: #333;
}

.ticket-desc {
  margin: 0;
  font-size: 14px;
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.ticket-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.ticket-time {
  font-size: 12px;
  color: #999;
}

.ticket-footer {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #999;
}

/* 工单详情 */
.detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.detail-header h1 {
  margin: 0;
  font-size: 24px;
}

.detail-content {
  background: white;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.detail-section {
  margin-bottom: 24px;
}

.detail-section h3 {
  margin: 0 0 16px 0;
  font-size: 16px;
  color: #333;
  border-bottom: 2px solid #ff5000;
  padding-bottom: 8px;
}

.description-content {
  background: #f5f7fa;
  padding: 16px;
  border-radius: 4px;
  line-height: 1.6;
  color: #333;
}

.action-buttons {
  display: flex;
  gap: 12px;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.empty-text {
  font-size: 16px;
  margin: 0;
}

/* 统计卡片 */
.stats-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 24px;
}

.stat-card {
  background: white;
  border-radius: 8px;
  padding: 24px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.stat-value {
  font-size: 48px;
  font-weight: 600;
  color: #ff5000;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #666;
}
</style>
