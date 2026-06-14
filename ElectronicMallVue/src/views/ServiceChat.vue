<template>
  <div class="service-chat-container">
    <!-- 左侧会话列表 -->
    <div class="chat-sidebar">
      <div class="sidebar-header">
        <h3>我的会话</h3>
        <div class="service-info">
          <span class="service-name">{{ serviceUser.nickname || serviceUser.username }}</span>
          <span class="online-badge">在线</span>
        </div>
      </div>
      
      <div class="chat-tabs">
        <button 
          :class="['tab', currentTab === 'active' ? 'active' : '']"
          @click="currentTab = 'active'">
          进行中 ({{ activeChats.length }})
        </button>
        <button 
          :class="['tab', currentTab === 'history' ? 'active' : '']"
          @click="currentTab = 'history'">
          历史记录
        </button>
      </div>
      
      <div class="chat-list">
        <div 
          v-for="chat in currentTab === 'active' ? activeChats : historyChats" 
          :key="chat.ticketId"
          :class="['chat-item', chat.ticketId === currentChatId ? 'active' : '']"
          @click="selectChat(chat)">
          <div class="chat-avatar">
            <img src="https://img.alicdn.com/tfs/TB1kxQzb.T1gK0jSZFrXXcNCXXa-64-64.png" alt="用户" class="avatar-img">
          </div>
          <div class="chat-info">
            <div class="chat-header">
              <span class="chat-username">{{ chat.userName || ('用户 ' + chat.userId) }}</span>
              <span class="chat-time">{{ formatTime(chat.lastTime) }}</span>
            </div>
            <div class="chat-preview">
              <span :class="['preview-text', !chat.lastRead ? 'unread' : '']">
                {{ chat.subject || chat.lastMessage || '暂无消息' }}
              </span>
              <el-badge :value="chat.unreadCount || 0" :hidden="!chat.unreadCount" class="chat-badge"></el-badge>
            </div>
          </div>
        </div>
        
        <div v-if="currentTab === 'active' && activeChats.length === 0" class="empty-state">
          <div class="empty-icon">💬</div>
          <p>暂无进行中的会话</p>
          <p class="empty-tips">系统会自动分配排队用户</p>
        </div>
      </div>
    </div>

    <!-- 右侧聊天区域 -->
    <div class="chat-main">
      <div v-if="currentChatId" class="chat-window">
        <!-- 聊天头部 -->
        <div class="chat-window-header">
          <div class="chat-user-info">
            <h4>{{ currentChat.userName || ('用户 ' + currentChat.userId) }}</h4>
            <span class="chat-status">工单号：{{ currentChat.ticketNo }}</span>
            <span class="chat-subject" v-if="currentChat.subject">{{ currentChat.subject }}</span>
          </div>
          <div class="chat-actions">
            <button class="action-btn" @click="endCurrentChat">结束对话</button>
          </div>
        </div>

        <!-- 消息列表 -->
        <div class="chat-window-messages" ref="messageContainer">
          <div v-for="msg in currentMessages" :key="msg.id" 
               :class="['message', msg.senderRole === 'service' ? 'message-right' : 'message-left']">
            <div class="avatar">
              <img :src="serviceAvatar" alt="客服" class="avatar-img">
            </div>
            <div class="message-content">
              <div class="message-sender" v-if="msg.senderRole === 'user'">
                用户
              </div>
              <div :class="['message-bubble', msg.messageType]">
                <!-- 文本消息 -->
                <p v-if="msg.messageType === 'text'">{{ msg.content }}</p>
                <!-- 订单卡片消息 -->
                <div v-else-if="msg.messageType === 'order_card'" class="order-card" @click="showOrderDetail(msg.orderData)">
                  <div class="order-card-header">
                    <span class="order-icon">📦</span>
                    <span class="order-card-title">订单卡片</span>
                  </div>
                  <div class="order-card-body">
                    <img 
                      :src="getOrderImage(msg.orderData.imgs)" 
                      class="order-card-img" 
                      alt=""
                      @error="handleImageError"
                    >
                    <div class="order-card-info">
                      <div class="order-no">订单号：{{ msg.orderData.orderNo }}</div>
                      <div class="order-product">{{ msg.orderData.goodName || '商品' }}</div>
                      <div class="order-price">¥{{ msg.orderData.totalPrice || 0 }}</div>
                    </div>
                  </div>
                  <div class="order-card-footer">
                    <span class="order-status" :class="'status-' + getStatusNum(msg.orderData.state)">{{ msg.orderData.state }}</span>
                    <span class="order-view">点击查看详情 ></span>
                  </div>
                </div>
              </div>
              <div class="message-time">
                {{ formatTime(msg.createdAt) }}
              </div>
            </div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="chat-window-input">
          <div class="input-tools">
            <button class="tool-btn" title="发送图片">📷</button>
            <button class="tool-btn" title="发送文件">📁</button>
            <button class="tool-btn" title="发送表情">😊</button>
            <button class="tool-btn order-btn" title="发送订单" @click="openOrderSelector">📦</button>
          </div>
          <div class="input-wrapper">
            <textarea 
              v-model="messageInput" 
              placeholder="输入消息..." 
              @keydown.enter.exact.prevent="sendMessage"
              rows="3"
            ></textarea>
            <button class="send-btn" @click="sendMessage" :disabled="!messageInput.trim()">
              发送 (Enter)
            </button>
          </div>
        </div>
      </div>
      
      <div v-else class="no-chat-selected">
        <div class="no-chat-icon">💬</div>
        <p>请选择一个会话</p>
      </div>
    </div>

    <!-- 订单选择弹窗 -->
    <el-dialog 
      :visible.sync="showOrderSelector" 
      title="选择订单" 
      width="500px"
      custom-class="order-selector-dialog"
    >
      <div class="order-selector-content">
        <div v-if="orderLoading" class="loading-text">加载中...</div>
        <div v-else-if="myOrders.length === 0" class="empty-text">暂无订单</div>
        <div v-else class="order-list">
          <div 
            v-for="order in myOrders" 
            :key="order.id" 
            class="order-item"
            @click="selectOrder(order)"
          >
            <div class="order-item-header">
              <span class="order-item-no">{{ order.orderNo }}</span>
              <span class="order-item-status" :class="'status-' + getStatusNum(order.state)">{{ order.state }}</span>
            </div>
            <div class="order-item-body">
              <img 
                :src="getOrderImage(order.imgs)" 
                class="order-item-img" 
                alt=""
                @error="handleImageError"
              >
              <div class="order-item-info">
                <div class="order-item-name">{{ order.goodName || '商品' }}</div>
                <div class="order-item-price">¥{{ order.totalPrice || 0 }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 订单详情弹窗 -->
    <el-dialog 
      :visible.sync="showOrderDetailDialog" 
      title="订单详情" 
      width="600px"
      custom-class="order-detail-dialog"
    >
      <div v-if="selectedOrder" class="order-detail-content">
        <div class="detail-header">
          <div class="detail-order-no">订单号：{{ selectedOrder.orderNo }}</div>
          <div class="detail-status" :class="'status-' + getStatusNum(selectedOrder.state)">{{ selectedOrder.state }}</div>
        </div>
        
        <div class="detail-product">
          <img 
            :src="getOrderImage(selectedOrder.imgs)" 
            class="detail-product-img" 
            alt=""
            @error="handleImageError"
          >
          <div class="detail-product-info">
            <div class="detail-product-name">{{ selectedOrder.goodName || '商品' }}</div>
            <div class="detail-product-spec">{{ selectedOrder.standard || '标准版' }}</div>
            <div class="detail-product-price">
              <span class="price-label">单价：</span>
              <span class="price-value">¥{{ selectedOrder.price }}</span>
            </div>
            <div class="detail-product-qty">
              <span class="qty-label">数量：</span>
              <span class="qty-value">{{ selectedOrder.count || 1 }}</span>
            </div>
          </div>
        </div>
        
        <div class="detail-info">
          <div class="info-row">
            <span class="info-label">订单金额：</span>
            <span class="info-value price-highlight">¥{{ selectedOrder.totalPrice }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">下单时间：</span>
            <span class="info-value">{{ selectedOrder.createTime }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">收货人：</span>
            <span class="info-value">{{ selectedOrder.linkUser || '暂无' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">联系电话：</span>
            <span class="info-value">{{ selectedOrder.linkPhone || '暂无' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">收货地址：</span>
            <span class="info-value">{{ selectedOrder.linkAddress || '暂无' }}</span>
          </div>
          <div class="info-row" v-if="selectedOrder.deliveryAddress">
            <span class="info-label">发货地址：</span>
            <span class="info-value">{{ selectedOrder.deliveryAddress }}</span>
          </div>
          <div class="info-row" v-if="selectedOrder.expressCompany">
            <span class="info-label">物流公司：</span>
            <span class="info-value">{{ selectedOrder.expressCompany }}</span>
          </div>
          <div class="info-row" v-if="selectedOrder.expressNo">
            <span class="info-label">物流单号：</span>
            <span class="info-value">{{ selectedOrder.expressNo }}</span>
          </div>
        </div>
        
        <!-- 物流地图 -->
        <div v-if="selectedOrder.linkAddress && selectedOrder.deliveryAddress" class="detail-logistics-map">
          <h4 class="map-section-title">📍 物流路线</h4>
          <div :id="'detailMap_' + selectedOrder.orderNo" class="detail-baidu-map"></div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'ServiceChat',
  data() {
    return {
      // WebSocket
      websocket: null,
      
      // 当前标签页
      currentTab: 'active',
      
      // 会话列表
      activeChats: [],
      historyChats: [],
      
      // 当前选中的会话
      currentChatId: null,
      currentChat: null,
      
      // 消息列表
      messages: {},
      
      // 输入
      messageInput: '',
      
      // 服务信息
      serviceUser: {},
      serviceAvatar: 'https://img.alicdn.com/tfs/TB1kxQzb.T1gK0jSZFrXXcNCXXa-64-64.png',
      
      // API 基础地址
      baseApi: 'http://localhost:9191',
      
      // 订单相关
      showOrderSelector: false,
      showOrderDetailDialog: false,
      myOrders: [],
      selectedOrder: null,
      orderLoading: false
    }
  },
  
  computed: {
    currentMessages() {
      return this.currentChatId ? (this.messages[this.currentChatId] || []) : []
    }
  },
  
  created() {
    this.initServiceUser()
    this.connectWebSocket()
  },
  
  beforeDestroy() {
    if (this.websocket) {
      this.websocket.close()
    }
  },
  
  methods: {
    // 初始化客服信息
    initServiceUser() {
      const serviceUserStr = localStorage.getItem('serviceUser')
      if (serviceUserStr) {
        this.serviceUser = JSON.parse(serviceUserStr)
      } else {
        this.$message.error('请先登录')
        this.$router.push('/service-login')
      }
    },
    
    // 连接 WebSocket
    connectWebSocket() {
      const serviceUserStr = localStorage.getItem('serviceUser')
      if (!serviceUserStr) {
        return
      }
      
      const serviceUser = JSON.parse(serviceUserStr)
      const token = serviceUser.token
      
      const wsUrl = `ws://localhost:9191/ws/chat?token=${token}&role=service`
      
      this.websocket = new WebSocket(wsUrl)
      
      this.websocket.onopen = () => {
        console.log('WebSocket 连接成功')
      }
      
      this.websocket.onmessage = (event) => {
        const data = JSON.parse(event.data)
        this.handleMessage(data)
      }
      
      this.websocket.onerror = (error) => {
        console.error('WebSocket 错误:', error)
      }
      
      this.websocket.onclose = () => {
        console.log('WebSocket 连接关闭')
      }
    },
    
    // 处理消息
    handleMessage(data) {
      console.log('收到消息:', data)
      
      switch (data.type) {
        case 'new_chat':
          // 新聊天
          this.handleNewChat(data)
          break
          
        case 'chat':
          // 聊天消息
          this.handleChatMessage(data)
          break
          
        case 'chat_ended':
          // 聊天结束
          this.handleChatEnded(data)
          break
          
        case 'chat_sent':
          // 消息发送确认
          console.log('消息发送成功:', data)
          break
      }
    },
    
    // 处理新聊天
    async handleNewChat(data) {
      // 获取用户名
      let userName = '用户' + data.userId
      try {
        const res = await this.request.get(`/api/user/info/${data.userId}`)
        if (res.code === '200' && res.data) {
          userName = res.data.nickname || res.data.username || userName
        }
      } catch (e) {
        console.error('获取用户名失败:', e)
      }
      
      const chat = {
        ticketId: data.ticketId,
        ticketNo: 'TKT-' + data.ticketId,
        userId: data.userId,
        userName: userName,
        roomId: data.roomId,
        subject: data.subject || '',
        description: data.description || data.subject || '',
        lastMessage: data.description || data.subject || '新会话',
        lastTime: Date.now(),
        unreadCount: 0
      }
      
      // 添加到活动列表
      const existingIndex = this.activeChats.findIndex(c => c.ticketId === data.ticketId)
      if (existingIndex >= 0) {
        this.activeChats[existingIndex] = chat
      } else {
        this.activeChats.unshift(chat)
      }
      
      // 初始化消息列表
      this.messages[data.ticketId] = []
      
      // 播放提示音
      this.playNotification()
      
      this.$message.success(`新会话：${userName}`)
    },
    
    // 处理聊天消息
    handleChatMessage(data) {
      const ticketId = data.ticketId
      
      // 添加到消息列表
      if (!this.messages[ticketId]) {
        this.messages[ticketId] = []
      }
      
      const msg = {
        id: data.messageId,
        senderId: data.senderId,
        senderRole: data.senderRole,
        content: data.content,
        messageType: data.messageType,
        createdAt: data.timestamp
      }
      
      // 如果是订单卡片，解析 content 中的订单数据
      if (data.messageType === 'order_card' && data.content) {
        try {
          msg.orderData = typeof data.content === 'string' ? JSON.parse(data.content) : data.content
          console.log('订单卡片数据解析成功:', msg.orderData)
          console.log('收货地址:', msg.orderData.linkAddress, '发货地址:', msg.orderData.deliveryAddress)
        } catch (e) {
          console.error('解析订单卡片数据失败:', e)
        }
      }
      
      this.messages[ticketId].push(msg)
      
      // 更新会话列表
      const chat = this.activeChats.find(c => c.ticketId === ticketId)
      if (chat) {
        chat.lastMessage = data.content
        chat.lastTime = data.timestamp
        
        // 如果当前不是这个会话，增加未读数
        if (this.currentChatId !== ticketId && data.senderRole === 'user') {
          chat.unreadCount = (chat.unreadCount || 0) + 1
        }
      }
      
      // 如果当前正在查看这个会话，滚动到底部
      if (this.currentChatId === ticketId) {
        this.scrollToBottom()
      }
    },
    
    // 处理聊天结束
    handleChatEnded(data) {
      const ticketId = data.ticketId
      
      // 从活动列表移除
      const index = this.activeChats.findIndex(c => c.ticketId === ticketId)
      if (index >= 0) {
        const chat = this.activeChats.splice(index, 1)[0]
        // 添加到历史列表
        this.historyChats.unshift(chat)
      }
      
      // 如果当前正在查看这个会话
      if (this.currentChatId === ticketId) {
        this.$message.info('对话已结束')
        this.currentChatId = null
        this.currentChat = null
      }
    },
    
    // 选择会话
    selectChat(chat) {
      this.currentChatId = chat.ticketId
      this.currentChat = chat
      
      // 清除未读数
      chat.unreadCount = 0
      
      // 滚动到底部
      this.$nextTick(() => {
        this.scrollToBottom()
      })
    },
    
    // 发送消息
    sendMessage() {
      if (!this.messageInput.trim() || !this.currentChatId) return
      
      const content = this.messageInput.trim()
      
      // 添加到消息列表
      const tempMsg = {
        id: Date.now(),
        senderId: this.serviceUser.userId,
        senderRole: 'service',
        content: content,
        messageType: 'text',
        createdAt: Date.now()
      }
      
      if (!this.messages[this.currentChatId]) {
        this.messages[this.currentChatId] = []
      }
      this.messages[this.currentChatId].push(tempMsg)
      this.scrollToBottom()
      
      // 通过 WebSocket 发送
      if (this.websocket && this.websocket.readyState === WebSocket.OPEN) {
        const message = {
          type: 'chat',
          ticketId: this.currentChatId,
          content: content,
          messageType: 'text'
        }
        this.websocket.send(JSON.stringify(message))
      }
      
      this.messageInput = ''
    },
    
    // 结束当前聊天
    endCurrentChat() {
      if (!this.currentChatId) return
      
      this.$confirm('确定要结束对话吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        if (this.websocket && this.websocket.readyState === WebSocket.OPEN) {
          const message = {
            type: 'end_chat',
            ticketId: this.currentChatId
          }
          this.websocket.send(JSON.stringify(message))
        }
      }).catch(() => {})
    },
    
    // 滚动到底部
    scrollToBottom() {
      this.$nextTick(() => {
        const container = this.$refs.messageContainer
        if (container) {
          container.scrollTop = container.scrollHeight
        }
      })
    },
    
    // 格式化时间
    formatTime(timestamp) {
      if (!timestamp) return ''
      const date = new Date(timestamp)
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      return `${hours}:${minutes}`
    },
    
    // 播放提示音
    playNotification() {
      // 可以添加提示音
      console.log('播放提示音')
    },
    
    // 打开订单选择器
    async openOrderSelector() {
      if (!this.currentChatId || !this.currentChat) {
        this.$message.warning('请先选择一个会话')
        return
      }
      
      this.showOrderSelector = true
      this.orderLoading = true
      try {
        // 获取当前聊天用户的订单
        const userId = this.currentChat.userId
        const res = await this.request.get(`/api/order/userid/${userId}`)
        if (res.code === '200') {
          this.myOrders = res.data || []
        }
      } catch (e) {
        console.error('获取用户订单列表失败:', e)
        this.$message.error('获取订单列表失败')
      } finally {
        this.orderLoading = false
      }
    },
    
    // 选择订单并发送
    selectOrder(order) {
      this.showOrderSelector = false
      this.sendOrderCard(order)
    },
    
    // 发送订单卡片
    sendOrderCard(order) {
      if (!this.currentChatId) {
        this.$message.warning('请先选择一个会话')
        return
      }
      
      // 后端返回的字段名
      const orderCard = {
        tempId: Date.now(),
        senderId: this.serviceUser.id,
        senderRole: 'service',
        senderName: '客服',
        content: '',
        messageType: 'order_card',
        orderData: {
          id: order.id,
          orderNo: order.orderNo,
          goodName: order.goodName || '商品',
          imgs: order.imgs || '',
          totalPrice: order.totalPrice || 0,
          price: order.goodPrice || order.price || 0,
          count: order.count || 1,
          standard: order.standard || '标准版',
          state: order.state || '待付款',
          createTime: order.createTime,
          linkUser: order.linkUser || '',
          linkPhone: order.linkPhone || '',
          linkAddress: order.linkAddress || '',
          deliveryAddress: order.deliveryAddress || '',
          expressCompany: order.expressCompany || '',
          expressNo: order.expressNo || ''
        },
        timestamp: Date.now(),
        isRead: false
      }
      
      // 添加到当前会话消息
      if (!this.messages[this.currentChatId]) {
        this.messages[this.currentChatId] = []
      }
      this.messages[this.currentChatId].push(orderCard)
      this.scrollToBottom()
      
      // 通过 WebSocket 发送
      if (this.websocket && this.websocket.readyState === WebSocket.OPEN) {
        const message = {
          type: 'chat',
          ticketId: this.currentChatId,
          content: JSON.stringify(orderCard.orderData),
          messageType: 'order_card'
        }
        this.websocket.send(JSON.stringify(message))
      } else {
        this.$message.error('连接已断开，请刷新页面')
      }
    },
    
    // 显示订单详情
    showOrderDetail(orderData) {
      console.log('显示订单详情:', orderData)
      console.log('收货地址:', orderData.linkAddress, '发货地址:', orderData.deliveryAddress)
      this.selectedOrder = orderData
      this.showOrderDetailDialog = true
      // 等待 DOM 更新后初始化地图，添加延迟确保 el-dialog 动画完成
      setTimeout(() => {
        if (orderData.linkAddress && orderData.deliveryAddress) {
          this.initDetailMap(orderData)
        } else {
          console.log('地址信息不完整，不显示地图')
        }
      }, 300)
    },
    
    // 初始化订单详情地图
    initDetailMap(order) {
      const mapId = 'detailMap_' + order.orderNo
      const mapContainer = document.getElementById(mapId)
      if (!mapContainer) {
        console.error('地图容器未找到:', mapId)
        return
      }
      
      // 确保容器有尺寸
      mapContainer.style.width = '100%'
      mapContainer.style.height = '300px'
      
      console.log('初始化地图，发货地址:', order.deliveryAddress, '收货地址:', order.linkAddress)
      
      // 加载百度地图
      if (window.BMap) {
        console.log('BMap 已加载，创建地图')
        this.createDetailBMap(mapId, order)
      } else {
        if (document.querySelector('script[src*="api.map.baidu.com"]')) {
          console.log('BMap 正在加载，等待...')
          const checkBMap = setInterval(() => {
            if (window.BMap) {
              clearInterval(checkBMap)
              console.log('BMap 加载完成，创建地图')
              this.createDetailBMap(mapId, order)
            }
          }, 200)
          setTimeout(() => clearInterval(checkBMap), 10000)
        } else {
          console.log('开始加载 BMap API...')
          const script = document.createElement('script')
          script.src = 'https://api.map.baidu.com/api?v=3.0&ak=RNsH2PuOKazm6ifsOG6LrJ8Ir95uX5JT'
          script.onload = () => {
            console.log('BMap API 加载完成')
            this.createDetailBMap(mapId, order)
          }
          script.onerror = () => {
            console.error('BMap API 加载失败')
          }
          document.head.appendChild(script)
        }
      }
    },
    
    // 创建百度地图
    createDetailBMap(mapId, order) {
      const mapContainer = document.getElementById(mapId)
      if (!mapContainer) return
      
      try {
        console.log('创建地图实例...')
        const map = new BMap.Map(mapId)
        map.centerAndZoom('中国', 5)
        map.enableScrollWheelZoom(true)
        
        const geocoder = new BMap.Geocoder()
        
        geocoder.getPoint(order.deliveryAddress, (startPoint) => {
          if (startPoint) {
            console.log('发货地址解析成功:', startPoint)
            const startMarker = new BMap.Marker(startPoint)
            startMarker.setTitle('发货地：' + order.deliveryAddress)
            map.addOverlay(startMarker)
            
            geocoder.getPoint(order.linkAddress, (endPoint) => {
              if (endPoint) {
                console.log('收货地址解析成功:', endPoint)
                const endMarker = new BMap.Marker(endPoint)
                endMarker.setTitle('收货地：' + order.linkAddress)
                map.addOverlay(endMarker)
                
                const points = [startPoint, endPoint]
                map.setViewport(points)
                
                const driving = new BMap.DrivingRoute(map, {
                  onSearchComplete: (results) => {
                    if (results.getNumPlans() > 0) {
                      console.log('路线规划成功')
                    } else {
                      console.log('路线规划无结果')
                    }
                  }
                })
                driving.search(startPoint, endPoint)
              } else {
                console.error('收货地址解析失败:', order.linkAddress)
              }
            })
          } else {
            console.error('发货地址解析失败:', order.deliveryAddress)
            geocoder.getPoint(order.linkAddress, (endPoint) => {
              if (endPoint) {
                const endMarker = new BMap.Marker(endPoint)
                endMarker.setTitle('收货地：' + order.linkAddress)
                map.addOverlay(endMarker)
                map.centerAndZoom(endPoint, 12)
              }
            })
          }
        })
      } catch (e) {
        console.error('地图初始化失败:', e)
      }
    },
    
    // 状态文字转数字
    getStatusNum(state) {
      const map = { '待付款': 0, '待发货': 1, '待收货': 2, '已完成': 3, '已取消': 4 }
      return map[state] ?? 0
    },
    
    // 获取订单图片
    getOrderImage(imgs) {
      if (!imgs) return this.serviceAvatar
      // 如果是 emoji 格式，返回默认头像
      if (imgs.startsWith('emoji:') || imgs.startsWith('data:')) {
        return this.serviceAvatar
      }
      // 如果是完整 URL，直接返回
      if (imgs.startsWith('http://') || imgs.startsWith('https://')) {
        return imgs
      }
      // 否则加上 baseApi 前缀
      return this.baseApi + imgs
    },
    
    // 图片加载失败处理
    handleImageError(e) {
      e.target.src = this.serviceAvatar
    }
  }
}
</script>

<style scoped>
/* 聊天容器 - 固定高度布局，填充父容器 */
.service-chat-container {
  display: flex;
  height: 100%;
  max-height: 100%;
  overflow: hidden;
  background: #f5f5f5;
}

/* 左侧边栏 */
.chat-sidebar {
  width: 320px;
  min-width: 320px;
  background: white;
  border-right: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-header {
  flex-shrink: 0;
  padding: 20px;
  border-bottom: 1px solid #e0e0e0;
  background: linear-gradient(135deg, #ff9000 0%, #ff5000 100%);
  color: white;
}

.sidebar-header h3 {
  margin: 0 0 10px;
  font-size: 16px;
}

.service-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.service-name {
  font-size: 14px;
}

.online-badge {
  background: rgba(255, 255, 255, 0.3);
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
}

/* 标签页 */
.chat-tabs {
  flex-shrink: 0;
  display: flex;
  border-bottom: 1px solid #e0e0e0;
}

.tab {
  flex: 1;
  background: none;
  border: none;
  padding: 12px;
  font-size: 14px;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
}

.tab:hover {
  background: #f5f5f5;
}

.tab.active {
  color: #ff5000;
  border-bottom-color: #ff5000;
  font-weight: 600;
}

/* 会话列表 - 固定高度，滚动 */
.chat-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}

.chat-item {
  display: flex;
  padding: 15px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.2s;
}

.chat-item:hover {
  background: #f5f5f5;
}

.chat-item.active {
  background: #fff5e6;
  border-left: 3px solid #ff5000;
}

.chat-avatar {
  margin-right: 12px;
}

.avatar-img {
  width: 45px;
  height: 45px;
  border-radius: 50%;
}

.chat-info {
  flex: 1;
  min-width: 0;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 5px;
}

.chat-username {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.chat-time {
  font-size: 12px;
  color: #999;
}

.chat-preview {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.preview-text {
  font-size: 13px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-text.unread {
  color: #333;
  font-weight: 600;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.empty-icon {
  font-size: 60px;
  margin-bottom: 15px;
}

.empty-tips {
  font-size: 12px;
  margin-top: 10px;
}

/* 右侧聊天区域 */
.chat-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-window {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.chat-window-header {
  flex-shrink: 0;
  padding: 15px 20px;
  background: white;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chat-user-info h4 {
  margin: 0 0 5px;
  font-size: 16px;
  color: #333;
}

.chat-status {
  font-size: 12px;
  color: #999;
}

.chat-subject {
  font-size: 12px;
  color: #666;
  margin-left: 10px;
  background: #f5f5f5;
  padding: 2px 8px;
  border-radius: 4px;
}

.action-btn {
  background: #ff5000;
  color: white;
  border: none;
  padding: 8px 20px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}

.action-btn:hover {
  background: #ff7000;
}

/* 消息窗口 - 固定高度，flex 自适应 */
.chat-window-messages {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 20px;
  background: #f5f5f5;
}

.message {
  display: flex;
  margin-bottom: 20px;
}

.message-left {
  flex-direction: row;
}

.message-right {
  flex-direction: row-reverse;
}

.message-content {
  max-width: 60%;
}

.message-sender {
  font-size: 12px;
  color: #999;
  margin-bottom: 5px;
}

.message-bubble {
  background: white;
  padding: 12px 15px;
  border-radius: 12px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.message-left .message-bubble {
  border-top-left-radius: 4px;
}

.message-right .message-bubble {
  background: linear-gradient(135deg, #ff9000 0%, #ff5000 100%);
  color: white;
  border-top-right-radius: 4px;
}

.message-bubble p {
  margin: 0;
  line-height: 1.5;
  font-size: 14px;
}

.message-time {
  font-size: 11px;
  color: #999;
  margin-top: 5px;
}

/* 输入区域 - 固定在底部 */
.chat-window-input {
  flex-shrink: 0;
  background: white;
  border-top: 1px solid #e0e0e0;
  padding: 15px 20px;
}

.input-tools {
  display: flex;
  gap: 10px;
  margin-bottom: 10px;
}

.tool-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  padding: 5px;
}

.tool-btn:hover {
  transform: scale(1.1);
}

.input-wrapper {
  display: flex;
  gap: 10px;
}

textarea {
  flex: 1;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 10px;
  font-size: 14px;
  resize: none;
  font-family: inherit;
}

textarea:focus {
  outline: none;
  border-color: #ff9000;
}

.send-btn {
  background: linear-gradient(135deg, #ff9000 0%, #ff5000 100%);
  color: white;
  border: none;
  padding: 0 25px;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  font-weight: 600;
}

.send-btn:hover {
  opacity: 0.9;
}

.send-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

/* 未选择会话 */
.no-chat-selected {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #999;
}

.no-chat-icon {
  font-size: 80px;
  margin-bottom: 20px;
}

/* 订单按钮 */
.order-btn {
  background: linear-gradient(135deg, #ff9000 0%, #ff5000 100%);
  border-radius: 50%;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px !important;
}

/* 订单卡片消息 */
.order-card {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  min-width: 240px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  transition: transform 0.2s;
}

.order-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}

.order-card-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 12px;
  background: linear-gradient(135deg, #ff9000 0%, #ff5000 100%);
  color: #fff;
}

.order-icon {
  font-size: 16px;
}

.order-card-title {
  font-size: 13px;
  font-weight: 600;
}

.order-card-body {
  padding: 12px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  gap: 12px;
  align-items: center;
}

.order-card-img {
  width: 60px;
  height: 60px;
  border-radius: 6px;
  object-fit: cover;
  flex-shrink: 0;
}

.order-card-info {
  flex: 1;
  min-width: 0;
}

.order-no {
  font-size: 12px;
  color: #999;
  margin-bottom: 6px;
}

.order-product {
  font-size: 14px;
  color: #333;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-price {
  font-size: 16px;
  color: #ff5000;
  font-weight: 600;
}

.order-card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #fafafa;
}

.order-status {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
}

.order-status.status-0 {
  background: #fff5f0;
  color: #ff5000;
}

.order-status.status-1 {
  background: #e6f7ff;
  color: #1890ff;
}

.order-status.status-2 {
  background: #f6ffed;
  color: #52c41a;
}

.order-status.status-3 {
  background: #f0f0f0;
  color: #999;
}

.order-view {
  font-size: 12px;
  color: #ff9000;
}

/* 订单选择弹窗 */
.order-selector-dialog .el-dialog__body {
  padding: 16px !important;
  max-height: 400px;
  overflow-y: auto;
}

.order-selector-content {
  min-height: 100px;
}

.loading-text,
.empty-text {
  text-align: center;
  padding: 40px 0;
  color: #999;
  font-size: 14px;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.order-item {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.order-item:hover {
  border-color: #ff9000;
  background: #fff5f0;
}

.order-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.order-item-no {
  font-size: 13px;
  color: #666;
}

.order-item-status {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
}

.order-item-body {
  display: flex;
  gap: 12px;
}

.order-item-img {
  width: 60px;
  height: 60px;
  border-radius: 6px;
  object-fit: cover;
}

.order-item-info {
  flex: 1;
}

.order-item-name {
  font-size: 14px;
  color: #333;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-item-price {
  font-size: 16px;
  color: #ff5000;
  font-weight: 600;
}

/* 订单详情弹窗 */
.order-detail-dialog .el-dialog__body {
  padding: 20px !important;
}

.order-detail-content {
  padding: 0;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 16px;
}

.detail-order-no {
  font-size: 14px;
  color: #666;
}

.detail-status {
  font-size: 13px;
  padding: 4px 12px;
  border-radius: 12px;
}

.detail-product {
  display: flex;
  gap: 16px;
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
  margin-bottom: 16px;
}

.detail-product-img {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  object-fit: cover;
}

.detail-product-info {
  flex: 1;
}

.detail-product-name {
  font-size: 15px;
  color: #333;
  font-weight: 500;
  margin-bottom: 8px;
}

.detail-product-spec {
  font-size: 13px;
  color: #999;
  margin-bottom: 8px;
}

.detail-product-price,
.detail-product-qty {
  font-size: 13px;
  color: #666;
  margin-bottom: 4px;
}

.price-value,
.qty-value {
  color: #333;
  font-weight: 500;
}

.detail-info {
  padding: 0;
}

.info-row {
  display: flex;
  padding: 10px 0;
  border-bottom: 1px solid #f5f5f5;
}

.info-label {
  width: 100px;
  font-size: 13px;
  color: #999;
  flex-shrink: 0;
}

.info-value {
  flex: 1;
  font-size: 13px;
  color: #333;
}

.price-highlight {
  color: #ff5000;
  font-size: 16px;
  font-weight: 600;
}

/* 物流地图 */
.detail-logistics-map {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.map-section-title {
  font-size: 15px;
  color: #333;
  font-weight: 500;
  margin-bottom: 12px;
}

.detail-baidu-map {
  width: 100%;
  height: 300px;
  border-radius: 8px;
  overflow: hidden;
}
</style>
