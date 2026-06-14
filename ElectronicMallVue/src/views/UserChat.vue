<template>
  <div class="chat-container">
    <!-- 聊天界面 -->
    <div class="chat-layout">
      <!-- 聊天头部 -->
      <div class="chat-header">
        <div class="header-content">
          <div class="back-btn" @click="goBack">
            <span class="icon">◀</span>
          </div>
          <div class="header-info">
            <h3>在线客服</h3>
            <p v-if="serviceStatus.online" class="online-status">
              <span class="dot"></span> 客服在线
            </p>
            <p v-else class="offline-status">客服暂时不在线</p>
          </div>
          <div class="header-actions">
            <button class="action-btn" @click="endChat" v-if="inChat">结束对话</button>
          </div>
        </div>
      </div>

      <!-- 排队信息 -->
      <div v-if="!inChat && queueInfo.position" class="queue-info">
        <div class="queue-card">
          <div class="queue-icon">⏳</div>
          <div class="queue-text">
            <p class="queue-title">正在为您排队</p>
            <p class="queue-desc">前面还有 <span class="highlight">{{ queueInfo.position - 1 }}</span> 人</p>
            <p class="queue-tips">客服将按顺序接入，请耐心等待</p>
          </div>
        </div>
      </div>

      <!-- 聊天消息区域 -->
      <div class="chat-messages" ref="messageContainer">
        <div v-for="msg in messages" :key="msg.id || msg.tempId" 
             :class="['message', msg.senderRole === 'user' ? 'message-right' : 'message-left']">
          <!-- 头像 -->
          <div class="avatar">
            <img v-if="msg.senderRole === 'user'" :src="userAvatar" alt="用户" class="avatar-img">
            <img v-else src="https://img.alicdn.com/tfs/TB1kxQzb.T1gK0jSZFrXXcNCXXa-64-64.png" alt="客服" class="avatar-img">
          </div>
          
          <!-- 消息内容 -->
          <div class="message-content">
            <div class="message-sender" v-if="msg.senderRole === 'service'">
              {{ msg.senderName || '客服' }}
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
              {{ formatTime(msg.createdAt || msg.timestamp) }}
            </div>
          </div>
        </div>
        
        <!-- 系统消息 -->
        <div v-if="systemMessages.length > 0" class="system-messages">
          <div v-for="(sysMsg, index) in systemMessages" :key="index" class="system-message">
            {{ sysMsg }}
          </div>
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="chat-input-area">
        <div class="input-tools">
          <button class="tool-btn" title="发送图片">📷</button>
          <button class="tool-btn" title="发送文件">📁</button>
          <button class="tool-btn" title="发送表情">😊</button>
          <button class="tool-btn order-btn" title="发送订单" @click="openOrderSelector">📦</button>
        </div>
        <div class="input-wrapper">
          <textarea 
            v-model="messageInput" 
            placeholder="请输入您的问题..." 
            @keydown.enter.exact.prevent="sendMessage"
            rows="3"
          ></textarea>
          <button class="send-btn" @click="sendMessage" :disabled="!messageInput.trim()">
            发送
          </button>
        </div>
      </div>

      <!-- 结束评价弹窗 -->
      <el-dialog 
        :visible.sync="showRating" 
        width="420px" 
        center
        :show-close="false"
        custom-class="rating-dialog"
      >
        <div class="rating-content">
          <div class="rating-header">
            <div class="rating-icon">💬</div>
            <h3 class="rating-title">服务评价</h3>
            <p class="rating-subtitle">工单编号：{{ ticketId }}</p>
          </div>
          <div class="rating-body">
            <p class="rating-question">您对本次客服服务满意吗？</p>
            <div class="stars">
              <span v-for="i in 5" :key="i" 
                    class="star" 
                    :class="{ active: i <= rating }"
                    @click="rating = i">★</span>
            </div>
            <div class="rating-tags">
              <span 
                v-for="tag in ratingTags" 
                :key="tag"
                class="rating-tag"
                :class="{ active: selectedTags.includes(tag) }"
                @click="toggleTag(tag)"
              >{{ tag }}</span>
            </div>
            <el-input 
              v-model="ratingComment" 
              type="textarea" 
              placeholder="请留下您的评价（选填）"
              rows="3"
              class="rating-textarea"
            ></el-input>
          </div>
        </div>
        <span slot="footer" class="dialog-footer">
          <el-button class="btn-cancel" @click="showRating = false; $router.push('/user')">稍后再评</el-button>
          <el-button class="btn-submit" @click="submitRating">提交评价</el-button>
        </span>
      </el-dialog>

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
  </div>
</template>

<script>
export default {
  name: 'UserChat',
  data() {
    return {
      // 聊天相关
      websocket: null,
      messages: [],
      systemMessages: [],
      messageInput: '',
      inChat: false,
      
      // 排队信息
      queueInfo: {
        position: null,
        queueSize: 0
      },
      
      // 客服状态
      serviceStatus: {
        online: false,
        count: 0
      },
      
      // 用户信息
      user: {},
      ticketId: null,
      serviceId: null,
      roomId: null,
      
      // 评价相关
      showRating: false,
      rating: 5,
      ratingComment: '',
      ratingTags: ['响应迅速', '态度友好', '专业解答', '解决问题', '服务周到'],
      selectedTags: [],
      
      // 头像
      userAvatar: 'https://img.alicdn.com/tfs/TB1kxQzb.T1gK0jSZFrXXcNCXXa-64-64.png',
      
      // API 基础地址
      baseApi: 'http://localhost:9191',
      
      // 加载状态
      loading: false,
      
      // 订单相关
      showOrderSelector: false,
      showOrderDetailDialog: false,
      myOrders: [],
      selectedOrder: null,
      orderLoading: false
    }
  },
  
  created() {
    this.initUserData()
    this.checkRouteParams() // 检查路由参数
    this.connectWebSocket() // 直接连接 WebSocket
  },
  
  beforeDestroy() {
    if (this.websocket) {
      this.websocket.close()
    }
  },
  
  methods: {
    // 初始化用户数据
    initUserData() {
      const userStr = localStorage.getItem('user')
      if (userStr) {
        this.user = JSON.parse(userStr)
      }
    },
    
    // 检查路由参数（从创建工单页面跳转过来的工单信息）
    checkRouteParams() {
      const { ticketId, ticketNo, showRating } = this.$route.query
      if (ticketId) {
        console.log('从工单页面跳转，工单 ID:', ticketId)
        this.ticketId = ticketId
        
        // 如果是从"去评价"按钮跳转过来，直接显示评价弹窗
        if (showRating === 'true') {
          this.showRating = true
        } else {
          this.$message.success('正在为您连接客服...')
        }
      }
    },
    
    // 检查客服在线状态
    async checkServiceOnline() {
      try {
        const res = await this.request.get('/api/chat/service/online')
        if (res.code === '200') {
          this.serviceStatus.online = res.onlineCount > 0
          this.serviceStatus.count = res.onlineCount
        }
      } catch (e) {
        console.error('获取客服在线状态失败:', e)
      }
    },
    
    // 连接 WebSocket
    connectWebSocket() {
      const userStr = localStorage.getItem('user')
      if (!userStr) {
        this.$message.error('请先登录')
        this.$router.push('/login')
        return
      }
      
      const user = JSON.parse(userStr)
      const token = user.token
      
      // 构建 WebSocket URL
      const wsUrl = `ws://localhost:9191/ws/chat?token=${token}&role=user`
      
      this.websocket = new WebSocket(wsUrl)
      
      this.websocket.onopen = () => {
        console.log('WebSocket 连接成功')
        this.systemMessages.push('已连接到在线客服')
      }
      
      this.websocket.onmessage = (event) => {
        const data = JSON.parse(event.data)
        this.handleMessage(data)
      }
      
      this.websocket.onerror = (error) => {
        console.error('WebSocket 错误:', error)
        this.systemMessages.push('连接异常，请刷新页面重试')
      }
      
      this.websocket.onclose = () => {
        console.log('WebSocket 连接关闭')
        this.systemMessages.push('已断开连接')
      }
    },
    
    // 处理接收到的消息
    handleMessage(data) {
      console.log('收到消息:', data)
      
      switch (data.type) {
        case 'queue_info':
          // 排队信息
          this.queueInfo.position = data.position
          this.queueInfo.queueSize = data.queueSize
          this.ticketId = data.ticketId
          this.systemMessages.push(`当前排队位置：第 ${data.position} 位`)
          break
          
        case 'assigned':
          // 客服已接入
          this.inChat = true
          this.serviceId = data.serviceId
          this.roomId = data.roomId
          this.queueInfo.position = null
          this.systemMessages.push('客服已接入，开始聊天')
          break
          
        case 'new_chat':
          // 新聊天（客服主动发起）
          this.inChat = true
          this.ticketId = data.ticketId
          this.serviceId = data.serviceId
          this.roomId = data.roomId
          this.systemMessages.push('客服已接入，开始聊天')
          break
          
        case 'chat':
          // 聊天消息
          const msg = {
            id: data.messageId,
            senderId: data.senderId,
            senderRole: data.senderRole,
            senderName: data.senderRole === 'service' ? '客服' : '我',
            content: data.content,
            messageType: data.messageType || 'text',
            timestamp: data.timestamp,
            isRead: true
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
          
          this.messages.push(msg)
          this.scrollToBottom()
          break
          
        case 'chat_ended':
          // 聊天结束
          this.inChat = false
          this.systemMessages.push('对话已结束')
          if (data.endedBy === 'service') {
            this.showRating = true
          }
          break
          
        case 'error':
          // 错误消息
          this.$message.error(data.message)
          break
      }
    },
    
    // 发送消息
    sendMessage() {
      if (!this.messageInput.trim()) return
      
      const content = this.messageInput.trim()
      
      // 添加到消息列表（临时显示）
      const tempMsg = {
        tempId: Date.now(),
        senderId: this.user.id,
        senderRole: 'user',
        senderName: '我',
        content: content,
        messageType: 'text',
        timestamp: Date.now(),
        isRead: false
      }
      this.messages.push(tempMsg)
      this.scrollToBottom()
      
      // 通过 WebSocket 发送
      if (this.websocket && this.websocket.readyState === WebSocket.OPEN) {
        const message = {
          type: 'chat',
          ticketId: this.ticketId,
          content: content,
          messageType: 'text'
        }
        this.websocket.send(JSON.stringify(message))
      } else {
        this.$message.error('连接已断开，请刷新页面')
      }
      
      this.messageInput = ''
    },
    
    // 结束聊天
    endChat() {
      this.$confirm('确定要结束对话吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        if (this.websocket && this.websocket.readyState === WebSocket.OPEN) {
          const message = {
            type: 'end_chat',
            ticketId: this.ticketId
          }
          this.websocket.send(JSON.stringify(message))
        }
        this.showRating = true
      }).catch(() => {})
    },
    
    // 切换评价标签
    toggleTag(tag) {
      const index = this.selectedTags.indexOf(tag)
      if (index > -1) {
        this.selectedTags.splice(index, 1)
      } else {
        this.selectedTags.push(tag)
      }
    },
    
    // 提交评价
    async submitRating() {
      if (!this.ticketId) {
        this.$message.error('工单信息丢失，无法提交评价')
        return
      }
      
      try {
        const comment = this.ratingComment + (this.selectedTags.length ? ' [' + this.selectedTags.join(', ') + ']' : '')
        
        const response = await this.request.post('/api/ticket/rate', {
          ticketId: this.ticketId,
          score: this.rating,
          comment: comment || ' '
        })
        
        if (response.code === '200') {
          this.$message.success('感谢您的评价！')
          this.showRating = false
          this.$router.push('/user')
        } else {
          this.$message.error(response.msg || '评价提交失败')
        }
      } catch (e) {
        console.error('评价提交失败:', e)
        this.$message.error('评价提交失败，请稍后重试')
      }
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
    
    // 返回上一页
    goBack() {
      this.$router.push('/user')
    },
    
    // 打开订单选择器
    async openOrderSelector() {
      this.showOrderSelector = true
      this.orderLoading = true
      try {
        const userId = parseInt(this.user.id)
        if (isNaN(userId)) {
          this.$message.error('用户ID无效')
          this.orderLoading = false
          return
        }
        const res = await this.request.get(`/api/order/userid/${userId}`)
        console.log('订单列表返回数据:', res)
        if (res.code === '200') {
          this.myOrders = res.data || []
          console.log('解析后的订单:', this.myOrders)
        }
      } catch (e) {
        console.error('获取订单列表失败:', e)
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
      // 后端返回的字段名
      const orderCard = {
        tempId: Date.now(),
        senderId: this.user.id,
        senderRole: 'user',
        senderName: '我',
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
      this.messages.push(orderCard)
      this.scrollToBottom()
      
      // 通过 WebSocket 发送
      if (this.websocket && this.websocket.readyState === WebSocket.OPEN) {
        const message = {
          type: 'chat',
          ticketId: this.ticketId,
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
      if (!mapContainer) return
      
      // 确保容器有尺寸
      mapContainer.style.width = '100%'
      mapContainer.style.height = '300px'
      
      // 加载百度地图
      if (window.BMap) {
        this.createDetailBMap(mapId, order)
      } else {
        // 检查是否已经在加载
        if (document.querySelector('script[src*="api.map.baidu.com"]')) {
          // 等待加载完成
          const checkBMap = setInterval(() => {
            if (window.BMap) {
              clearInterval(checkBMap)
              this.createDetailBMap(mapId, order)
            }
          }, 200)
          setTimeout(() => clearInterval(checkBMap), 10000)
        } else {
          const script = document.createElement('script')
          script.src = 'https://api.map.baidu.com/api?v=3.0&ak=RNsH2PuOKazm6ifsOG6LrJ8Ir95uX5JT'
          script.onload = () => {
            this.createDetailBMap(mapId, order)
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
        const map = new BMap.Map(mapId)
        map.centerAndZoom('中国', 5)
        map.enableScrollWheelZoom(true)
        
        const geocoder = new BMap.Geocoder()
        
        // 先解析发货地址
        geocoder.getPoint(order.deliveryAddress, (startPoint) => {
          if (startPoint) {
            const startMarker = new BMap.Marker(startPoint)
            startMarker.setTitle('发货地：' + order.deliveryAddress)
            map.addOverlay(startMarker)
            
            // 再解析收货地址
            geocoder.getPoint(order.linkAddress, (endPoint) => {
              if (endPoint) {
                const endMarker = new BMap.Marker(endPoint)
                endMarker.setTitle('收货地：' + order.linkAddress)
                map.addOverlay(endMarker)
                
                // 自适应显示两个点
                const points = [startPoint, endPoint]
                map.setViewport(points)
                
                // 画路线
                const driving = new BMap.DrivingRoute(map, {
                  onSearchComplete: (results) => {
                    if (results.getNumPlans() > 0) {
                      // 路线绘制成功
                    }
                  }
                })
                driving.search(startPoint, endPoint)
              }
            })
          } else {
            // 发货地址解析失败，只显示收货地址
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
      if (!imgs) return this.userAvatar
      // 如果是 emoji 格式，返回默认头像
      if (imgs.startsWith('emoji:') || imgs.startsWith('data:')) {
        return this.userAvatar
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
      e.target.src = this.userAvatar
    }
  }
}
</script>

<style>
/* 评价弹窗样式 - 必须非 scoped，因为 el-dialog 挂载到 body */
.rating-dialog {
  border-radius: 16px !important;
  overflow: hidden;
}

.rating-dialog .el-dialog__header {
  display: none !important;
}

.rating-dialog .el-dialog__body {
  padding: 0 !important;
}

.rating-dialog .el-dialog__footer {
  padding: 16px 24px 20px !important;
  border-top: 1px solid #f0f0f0;
  text-align: center;
}

/* 提交按钮 - 强制覆盖 Element UI 默认样式 */
.rating-dialog .btn-submit.el-button {
  border-radius: 24px !important;
  padding: 12px 36px !important;
  font-size: 15px !important;
  font-weight: 600 !important;
  background: linear-gradient(135deg, #ff9000 0%, #ff5000 100%) !important;
  border: none !important;
  color: #fff !important;
  letter-spacing: 1px;
  box-shadow: 0 4px 12px rgba(255, 80, 0, 0.3) !important;
}

.rating-dialog .btn-submit.el-button:hover,
.rating-dialog .btn-submit.el-button:focus {
  background: linear-gradient(135deg, #ff5000 0%, #e04000 100%) !important;
  color: #fff !important;
  border-color: transparent !important;
}

.rating-dialog .btn-submit.el-button:active {
  transform: scale(0.98);
}

/* 取消按钮 */
.rating-dialog .btn-cancel.el-button {
  border-radius: 24px !important;
  padding: 12px 36px !important;
  font-size: 15px !important;
  border: 1px solid #e0e0e0 !important;
  color: #666 !important;
  background: #fff !important;
}

.rating-dialog .btn-cancel.el-button:hover,
.rating-dialog .btn-cancel.el-button:focus {
  border-color: #ff9000 !important;
  color: #ff9000 !important;
  background: #fff !important;
}
</style>

<style scoped>
/* 聊天容器 - 固定高度布局，填充父容器 */
.chat-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  max-height: 100%;
  overflow: hidden;
  background: #f5f5f5;
}

/* 聊天布局 - v-else 分支的 flex 容器 */
.chat-layout {
  display: flex;
  flex-direction: column;
  height: 100%;
  max-height: 100%;
  overflow: hidden;
}

/* 头部样式 - 淘宝橙色风格 */
.chat-header {
  flex-shrink: 0;
  background: linear-gradient(135deg, #ff9000 0%, #ff5000 100%);
  color: white;
  padding: 15px 20px;
  box-shadow: 0 2px 8px rgba(255, 80, 0, 0.3);
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.back-btn {
  background: none;
  border: none;
  color: white;
  font-size: 18px;
  cursor: pointer;
  padding: 5px 10px;
}

.header-info h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.online-status, .offline-status {
  margin: 5px 0 0;
  font-size: 12px;
  opacity: 0.9;
}

.dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  background: #4CAF50;
  border-radius: 50%;
  margin-right: 5px;
}

.action-btn {
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.5);
  color: white;
  padding: 6px 15px;
  border-radius: 20px;
  cursor: pointer;
  font-size: 13px;
}

.action-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

/* 排队信息 */
.queue-info {
  flex-shrink: 0;
  padding: 20px;
  background: #fff;
  border-bottom: 1px solid #eee;
}

.queue-card {
  display: flex;
  align-items: center;
  background: linear-gradient(135deg, #fff5e6 0%, #ffe4cc 100%);
  border-radius: 12px;
  padding: 20px;
  border: 2px solid #ff9000;
}

.queue-icon {
  font-size: 40px;
  margin-right: 15px;
}

.queue-text {
  flex: 1;
}

.queue-title {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 600;
  color: #ff5000;
}

.queue-desc {
  margin: 0 0 5px;
  font-size: 14px;
  color: #666;
}

.highlight {
  color: #ff5000;
  font-weight: bold;
  font-size: 18px;
}

.queue-tips {
  margin: 0;
  font-size: 12px;
  color: #999;
}

/* 消息区域 - 固定高度，flex 自适应 */
.chat-messages {
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

.avatar {
  flex-shrink: 0;
  margin: 0 10px;
}

.avatar-img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 2px solid #fff;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
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

/* 系统消息 */
.system-messages {
  text-align: center;
  margin: 15px 0;
}

.system-message {
  display: inline-block;
  background: rgba(0,0,0,0.05);
  padding: 5px 15px;
  border-radius: 15px;
  font-size: 12px;
  color: #999;
  margin: 5px 0;
}

/* 输入区域 - 固定在底部 */
.chat-input-area {
  flex-shrink: 0;
  background: white;
  border-top: 1px solid #e0e0e0;
  padding: 10px 15px;
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
  transition: transform 0.2s;
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

/* 评价弹窗 */
.rating-dialog .el-dialog__header {
  display: none !important;
}

.rating-dialog .el-dialog__body {
  padding: 0 !important;
}

.rating-dialog .el-dialog__footer {
  padding: 16px 24px !important;
  border-top: 1px solid #f0f0f0;
}

.rating-dialog .el-dialog {
  border-radius: 16px !important;
  overflow: hidden;
}

.rating-content {
  padding: 0;
}

.rating-header {
  background: linear-gradient(135deg, #ff9000 0%, #ff5000 100%);
  padding: 24px 20px 20px;
  text-align: center;
  border-radius: 0;
}

.rating-icon {
  font-size: 32px;
  margin-bottom: 8px;
}

.rating-title {
  font-size: 18px;
  font-weight: 600;
  color: #fff;
  margin: 0 0 6px;
}

.rating-subtitle {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.85);
  margin: 0;
}

.rating-body {
  padding: 24px 20px;
  text-align: center;
}

.rating-question {
  font-size: 15px;
  color: #333;
  margin: 0 0 20px;
  font-weight: 500;
}

.stars {
  font-size: 36px;
  margin-bottom: 16px;
  display: flex;
  justify-content: center;
  gap: 8px;
}

.star {
  cursor: pointer;
  color: #e0e0e0;
  transition: all 0.2s;
  font-size: 36px;
  line-height: 1;
}

.star.active,
.star:hover {
  color: #ff9000;
  transform: scale(1.15);
}

.rating-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
  margin-bottom: 16px;
}

.rating-tag {
  padding: 6px 14px;
  border-radius: 16px;
  font-size: 12px;
  color: #666;
  background: #f5f5f5;
  border: 1px solid #e8e8e8;
  cursor: pointer;
  transition: all 0.2s;
}

.rating-tag.active {
  color: #ff5000;
  background: #fff5f0;
  border-color: #ff5000;
}

.rating-tag:hover {
  border-color: #ff9000;
}

.rating-textarea {
  margin-top: 8px;
}

.rating-textarea .el-textarea__inner {
  border-radius: 8px;
  border: 1px solid #e8e8e8;
  resize: none;
}

.rating-textarea .el-textarea__inner:focus {
  border-color: #ff9000;
}

.dialog-footer {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.btn-cancel {
  border-radius: 20px;
  padding: 10px 28px;
  font-size: 14px;
  border: 1px solid #d9d9d9;
  color: #666;
  background: #fff;
}

.btn-cancel:hover {
  border-color: #ff9000;
  color: #ff9000;
}

.btn-submit {
  border-radius: 20px;
  padding: 10px 28px;
  font-size: 14px;
  background: linear-gradient(135deg, #ff9000 0%, #ff5000 100%);
  border: none;
}

.btn-submit:hover {
  background: linear-gradient(135deg, #ff5000 0%, #e04000 100%);
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
