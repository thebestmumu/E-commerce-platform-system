<template>
  <div class="ai-chat-root">
    <!-- 聊天按钮 - 窗口关闭时显示在右下角 -->
    <div 
      class="ai-chat-float-btn" 
      v-if="!isOpen"
      @click="toggleChatWindow"
    >
      <span>💬</span>
    </div>
    
    <!-- 聊天窗口 -->
    <div 
      class="ai-chat-wrapper"
      :style="{ transform: `translate(${position.x}px, ${position.y}px)` }"
    >
      <div 
        class="ai-chat-container"
        :class="{ 'is-open': isOpen, 'show-history': showHistoryPanel }"
      >
        <!-- 统一顶部标题栏 -->
        <div class="unified-header" @mousedown="startDrag">
          <!-- 左侧：历史对话切换 + 标题 -->
          <div class="header-left-section">
            <button class="history-toggle-btn" @click="toggleHistoryPanel" title="历史记录" @mousedown.stop>
              📋
            </button>
            <h3 class="header-title">小皮助手</h3>
            <span v-if="showHistoryPanel" class="header-divider">|</span>
            <span v-if="showHistoryPanel" class="header-subtitle">历史对话</span>
          </div>
          
          <!-- 中间：模式选择器 -->
          <div class="mode-selector">
            <button 
              :class="['mode-btn', { active: currentMode === 'chat' }]"
              @click="switchMode('chat')"
              @mousedown.stop
            >
              对话
            </button>
            <button 
              :class="['mode-btn', { active: currentMode === 'help' }]"
              @click="switchMode('help')"
              @mousedown.stop
            >
              智能帮助
            </button>
            <button 
              :class="['mode-btn', { active: currentMode === 'customer' }]"
              @click="switchMode('customer')"
              @mousedown.stop
            >
              智能客服
            </button>
            <button 
              :class="['mode-btn', { active: currentMode === 'purchase' }]"
              @click="switchMode('purchase')"
              @mousedown.stop
            >
              真实购买
            </button>
          </div>
          
          <!-- 右侧：操作按钮 -->
          <div class="header-actions">
            <button 
              class="pause-btn" 
              @click="togglePause"
              v-if="loading"
              @mousedown.stop
            >
              {{ isPaused ? '继续' : '暂停' }}
            </button>
            <button class="close-btn" @click="toggleChatWindow" @mousedown.stop>×</button>
          </div>
        </div>
        
        <!-- 内容区域 -->
        <div class="chat-content-wrapper">
          <!-- 历史记录侧边栏 -->
          <div class="history-sidebar" v-show="showHistoryPanel">
            <!-- 新建对话按钮 -->
            <div class="new-conversation-top" @click="createNewConversation">
              <div class="item-icon">➕</div>
              <div class="item-info">
                <div class="item-title">新建对话</div>
                <div class="item-time">开始新的对话</div>
              </div>
            </div>
            
            <div class="history-list">
              <!-- 历史对话列表 -->
              <div 
                v-for="(conv, index) in conversations" 
                :key="conv.id"
                :class="['history-item', { active: currentConversationId === conv.id }]"
                @click="loadConversation(conv.id)"
              >
                <div class="item-icon">{{ getConversationIcon(conv.type) }}</div>
                <div class="item-info">
                  <div class="item-title">{{ conv.title || '新对话' }}</div>
                  <div class="item-time">{{ formatTime(conv.lastMessageTime) }}</div>
                </div>
                <div class="item-actions">
                  <button class="delete-btn" @click.stop="deleteConversation(conv.id)" title="删除">
                    🗑️
                  </button>
                </div>
              </div>
              
              <!-- 空状态 -->
              <div v-if="conversations.length === 0" class="empty-state">
                <div>暂无历史对话</div>
              </div>
            </div>
            
            <div class="history-footer">
              <button class="clear-all-btn" @click="clearAllConversations">
                清空所有对话
              </button>
            </div>
          </div>
          
          <!-- 聊天主区域 -->
          <div class="chat-main-area">
            <!-- 搜索结果提示 -->
            <div v-if="searchKeyword && searchResults.length > 0" class="search-results-hint">
              找到 {{ searchResults.length }} 条相关消息
              <button class="close-search-hint" @click="closeSearch">×</button>
            </div>
            
            <!-- 聊天消息列表 -->
            <div class="ai-chat-messages" ref="messagesContainer">
              <!-- 问候语浮动按钮 - 智能帮助模式下显示 -->
              <div v-if="(currentMode === 'help' || currentMode === 'customer') && !showGreetingCard" class="greeting-float-btn" @click="showGreetingCard = true">
                <span class="greeting-icon">🤖</span>
                <span class="greeting-text">小皮助手</span>
              </div>
              
              <!-- 问候语卡片 - 智能帮助模式下可展开 -->
              <div v-if="(currentMode === 'help' || currentMode === 'customer') && showGreetingCard" class="greeting-card-wrapper">
                <div class="greeting-card-header">
                  <h3>👋 你好，我是小皮助手</h3>
                  <button class="greeting-close-btn" @click="showGreetingCard = false">×</button>
                </div>
                <GreetingCard
                  @quick-action="handleQuickAction"
                  @ask-question="handleAskQuestion"
                  @open-tool="handleToolOpen"
                />
              </div>
              
              <div 
                v-for="(message, index) in displayMessages" 
                :key="index"
                :class="['message-item', message.role === 'user' ? 'user-message' : 'ai-message', { 'search-highlight': isSearchHighlight(message) }]"
              >
                <div class="message-avatar">{{ message.role === 'user' ? '我' : '小皮' }}</div>
                <div class="message-content">
                  <!-- 消息操作按钮 -->
                  <div class="message-actions">
                    <button class="msg-action-btn" @click="copyMessage(message)" title="复制消息">
                      📋
                    </button>
                    <button class="msg-action-btn" @click="deleteMessage(index)" title="删除消息">
                      🗑️
                    </button>
                  </div>
                  
                <div v-if="!message.toolForm">{{ message.content }}</div>
                
                <!-- AI工具表单展示 -->
                <div v-if="message.toolForm" class="tool-form-message">
                  <div class="tool-form-header">
                    <span class="tool-form-icon">{{ message.toolForm.icon }}</span>
                    <span class="tool-form-title">{{ message.toolForm.title }}</span>
                  </div>
                  
                  <!-- 文案生成表单 -->
                  <div v-if="message.toolForm.type === 'copywriting'" class="tool-form-body">
                    <div class="tool-form-group">
                      <label>文案类型</label>
                      <el-select v-model="message.toolForm.data.type" placeholder="选择文案类型" class="tool-form-select">
                        <el-option label="商品标题" value="标题"></el-option>
                        <el-option label="核心卖点" value="卖点"></el-option>
                        <el-option label="客服话术" value="话术"></el-option>
                      </el-select>
                    </div>
                    <div class="tool-form-group">
                      <label>商品名称</label>
                      <el-input v-model="message.toolForm.data.productName" placeholder="请输入商品名称" class="tool-form-input"></el-input>
                    </div>
                    <div class="tool-form-group">
                      <label>品类（可选）</label>
                      <el-input v-model="message.toolForm.data.category" placeholder="如：手机、服装、食品" class="tool-form-input"></el-input>
                    </div>
                    <el-button type="primary" class="tool-form-submit" @click="executeTool(message, 'copywriting')" :loading="message.toolForm.loading" :disabled="message.toolForm.loading">
                      <i class="el-icon-magic-stick"></i>
                      生成文案
                    </el-button>
                  </div>
                  
                  <!-- 价格分析表单 -->
                  <div v-if="message.toolForm.type === 'price'" class="tool-form-body">
                    <div class="tool-form-group">
                      <label>商品名称</label>
                      <el-input v-model="message.toolForm.data.productName" placeholder="请输入商品名称" class="tool-form-input"></el-input>
                    </div>
                    <div class="tool-form-group">
                      <label>当前售价（可选）</label>
                      <el-input-number v-model="message.toolForm.data.currentPrice" :min="0" :precision="2" placeholder="请输入当前售价" class="tool-form-input"></el-input-number>
                    </div>
                    <el-button type="primary" class="tool-form-submit" @click="executeTool(message, 'price')" :loading="message.toolForm.loading" :disabled="message.toolForm.loading">
                      <i class="el-icon-data-analysis"></i>
                      开始分析
                    </el-button>
                  </div>
                  
                  <!-- 销量预测表单 -->
                  <div v-if="message.toolForm.type === 'forecast'" class="tool-form-body">
                    <div class="tool-form-group">
                      <label>商品名称</label>
                      <el-input v-model="message.toolForm.data.productName" placeholder="请输入商品名称" class="tool-form-input"></el-input>
                    </div>
                    <div class="tool-form-group">
                      <label>当前库存（可选）</label>
                      <el-input-number v-model="message.toolForm.data.currentStock" :min="0" placeholder="请输入当前库存" class="tool-form-input"></el-input-number>
                    </div>
                    <el-button type="primary" class="tool-form-submit" @click="executeTool(message, 'forecast')" :loading="message.toolForm.loading" :disabled="message.toolForm.loading">
                      <i class="el-icon-data-line"></i>
                      开始预测
                    </el-button>
                  </div>
                  
                  <!-- 流式结果展示 -->
                  <div v-if="message.toolForm.result || message.toolForm.streaming" class="tool-form-result">
                    <div class="result-header">
                      <span>{{ message.toolForm.resultIcon }}</span>
                      <el-button v-if="message.toolForm.result && !message.toolForm.streaming" size="mini" type="primary" @click="copyResult(message.toolForm.result)">复制</el-button>
                      <span v-if="message.toolForm.streaming" class="streaming-indicator">
                        <i class="el-icon-loading"></i> 生成中...
                      </span>
                    </div>
                    <div class="result-content" v-html="formatResult(message.toolForm.result)"></div>
                  </div>
                </div>
                <!-- 思考过程展示（豆包风格）- 优先展示 -->
                <div v-if="message.thinkingSteps && message.thinkingSteps.length > 0" 
                     class="doubao-thinking-container"
                     :class="{ 'thinking-collapsed': message.thinkingCollapsed }">
                  <div class="doubao-thinking-header" @click="toggleThinking(message)">
                    <span class="thinking-icon">
                      <i :class="message.thinkingCollapsed ? 'el-icon-arrow-right' : 'el-icon-arrow-down'"></i>
                    </span>
                    <span class="thinking-title">思考过程</span>
                    <span v-if="!message.thinkingFinished" class="thinking-loading">
                      <i class="el-icon-loading"></i>
                    </span>
                  </div>
                  <div v-show="!message.thinkingCollapsed" class="doubao-thinking-content">
                    <div v-for="(step, index) in message.thinkingSteps" :key="index" class="thinking-step">
                      <span class="step-number">{{ index + 1 }}</span>
                      <span class="step-text">{{ step }}</span>
                    </div>
                  </div>
                </div>
                
                <!-- 商品推荐展示 - 使用独立组件 -->
                <ProductRecommendation
                  v-if="message.topGood || (message.otherGoods && message.otherGoods.length > 0) || (message.recommendations && message.recommendations.length > 0)"
                  :top-good="message.topGood"
                  :other-goods="message.otherGoods || []"
                  :recommendations="message.recommendations || []"
                  @view-detail="viewGoodDetail"
                  @add-to-cart="addToCart"
                  @buy-now="buyNow"
                />
                
                <!-- 搜索结果展示 - 使用独立组件 -->
                <SearchResults
                  v-if="message.searchResults && message.searchResults.length > 0"
                  :search-results="message.searchResults"
                  :data-source="message.dataSource || ''"
                  @view-detail="viewGoodDetail"
                  @add-to-cart="addToCart"
                  @buy-now="buyNow"
                />
                
                <!-- 订单分析报告 - 使用独立组件 -->
                <OrderAnalysis
                  v-if="message.orderAnalysis"
                  :order-data="message.orderAnalysis"
                  @view-orders="handleViewOrders"
                />

                <!-- 订单列表展示 - 使用独立组件 -->
                <OrderList
                  v-if="message.orderList && message.orderList.length > 0"
                  :orders="message.orderList"
                  :order-count="message.orderCount || 0"
                  :total-count="message.totalCount || message.orderCount || 0"
                  :has-more="message.hasMore || false"
                  :current-page="message.currentPage || 1"
                  :page-size="message.pageSize || 10"
                  @view-detail="viewOrderDetail"
                  @pay-order="payOrder"
                  @confirm-receive="confirmReceive"
                  @load-more="handleLoadMoreOrders"
                />
                
                <!-- 评论舆情分析展示 - 使用独立组件 -->
                <SentimentAnalysis
                  v-if="message.sentimentData"
                  :sentiment-data="message.sentimentData"
                />
                
                <!-- 销售数据报告展示 - 使用独立组件 -->
                <SalesReport
                  v-if="message.salesData"
                  :sales-data="message.salesData"
                />
                
                <!-- 确认推荐卡片 -->
                <div v-if="message.type === 'confirmRecommend'" class="confirm-recommend-card">
                  <div class="confirm-icon">🔍</div>
                  <div class="confirm-content">
                    <div class="confirm-text">{{ message.content }}</div>
                    <div class="confirm-actions">
                      <button class="confirm-btn agree" @click="handleConfirmRecommend(message.category, true)">
                        ✅ 同意
                      </button>
                      <button class="confirm-btn reject" @click="handleConfirmRecommend(message.category, false)">
                        ❌ 拒绝
                      </button>
                    </div>
                  </div>
                </div>
                
                <!-- 工单信息卡片 - 转人工客服时显示（旧版，保留兼容） -->
                <div v-if="message.ticketInfo" class="ticket-info-card">
                  <div class="ticket-icon"></div>
                  <div class="ticket-content">
                    <div class="ticket-text">
                      <div class="ticket-title">工单已创建</div>
                      <div class="ticket-no" v-if="message.ticketInfo.ticketNo">
                        工单编号：<strong>{{ message.ticketInfo.ticketNo }}</strong>
                      </div>
                      <div class="ticket-desc">{{ message.ticketInfo.message }}</div>
                    </div>
                    
                    <!-- 问题描述输入框 -->
                    <div class="ticket-description-input">
                      <textarea 
                        v-model="message.ticketInfo.description" 
                        placeholder="请简要描述您的问题（选填）..."
                        rows="3"
                        class="description-textarea"
                      ></textarea>
                    </div>
                    
                    <!-- 操作按钮 -->
                    <div class="ticket-actions">
                      <button 
                        class="ticket-btn enter-chat"
                        @click="enterCustomerService(message.ticketInfo)"
                      >
                        💬 进入在线客服
                      </button>
                    </div>
                    
                    <!-- 其他联系渠道 -->
                    <div class="ticket-channels" v-if="message.ticketInfo.channels && message.ticketInfo.channels.length > 0">
                      <div v-for="(channel, idx) in message.ticketInfo.channels" :key="idx" class="channel-item">
                        <span class="channel-label">{{ channel.label }}</span>
                        <span class="channel-desc">{{ channel.desc }}</span>
                      </div>
                    </div>
                  </div>
                </div>
                
                <!-- 转人工客服卡片 - 新版（类似推荐商品确认卡片） -->
                <div v-if="message.type === 'transferToHuman'" class="transfer-human-card">
                  <div class="transfer-icon"></div>
                  <div class="transfer-content">
                    <div class="transfer-text">
                      <div class="transfer-title">工单已创建</div>
                      <div class="transfer-no" v-if="message.ticketNo">
                        工单编号：<strong>{{ message.ticketNo }}</strong>
                      </div>
                      <div class="transfer-desc">{{ message.message }}</div>
                    </div>
                    
                    <!-- 问题描述输入框 -->
                    <div class="transfer-description-input">
                      <textarea 
                        v-model="message.description" 
                        placeholder="请简要描述您的问题（选填）..."
                        rows="3"
                        class="description-textarea"
                      ></textarea>
                    </div>
                    
                    <!-- 操作按钮 -->
                    <div class="transfer-actions">
                      <button 
                        class="transfer-btn enter-chat"
                        @click="enterCustomerService(message)"
                      >
                        💬 进入在线客服
                      </button>
                    </div>
                    
                    <!-- 其他联系渠道 -->
                    <div class="transfer-channels" v-if="message.channels && message.channels.length > 0">
                      <div v-for="(channel, idx) in message.channels" :key="idx" class="channel-item">
                        <span class="channel-label">{{ channel.label }}</span>
                        <span class="channel-desc">{{ channel.desc }}</span>
                      </div>
                    </div>
                  </div>
                </div>
                </div>
              </div>
            </div>
            <div v-if="loading" class="loading-message">
              <span>{{ isPaused ? '已暂停' : '小皮正在思考...' }}</span>
            </div>
            
            <!-- AI工具快捷入口 - 固定在输入框上方 -->
            <div class="ai-tools-fixed-bar" v-show = 'false'>
              <button class="ai-tool-quick-btn" @click="handleToolOpen('copywriting')">
                <span class="quick-tool-icon">✍️</span>
                <span class="quick-tool-name">AI文案</span>
              </button>
              <button class="ai-tool-quick-btn" @click="handleToolOpen('price')">
                <span class="quick-tool-icon">📊</span>
                <span class="quick-tool-name">价格分析</span>
              </button>
              <button class="ai-tool-quick-btn" @click="handleToolOpen('forecast')">
                <span class="quick-tool-icon">📦</span>
                <span class="quick-tool-name">销量预测</span>
              </button>
            </div>
            
            <!-- 快捷指令区域 -->
            <div v-if="quickCommands && quickCommands.length > 0" class="quick-commands">
              <div class="quick-commands-title">快捷指令：</div>
              <div class="quick-commands-list">
                <button 
                  v-for="(cmd, index) in quickCommands" 
                  :key="index"
                  class="quick-command-btn"
                  @click="executeQuickCommand(cmd)"
                >
                  {{ cmd }}
                </button>
              </div>
            </div>
            
            <!-- 输入框 -->
            <div class="ai-chat-input">
              <input 
                type="text" 
                v-model="inputMessage" 
                placeholder="请输入您的问题或需求，如：帮我找手机、添加购物车..."
                @keyup.enter="sendMessage"
                :class="{ 'listening': isListening }"
              />
              <!-- 语音输入按钮 -->
              <button 
                class="voice-btn"
                @click="toggleVoiceInput"
                :class="{ 'listening': isListening }"
                title="语音输入"
              >
                <span class="voice-icon">🎤</span>
                <span v-if="isListening" class="voice-wave">
                  <span class="wave-bar"></span>
                  <span class="wave-bar"></span>
                  <span class="wave-bar"></span>
                </span>
              </button>
              <!-- 发送按钮和停止按钮切换 -->
              <button 
                v-if="!loading" 
                class="send-btn" 
                @click="sendMessage" 
                :disabled="!inputMessage.trim()"
              >
                发送
              </button>
              <button 
                v-else 
                class="stop-btn" 
                @click="stopGeneration"
              >
                ⏹ 停止
              </button>
            </div>
          </div>
        </div>
        
        <!-- 小三角 - 切换按钮显示 -->
        <div class="tools-toggle-arrow" v-if="isOpen && currentMode === 'help' && !showHistoryPanel" @click="showMessageMgmtBtn = !showMessageMgmtBtn" :class="{ 'arrow-up': showMessageMgmtBtn }"></div>
        
        <!-- AI工具浮动按钮 - 发送按钮上方偏右 -->
        <div class="ai-tools-float-btn" v-if="isOpen && currentMode === 'help' && !showHistoryPanel">
          <span class="tools-icon" @click="showAiTools = !showAiTools">🛠</span>
        </div>
        
        <!-- 蓝色消息管理按钮 - AI工具正上方，可切换显示 -->
        <div class="message-mgmt-toggle-btn" :class="{ 'show': showMessageMgmtBtn }" v-if="isOpen && currentMode === 'help' && !showHistoryPanel" @click="showMessagePanel = !showMessagePanel">
          <span class="toggle-icon">📋</span>
        </div>
        
        <!-- AI工具面板 -->
        <div class="ai-tools-panel" v-if="showAiTools">
          <div class="panel-header">
            <span>AI 工具</span>
            <button class="panel-close" @click="showAiTools = false">×</button>
          </div>
          <div class="panel-body">
            <button class="panel-tool-btn" @click="handleToolOpen('copywriting'); showAiTools = false">
              <span class="panel-tool-icon">✍️</span>
              <span>文案生成</span>
            </button>
            <button class="panel-tool-btn" @click="handleToolOpen('price'); showAiTools = false">
              <span class="panel-tool-icon"></span>
              <span>价格分析</span>
            </button>
            <button class="panel-tool-btn" @click="handleToolOpen('forecast'); showAiTools = false">
              <span class="panel-tool-icon">📦</span>
              <span>销量预测</span>
            </button>
          </div>
        </div>
        
        <!-- 消息管理面板 -->
        <div class="message-mgmt-panel" v-if="showMessagePanel">
          <div class="mgmt-panel-header">
            <span>消息管理</span>
            <button class="mgmt-panel-close" @click="showMessagePanel = false">×</button>
          </div>
          <div class="mgmt-panel-body">
            <button class="mgmt-panel-btn" @click="toggleSearch; showMessagePanel = false">
              <span class="mgmt-panel-icon">🔍</span>
              <span>搜索</span>
            </button>
            <button class="mgmt-panel-btn" @click="copyAllMessages; showMessagePanel = false">
              <span class="mgmt-panel-icon">📋</span>
              <span>复制全部</span>
            </button>
            <button class="mgmt-panel-btn" @click="exportMessages; showMessagePanel = false">
              <span class="mgmt-panel-icon">💾</span>
              <span>导出</span>
            </button>
            <button class="mgmt-panel-btn danger" @click="clearCurrentMessages; showMessagePanel = false">
              <span class="mgmt-panel-icon">🗑️</span>
              <span>清空</span>
            </button>
          </div>
          <!-- 搜索框 -->
          <div class="mgmt-panel-search" v-if="showSearchBox">
            <input 
              type="text" 
              v-model="searchKeyword" 
              placeholder="搜索消息..."
              class="mgmt-panel-search-input"
              @input="searchMessages"
            />
            <button class="mgmt-panel-search-close" @click="closeSearch">×</button>
          </div>
          <!-- 搜索结果统计 -->
          <div v-if="searchKeyword && searchResults.length > 0" class="mgmt-panel-search-stats">
            找到 {{ searchResults.length }} 条相关消息
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { fetchEventSource } from '@microsoft/fetch-event-source'
import request from '../../utils/request'
import eventBus from '../../utils/eventBus'
import SentimentAnalysis from './SentimentAnalysis.vue'
import SalesReport from './SalesReport.vue'
import ProductRecommendation from './ProductRecommendation.vue'
import SearchResults from './SearchResults.vue'
import OrderAnalysis from './OrderAnalysis.vue'
import OrderList from './OrderList.vue'
import GreetingCard from './GreetingCard.vue'

export default {
  name: 'AiChat',
  components: {
    SentimentAnalysis,
    SalesReport,
    ProductRecommendation,
    SearchResults,
    OrderAnalysis,
    OrderList,
    GreetingCard
  },
  data() {
    return {
      isOpen: false, // 聊天窗口是否打开
      messages: [], // 消息列表
      inputMessage: '', // 输入消息
      loading: false, // 加载状态
      isPaused: false, // 是否暂停
      userId: null, // 用户 ID
      userRole: 'user', // 用户角色：user 或 admin
      currentMode: 'help', // 当前模式：chat（对话）或 help（智能帮助），默认使用智能帮助模式
      position: {
        x: 0,
        y: 0
      },
      isDragging: false,
      startX: 0,
      startY: 0,
      abortController: null, // 用于取消流式请求
      aiMessageIndex: -1, // 当前 AI 消息的索引
      aiMessageContent: '', // 当前 AI 消息的内容
      // 语音输入相关
      recognition: null, // 语音识别对象
      isListening: false, // 是否正在监听
      isSupported: false, // 是否支持语音识别
      retryCount: 0, // 重试次数
      maxRetryCount: 2, // 最大重试次数
      // 快捷指令
      quickCommands: [], // 快捷指令列表
      // 历史记录相关
      showHistoryPanel: false, // 是否显示历史记录面板
      conversations: [], // 对话列表
      currentConversationId: null, // 当前对话 ID
      isSaving: false, // 是否正在保存
      // 消息管理相关
      showSearchBox: false, // 是否显示搜索框
      searchKeyword: '', // 搜索关键词
      searchResults: [], // 搜索结果
      editingMessageIndex: -1, // 正在编辑的消息索引
      editingMessageContent: '', // 编辑中的消息内容
      showExpandToolbar: false, // 是否展开更多功能区域
      showGreetingCard: true, // 是否显示问候语卡片
      showAiTools: false, // 是否显示AI工具面板
      showMessagePanel: false, // 是否显示消息管理面板
      showMessageMgmtBtn: false // 是否显示蓝色消息管理按钮
    }
  },
  computed: {
    hasUserMessages() {
      return this.messages.some(msg => msg.role === 'user')
    },
    isChatOpen() {
      return this.isOpen
    },
    displayMessages() {
      if (this.searchKeyword && this.searchResults.length > 0) {
        return this.searchResults
      }
      return this.messages
    }
  },
  mounted() {
    // 初始化用户 ID（如果已登录）
    this.initUserId()
    
    // 添加欢迎消息
    this.addWelcomeMessage()
    
    // 初始化语音识别
    this.initVoiceRecognition()
    
    // 加载历史对话列表
    this.loadConversations()
    
    // 监听鼠标移动和释放事件
    document.addEventListener('mousemove', this.onMouseMove)
    document.addEventListener('mouseup', this.onMouseUp)

    // 监听打开客服事件
    eventBus.$on('open-customer-service', () => {
      if (!this.isOpen) {
        this.toggleChatWindow()
      }
      if (this.currentMode !== 'customer') {
        this.switchMode('customer')
      }
      this.showGreetingCard = false
    })

    // 监听登录事件
    window.addEventListener('storage', (e) => {
      if (e.key === 'user') {
        console.log('检测到用户登录状态变化')
        this.initUserId()
        this.loadConversations()
      }
    })
  },
  beforeDestroy() {
    // 移除事件监听
    document.removeEventListener('mousemove', this.onMouseMove)
    document.removeEventListener('mouseup', this.onMouseUp)

    // 移除事件总线监听
    eventBus.$off('open-customer-service')

    // 取消正在进行的请求
    if (this.abortController) {
      this.abortController.abort()
    }
  },
  methods: {
    // 初始化语音识别
    initVoiceRecognition() {
      // 检查浏览器是否支持语音识别
      const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
      
      if (!SpeechRecognition) {
        console.warn('浏览器不支持语音识别功能')
        this.isSupported = false
        return
      }
      
      this.isSupported = true
      this.recognition = new SpeechRecognition()
      
      // 配置语音识别
      this.recognition.lang = 'zh-CN' // 设置语言为中文
      this.recognition.continuous = false // 不连续识别，说完一句自动停止
      this.recognition.interimResults = true // 显示临时结果
      
      // 语音识别开始事件
      this.recognition.onstart = () => {
        this.isListening = true
        console.log('语音识别已开始')
      }
      
      // 语音识别结束事件
      this.recognition.onend = () => {
        this.isListening = false
        console.log('语音识别已结束')
      }
      
      // 语音识别结果事件
      this.recognition.onresult = (event) => {
        const results = event.results
        const lastResultIndex = results.length - 1
        const transcript = results[lastResultIndex][0].transcript.trim()
        
        console.log('语音识别结果:', transcript)
        
        // 将识别结果添加到输入框
        if (transcript) {
          this.inputMessage = transcript
          
          // 如果是最终结果，自动发送
          if (results[lastResultIndex].isFinal) {
            // 可以添加提示音或视觉效果
            this.$message.success('语音识别成功，已自动填充到输入框')
          }
        }
      }
      
      // 语音识别错误事件
      this.recognition.onerror = (event) => {
        console.error('语音识别错误:', event.error)
        this.isListening = false
        
        // 网络错误时尝试重试
        if (event.error === 'network' && this.retryCount < this.maxRetryCount) {
          this.retryCount++
          console.log(`语音识别网络错误，第${this.retryCount}次重试...`)
          
          setTimeout(() => {
            try {
              this.recognition.start()
              this.$message.info(`网络不稳定，正在重试语音识别 (${this.retryCount}/${this.maxRetryCount})`)
            } catch (error) {
              console.error('重试失败:', error)
            }
          }, 1000)
          return
        }
        
        // 重置重试次数
        this.retryCount = 0
        
        let errorMessage = '语音识别失败'
        switch (event.error) {
          case 'no-speech':
            errorMessage = '未检测到语音，请对着麦克风说话'
            break
          case 'audio-capture':
            errorMessage = '无法访问麦克风，请检查麦克风设备'
            break
          case 'not-allowed':
            errorMessage = '麦克风权限被拒绝，请在浏览器设置中允许麦克风权限'
            break
          case 'network':
            errorMessage = '语音服务连接失败。这可能是因为您的网络无法连接浏览器的语音识别服务器（如 Google 服务）。建议：1.使用国内浏览器如 360、QQ 浏览器；2.或使用文字输入'
            break
          case 'aborted':
            errorMessage = '语音识别已取消'
            break
          case 'no-speech':
            errorMessage = '未检测到语音输入'
            break
          case 'service-not-allowed':
            errorMessage = '浏览器不允许使用语音识别服务'
            break
          case 'bad-grammar':
            errorMessage = '语法错误'
            break
          case 'language-not-supported':
            errorMessage = '不支持的语言设置'
            break
          default:
            errorMessage = '语音识别失败：' + event.error + '。如果多次失败，建议使用文字输入'
        }
        
        this.$message.warning(errorMessage)
      }
    },
    
    // 切换语音输入
    toggleVoiceInput() {
      if (!this.isSupported) {
        this.$message.warning('您的浏览器不支持语音输入功能，建议使用 Chrome、Edge 等现代浏览器')
        return
      }
      
      if (!this.recognition) {
        this.$message.error('语音识别未初始化')
        return
      }
      
      if (this.isListening) {
        // 正在监听，停止识别
        this.recognition.stop()
        this.retryCount = 0 // 重置重试次数
      } else {
        // 重置重试次数
        this.retryCount = 0
        
        // 未监听，开始识别
        try {
          this.recognition.start()
          this.$message.info('请开始说话，我会识别您的语音')
        } catch (error) {
          console.error('启动语音识别失败:', error)
          this.$message.error('启动语音识别失败，请检查麦克风权限')
        }
      }
    },
    
    // 初始化用户 ID（如果已登录）
    initUserId() {
      // 从 localStorage 获取用户信息（项目中存储的键名是 "user"）
      const userStr = localStorage.getItem('user')
      if (userStr) {
        try {
          const user = JSON.parse(userStr)
          this.userId = user.id
          console.log('已获取用户 ID:', this.userId)
          
          // 获取用户角色
          this.getUserRole()
          
          // 如果有用户 ID，重新加载对话列表
          if (this.userId) {
            this.loadConversations()
          }
        } catch (e) {
          console.error('解析用户信息失败:', e)
          this.userId = null
        }
      } else {
        console.log('用户未登录或用户信息不存在')
        this.userId = null
      }
    },
    
    // 获取用户角色
    async getUserRole() {
      try {
        const res = await this.request.post('/role')
        if (res.code === '200') {
          this.userRole = res.data
          console.log('用户角色:', this.userRole)
          
          // 如果是管理员，添加管理员欢迎消息
          if (this.userRole === 'admin') {
            this.addAdminWelcomeMessage()
          }
        }
      } catch (e) {
        console.error('获取用户角色失败:', e)
        this.userRole = 'user'
      }
    },
    
    // 添加管理员欢迎消息
    addAdminWelcomeMessage() {
      if (this.currentMode === 'help') {
        return
      }
      this.messages.push({
        role: 'assistant',
        content: '你好！我是小皮助手，管理员专属AI助手。我可以帮您：\n\n' +
          '📊 数据分析：营收分析、用户增长、商品销量统计\n' +
          '🛒 商品管理：智能文案生成、价格分析、库存预测\n' +
          '📋 订单管理：订单查询、异常订单分析、物流跟踪\n' +
          '👥 用户管理：用户行为分析、活跃度统计、权限管理\n' +
          '🤖 AI工具：智能客服话术生成、营销文案创作、竞品分析\n' +
          '\n请问有什么可以帮助您的？'
      })
    },
    
    // 切换模式
    switchMode(mode) {
      this.currentMode = mode
      // 添加模式切换消息
      let modeMessage = ''
      if (mode === 'chat') {
        modeMessage = '已切换到对话模式，您可以与我自由聊天。'
      } else if (mode === 'help') {
        modeMessage = '已切换到智能帮助模式，我可以帮您执行购物相关操作。'
      } else if (mode === 'customer') {
        modeMessage = '已切换到智能客服模式！\n\n' +
          '🤖 基于 ReAct 推理引擎，支持以下能力：\n' +
          '• 商品搜索、推荐、详情查询\n' +
          '• 购物车管理、订单创建与查询\n' +
          '• 售后服务、退换货政策\n' +
          '• 机票/酒店出行服务\n' +
          '• 知识库问答\n\n' +
          '💡 试试说："推荐适合我的商品"、"帮我查一下订单"'
      } else if (mode === 'purchase') {
        modeMessage = '已切换到真实购买模式！\n\n' +
          '🛍️ 支持以下真实购买功能：\n' +
          '• 商品搜索：根据您的需求搜索真实商品\n' +
          '• 商品推荐：为您推荐热销和好评商品\n' +
          '• 商品详情：查看价格、规格、评价等信息\n' +
          '• 加入购物车：将心仪商品加入购物车\n' +
          '• 创建订单：确认购买并生成订单\n' +
          '• 订单查询：查看您的历史订单\n\n' +
          '💡 试试说："我想买手机"、"推荐一些好用的护肤品"'
      }
      this.messages.push({
        role: 'assistant',
        content: modeMessage
      })
      // 强制更新视图并滚动到底部
      this.$nextTick(() => {
        this.scrollToBottom()
      })
    },
    
    // 添加欢迎消息
    addWelcomeMessage() {
      // 智能帮助模式下不添加欢迎消息，因为已有问候卡片
      if (this.currentMode === 'help') {
        return
      }
      this.messages.push({
        role: 'assistant',
        content: '你好！我是小皮助手，很高兴为您服务。我可以帮您搜索商品、添加购物车、查询订单、导航到指定页面，以及解答购物相关的问题。请问有什么可以帮助您的？'
      })
    },
    
    // 切换聊天窗口
    toggleChatWindow() {
      this.isOpen = !this.isOpen
      if (this.isOpen) {
        // 重新初始化用户 ID
        this.initUserId()
        // 滚动到底部
        this.scrollToBottom()
        // 如果没有当前对话，创建一个
        if (!this.currentConversationId) {
          this.createNewConversation()
        }
      } else {
        // 关闭窗口时重置状态
        this.showHistoryPanel = false
        this.showAiTools = false
        this.showMessagePanel = false
        this.showMessageMgmtBtn = false
      }
    },
    
    // 开始拖动
    startDrag(e) {
      if (!this.isOpen) return
      
      // 阻止事件冒泡，避免影响其他元素
      e.stopPropagation()
      
      this.isDragging = true
      this.startX = e.clientX - this.position.x
      this.startY = e.clientY - this.position.y
    },
    
    // 鼠标移动
    onMouseMove(e) {
      if (!this.isDragging) return
      
      // 阻止事件冒泡
      e.stopPropagation()
      
      // 计算新位置
      const newX = e.clientX - this.startX
      const newY = e.clientY - this.startY
      
      // 更新位置
      this.position.x = newX
      this.position.y = newY
    },
    
    // 鼠标释放
    onMouseUp() {
      this.isDragging = false
    },
    
    // 切换暂停状态
    togglePause() {
      this.isPaused = !this.isPaused
      if (!this.isPaused && this.loading) {
        // 如果从暂停状态恢复，继续处理
        this.scrollToBottom()
      }
    },
    
    // 发送消息
    async sendMessage() {
      if (!this.inputMessage.trim() || this.loading) return
      
      // 添加用户消息到列表
      const userMessage = {
        role: 'user',
        content: this.inputMessage.trim()
      }
      this.messages.push(userMessage)
      
      // 清空输入框
      const message = this.inputMessage.trim()
      this.inputMessage = ''
      
      // 滚动到底部
      this.scrollToBottom()
      
      // 设置加载状态
      this.loading = true
      this.isPaused = false
      
      // 如果是第一条消息，先保存到数据库（触发 AI 生成标题）
      const isFirstMessage = this.messages.filter(m => m.role === 'user').length === 1
      if (isFirstMessage && this.currentConversationId) {
        await this.addMessageToConversation('user', message, null, null)
        // 延迟一点刷新列表，等待 AI 生成标题
        setTimeout(() => {
          this.loadConversations()
        }, 2000)
      } else if (this.currentConversationId) {
        // 非第一条消息也要保存
        await this.addMessageToConversation('user', message, null, null)
      }
      
      // 发送消息到后端
      await this.sendToBackend(message)
    },
    
    // 发送消息到后端
    async sendToBackend(message) {
      const self = this; // ✅ 固定 this 指向，避免回调中 this 丢失
      
      // 构建请求参数
      const requestData = {
        message: message,
        userId: this.userId,
        history: this.messages.slice(-10), // 只发送最近 10 条消息作为上下文
        mode: this.currentMode // 当前模式：chat（对话）或 help（智能帮助）或 purchase（MCP购买）
      }
      
      // 初始化 AI 消息
      this.aiMessageIndex = this.messages.length
      this.aiMessageContent = ''
      this.hasReceivedAction = false // 新增：标记是否收到action事件
      this.messages.push({
        role: 'assistant',
        content: ''
      })
      
      // 尝试使用流式请求
      try {
        this.loading = true
        
        // 创建中止控制器
        this.abortController = new AbortController()
        
        // 使用 fetchEventSource 发送流式请求
        const headers = {
          'Content-Type': 'application/json'
        }
        // 添加 token，与 request.js 中的逻辑保持一致
        let userStr = localStorage.getItem("user")
        if(userStr){
          const user = JSON.parse(userStr)
          if(user?.token) headers.token = user.token
        }
        
        // 根据模式选择不同的API端点
        let apiUrl = 'http://localhost:9191/api/ai/chat/stream'
        if (this.currentMode === 'purchase') {
          apiUrl = 'http://localhost:9191/api/mcp/purchase/chat'
        } else if (this.currentMode === 'customer') {
          apiUrl = 'http://localhost:9191/api/ai/smart/chat/stream'
        }
        
        console.log('开始发送流式请求')
        console.log('请求 URL:', apiUrl)
        console.log('请求头:', headers)
        console.log('请求体:', JSON.stringify(requestData))
        
        // ✅ 关键修复：POST 请求必须手动处理流
        await fetchEventSource(apiUrl, {
          method: 'POST',
          responseType: 'text', // ✅ 必须加！否则流无法解析
          headers: headers,
          body: JSON.stringify(requestData),
          signal: this.abortController.signal,
          
          // ✅ 关键：POST 请求必须手动解析流
          async onopen(response) {
            console.log('====== 连接已打开 ======')
            console.log('响应状态:', response.status)
            if (!response.ok) {
              throw new Error('请求失败：' + response.status)
            }
          },
          
          // ✅ 关键：收到消息（使用 function 而不是箭头函数，保持 this 指向）
          onmessage: function(event) {
            console.log('✅ 真正收到消息：', event)
            console.log('event.event:', event.event)
            console.log('event.data:', event.data)
            console.log('收到数据的时间:', new Date().toISOString())
            
            if (self.isPaused) {
              console.log('当前处于暂停状态，跳过处理')
              return
            }
            
            // ✅ 规范性：同时检查 [DONE] 和 event: done
            if (event.data === '[DONE]' || (event.event && event.event === 'done')) {
              // 流式结束
              console.log('🎉 流式结束 - 收到 DONE 标志')
              console.log('结束时间:', new Date().toISOString())
              self.loading = false
              self.isPaused = false
              self.abortController = null
              self.scrollToBottom()
              
              // 确保AI消息内容已更新到messages数组
              if (self.aiMessageIndex >= 0 && self.aiMessageContent) {
                self.messages[self.aiMessageIndex].content = self.aiMessageContent
              }
              
              // 保存对话到数据库
              self.saveConversation()
              
              // 刷新对话列表
              setTimeout(() => {
                self.loadConversations()
              }, 1000)
              return
            }
            
            try {
              console.log('尝试解析数据:', event.data)
              const data = JSON.parse(event.data)
              console.log('解析后的数据:', data)
              
              // ✅ 关键：处理 action 事件（多重检查确保能收到）
              // 检查 1: event.event === 'action'
              // 检查 2: data.action 存在
              // 检查 3: 直接检查 data 中是否包含 action 字段
              const isActionEvent = event.event === 'action' || data.action || data.type === 'action'
              
              if (isActionEvent) {
                console.log('🎯 收到 action 事件！')
                console.log('完整 data 对象:', JSON.stringify(data, null, 2))
                console.log('data.action:', data.action)
                console.log('data.actionData:', data.actionData)
                console.log('data.actionData.topGood:', data.actionData?.topGood)
                console.log('data.actionData.otherGoods:', data.actionData?.otherGoods)
                console.log('data.type:', data.type)
                
                // 设置标志位，表示已收到action事件
                self.hasReceivedAction = true
                
                // 提取 action 和 params
                const actionType = data.action || data.type
                // 直接传递整个 actionData，让 handleAction 方法自己处理
                const params = data.actionData
                
                console.log('提取的 actionType:', actionType)
                console.log('提取的 params:', JSON.stringify(params, null, 2))
                
                // 构建 action 对象传给 handleAction
                const actionObj = {
                  type: actionType,
                  params: params
                }
                
                console.log('准备调用 handleAction:', actionObj)
                self.handleAction(actionObj)
                console.log('handleAction 调用完成')
                
                // 收到action事件后，更新消息内容（如果带了文案）
                if (data.content) {
                  self.aiMessageContent = data.content
                  if (self.aiMessageIndex >= 0) {
                    self.messages[self.aiMessageIndex].content = data.content
                  }
                } else {
                  self.aiMessageContent = ''
                  if (self.aiMessageIndex >= 0) {
                    self.messages[self.aiMessageIndex].content = ''
                  }
                }
              }
              
              // 处理 content 事件
              if (data.content) {
                console.log('收到content事件，data:', data)
                console.log('data.type:', data.type)
                
                // 如果已收到action事件，忽略后续的content事件，避免显示"抱歉，我暂时无法处理该请求"
                // 注意：智能客服模式（customer）需要同时显示商品卡片和文本内容，不跳过
                if (self.hasReceivedAction && self.currentMode !== 'customer') {
                  console.log('已收到action事件，忽略content事件')
                  return
                }
                
                // 检查是否是思考过程消息
                if (data.type === 'thinking') {
                  console.log('✅ 收到思考过程消息！')
                  // 将思考过程添加到当前 AI 消息
                  if (data.thinkingSteps && data.thinkingSteps.length > 0) {
                    self.messages[self.aiMessageIndex].thinkingSteps = data.thinkingSteps
                    // 思考过程默认展开，让用户先看到
                    self.$set(self.messages[self.aiMessageIndex], 'thinkingCollapsed', false)
                    self.$set(self.messages[self.aiMessageIndex], 'thinkingFinished', true)
                    self.$forceUpdate()
                    self.scrollToBottom()
                  }
                }
                // 检查是否是确认推荐消息
                else if (data.type === 'confirmRecommend') {
                  console.log('✅ 收到确认推荐消息！')
                  console.log('category:', data.category)
                  console.log('content:', data.content)
                  
                  // 添加确认推荐消息
                  self.messages.push({
                    role: 'ai',
                    content: data.content,
                    type: 'confirmRecommend',
                    category: data.category
                  })
                  console.log('消息已添加到messages数组')
                  self.scrollToBottom()
                } else {
                  // 更新 AI 消息内容
                  self.aiMessageContent += data.content
                  console.log('当前消息内容:', self.aiMessageContent)
                  console.log('更新消息的时间:', new Date().toISOString())
                  
                  // 直接更新消息内容，确保实时显示
                  self.messages[self.aiMessageIndex].content = self.aiMessageContent
                  console.log('消息对象:', self.messages[self.aiMessageIndex])
                  // 强制 Vue 更新
                  self.$forceUpdate()
                  console.log('已强制更新 UI')
                  // 立即滚动到底部
                  self.scrollToBottom()
                  console.log('已滚动到底部')
                }
              }
            } catch (error) {
              console.error('解析流式数据失败:', error)
              console.error('原始数据:', event.data)
            }
          },
          
          onerror: function(error) {
            console.error('====== 流式连接错误 ======')
            console.error('错误详情:', error)
            self.fallbackToRegularRequest(requestData)
          }
        })
      } catch (error) {
        console.error('发送流式请求失败:', error)
        
        // 尝试使用普通请求作为 fallback
        self.fallbackToRegularRequest(requestData)
      }
    },
    
    // 使用普通请求作为 fallback
    async fallbackToRegularRequest(requestData) {
      try {
        let fallbackUrl = '/api/ai/chat'
        if (this.currentMode === 'purchase') {
          fallbackUrl = '/api/mcp/purchase/chat'
        } else if (this.currentMode === 'customer') {
          fallbackUrl = '/api/ai/smart/chat'
        }
        const data = await request.post(fallbackUrl, requestData)
        if (data.content) {
          this.messages[this.aiMessageIndex].content = data.content
          this.scrollToBottom()
        }
        
        // 处理 AI 执行的操作
        if (data.action) {
          this.handleAction(data.action)
        }
        
        // 处理快捷指令
        if (data.quickCommands) {
          this.handleQuickCommands(data.quickCommands)
        }
        
        // 保存对话到数据库
        this.saveConversation()
      } catch (error) {
        console.error('发送请求失败:', error)
        
        // 添加错误消息
        if (this.aiMessageIndex >= 0) {
          this.messages[this.aiMessageIndex].content = '抱歉，我暂时无法回答您的问题，请稍后再试。'
        } else {
          this.messages.push({
            role: 'assistant',
            content: '抱歉，我暂时无法回答您的问题，请稍后再试。'
          })
        }
      } finally {
        this.loading = false
        this.abortController = null
      }
    },
    
    // 手动停止生成
    stopGeneration() {
      console.log('====== 手动停止生成 ======')
      if (this.abortController) {
        this.abortController.abort()
        console.log('已中止请求')
      }
      this.loading = false
      this.isPaused = false
      this.abortController = null
      
      // 如果 AI 消息内容为空，添加提示
      if (this.aiMessageIndex >= 0 && !this.messages[this.aiMessageIndex].content.trim()) {
        this.messages[this.aiMessageIndex].content = '已停止生成。'
      }
      
      this.scrollToBottom()
    },
    
    // 处理 AI 执行的操作
    handleAction(action) {
      console.log('===== 开始处理 action =====')
      console.log('执行 AI 操作:', action)
      console.log('action.type:', action.type)
      console.log('action.params:', action.params)
      
      switch (action.type) {
        case 'search':
        case 'search_goods':
          // 处理搜索操作
          this.handleSearch(action.params)
          break
        case 'addCart':
        case 'add_to_cart':
          // 处理添加购物车操作
          this.handleAddCart(action.params)
          break
        case 'queryOrder':
        case 'query_order':
        case 'query_orders':
          // 处理订单查询操作
          this.handleQueryOrder(action.params)
          break
        case 'navigate':
          // 处理导航操作
          this.handleNavigate(action.params)
          break
        case 'recommend':
        case 'recommend_goods':
          // 处理个性化推荐（使用 params）
          this.handleRecommend(action.params)
          break
        case 'specificRecommend':
        case 'specific_recommend':
          // 处理具体品类推荐（复用 recommend 逻辑）
          this.handleRecommend(action.params)
          break
        case 'quickOrder':
        case 'quick_order':
          // 处理一键下单
          this.handleQuickOrder(action.params)
          break
        case 'trackOrder':
        case 'track_order':
          // 处理订单跟踪
          this.handleTrackOrder(action.params)
          break
        case 'analyzeOrders':
        case 'analyze_orders':
          // 处理订单分析
          this.handleAnalyzeOrders(action.params)
          break
        case 'analyzeSentiment':
        case 'analyze_sentiment':
          // 处理评论舆情分析
          this.handleAnalyzeSentiment(action.params)
          break
        case 'analyzeSales':
        case 'analyze_sales':
          // 处理销售数据报告
          this.handleAnalyzeSales(action.params)
          break
        case 'viewGood':
        case 'view_good':
        case 'view_good_detail':
        case 'getProductDetail':
          // MCP 工具：查看商品详情
          this.handleViewGood(action.params)
          break
        case 'purchase_search':
        case 'search_products':
          // MCP购买：搜索结果
          this.handlePurchaseSearch(action.params)
          break
        case 'purchase_recommend':
          // MCP购买：推荐商品
          this.handlePurchaseRecommend(action.params)
          break
        case 'purchase_addToCart':
          // MCP购买：加入购物车
          this.handlePurchaseAddToCart(action.params)
          break
        case 'purchase_viewCart':
          // MCP购买：查看购物车
          this.handlePurchaseViewCart(action.params)
          break
        case 'purchase_createOrder':
          // MCP购买：创建订单
          this.handlePurchaseCreateOrder(action.params)
          break
        case 'purchase_viewOrders':
          // MCP购买：查看订单
          this.handlePurchaseViewOrders(action.params)
          break
        case 'product_detail':
          // MCP 购买：商品详情选择
          this.handleProductDetail(action.params)
          break
        case 'transfer_to_human':
        case 'transferToHuman':
          // 处理转人工客服
          this.handleTransferToHuman(action.params)
          break
        default:
          console.log('未知操作类型:', action.type)
          break
      }
    },

    // 处理 MCP 商品详情操作 - 现在用于显示商品销售报告
    handleViewGood(params) {
      console.log('MCP 查看商品详情/销售报告:', params)
      
      // 检查是否是销售报告数据（包含 salesTrend、salesRank 等字段）
      if (params && (params.salesTrend || params.salesRank || params.targetGood)) {
        console.log('检测到销售报告数据，调用 handleAnalyzeSales')
        this.handleAnalyzeSales(params)
        return
      }
      
      // 处理 view_good_detail 返回的数据结构: {success: true, good: {...}, goodId: 2405}
      if (params && params.goodId) {
        console.log('从 params.goodId 获取商品ID:', params.goodId)
        this.$router.push(`/goodView/${params.goodId}`)
        return
      }
      
      // 处理 params.good 对象
      if (params && params.good && params.good.id) {
        console.log('从 params.good.id 获取商品ID:', params.good.id)
        this.$router.push(`/goodView/${params.good.id}`)
        return
      }
      
      // 处理 params.id
      if (params && params.id) {
        console.log('从 params.id 获取商品ID:', params.id)
        this.$router.push(`/goodView/${params.id}`)
        return
      }
      
      // 处理 params.goods 数组
      if (params && params.goods && params.goods.length > 0) {
        console.log('从 params.goods[0].id 获取商品ID:', params.goods[0].id)
        this.$router.push(`/goodView/${params.goods[0].id}`)
        return
      }
      
      console.warn('无法从 params 中提取商品ID:', params)
      this.$message.error('商品信息不完整，无法查看详情')
    },

    // MCP购买：处理搜索结果
    handlePurchaseSearch(params) {
      console.log('MCP购买 搜索结果:', params)
      const lastAiMessage = this.messages
        .slice()
        .reverse()
        .find(msg => msg.role === 'assistant')
      
      if (lastAiMessage && params && params.goods && params.goods.length > 0) {
        lastAiMessage.searchResults = params.goods
        lastAiMessage.dataSource = params.dataSource || params.source || '百度优选MCP'
        this.$message.success(`为您找到 ${params.total || params.goods.length} 个商品`)
        this.$forceUpdate()
      }
    },

    // MCP购买：处理推荐
    handlePurchaseRecommend(params) {
      console.log('MCP购买 推荐结果:', params)
      const lastAiMessage = this.messages
        .slice()
        .reverse()
        .find(msg => msg.role === 'assistant')
      
      if (lastAiMessage && params && params.goods && params.goods.length > 0) {
        lastAiMessage.recommendations = params.goods
        this.$message.success('为您推荐以下商品')
        this.$forceUpdate()
      }
    },

    // MCP购买：处理加入购物车
    handlePurchaseAddToCart(params) {
      console.log('MCP购买 加入购物车:', params)
      if (params && params.success) {
        this.$message.success('商品已添加到购物车')
      } else if (params && params.error) {
        this.$message.error(params.error)
      }
    },

    // MCP购买：处理查看购物车
    handlePurchaseViewCart(params) {
      console.log('MCP购买 查看购物车:', params)
      const lastAiMessage = this.messages
        .slice()
        .reverse()
        .find(msg => msg.role === 'assistant')
      
      if (lastAiMessage && params && params.items && params.items.length > 0) {
        lastAiMessage.cartItems = params.items
        this.$message.info(`购物车中有 ${params.total} 件商品`)
        this.$forceUpdate()
      }
    },

    // MCP购买：处理创建订单
    handlePurchaseCreateOrder(params) {
      console.log('MCP购买 创建订单:', params)
      if (params && params.success) {
        this.$message.success(`订单创建成功！订单号：${params.orderNo}`)
        // 跳转到订单页面
        this.$router.push('/orderList')
      } else if (params && params.error) {
        this.$message.error(params.error)
      }
    },

    // MCP购买：处理查看订单
    handlePurchaseViewOrders(params) {
      console.log('MCP购买 查看订单:', params)
      const lastAiMessage = this.messages
        .slice()
        .reverse()
        .find(msg => msg.role === 'assistant')
      
      if (lastAiMessage && params && params.orders && params.orders.length > 0) {
        lastAiMessage.orderList = params.orders
        this.$message.info(`您有 ${params.total} 个订单`)
        this.$forceUpdate()
      }
    },

    // 处理商品详情选择（用户说"第一款"等）
    handleProductDetail(params) {
      console.log('MCP购买 商品详情选择:', params)
      if (params && params.product) {
        const product = params.product
        const index = params.index
        const source = params.source || '百度优选MCP'
        
        this.$message.success(`已选择第${index}款商品（来自${source}）`)
        
        // 可以在这里添加更多处理逻辑，如显示商品详情弹窗等
      }
    },

    // 处理搜索操作
    handleSearch(params) {
      console.log('搜索操作，导航到商品列表页面:', params?.keyword)
      
      // 直接跳转到商品列表页面，携带搜索关键词
      if (params && params.keyword) {
        this.$router.push({ path: '/goodList', query: { keyword: params.keyword } })
      } else {
        this.$router.push('/goodList')
      }
    },
    
    // 处理添加购物车操作
    handleAddCart(params) {
      console.log('添加到购物车:', params)
      // 显示添加成功提示
      this.$message.success('商品已成功添加到购物车！')
      // 触发购物车刷新事件
      this.$root.$emit('refresh-cart')
    },
    
    // 处理订单查询操作
    handleQueryOrder(params) {
      console.log('查询订单:', params)
      
      // 如果订单数据存在，直接在聊天中显示
      if (params && params.orders && params.orders.length > 0) {
        // 获取最后一条AI消息
        const lastAiMessage = this.messages
          .slice()
          .reverse()
          .find(msg => msg.role === 'assistant')
        
        if (lastAiMessage) {
          // 兼容后端返回的字段名：total/page/limit 和 totalCount/currentPage/pageSize
          const totalCount = params.totalCount || params.total || params.count || params.orders.length
          const currentPage = params.currentPage || params.page || 1
          const pageSize = params.pageSize || params.limit || 10
          const hasMore = params.hasMore !== undefined ? params.hasMore : (currentPage * pageSize < totalCount)
          
          // 添加订单列表到消息中
          this.$set(lastAiMessage, 'orderList', params.orders)
          this.$set(lastAiMessage, 'orderCount', params.count || params.orders.length)
          this.$set(lastAiMessage, 'totalCount', totalCount)
          this.$set(lastAiMessage, 'currentPage', currentPage)
          this.$set(lastAiMessage, 'pageSize', pageSize)
          this.$set(lastAiMessage, 'hasMore', hasMore)
          this.$set(lastAiMessage, 'loadingMore', false)
          
          // 显示查询提示
          this.$message.success(`为您找到 ${params.count || params.orders.length} 个订单`)
          
          // 强制更新视图
          this.$forceUpdate()
        } else {
          console.warn('未找到最后一条AI消息')
        }
      } else {
        // 导航到订单列表页面
        this.$router.push('/orderList')
      }
    },
    
    // 查看订单详情
    viewOrderDetail(order) {
      console.log('查看订单详情:', order)
      this.$router.push(`/orderList`)
    },

    // 从订单分析报告跳转到订单列表
    handleViewOrders() {
      console.log('查看订单列表')
      const lastAiMessage = this.messages
        .slice()
        .reverse()
        .find(msg => msg.role === 'assistant' && msg.orderList && msg.orderList.length > 0)
      
      if (lastAiMessage) {
        this.$nextTick(() => {
          this.$forceUpdate()
        })
      } else {
        this.$router.push('/orderList')
      }
    },

    // 支付订单
    payOrder(order) {
      console.log('支付订单:', order)
      this.$message.info('正在跳转到支付页面...')
      // 可以跳转到支付页面
    },
    
    // 确认收货
    confirmReceive(order) {
      console.log('确认收货:', order)
      this.$message.success('确认收货成功！')
      // 可以调用API确认收货
    },
    
    // 处理导航操作
    handleNavigate(params) {
      console.log('===== 开始导航 =====')
      console.log('导航到:', params)
      console.log('params.path:', params?.path)
      console.log('params.page:', params?.page)
      
      if (params && params.path) {
        let path = params.path;
        console.log('原始路径:', path)
        
        // 确保路径以 / 开头
        if (!path.startsWith('/')) {
          path = '/' + path;
        }
        
        console.log('最终导航路径:', path)
        
        // 执行路由跳转
        this.$router.push(path).then(() => {
          console.log('路由跳转成功:', path)
          this.$message.success(`正在跳转到${params.page || '目标页面'}...`)
        }).catch(err => {
          console.error('路由跳转失败:', err)
          this.$message.error('跳转失败，请重试')
        })
      } else {
        console.warn('导航参数不完整，缺少 path 字段')
        this.$message.error('导航参数不完整')
      }
    },
    
    // 处理个性化推荐
    handleRecommend(params) {
      console.log('===== handleRecommend 开始 =====')
      console.log('个性化推荐 params:', params)
      console.log('params 类型:', Array.isArray(params) ? '数组' : '对象')
      console.log('params 长度:', Array.isArray(params) ? params.length : 'N/A')
      if (params && !Array.isArray(params)) {
        console.log('params 对象内容:', JSON.stringify(params, null, 2))
      }
      
      // 获取最后一条 AI 消息（注意：role 是'assistant'，不是'ai'）
      const lastAiMessage = this.messages
        .slice()
        .reverse()
        .find(msg => msg.role === 'assistant')
      
      console.log('找到的最后一条 AI 消息:', lastAiMessage)
      console.log('当前消息列表长度:', this.messages.length)
      console.log('aiMessageIndex:', this.aiMessageIndex)
      
      if (!lastAiMessage) {
        console.warn('未找到最后一条 AI 消息')
        return
      }
      
      // 检查是否是新格式（包含 topGood 和 otherGoods）
      if (params && params.topGood) {
        console.log('使用新格式推荐（首推 + 其他）')
        console.log('topGood 数据:', JSON.stringify(params.topGood, null, 2))
        console.log('otherGoods 数据:', JSON.stringify(params.otherGoods, null, 2))
        
        // 设置首推商品（使用 $set 确保响应式）
        this.$set(lastAiMessage, 'topGood', params.topGood)
        
        // 设置其他商品
        if (params.otherGoods && params.otherGoods.length > 0) {
          this.$set(lastAiMessage, 'otherGoods', params.otherGoods)
        } else {
          this.$set(lastAiMessage, 'otherGoods', [])
        }
        
        // 设置思考过程
        if (params.thinkingSteps && params.thinkingSteps.length > 0) {
          this.$set(lastAiMessage, 'thinkingSteps', params.thinkingSteps)
          // 思考完成后自动折叠
          this.$set(lastAiMessage, 'thinkingCollapsed', true)
          this.$set(lastAiMessage, 'thinkingFinished', true)
        }
        
        // 显示推荐提示
        const count = (params.otherGoods ? params.otherGoods.length : 0) + 1
        this.$message.success(`已为您推荐 ${count} 个商品`)
        
        // 强制更新视图
        this.$forceUpdate()
        console.log('已强制更新视图')
        console.log('lastAiMessage.topGood:', lastAiMessage.topGood)
        console.log('lastAiMessage.otherGoods:', lastAiMessage.otherGoods)
      } else {
        // 旧格式处理
        let recommendations = null
        let reason = 'AI 个性化推荐'
        
        if (params && Array.isArray(params)) {
          // params 直接是商品列表
          recommendations = params
          console.log('使用 params 直接作为推荐列表')
        } else if (params && params.recommendations) {
          // params 是包含 recommendations 的对象
          recommendations = params.recommendations
          reason = params.reason || reason
          console.log('使用 params.recommendations 作为推荐列表')
        } else if (params && params.items) {
          // params 是包含 items 的对象（兼容后端）
          recommendations = params.items
          reason = params.reason || reason
          console.log('使用 params.items 作为推荐列表')
        } else if (params && params.goods) {
          // params 是包含 goods 的对象（兼容 smartRecommend）
          recommendations = params.goods
          reason = params.reason || reason
          console.log('使用 params.goods 作为推荐列表')
        }
        
        console.log('最终推荐列表:', recommendations)
        console.log('推荐列表长度:', recommendations ? recommendations.length : 0)
        
        // 如果推荐结果中有商品数据，直接在聊天中显示
        if (recommendations && recommendations.length > 0) {
          console.log('推荐商品列表:', recommendations)
          
          // 添加推荐商品到消息中
          lastAiMessage.recommendations = recommendations
          console.log('已设置 lastAiMessage.recommendations')
          
          // 保存推荐信息（来源、类型等）
          lastAiMessage.recommendationInfo = {
            reason: reason,
            type: 'mixed',
            success: true,
            message: `为您推荐 ${recommendations.length} 个商品`
          }
          console.log('已设置 lastAiMessage.recommendationInfo')
          
          // 显示推荐提示
          this.$message.success(`已为您推荐 ${recommendations.length} 个商品`)
          
          // 强制更新视图
          this.$forceUpdate()
          console.log('已强制更新视图')
        } else {
          console.log('没有推荐数据，导航到商品列表页面')
          // 导航到商品列表页面，可以传递推荐参数
          this.$router.push('/goodList')
          // 显示推荐提示
          this.$message.success('已为您找到相关商品推荐！')
        }
      }
      console.log('===== handleRecommend 结束 =====')
    },
    
    // 处理一键下单
    handleQuickOrder(params) {
      console.log('一键下单:', params)
      
      // 如果返回了商品ID或商品信息，跳转到商品详情页
      if (params && params.goodId) {
        console.log('从 params.goodId 获取商品ID:', params.goodId)
        this.$router.push(`/goodView/${params.goodId}`)
        return
      }
      
      if (params && params.good && params.good.id) {
        console.log('从 params.good.id 获取商品ID:', params.good.id)
        this.$router.push(`/goodView/${params.good.id}`)
        return
      }
      
      // 如果没有商品信息，提示用户
      this.$message.warning('商品信息不完整，无法查看详情')
    },
    
    // 处理订单跟踪
    handleTrackOrder(params) {
      console.log('订单跟踪:', params)
      
      // 如果有订单数据，显示在聊天中
      if (params && params.orders && params.orders.length > 0) {
        // 获取最后一条AI消息
        const lastAiMessage = this.messages
          .slice()
          .reverse()
          .find(msg => msg.role === 'assistant')
        
        if (lastAiMessage) {
          // 兼容后端返回的字段名：total/page/limit 和 totalCount/currentPage/pageSize
          const totalCount = params.totalCount || params.total || params.count || params.orders.length
          const currentPage = params.currentPage || params.page || 1
          const pageSize = params.pageSize || params.limit || 10
          const hasMore = params.hasMore !== undefined ? params.hasMore : (currentPage * pageSize < totalCount)
          
          // 设置订单列表数据
          this.$set(lastAiMessage, 'orderList', params.orders)
          this.$set(lastAiMessage, 'orderCount', params.count || params.orders.length)
          this.$set(lastAiMessage, 'totalCount', totalCount)
          this.$set(lastAiMessage, 'currentPage', currentPage)
          this.$set(lastAiMessage, 'pageSize', pageSize)
          this.$set(lastAiMessage, 'hasMore', hasMore)
          this.$set(lastAiMessage, 'loadingMore', false)
          this.$message.success(`为您找到 ${params.count || params.orders.length} 个订单`)
          this.$forceUpdate()
        }
      } else if (params && params.orderNo) {
        // 导航到订单详情页
        this.$router.push('/orderDetail?orderNo=' + params.orderNo)
      } else {
        // 导航到订单列表页
        this.$router.push('/orderList')
      }
    },
    
    // 处理订单分析
    handleAnalyzeOrders(params) {
      console.log('订单分析:', params)
      
      // 获取最后一条AI消息
      const lastAiMessage = this.messages
        .slice()
        .reverse()
        .find(msg => msg.role === 'assistant')
      
      if (lastAiMessage && params && params.orders && params.orders.length > 0) {
        // 添加订单列表到消息中
        this.$set(lastAiMessage, 'orderList', params.orders)
        this.$set(lastAiMessage, 'orderCount', params.totalOrders || params.orders.length)
        this.$set(lastAiMessage, 'orderAnalysis', {
          totalOrders: params.totalOrders,
          totalAmount: params.totalAmount,
          avgAmount: params.avgAmount,
          maxAmount: params.maxAmount,
          pendingPay: params.pendingPay,
          shipped: params.shipped,
          completed: params.completed,
          cancelled: params.cancelled,
          insights: params.insights || [],
          monthlyConsumption: params.monthlyConsumption || {},
          hasMore: params.hasMore || false,
          currentOffset: params.currentOffset || 0,
          currentLimit: params.currentLimit || 10
        })
        
        // 显示分析提示
        const totalAmount = params.totalAmount ? `总消费 ¥${params.totalAmount}` : ''
        this.$message.success(`为您分析了 ${params.totalOrders || params.orders.length} 个订单，${totalAmount}`)
        
        // 强制更新视图
        this.$forceUpdate()
      } else {
        // 如果没有数据，导航到订单列表页面
        this.$router.push('/orderList')
      }
    },
    
    // 处理转人工客服
    handleTransferToHuman(params) {
      console.log('===== 转人工客服 =====')
      console.log('params:', params)
      
      if (params && params.ticketNo) {
        // 添加一个新的交互式消息卡片（类似推荐商品确认卡片）
        this.messages.push({
          role: 'assistant',
          type: 'transferToHuman',
          ticketNo: params.ticketNo,
          message: params.message || '已为您创建工单，请填写问题描述后进入在线客服',
          description: '',
          channels: params.channels || [],
          chatButtonUrl: params.chatButtonUrl || '/user-chat'
        })
        
        // 显示成功提示
        const ticketNo = params.ticketNo ? `工单编号：${params.ticketNo}` : ''
        this.$message.success(`工单已创建，${ticketNo}，请填写问题描述后进入在线客服`)
        
        // 强制更新视图
        this.$nextTick(() => {
          this.scrollToBottom()
        })
      } else {
        // 如果没有数据，显示默认提示
        this.$message.info('正在为您转接人工客服，请稍候...')
      }
    },
    
    // 进入在线客服
    enterCustomerService(ticketInfo) {
      console.log('进入在线客服:', ticketInfo)
      // 跳转到在线客服页面，并传递工单信息
      this.$router.push({
        path: ticketInfo.chatButtonUrl || '/user-chat',
        query: {
          ticketNo: ticketInfo.ticketNo,
          ticketId: ticketInfo.ticketId,
          fromAiChat: 'true'  // 标记是从 AI 客服跳转过来的
        }
      })
    },
    
    // 处理加载更多订单
    async handleLoadMoreOrders({ page, limit }) {
      console.log('加载更多订单:', { page, limit })
      
      // 获取最后一条AI消息
      const lastAiMessage = this.messages
        .slice()
        .reverse()
        .find(msg => msg.role === 'assistant' && msg.orderList)
      
      if (!lastAiMessage) return
      
      // 设置加载状态
      this.$set(lastAiMessage, 'loadingMore', true)
      
      try {
        const userId = this.$store.state.user?.id || this.$store.state.userId
        console.log('加载更多订单，userId:', userId)
        const response = await this.$http.post('/api/ai/orders/track-more', {
          userId,
          page,
          limit
        })
        
        console.log('加载更多订单响应:', response.data)
        
        if (response.data.success && response.data.data) {
          const data = response.data.data
          
          console.log('后端返回的分页数据:', {
            orders: data.orders?.length,
            total: data.total,
            hasMore: data.hasMore,
            page: data.page,
            limit: data.limit
          })
          
          // 追加新订单到现有列表
          const currentOrders = lastAiMessage.orderList || []
          const newOrders = data.orders || []
          this.$set(lastAiMessage, 'orderList', [...currentOrders, ...newOrders])
          
          // 更新分页状态
          this.$set(lastAiMessage, 'currentPage', data.page || page)
          this.$set(lastAiMessage, 'hasMore', data.hasMore !== undefined ? data.hasMore : false)
          this.$set(lastAiMessage, 'totalCount', data.total || lastAiMessage.totalCount || currentOrders.length)
          this.$set(lastAiMessage, 'loadingMore', false)
          
          console.log('更新后的分页状态:', {
            currentPage: lastAiMessage.currentPage,
            hasMore: lastAiMessage.hasMore,
            totalCount: lastAiMessage.totalCount,
            orderListLength: lastAiMessage.orderList.length
          })
          
          this.$message.success(`已加载 ${newOrders.length} 个更多订单`)
          this.$forceUpdate()
        } else {
          this.$message.error('加载失败：' + (response.data.message || '未知错误'))
          this.$set(lastAiMessage, 'loadingMore', false)
        }
      } catch (error) {
        console.error('加载更多订单失败:', error)
        this.$message.error('加载失败：' + error.message)
        this.$set(lastAiMessage, 'loadingMore', false)
      }
    },
    
    // 处理快捷操作
    handleQuickAction(action) {
      console.log('快捷操作:', action)
      this.inputMessage = action
      this.sendMessage()
    },
    
    // 处理提问
    handleAskQuestion(question) {
      console.log('提问:', question)
      this.inputMessage = question
      this.sendMessage()
    },
    
    // 处理工具打开
    handleToolOpen(tool) {
      const toolConfigs = {
        copywriting: {
          icon: '✍️',
          title: 'AI 文案生成',
          type: 'copywriting',
          resultIcon: '✨ 生成结果',
          data: { type: '标题', productName: '', category: '' }
        },
        price: {
          icon: '📊',
          title: '竞品价格分析',
          type: 'price',
          resultIcon: '📈 分析结果',
          data: { productName: '', currentPrice: null }
        },
        forecast: {
          icon: '📦',
          title: '库存销量预测',
          type: 'forecast',
          resultIcon: '🔮 预测结果',
          data: { productName: '', currentStock: null }
        }
      }
      
      const config = toolConfigs[tool]
      if (!config) return
      
      const userMessage = {
        role: 'user',
        content: `打开${config.title}工具`
      }
      
      const aiMessage = {
        role: 'assistant',
        content: `已为您打开${config.title}工具，请填写以下信息：`,
        toolForm: {
          ...config,
          loading: false,
          streaming: false,
          result: ''
        }
      }
      
      this.messages.push(userMessage, aiMessage)
      this.scrollToBottom()
      
      // 保存工具打开消息到历史
      if (this.currentConversationId) {
        this.saveConversation()
      }
    },
    
    // 执行工具
    async executeTool(message, toolType) {
      const form = message.toolForm.data
      if (!form.productName) {
        this.$message.warning('请输入商品名称')
        return
      }
      
      message.toolForm.loading = true
      message.toolForm.streaming = true
      message.toolForm.result = ''
      
      const urls = {
        copywriting: 'http://localhost:9191/api/ai/admin/copywriting/stream',
        price: 'http://localhost:9191/api/ai/admin/price-analysis/stream',
        forecast: 'http://localhost:9191/api/ai/admin/sales-forecast/stream'
      }
      
      const successMessages = {
        copywriting: '文案生成成功',
        price: '分析完成',
        forecast: '预测完成'
      }
      
      try {
        const user = JSON.parse(localStorage.getItem('user'))
        const token = user ? user.token : ''
        
        const response = await fetch(urls[toolType], {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'token': token
          },
          body: JSON.stringify(form)
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
                message.toolForm.streaming = false
                this.$message.success(successMessages[toolType])
                break
              }
              try {
                const parsed = JSON.parse(data)
                if (parsed.content) {
                  message.toolForm.result += parsed.content
                }
              } catch (e) {
                // ignore
              }
            }
          }
          
          this.scrollToBottom()
        }
      } catch (e) {
        this.$message.error('请求失败：' + e.message)
        message.toolForm.streaming = false
      } finally {
        message.toolForm.loading = false
        // 保存工具使用结果到历史消息
        if (this.currentConversationId) {
          await this.saveConversation()
        }
      }
    },
    
    // 格式化结果
    formatResult(text) {
      if (!text) return ''
      return text.replace(/\n/g, '<br>')
    },
    
    // 复制结果
    copyResult(text) {
      navigator.clipboard.writeText(text).then(() => {
        this.$message.success('已复制到剪贴板')
      })
    },
    
    // 处理评论舆情分析
    handleAnalyzeSentiment(params) {
      console.log('评论舆情分析:', params)
      
      // 获取最后一条 AI 消息
      const lastAiMessage = this.messages
        .slice()
        .reverse()
        .find(msg => msg.role === 'assistant')
      
      if (!lastAiMessage) {
        console.warn('未找到最后一条 AI 消息')
        return
      }
      
      // 设置舆情分析数据
      lastAiMessage.sentimentData = params
      
      // 显示提示
      this.$message.success('已为您生成评论舆情分析报告')
      
      // 强制更新视图
      this.$forceUpdate()
    },
    
    // 处理销售数据报告
    handleAnalyzeSales(params) {
      console.log('===== 销售数据报告 =====')
      console.log('params:', JSON.stringify(params, null, 2))
      console.log('params.sentimentDistribution:', params?.sentimentDistribution)
      console.log('params.wordCloudData:', params?.wordCloudData)
      console.log('params.ratingStats:', params?.ratingStats)
      console.log('params.totalReviews:', params?.totalReviews)
      
      // 获取最后一条 AI 消息
      const lastAiMessage = this.messages
        .slice()
        .reverse()
        .find(msg => msg.role === 'assistant')
      
      if (!lastAiMessage) {
        console.warn('未找到最后一条 AI 消息')
        return
      }
      
      // 设置销售报告数据
      lastAiMessage.salesData = params
      
      // 更新消息内容为后端返回的aiResponse，而不是LLM生成的内容
      if (params && params.message) {
        lastAiMessage.content = params.message
      } else {
        lastAiMessage.content = '已为您生成销售数据报告，请查看分析结果。'
      }
      
      // 强制更新视图
      this.$forceUpdate()
      
      // 延迟绘制图表，确保DOM已更新
      this.$nextTick(() => {
        this.drawSalesTrendChart(params.salesTrend)
      })
    },
    
    // 绘制销售趋势图表
    drawSalesTrendChart(trendData) {
      if (!trendData || trendData.length === 0) return
      
      const chartContainer = this.$refs.salesTrendChart
      if (!chartContainer) return
      
      // 清空容器
      chartContainer.innerHTML = ''
      
      // 创建简单的SVG折线图
      const width = chartContainer.clientWidth - 24
      const height = 176
      const padding = { top: 20, right: 20, bottom: 30, left: 50 }
      const chartWidth = width - padding.left - padding.right
      const chartHeight = height - padding.top - padding.bottom
      
      // 计算数据范围
      const salesValues = trendData.map(d => d.sales)
      const maxSales = Math.max(...salesValues)
      const minSales = Math.min(...salesValues)
      
      // 创建SVG
      const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg')
      svg.setAttribute('width', width)
      svg.setAttribute('height', height)
      svg.style.width = '100%'
      svg.style.height = '100%'
      
      // 创建折线
      let pathData = ''
      trendData.forEach((d, i) => {
        const x = padding.left + (i / (trendData.length - 1)) * chartWidth
        const y = padding.top + chartHeight - ((d.sales - minSales) / (maxSales - minSales || 1)) * chartHeight
        pathData += (i === 0 ? 'M' : 'L') + `${x},${y}`
      })
      
      const path = document.createElementNS('http://www.w3.org/2000/svg', 'path')
      path.setAttribute('d', pathData)
      path.setAttribute('fill', 'none')
      path.setAttribute('stroke', '#409eff')
      path.setAttribute('stroke-width', '2')
      svg.appendChild(path)
      
      // 添加数据点
      trendData.forEach((d, i) => {
        const x = padding.left + (i / (trendData.length - 1)) * chartWidth
        const y = padding.top + chartHeight - ((d.sales - minSales) / (maxSales - minSales || 1)) * chartHeight
        
        const circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle')
        circle.setAttribute('cx', x)
        circle.setAttribute('cy', y)
        circle.setAttribute('r', '3')
        circle.setAttribute('fill', '#409eff')
        svg.appendChild(circle)
      })
      
      // 添加X轴标签（只显示部分日期）
      const labelInterval = Math.ceil(trendData.length / 6)
      trendData.forEach((d, i) => {
        if (i % labelInterval === 0 || i === trendData.length - 1) {
          const x = padding.left + (i / (trendData.length - 1)) * chartWidth
          const text = document.createElementNS('http://www.w3.org/2000/svg', 'text')
          text.setAttribute('x', x)
          text.setAttribute('y', height - 5)
          text.setAttribute('text-anchor', 'middle')
          text.setAttribute('font-size', '10')
          text.setAttribute('fill', '#909399')
          text.textContent = d.date.substring(5) // 只显示月-日
          svg.appendChild(text)
        }
      })
      
      chartContainer.appendChild(svg)
    },
    
    // 查看商品详情
    viewGoodDetail(good) {
      console.log('查看商品详情:', good)
      if (good && good.id) {
        const targetPath = `/goodView/${good.id}`
        const currentPath = this.$route.path
        
        console.log('当前路径:', currentPath)
        console.log('目标路径:', targetPath)
        
        // 如果已经在商品详情页，但商品 ID 不同，需要刷新页面
        if (currentPath.startsWith('/goodView/')) {
          const currentGoodId = currentPath.split('/')[2]
          const targetGoodId = good.id.toString()
          
          if (currentGoodId !== targetGoodId) {
            console.log('商品 ID 不同，需要跳转')
            // 强制刷新页面，使用 query 参数触发更新
            this.$router.push(targetPath + '?refresh=' + Date.now()).catch(err => {
              if (err.name !== 'NavigationDuplicated') {
                console.error('导航失败:', err)
                this.$message.error('跳转失败，请稍后重试')
              }
            })
          } else {
            console.log('已在当前商品页面，跳过导航')
          }
        } else {
          // 不在商品详情页，直接跳转
          console.log('不在商品详情页，直接跳转')
          this.$router.push(targetPath).catch(err => {
            if (err.name !== 'NavigationDuplicated') {
              console.error('导航失败:', err)
              this.$message.error('跳转失败，请稍后重试')
            }
          })
        }
      } else {
        console.error('商品信息不完整，无法跳转:', good)
        this.$message.error('商品信息不完整，无法查看详情')
      }
    },
    
    // 截断文本
    truncateText(text, maxLength) {
      if (!text) return ''
      if (text.length <= maxLength) return text
      return text.substring(0, maxLength) + '...'
    },
    
    // 词云图相关方法
    getWordFontSize(value, wordCloudData) {
      if (!wordCloudData || wordCloudData.length === 0) return 12
      const maxValue = Math.max(...wordCloudData.map(w => w.value))
      const minValue = Math.min(...wordCloudData.map(w => w.value))
      const range = maxValue - minValue || 1
      const normalized = (value - minValue) / range
      return 12 + normalized * 18 // 12px - 30px
    },
    
    getWordColor(word, index) {
      const colors = [
        '#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399',
        '#1abc9c', '#3498db', '#9b59b6', '#e74c3c', '#2ecc71',
        '#f39c12', '#16a085', '#2980b9', '#8e44ad', '#c0392b'
      ]
      return colors[index % colors.length]
    },
    
    getWordOpacity(value, wordCloudData) {
      if (!wordCloudData || wordCloudData.length === 0) return 0.6
      const maxValue = Math.max(...wordCloudData.map(w => w.value))
      const minValue = Math.min(...wordCloudData.map(w => w.value))
      const range = maxValue - minValue || 1
      const normalized = (value - minValue) / range
      return 0.6 + normalized * 0.4 // 0.6 - 1.0
    },
    
    // 商品排序功能
    sortGoods(message, sortBy) {
      if (!message || !message.otherGoods || message.otherGoods.length === 0) {
        return
      }
      
      // 设置当前排序方式
      message.sortBy = sortBy
      
      // 根据排序方式排序
      switch (sortBy) {
        case 'sales':
          // 按销量降序
          message.otherGoods.sort((a, b) => (b.sales || 0) - (a.sales || 0))
          break
        case 'price':
          // 按价格升序
          message.otherGoods.sort((a, b) => {
            const priceA = a.price != null ? a.price : (a.saleMoney != null ? a.saleMoney / 100 : 0)
            const priceB = b.price != null ? b.price : (b.saleMoney != null ? b.saleMoney / 100 : 0)
            return priceA - priceB
          })
          break
        case 'rating':
          // 按评分降序
          message.otherGoods.sort((a, b) => (b.goodRating || 0) - (a.goodRating || 0))
          break
      }
      
      // 强制更新视图
      this.$forceUpdate()
    },
    
    // 添加到购物车
    addToCart(good) {
      console.log('添加到购物车:', good)
      if (!good || !good.id) {
        this.$message.error('商品信息不完整')
        return
      }
      
      // 每次添加时重新获取用户 ID
      const userStr = localStorage.getItem('user')
      if (!userStr) {
        this.$message.error('请先登录后再添加购物车')
        return
      }
      
      let user
      try {
        user = JSON.parse(userStr)
      } catch (e) {
        console.error('解析用户信息失败:', e)
        this.$message.error('登录信息异常，请重新登录')
        return
      }
      
      if (!user.id || !user.token) {
        this.$message.error('登录信息不完整，请重新登录')
        return
      }
      
      this.userId = user.id
      console.log('用户 ID:', this.userId)
      console.log('商品 ID:', good.id)
      
      // 先获取商品信息，获取第一个规格
      request.get('/api/good/' + good.id).then(res => {
        if (res.code === '200') {
          const goodDetail = res.data
          console.log('商品详情:', goodDetail)
          
          // 获取第一个规格
          let standard = '默认'
          if (goodDetail.standardList && goodDetail.standardList.length > 0) {
            standard = goodDetail.standardList[0]
          }
          
          console.log('使用规格:', standard)
          
          // 调用购物车 API
          request.post('/api/cart', {
            goodId: good.id,
            count: 1,
            userId: this.userId,
            standard: standard
          }).then(res => {
            console.log('添加购物车响应:', res)
            if (res.code === '200') {
                this.$message.success('商品已成功添加到购物车！')
                // 判断当前是否在购物车页面，如果是则刷新
                if (this.$route.path === '/cart') {
                  // 触发购物车页面刷新事件
                  this.$root.$emit('refresh-cart')
                }
              } else {
              this.$message.error(res.msg || '添加购物车失败')
            }
          }).catch(error => {
            console.error('添加购物车失败:', error)
            if (error.response) {
              console.error('错误状态码:', error.response.status)
              console.error('错误数据:', error.response.data)
            }
            this.$message.error('添加购物车失败，请稍后重试')
          })
        }
      }).catch(error => {
        console.error('获取商品信息失败:', error)
        this.$message.error('获取商品信息失败')
      })
    },
    
    // 立即购买 - 直接跳转到商品详情页
    buyNow(good) {
      console.log('===== 立即购买 =====')
      console.log('商品对象:', good)
      console.log('商品ID:', good.id)
      console.log('商品ID类型:', typeof good.id)
      console.log('商品名称:', good.name)
      
      if (!good || !good.id) {
        this.$message.error('商品信息不完整')
        return
      }
      
      // 检查是否登录
      const userStr = localStorage.getItem('user')
      if (!userStr) {
        this.$message.error('请先登录后再购买')
        return
      }
      
      let user
      try {
        user = JSON.parse(userStr)
      } catch (e) {
        this.$message.error('登录信息异常，请重新登录')
        return
      }
      
      if (!user.id || !user.token) {
        this.$message.error('登录信息不完整，请重新登录')
        return
      }
      
      // 直接跳转到商品详情页面 - 注意路由路径是 goodView（V大写）
      const targetPath = `/goodView/${good.id}`
      console.log('导航路径:', targetPath)
      this.$router.push({
        path: targetPath
      }).then(() => {
        console.log('跳转成功:', targetPath)
      }).catch(err => {
        console.error('跳转失败:', err)
        this.$message.error('跳转失败，请重试')
      })
    },
    
    // 获取商品在搜索结果中的索引
    getProductIndex(good) {
      const lastAiMessage = this.messages
        .slice()
        .reverse()
        .find(msg => msg.role === 'assistant' && msg.searchResults)
      
      if (lastAiMessage && lastAiMessage.searchResults) {
        const index = lastAiMessage.searchResults.findIndex(item => item.id === good.id)
        return index >= 0 ? index + 1 : 1
      }
      return 1
    },
    
    // 获取商品价格
    getGoodPrice(good) {
      if (good.price != null) return good.price
      if (good.saleMoney != null) return (good.saleMoney / 100).toFixed(2)
      return '0.00'
    },
    
    // 处理图片加载错误
    handleImageError(event) {
      event.target.style.display = 'none'
      const placeholder = document.createElement('div')
      placeholder.className = 'image-error-placeholder'
      placeholder.textContent = '📦'
      event.target.parentNode.appendChild(placeholder)
    },
    
    // 获取图片完整 URL
    getImageUrl(imgPath) {
      if (!imgPath) return ''
      // 如果已经是完整 URL，直接返回
      if (imgPath.startsWith('http://') || imgPath.startsWith('https://')) {
        return imgPath
      }
      // 如果是 emoji 图片，返回空字符串（会在模板中显示占位符）
      if (imgPath.startsWith('emoji:')) {
        return ''
      }
      // 添加 base URL
      const baseURL = 'http://localhost:9191'
      return baseURL + imgPath
    },
    
    // 判断是否为 emoji 图片
    isEmojiImage(imgPath) {
      return imgPath && imgPath.startsWith('emoji:')
    },
    
    // 获取 emoji
    getEmoji(imgPath) {
      if (!imgPath || !imgPath.startsWith('emoji:')) return '📦'
      const parts = imgPath.split(':')
      return parts.length >= 2 ? parts[1] : '📦'
    },
    
    // 获取背景颜色
    getEmojiBgColor(imgPath) {
      if (!imgPath || !imgPath.startsWith('emoji:')) return '#f5f5f5'
      const parts = imgPath.split(':')
      return parts.length >= 3 ? parts[2] : '#f5f5f5'
    },
    
    // 获取商品价格（兼容 price 和 saleMoney 字段）
    getGoodPrice(good) {
      if (!good) return '0.00'
      // 优先使用 price 字段
      if (good.price != null && good.price > 0) {
        return good.price.toFixed(2)
      }
      // 兼容 saleMoney 字段（需要除以 100）
      if (good.saleMoney != null && good.saleMoney > 0) {
        return (good.saleMoney / 100).toFixed(2)
      }
      return '--'
    },
    
    // 滚动到底部
    scrollToBottom() {
      this.$nextTick(() => {
        const container = this.$refs.messagesContainer
        if (container) {
          container.scrollTop = container.scrollHeight
        }
      })
    },
    
    // 执行快捷指令
    executeQuickCommand(command) {
      console.log('执行快捷指令:', command)
      this.inputMessage = command
      this.sendMessage()
    },
    
    // 处理后端响应中的快捷指令
    handleQuickCommands(quickCommands) {
      if (quickCommands && quickCommands.length > 0) {
        this.quickCommands = quickCommands
      } else {
        // 默认快捷指令
        this.quickCommands = ['查看订单', '商品推荐', '售后服务', '常见问题']
      }
    },
    
    // 处理确认推荐卡片
    handleConfirmRecommend(category, agreed) {
      console.log('确认推荐:', category, agreed)
      if (agreed) {
        // 用户同意，重新推荐（不带价格限制）
        this.inputMessage = `推荐性价比高的${category}`
        this.sendMessage()
      } else {
        // 用户拒绝，添加一条消息
        this.messages.push({
          role: 'user',
          content: '不用了'
        })
        this.messages.push({
          role: 'ai',
          content: '好的，如果您有其他需求，随时告诉我哦~'
        })
        this.scrollToBottom()
      }
    },
    
    // 切换思考过程折叠状态
    toggleThinking(message) {
      this.$set(message, 'thinkingCollapsed', !message.thinkingCollapsed)
    },
    
    // ========== 历史记录管理方法 ==========
    
    // 加载对话列表
    async loadConversations() {
      if (!this.userId) {
        console.log('用户未登录，跳过加载对话列表')
        return
      }
      
      try {
        console.log('开始加载对话列表，userId:', this.userId)
        const res = await request.get('/api/ai/conversations/list', {
          params: { userId: this.userId }
        })
        console.log('对话列表响应:', res)
        if (res.code === '200') {
          this.conversations = res.data || []
          console.log('对话列表加载成功，数量:', this.conversations.length)
        } else {
          console.error('对话列表加载失败:', res.msg)
          if (res.code === '401') {
            this.$message.error('登录已过期，请重新登录')
          }
        }
      } catch (error) {
        console.error('加载对话列表失败:', error)
        // 不显示错误提示，避免打扰用户
      }
    },
    
    // 切换历史记录面板显示
    toggleHistoryPanel() {
      this.showHistoryPanel = !this.showHistoryPanel
      if (this.showHistoryPanel) {
        this.loadConversations()
      }
    },
    
    // 收起侧边栏
    toggleCollapse() {
      this.showHistoryPanel = false
    },
    
    // 创建新对话
    async createNewConversation() {
      if (!this.userId) {
        this.$message.error('请先登录')
        return
      }
      
      try {
        console.log('创建新对话，userId:', this.userId)
        const res = await request.post('/api/ai/conversations/create', {
          userId: this.userId,
          title: '新对话',
          type: 'chat'
        })
        
        console.log('创建对话响应:', res)
        if (res.code === '200') {
          this.currentConversationId = res.data.id
          this.messages = [] // 清空消息列表
          this.addWelcomeMessage() // 添加欢迎消息
          this.showHistoryPanel = false // 关闭侧边栏
          this.$message.success('新对话已创建')
          this.loadConversations() // 刷新列表
        } else {
          console.error('创建对话失败:', res.msg)
          this.$message.error(res.msg || '创建对话失败')
        }
      } catch (error) {
        console.error('创建对话失败:', error)
        this.$message.error('创建对话失败，请检查网络连接')
      }
    },
    
    // 加载对话
    async loadConversation(conversationId) {
      if (!conversationId) return
      
      try {
        const res = await request.get(`/api/ai/conversations/${conversationId}`)
        if (res.code === '200') {
          const conversation = res.data
          this.currentConversationId = conversationId
          
          // 解析消息
          if (conversation.messages) {
            try {
              this.messages = JSON.parse(conversation.messages)
            } catch (e) {
              console.error('解析消息失败:', e)
              this.messages = []
            }
          } else {
            this.messages = []
          }
          
          // 不要关闭侧边栏，保持展开状态
          // this.showHistoryPanel = false // 注释掉这行
        }
      } catch (error) {
        console.error('加载对话失败:', error)
        this.$message.error('加载对话失败')
      }
    },
    
    // 保存当前对话
    async saveConversation() {
      if (!this.currentConversationId || !this.userId || this.isSaving) return
      
      this.isSaving = true
      try {
        await request.put(`/api/ai/conversations/${this.currentConversationId}/messages`, {
          messages: this.messages
        })
      } catch (error) {
        console.error('保存对话失败:', error)
      } finally {
        this.isSaving = false
      }
    },
    
    // 添加消息到当前对话
    async addMessageToConversation(role, content, action, actionData) {
      if (!this.currentConversationId) {
        // 如果没有当前对话，创建一个
        await this.createNewConversation()
        if (!this.currentConversationId) return
      }
      
      try {
        await request.post(`/api/ai/conversations/${this.currentConversationId}/messages`, {
          role,
          content,
          action,
          actionData
        })
      } catch (error) {
        console.error('添加消息失败:', error)
      }
    },
    
    // 删除对话
    async deleteConversation(conversationId) {
      if (!conversationId) return
      
      this.$confirm('确定要删除这个对话吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const res = await request.delete(`/api/ai/conversations/${conversationId}`)
          if (res.code === '200') {
            this.$message.success('删除成功')
            if (this.currentConversationId === conversationId) {
              this.currentConversationId = null
              this.messages = []
              this.addWelcomeMessage()
            }
            this.loadConversations() // 刷新列表
          }
        } catch (error) {
          console.error('删除失败:', error)
          this.$message.error('删除失败')
        }
      }).catch(() => {})
    },
    
    // 清空所有对话
    clearAllConversations() {
      if (!this.userId) return
      
      this.$confirm('确定要清空所有对话吗？此操作不可恢复！', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const res = await request.delete('/api/ai/conversations/clear', {
            params: { userId: this.userId }
          })
          if (res.code === '200') {
            this.$message.success('清空成功')
            this.currentConversationId = null
            this.conversations = []
            this.messages = []
            this.addWelcomeMessage()
          }
        } catch (error) {
          console.error('清空失败:', error)
          this.$message.error('清空失败')
        }
      }).catch(() => {})
    },
    
    // 获取对话类型图标
    getConversationIcon(type) {
      const icons = {
        'chat': '💬',
        'search': '🔍',
        'recommend': '🎁',
        'order': '📦'
      }
      return icons[type] || '💬'
    },
    
    // 格式化时间
    formatTime(timeStr) {
      if (!timeStr) return ''
      const date = new Date(timeStr)
      const now = new Date()
      const diff = now - date
      
      // 1 分钟内
      if (diff < 60000) {
        return '刚刚'
      }
      // 1 小时内
      if (diff < 3600000) {
        return Math.floor(diff / 60000) + '分钟前'
      }
      // 24 小时内
      if (diff < 86400000) {
        return Math.floor(diff / 3600000) + '小时前'
      }
      // 7 天内
      if (diff < 604800000) {
        return Math.floor(diff / 86400000) + '天前'
      }
      
      // 格式化日期
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      return `${year}-${month}-${day}`
    },
    
    // ========== 消息管理方法 ==========
    
    // 切换搜索框
    toggleSearch() {
      this.showSearchBox = !this.showSearchBox
      if (!this.showSearchBox) {
        this.searchKeyword = ''
        this.searchResults = []
      }
    },
    
    // 切换展开更多功能区域
    toggleExpandToolbar() {
      this.showExpandToolbar = !this.showExpandToolbar
    },
    
    // 关闭搜索
    closeSearch() {
      this.showSearchBox = false
      this.searchKeyword = ''
      this.searchResults = []
    },
    
    // 搜索消息
    searchMessages() {
      if (!this.searchKeyword.trim()) {
        this.searchResults = []
        return
      }
      
      const keyword = this.searchKeyword.toLowerCase().trim()
      this.searchResults = this.messages.filter(msg => {
        // 搜索普通消息内容
        if (msg.content && msg.content.toLowerCase().includes(keyword)) {
          return true
        }
        // 搜索工具表单数据
        if (msg.toolForm) {
          if (msg.toolForm.title && msg.toolForm.title.toLowerCase().includes(keyword)) {
            return true
          }
          if (msg.toolForm.result && msg.toolForm.result.toLowerCase().includes(keyword)) {
            return true
          }
          // 搜索表单数据
          if (msg.toolForm.data) {
            const dataStr = JSON.stringify(msg.toolForm.data).toLowerCase()
            if (dataStr.includes(keyword)) {
              return true
            }
          }
        }
        return false
      })
    },
    
    // 判断是否是搜索高亮消息
    isSearchHighlight(message) {
      return this.searchKeyword && this.searchResults.includes(message)
    },
    
    // 复制单条消息
    copyMessage(message) {
      let text = ''
      if (message.content) {
        text = message.content
      } else if (message.toolForm && message.toolForm.result) {
        text = message.toolForm.result
      }
      
      if (!text) {
        this.$message.warning('消息内容为空')
        return
      }
      
      // 使用 Clipboard API
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(text).then(() => {
          this.$message.success('消息已复制到剪贴板')
        }).catch(err => {
          console.error('复制失败:', err)
          this.fallbackCopy(text)
        })
      } else {
        this.fallbackCopy(text)
      }
    },
    
    // 降级复制方法
    fallbackCopy(text) {
      const textarea = document.createElement('textarea')
      textarea.value = text
      textarea.style.position = 'fixed'
      textarea.style.opacity = '0'
      document.body.appendChild(textarea)
      textarea.select()
      try {
        document.execCommand('copy')
        this.$message.success('消息已复制到剪贴板')
      } catch (err) {
        console.error('复制失败:', err)
        this.$message.error('复制失败，请手动选择复制')
      }
      document.body.removeChild(textarea)
    },
    
    // 复制所有消息
    copyAllMessages() {
      if (this.messages.length === 0) {
        this.$message.warning('没有可复制的消息')
        return
      }
      
      const allText = this.messages.map(msg => {
        const role = msg.role === 'user' ? '我' : '小皮'
        let content = msg.content || ''
        
        if (msg.toolForm && msg.toolForm.result) {
          content = msg.toolForm.result
        }
        
        return `${role}: ${content}`
      }).join('\n\n')
      
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(allText).then(() => {
          this.$message.success(`已复制 ${this.messages.length} 条消息`)
        }).catch(err => {
          console.error('复制失败:', err)
          this.fallbackCopy(allText)
        })
      } else {
        this.fallbackCopy(allText)
      }
    },
    
    // 删除单条消息
    deleteMessage(index) {
      this.$confirm('确定要删除这条消息吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        // 如果在搜索模式下，同时从搜索结果中删除
        if (this.searchResults.length > 0) {
          const originalIndex = this.messages.indexOf(this.searchResults[index])
          if (originalIndex > -1) {
            this.messages.splice(originalIndex, 1)
          }
          this.searchResults.splice(index, 1)
        } else {
          this.messages.splice(index, 1)
        }
        this.$message.success('消息已删除')
        this.scrollToBottom()
      }).catch(() => {})
    },
    
    // 清空当前消息
    clearCurrentMessages() {
      if (this.messages.length === 0) {
        this.$message.warning('没有可清空的消息')
        return
      }
      
      this.$confirm('确定要清空当前所有消息吗？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.messages = []
        this.searchResults = []
        this.searchKeyword = ''
        this.addWelcomeMessage()
        this.$message.success('消息已清空')
      }).catch(() => {})
    },
    
    // 导出消息
    exportMessages() {
      if (this.messages.length === 0) {
        this.$message.warning('没有可导出的消息')
        return
      }
      
      // 构建导出内容
      const exportContent = this.messages.map(msg => {
        const role = msg.role === 'user' ? '我' : '小皮'
        let content = msg.content || ''
        let toolResult = ''
        
        // 如果是工具表单消息，提取结果
        if (msg.toolForm) {
          content = `[${msg.toolForm.title}]`
          if (msg.toolForm.result) {
            toolResult = msg.toolForm.result
          }
        }
        
        const timestamp = new Date().toLocaleString('zh-CN')
        
        let result = `${timestamp} ${role}: ${content}`
        if (toolResult) {
          result += `\n\n生成结果:\n${toolResult}`
        }
        
        return result
      }).join('\n\n---\n\n')
      
      // 添加头部信息
      const header = `小皮助手对话记录\n导出时间: ${new Date().toLocaleString('zh-CN')}\n对话ID: ${this.currentConversationId || '未保存'}\n消息数量: ${this.messages.length} 条\n\n${'='.repeat(50)}\n\n`
      const fullContent = header + exportContent
      
      // 创建下载链接
      const blob = new Blob([fullContent], { type: 'text/plain;charset=utf-8' })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `小皮助手对话记录_${new Date().getTime()}.txt`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      URL.revokeObjectURL(url)
      
      this.$message.success(`已导出 ${this.messages.length} 条消息`)
    },
    
    // 格式化结果文本（用于显示）
    formatResult(text) {
      if (!text) return ''
      // 将换行符转换为 <br>
      return text.replace(/\n/g, '<br>')
    }
  }
}
</script>

<style scoped>
.ai-chat-root {
  width: 100%;
  height: 100%;
}

.ai-chat-wrapper {
  position: fixed;
  bottom: 20px;
  right: 20px;
  z-index: 999;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
}

.ai-chat-container {
  width: 450px;
  height: 600px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15), 0 4px 12px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: none;
  border: 1px solid #e8e8e8;
  min-width: 450px;
  max-width: 450px;
  min-height: 600px;
  max-height: 600px;
  position: relative;
}

.ai-chat-container.is-open {
  display: flex;
}

.ai-chat-container.show-history {
  width: 750px;
  min-width: 750px;
  max-width: 750px;
}

/* 统一顶部标题栏 */
.unified-header {
  background: linear-gradient(135deg, #ff6a00 0%, #ff8c00 50%, #ffa500 100%);
  padding: 12px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
  cursor: move;
  box-shadow: 0 2px 12px rgba(255, 106, 0, 0.2);
  position: relative;
  z-index: 10;
}

.unified-header::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
}

.header-left-section {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #ffffff;
  letter-spacing: 0.5px;
}

.header-divider {
  color: rgba(255, 255, 255, 0.5);
  font-size: 14px;
}

.header-subtitle {
  color: rgba(255, 255, 255, 0.85);
  font-size: 13px;
  font-weight: 400;
}

.mode-badge,
.cs-badge {
  background: #ff5000;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  padding: 1px 8px;
  border-radius: 3px;
  letter-spacing: 0.5px;
}

.history-toggle-btn {
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.2);
  cursor: pointer;
  font-size: 16px;
  padding: 6px 10px;
  border-radius: 8px;
  transition: all 0.2s ease;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.history-toggle-btn:hover {
  background: rgba(255, 255, 255, 0.25);
  transform: scale(1.05);
}

.mode-selector {
  display: flex;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 8px;
  overflow: hidden;
  height: 36px;
}

.mode-btn {
  padding: 0 14px;
  border: none;
  background: transparent;
  color: rgba(255, 255, 255, 0.85);
  cursor: pointer;
  font-size: 13px !important;
  transition: all 0.2s ease;
  font-weight: 500;
  border-radius: 0;
  white-space: nowrap;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

.mode-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #ffffff;
}

.mode-btn.active {
  background: #ffffff;
  color: #ff6a00;
  font-weight: 600;
}

/* 强制隐藏所有子元素图标（浏览器插件或 iconfont 动态添加） */
.mode-btn i,
.mode-btn svg,
.mode-btn img,
.mode-btn span.iconfont,
.mode-btn .iconfont,
.mode-btn::before,
.mode-btn::after {
  display: none !important;
  visibility: hidden !important;
  width: 0 !important;
  height: 0 !important;
  opacity: 0 !important;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pause-btn {
  padding: 6px 12px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  background: rgba(255, 255, 255, 0.15);
  color: white;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s ease;
  font-weight: 500;
}

.pause-btn:hover {
  background: rgba(255, 255, 255, 0.25);
}

.close-btn {
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: #fff;
  font-size: 18px;
  cursor: pointer;
  padding: 4px 8px;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.25);
  transform: scale(1.1);
}

/* 内容区域 */
.chat-content-wrapper {
  flex: 1;
  display: flex;
  flex-direction: row;
  overflow: hidden;
  min-height: 0;
}

/* 聊天主区域 */
.chat-main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

/* 展开更多功能区域 */
.expand-toolbar {
  background: #ffffff;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
}

.expand-toggle-btn {
  width: 100%;
  padding: 8px 16px;
  background: transparent;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: all 0.2s ease;
  font-size: 12px;
  color: #666;
}

.expand-toggle-btn:hover {
  background: #f5f5f5;
  color: #ff6a00;
}

.expand-toggle-btn.expanded {
  background: #fff5f0;
  color: #ff6a00;
}

.expand-icon {
  font-size: 10px;
  transition: transform 0.2s ease;
}

.expand-content {
  padding: 12px 16px;
  border-top: 1px solid #e8e8e8;
}

.expand-section {
  margin-bottom: 16px;
}

.expand-section:last-child {
  margin-bottom: 0;
}

.section-title {
  font-size: 12px;
  color: #999;
  margin-bottom: 8px;
  font-weight: 500;
}

.ai-tools-quick-access {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.ai-tool-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  background: #ffffff;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 13px;
  color: #333;
}

.ai-tool-btn:hover {
  background: #fff5f0;
  border-color: #ff6a00;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(255, 106, 0, 0.15);
}

.tool-icon {
  font-size: 16px;
}

.tool-name {
  font-weight: 500;
}

.message-actions-bar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
}

.search-box-inline {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #f5f5f5;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  padding: 4px 8px;
}

.search-input-inline {
  border: none;
  background: transparent;
  outline: none;
  font-size: 13px;
  padding: 4px;
  width: 150px;
}

.search-close-inline {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 16px;
  color: #999;
  padding: 0;
  line-height: 1;
}

.search-close-inline:hover {
  color: #ff6a00;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  background: #ffffff;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 12px;
  color: #333;
}

.action-btn:hover {
  background: #fff5f0;
  border-color: #ff6a00;
  transform: translateY(-1px);
}

.action-btn.danger:hover {
  background: #fee2e2;
  border-color: #ef4444;
  color: #dc2626;
}

.action-icon {
  font-size: 14px;
}

/* 问候语浮动按钮 */
.greeting-float-btn {
  position: sticky;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: linear-gradient(135deg, #ff6a00 0%, #ff8c00 100%);
  color: #ffffff;
  border-radius: 24px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 16px rgba(255, 106, 0, 0.3);
  font-size: 14px;
  font-weight: 500;
  margin: 16px 0;
}

.greeting-float-btn:hover {
  transform: translateX(-50%) translateY(-2px);
  box-shadow: 0 8px 24px rgba(255, 106, 0, 0.4);
}

.greeting-icon {
  font-size: 20px;
}

.greeting-text {
  white-space: nowrap;
}

/* 问候语卡片头部 */
.greeting-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: linear-gradient(135deg, #ff6a00 0%, #ff8c00 100%);
  color: #ffffff;
  border-radius: 12px 12px 0 0;
}

.greeting-card-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.greeting-close-btn {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  border: none;
  color: #ffffff;
  font-size: 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.greeting-close-btn:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: scale(1.1);
}

/* 问候语卡片 */
.greeting-card-wrapper {
  margin: 16px 0;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  background: #ffffff;
}

/* AI工具快捷入口 - 固定在输入框上方 */
.ai-tools-fixed-bar {
  display: flex;
  gap: 8px;
  padding: 8px 16px;
  background: #ffffff;
  border-top: 1px solid #e8e8e8;
  flex-shrink: 0;
}

.ai-tool-quick-btn {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 8px 12px;
  background: linear-gradient(135deg, #fff5f0 0%, #ffe8d6 100%);
  border: 1px solid #ffe8d6;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 12px;
  color: #ff6a00;
}

.ai-tool-quick-btn:hover {
  background: linear-gradient(135deg, #ff6a00 0%, #ff8c00 100%);
  color: #ffffff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 106, 0, 0.3);
}

.quick-tool-icon {
  font-size: 20px;
}

.quick-tool-name {
  font-weight: 500;
  white-space: nowrap;
}

/* 搜索结果提示 */
.search-results-hint {
  padding: 6px 16px;
  background: #fff5f0;
  border-bottom: 1px solid #ffe8d6;
  font-size: 12px;
  color: #ff6a00;
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.close-search-hint {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 16px;
  color: #ff6a00;
  padding: 0;
  line-height: 1;
}

.close-search-hint:hover {
  color: #ff5000;
}

/* 消息操作按钮 */
.message-actions {
  position: absolute;
  top: 8px;
  right: 8px;
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s ease;
  z-index: 10;
}

.message-content:hover .message-actions {
  opacity: 1;
}

.msg-action-btn {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  background: #ffffff;
  border: 1px solid #e8e8e8;
  cursor: pointer;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
}

.msg-action-btn:hover {
  background: #fff5f0;
  border-color: #ff6a00;
  transform: scale(1.1);
}

/* 搜索高亮 */
.message-item.search-highlight {
  background: #fff9e6;
  border-radius: 8px;
  padding: 4px;
}

.message-item.search-highlight .message-content {
  background: #ffffff;
  box-shadow: 0 0 0 2px #ff6a00;
}

.history-sidebar {
  width: 280px;
  background: #f8f9fa;
  border-right: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  flex-shrink: 0;
}

.new-conversation-top {
  display: flex;
  align-items: center;
  padding: 12px 14px;
  margin: 10px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  background: #ffffff;
  border: 1px dashed #d9d9d9;
  flex-shrink: 0;
}

.new-conversation-top:hover {
  background: #fff5f0;
  border-color: #ff6a00;
}

.new-conversation-top .item-icon {
  font-size: 20px;
  margin-right: 10px;
  width: 28px;
  text-align: center;
  flex-shrink: 0;
  color: #ff6a00;
}

.new-conversation-top .item-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.new-conversation-top .item-time {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}

.history-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
  background: transparent;
}

.history-list::-webkit-scrollbar {
  width: 4px;
}

.history-list::-webkit-scrollbar-track {
  background: transparent;
}

.history-list::-webkit-scrollbar-thumb {
  background: #d9d9d9;
  border-radius: 2px;
}

.history-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  margin-bottom: 6px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  background-color: #ffffff;
  border: 1px solid transparent;
}

.history-item:hover {
  background: #f5f5f5;
}

.history-item.active {
  background: #fff5f0;
  border-color: #ff6a00;
}

.item-icon {
  font-size: 18px;
  margin-right: 10px;
  width: 24px;
  text-align: center;
  flex-shrink: 0;
}

.item-info {
  flex: 1;
  overflow: hidden;
  min-width: 0;
}

.item-title {
  font-size: 13px;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 3px;
  font-weight: 500;
}

.item-time {
  font-size: 11px;
  color: #999;
}

.item-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
  flex-shrink: 0;
}

.history-item:hover .item-actions {
  opacity: 1;
}

.delete-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 14px;
  padding: 2px;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.delete-btn:hover {
  background-color: #fee2e2;
}

.empty-state {
  text-align: center;
  padding: 40px 20px;
  color: #999;
  font-size: 13px;
}

.history-footer {
  padding: 10px;
  border-top: 1px solid #e8e8e8;
  background: #ffffff;
  flex-shrink: 0;
}

.clear-all-btn {
  width: 100%;
  padding: 8px;
  background: #fff5f0;
  color: #ff6a00;
  border: 1px solid #ffe8d6;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s ease;
  font-weight: 500;
}

.clear-all-btn:hover {
  background: #ff6a00;
  color: #ffffff;
  border-color: #ff6a00;
}

.greeting-card-wrapper {
  margin-bottom: 16px;
}

.greeting-card-wrapper .greeting-card-container {
  background: linear-gradient(135deg, #fff8f0 0%, #ffffff 100%);
  border: 1px solid #ffe8d6;
  border-radius: 12px;
  margin: 0;
}

.ai-chat-float-btn {
  position: fixed;
  right: 20px;
  bottom: 20px;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff6a00 0%, #ff8c00 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  cursor: pointer;
  box-shadow: 0 6px 20px rgba(255, 106, 0, 0.3);
  transition: all 0.3s ease;
  z-index: 9999;
  border: none;
}

/* 窗口展开时,聊天按钮显示在窗口右上角 */
.ai-chat-float-btn.in-window {
  position: absolute;
  top: 12px;
  right: 60px;
  width: 36px;
  height: 36px;
  font-size: 18px;
  box-shadow: 0 2px 12px rgba(255, 106, 0, 0.3);
}

.ai-chat-float-btn:hover {
  transform: scale(1.08);
  box-shadow: 0 8px 24px rgba(255, 106, 0, 0.4);
}

.ai-chat-float-btn.in-window:hover {
  transform: scale(1.1);
  box-shadow: 0 4px 16px rgba(255, 106, 0, 0.4);
}

.ai-chat-messages {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  background: #f5f5f5;
  cursor: default;
  min-height: 0;
}

.ai-chat-messages::-webkit-scrollbar {
  width: 6px;
}

.ai-chat-messages::-webkit-scrollbar-track {
  background: transparent;
}

.ai-chat-messages::-webkit-scrollbar-thumb {
  background: #d9d9d9;
  border-radius: 3px;
}

.message-item {
  display: flex;
  margin-bottom: 16px;
  animation: fadeIn 0.3s ease;
}

.user-message {
  flex-direction: row-reverse;
}

.ai-message {
  justify-content: flex-start;
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff6a00 0%, #ff8c00 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  margin-right: 10px;
  flex-shrink: 0;
}

.user-message .message-avatar {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  margin-right: 0;
  margin-left: 10px;
}

.message-content {
  max-width: 80%;
  padding: 10px 14px;
  border-radius: 16px;
  background: #ffffff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  line-height: 1.5;
  word-wrap: break-word;
  font-size: 14px;
  color: #333;
  position: relative;
}

.user-message .message-content {
  background: linear-gradient(135deg, #ff6a00 0%, #ff8c00 100%);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.ai-message .message-content {
  border-bottom-left-radius: 4px;
}

.loading-message {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 10px;
}

.loading-message span {
  font-size: 13px;
  color: #999;
  animation: pulse 1.5s infinite;
}

.ai-chat-input {
  padding: 12px 16px;
  border-top: 1px solid #e8e8e8;
  display: flex;
  align-items: center;
  background: #ffffff;
  cursor: default;
  flex-shrink: 0;
  gap: 8px;
}

.ai-chat-input input {
  flex: 1;
  padding: 10px 16px;
  border: 1px solid #e8e8e8;
  border-radius: 24px;
  outline: none;
  transition: all 0.2s ease;
  font-size: 14px;
  background: #f5f5f5;
}

.ai-chat-input input:focus {
  border-color: #ff6a00;
  background: #ffffff;
  box-shadow: 0 0 0 2px rgba(255, 106, 0, 0.1);
}

.voice-btn {
  padding: 8px 14px;
  background: #f5f5f5;
  border: 1px solid #e8e8e8;
  border-radius: 24px;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 40px;
  position: relative;
}

.voice-btn:hover {
  background: #fff5f0;
  border-color: #ff6a00;
}

.voice-btn.listening {
  background: #fee2e2;
  border-color: #ef4444;
  animation: pulse 1.5s infinite;
}

.voice-icon {
  font-size: 16px;
  line-height: 1;
}

.voice-wave {
  display: flex;
  align-items: center;
  gap: 3px;
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  z-index: 1;
}

.wave-bar {
  width: 3px;
  height: 12px;
  background: linear-gradient(to top, #ef4444, #dc2626);
  border-radius: 2px;
  animation: wave 0.8s ease-in-out infinite;
}

.wave-bar:nth-child(1) {
  animation-delay: 0s;
}

.wave-bar:nth-child(2) {
  animation-delay: 0.2s;
}

.wave-bar:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes wave {
  0%, 100% {
    height: 4px;
    opacity: 0.5;
  }
  50% {
    height: 16px;
    opacity: 1;
  }
}

@keyframes pulse {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.4);
  }
  50% {
    box-shadow: 0 0 0 10px rgba(239, 68, 68, 0);
  }
}

.ai-chat-input input.listening {
  border-color: #ef4444;
  background: #fee2e2;
}

.send-btn {
  padding: 10px 20px;
  background: linear-gradient(135deg, #ff6a00 0%, #ff8c00 100%);
  color: #fff;
  border: none;
  border-radius: 24px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-weight: 500;
  font-size: 14px;
}

.send-btn:hover {
  background: linear-gradient(135deg, #ff5000 0%, #e04400 100%);
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(255, 106, 0, 0.3);
}

.send-btn:disabled {
  background: #d9d9d9;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.stop-btn {
  padding: 10px 20px;
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: #fff;
  border: none;
  border-radius: 24px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 14px;
  font-weight: 500;
}

.stop-btn:hover {
  background: linear-gradient(135deg, #dc2626 0%, #b91c1c 100%);
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(239, 68, 68, 0.3);
}

.quick-commands {
  padding: 12px 16px;
  border-top: 1px solid #e8e8e8;
  background: #ffffff;
}

.quick-commands-title {
  font-size: 13px;
  color: #666;
  margin-bottom: 10px;
  font-weight: 500;
}

.quick-commands-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.quick-command-btn {
  padding: 8px 14px;
  background: #ffffff;
  color: #ff6a00;
  border: 1px solid #ffe8d6;
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
  font-weight: 500;
}

.quick-command-btn:hover {
  background: #ff6a00;
  color: #ffffff;
  border-color: #ff6a00;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(255, 106, 0, 0.2);
}

.quick-command-btn:active {
  transform: scale(0.98);
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

.recommendations-container,
.search-results-container {
  margin-top: 12px;
  border-top: 1px solid #e2e8f0;
  padding-top: 12px;
}

.thinking-steps-container {
  margin-top: 12px;
  padding: 10px 12px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 10px;
  border-left: 3px solid #3b82f6;
}

/* 豆包风格思考展示 */
.doubao-thinking-container {
  margin-top: 6px;
  margin-bottom: 6px;
  border-radius: 6px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  overflow: hidden;
  transition: all 0.3s ease;
}

.doubao-thinking-header {
  display: flex;
  align-items: center;
  padding: 6px 10px;
  cursor: pointer;
  user-select: none;
  transition: background 0.2s ease;
}

.doubao-thinking-header:hover {
  background: #f5f5f5;
}

.thinking-icon {
  margin-right: 6px;
  color: #bbb;
  font-size: 11px;
  transition: transform 0.2s ease;
}

.thinking-title {
  font-size: 11px;
  color: #bbb;
  font-weight: 400;
  flex: 1;
}

.thinking-loading {
  color: #409eff;
  font-size: 11px;
  animation: pulse 1.5s ease-in-out infinite;
}

.doubao-thinking-content {
  padding: 0 10px 10px 28px;
}

.thinking-step {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin-bottom: 4px;
  font-size: 11px;
  color: #bbb;
  line-height: 1.4;
}

.step-number {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #f0f0f0;
  color: #bbb;
  font-size: 9px;
  font-weight: 500;
  flex-shrink: 0;
  margin-top: 1px;
}

.step-text {
  flex: 1;
}

.thinking-steps-title {
  font-size: 12px;
  font-weight: 600;
  color: #475569;
  margin-bottom: 6px;
}

.thinking-steps-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.thinking-step {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  font-size: 11px;
  color: #64748b;
  line-height: 1.4;
}

.step-icon {
  color: #3b82f6;
  flex-shrink: 0;
  margin-top: 2px;
}

.step-text {
  flex: 1;
}

.top-recommendation-container {
  margin-top: 12px;
  padding: 12px;
  background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%);
  border-radius: 12px;
  border: 1px solid #fde68a;
}

.top-recommendation-title {
  font-size: 14px;
  font-weight: 600;
  color: #d97706;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.top-recommendation-item {
  display: flex;
  gap: 12px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  padding: 8px;
  border-radius: 10px;
}

.top-recommendation-item:hover {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
}

.top-recommendation-img-container {
  width: 100px;
  height: 100px;
  border-radius: 10px;
  overflow: hidden;
  flex-shrink: 0;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
}

.top-recommendation-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.top-recommendation-img-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
}

.top-recommendation-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  overflow: hidden;
}

.top-recommendation-name {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  line-height: 1.3;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.top-recommendation-desc {
  font-size: 11px;
  color: #64748b;
  line-height: 1.3;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.top-recommendation-price {
  font-size: 16px;
  font-weight: 700;
  color: #ef4444;
  margin-top: 4px;
}

.top-recommendation-sales {
  font-size: 11px;
  color: #64748b;
}

.top-recommendation-actions {
  display: flex;
  gap: 8px;
  margin-top: auto;
  padding-top: 6px;
}

.top-recommendation-detail-btn,
.top-recommendation-cart-btn {
  flex: 1;
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  font-weight: 500;
}

.top-recommendation-detail-btn {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: #fff;
  box-shadow: 0 2px 6px rgba(59, 130, 246, 0.2);
}

.top-recommendation-detail-btn:hover {
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 10px rgba(59, 130, 246, 0.3);
}

.top-recommendation-cart-btn {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: #fff;
  box-shadow: 0 2px 6px rgba(16, 185, 129, 0.2);
}

.top-recommendation-cart-btn:hover {
  background: linear-gradient(135deg, #059669 0%, #047857 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 10px rgba(16, 185, 129, 0.3);
}

.recommendations-title,
.search-results-title {
  font-size: 14px;
  font-weight: 600;
  color: #2563eb;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.recommendation-source {
  font-size: 12px;
  font-weight: normal;
  color: #64748b;
  font-style: italic;
}

.recommendations-grid,
.search-results-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  max-height: 500px;
  overflow-y: auto;
  padding: 5px;
}

.recommendation-item,
.search-result-item {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  overflow: hidden;
  background: #ffffff;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
  height: 200px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.recommendation-item:hover,
.search-result-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
  border-color: #3b82f6;
}

.recommendation-img-container {
  cursor: pointer;
  flex-shrink: 0;
}

.recommendation-img,
.search-result-img {
  width: 100%;
  height: 100px;
  object-fit: cover;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
}

.recommendation-img-placeholder,
.search-result-img-placeholder,
.image-error-placeholder {
  width: 100%;
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  font-size: 32px;
}

.recommendation-emoji-placeholder,
.search-result-emoji-placeholder,
.taobao-top-emoji-placeholder,
.taobao-other-emoji-placeholder {
  width: 100%;
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.recommendation-emoji-placeholder:hover,
.search-result-emoji-placeholder:hover,
.taobao-top-emoji-placeholder:hover,
.taobao-other-emoji-placeholder:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.recommendation-emoji-icon,
.search-result-emoji-icon,
.taobao-top-emoji-icon,
.taobao-other-emoji-icon {
  font-size: 48px;
  line-height: 1;
}

.recommendation-info,
.search-result-info {
  padding: 6px;
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  overflow: visible;
}

.recommendation-name,
.search-result-name {
  font-size: 12px;
  font-weight: 500;
  color: #1e293b;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
  line-height: 1.2;
  max-height: 2.4em;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

/* Emoji 占位符样式 */
.recommendation-emoji-placeholder,
.search-result-emoji-placeholder,
.taobao-top-emoji-placeholder,
.taobao-other-emoji-placeholder {
  width: 100%;
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.recommendation-emoji-placeholder:hover,
.search-result-emoji-placeholder:hover,
.taobao-top-emoji-placeholder:hover,
.taobao-other-emoji-placeholder:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.recommendation-emoji-icon,
.search-result-emoji-icon,
.taobao-top-emoji-icon,
.taobao-other-emoji-icon {
  font-size: 48px;
  line-height: 1;
}

.recommendation-info,
.search-result-info {
  padding: 4px;
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  overflow: visible;
}

.recommendation-name,
.search-result-name {
  font-size: 11px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
  line-height: 1.2;
  max-height: 2.4em;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.recommendation-name:hover,
.search-result-name:hover {
  color: #409eff;
}

.recommendation-price,
.search-result-price {
  font-size: 12px;
  font-weight: 600;
  color: #f56c6c;
  margin-bottom: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.recommendation-action {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: auto;
  padding-top: 4px;
}

.recommendation-detail-btn,
.recommendation-cart-btn {
  width: 100%;
  padding: 6px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.3s ease;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.recommendation-detail-btn {
  background-color: #67c23a;
  color: #fff;
}

.recommendation-detail-btn:hover {
  background-color: #85ce61;
  transform: translateY(-1px);
}

.recommendation-cart-btn {
  background-color: #409eff;
  color: #fff;
}

.recommendation-cart-btn:hover {
  background-color: #66b1ff;
  transform: translateY(-1px);
}

/* 淘宝问问风格样式 */
.taobao-thinking-container {
  margin-top: 12px;
  padding: 8px 12px;
  background: transparent;
  border-radius: 8px;
  font-size: 12px;
  line-height: 1.6;
}

.taobao-thinking-content {
  display: flex;
  align-items: center;
  gap: 8px;
}

.taobao-thinking-text {
  font-weight: 600;
  color: #8B4513;
  white-space: nowrap;
}

.taobao-thinking-detail {
  font-size: 11px;
  color: #A0522D;
  opacity: 0.8;
}

.taobao-top-recommendation {
  margin-top: 16px;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.taobao-recommendation-header {
  padding: 12px 16px;
  background: linear-gradient(90deg, #ff9000 0%, #ff5000 100%);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
}

.taobao-recommendation-text {
  display: block;
}

.taobao-top-item {
  display: flex;
  padding: 16px;
  gap: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: #fff;
}

.taobao-top-item:hover {
  background: #fafafa;
}

.taobao-top-img-wrapper {
  width: 120px;
  height: 120px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
  background: #f5f5f5;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.taobao-top-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.taobao-top-img-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  background: #f5f5f5;
}

.taobao-top-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 4px 0;
}

.taobao-top-name {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.taobao-top-price {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin-top: 8px;
}

.price-symbol {
  font-size: 14px;
  color: #ff5000;
  font-weight: 600;
}

.price-value {
  font-size: 22px;
  color: #ff5000;
  font-weight: 700;
  line-height: 1;
}

.taobao-top-sales {
  font-size: 12px;
  color: #999;
  margin-left: 8px;
}

/* 商品标签样式 */
.taobao-top-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-top: 4px;
}

.taobao-tag-item {
  padding: 2px 8px;
  background: linear-gradient(135deg, #fff3e0 0%, #ffe0b2 100%);
  color: #ff6f00;
  font-size: 11px;
  border-radius: 10px;
  font-weight: 500;
}

/* 评分和评论样式 */
.taobao-top-rating {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
}

.rating-stars {
  display: flex;
  gap: 2px;
}

.star-icon {
  font-size: 12px;
  color: #ddd;
}

.star-icon.filled {
  color: #ff5000;
}

.rating-score {
  font-size: 13px;
  color: #ff5000;
  font-weight: 600;
}

.review-count {
  font-size: 12px;
  color: #999;
}

/* 第一条五星评论样式 */
.taobao-first-review {
  margin-top: 12px;
  padding: 12px;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border-radius: 8px;
  border-left: 3px solid #ff9000;
}

.review-user {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 12px;
}

.user-avatar {
  font-size: 16px;
}

.user-name {
  color: #666;
  font-weight: 500;
}

.review-rating {
  display: flex;
  gap: 2px;
  font-size: 12px;
}

.review-rating span {
  color: #ddd;
}

.review-rating span.filled {
  color: #ff9000;
}

.review-content {
  font-size: 13px;
  color: #333;
  line-height: 1.5;
  margin-bottom: 8px;
}

.review-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.review-tag-item {
  padding: 2px 8px;
  background: #e3f2fd;
  color: #1976d2;
  font-size: 11px;
  border-radius: 10px;
  font-weight: 500;
}

.taobao-add-cart-btn {
  align-self: flex-start;
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff9000 0%, #ff5000 100%);
  color: #fff;
  font-size: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(255, 80, 0, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
}

.taobao-add-cart-btn:hover {
  transform: scale(1.1);
  box-shadow: 0 4px 12px rgba(255, 80, 0, 0.4);
}

.taobao-other-recommendations {
  margin-top: 16px;
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.taobao-other-header {
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 600;
  color: #333;
  padding-bottom: 8px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.taobao-other-text {
  display: block;
}

.sort-buttons {
  display: flex;
  gap: 8px;
}

.sort-btn {
  padding: 4px 12px;
  font-size: 12px;
  border: 1px solid #ddd;
  border-radius: 16px;
  background: #fff;
  color: #666;
  cursor: pointer;
  transition: all 0.3s ease;
  outline: none;
}

.sort-btn:hover {
  border-color: #ff5000;
  color: #ff5000;
}

.sort-btn.active {
  background: #ff5000;
  border-color: #ff5000;
  color: #fff;
}

.taobao-other-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 16px;
}

.taobao-other-item {
  background: #fafafa;
  border-radius: 8px;
  overflow: hidden;
  transition: all 0.3s ease;
  cursor: pointer;
}

.taobao-other-item:hover {
  background: #f5f5f5;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.taobao-other-img-wrapper {
  width: 100%;
  height: 140px;
  background: #f5f5f5;
  overflow: hidden;
}

.taobao-other-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.taobao-other-img-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  background: #f5f5f5;
}

.taobao-other-info {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.taobao-other-name {
  font-size: 13px;
  font-weight: 500;
  color: #333;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  cursor: pointer;
}

.taobao-other-price {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.other-price-symbol {
  font-size: 12px;
  color: #ff5000;
  font-weight: 600;
}

.other-price-value {
  font-size: 18px;
  color: #ff5000;
  font-weight: 700;
  line-height: 1;
}

.taobao-other-sales {
  font-size: 11px;
  color: #999;
}

/* 其他商品标签样式 */
.taobao-other-tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.taobao-other-tag-item {
  padding: 2px 6px;
  background: linear-gradient(135deg, #fff3e0 0%, #ffe0b2 100%);
  color: #ff6f00;
  font-size: 10px;
  border-radius: 8px;
  font-weight: 500;
}

/* 其他商品评分样式 */
.taobao-other-rating {
  display: flex;
  align-items: center;
  gap: 6px;
}

.other-rating-score {
  font-size: 12px;
  color: #ff5000;
  font-weight: 600;
}

.other-review-count {
  font-size: 11px;
  color: #999;
}

.taobao-other-cart-btn {
  align-self: flex-start;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff9000 0%, #ff5000 100%);
  color: #fff;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 6px rgba(255, 80, 0, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 4px;
}

.taobao-other-cart-btn:hover {
  transform: scale(1.08);
  box-shadow: 0 3px 8px rgba(255, 80, 0, 0.4);
}

/* 评论舆情分析样式 */
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
  width: 40px;
  font-size: 12px;
  color: #606266;
  text-align: right;
}

.rating-bar-wrapper {
  flex: 1;
  height: 8px;
  background-color: #ebeef5;
  border-radius: 4px;
  overflow: hidden;
}

.rating-bar {
  height: 100%;
  background: linear-gradient(90deg, #409eff 0%, #66b1ff 100%);
  border-radius: 4px;
  transition: width 0.3s ease;
}

.rating-count {
  width: 100px;
  font-size: 11px;
  color: #909399;
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
  justify-content: space-around;
}

.sentiment-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.sentiment-color {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.sentiment-item span {
  font-size: 12px;
  color: #606266;
}

.sentiment-value {
  font-weight: 600;
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
}

.tag-item {
  padding: 4px 10px;
  background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%);
  color: #1976d2;
  border-radius: 12px;
  cursor: default;
  transition: all 0.2s ease;
}

.tag-item:hover {
  transform: scale(1.05);
  box-shadow: 0 2px 6px rgba(25, 118, 210, 0.2);
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

/* 词云图样式 */
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
  min-height: 200px;
  padding: 16px;
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
  border-radius: 8px;
  border: 1px solid #e4e7ed;
  overflow: hidden;
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

/* 销售数据报告样式 */
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

.total-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.stat-icon {
  font-size: 24px;
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
  font-size: 16px;
  font-weight: 600;
  color: #303133;
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
  height: 200px;
  background-color: #fff;
  border-radius: 8px;
  padding: 12px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
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

.rank-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.rank-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  transition: all 0.2s ease;
}

.rank-item:hover {
  transform: translateX(4px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.rank-number {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #e4e7ed;
  color: #606266;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 600;
}

.rank-number.top3 {
  background: linear-gradient(135deg, #ff9000 0%, #ff5000 100%);
  color: #fff;
}

.rank-info {
  flex: 1;
}

.rank-name {
  font-size: 13px;
  color: #303133;
  margin-bottom: 4px;
}

.rank-stats {
  display: flex;
  gap: 16px;
  font-size: 11px;
  color: #909399;
}

/* 确认推荐卡片样式 */
.confirm-recommend-card {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  background: linear-gradient(135deg, #fff9f0 0%, #fff5eb 100%);
  border-radius: 12px;
  border: 1px solid #ffe4c4;
  box-shadow: 0 2px 8px rgba(255, 144, 0, 0.1);
  margin-top: 8px;
}

.confirm-icon {
  font-size: 24px;
  flex-shrink: 0;
}

.confirm-content {
  flex: 1;
}

.confirm-text {
  font-size: 14px;
  color: #333;
  line-height: 1.5;
  margin-bottom: 12px;
}

.confirm-actions {
  display: flex;
  gap: 12px;
}

.confirm-btn {
  padding: 8px 20px;
  border-radius: 20px;
  border: none;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.confirm-btn.agree {
  background: linear-gradient(135deg, #ff9000 0%, #ff5000 100%);
  color: #fff;
}

.confirm-btn.agree:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 144, 0, 0.3);
}

.confirm-btn.reject {
  background: #f5f5f5;
  color: #666;
  border: 1px solid #e0e0e0;
}

.confirm-btn.reject:hover {
  background: #e8e8e8;
  border-color: #d0d0d0;
}

/* 聊天按钮 - 窗口展开时显示在窗口右上角 */
.chat-btn-in-window {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff6a00 0%, #ff8c00 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  cursor: pointer;
  box-shadow: 0 2px 12px rgba(255, 106, 0, 0.3);
  transition: all 0.3s ease;
  z-index: 1000;
}

.chat-btn-in-window:hover {
  transform: scale(1.1);
  box-shadow: 0 4px 16px rgba(255, 106, 0, 0.4);
}

/* AI工具浮动按钮 - 发送按钮上方偏右 */
.ai-tools-float-btn {
  position: absolute;
  bottom: 70px;
  right: 40px;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #ff6a00 0%, #ff8c00 100%);
  color: #ffffff;
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 12px rgba(255, 106, 0, 0.3);
  z-index: 1000;
}

.ai-tools-float-btn:hover {
  transform: scale(1.1);
  box-shadow: 0 4px 16px rgba(255, 106, 0, 0.4);
}

.tools-icon {
  font-size: 18px;
}

/* 小三角 - 切换按钮显示 */
.tools-toggle-arrow {
  position: absolute;
  bottom: 120px;
  right: 52px;
  width: 0;
  height: 0;
  border-left: 8px solid transparent;
  border-right: 8px solid transparent;
  border-bottom: 10px solid #ff6a00;
  cursor: pointer;
  transition: all 0.3s ease;
  z-index: 1001;
}

.tools-toggle-arrow:hover {
  transform: scale(1.2);
}

.tools-toggle-arrow.arrow-up {
  bottom: 166px;
  right: 52px;
  border-bottom: none;
  border-top: 10px solid #409eff;
}

/* 蓝色消息管理切换按钮 - AI工具正上方 */
.message-mgmt-toggle-btn {
  position: absolute;
  bottom: 120px;
  right: 42px;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
  color: #ffffff;
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.3);
  z-index: 1000;
  opacity: 0;
  transform: translateY(10px);
  pointer-events: none;
}

.message-mgmt-toggle-btn.show {
  opacity: 1;
  transform: translateY(0);
  pointer-events: auto;
}

.message-mgmt-toggle-btn:hover {
  transform: scale(1.1);
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.4);
}

.toggle-icon {
  font-size: 16px;
}

/* 消息管理面板 */
.message-mgmt-panel {
  position: absolute;
  top: 60px;
  right: 240px;
  width: 200px;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  overflow: hidden;
  border: 1px solid #e8e8e8;
}

.mgmt-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
  color: #ffffff;
  font-size: 14px;
  font-weight: 600;
}

.mgmt-panel-close {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  color: #ffffff;
  font-size: 18px;
  cursor: pointer;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.2s ease;
}

.mgmt-panel-close:hover {
  background: rgba(255, 255, 255, 0.3);
}

.mgmt-panel-body {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.mgmt-panel-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  background: #ffffff;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 13px;
  color: #333;
}

.mgmt-panel-btn:hover {
  background: #f0f7ff;
  border-color: #409eff;
  transform: translateX(4px);
}

.mgmt-panel-btn.danger:hover {
  background: #fee2e2;
  border-color: #ef4444;
  color: #dc2626;
}

.mgmt-panel-icon {
  font-size: 18px;
}

.mgmt-panel-search {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 12px;
  border-top: 1px solid #e8e8e8;
}

.mgmt-panel-search-input {
  flex: 1;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  padding: 6px 10px;
  font-size: 12px;
  outline: none;
}

.mgmt-panel-search-input:focus {
  border-color: #409eff;
}

.mgmt-panel-search-close {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 16px;
  color: #999;
  padding: 0;
  line-height: 1;
}

.mgmt-panel-search-close:hover {
  color: #409eff;
}

.mgmt-panel-search-stats {
  font-size: 12px;
  color: #409eff;
  background: #f0f7ff;
  padding: 8px 12px;
  text-align: center;
  border-top: 1px solid #e8e8e8;
}

/* 消息管理面板 - 右侧伸缩 */
.message-management-panel {
  position: relative;
  width: 40px;
  background: #f8f9fa;
  border-left: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  transition: width 0.3s ease;
  overflow: hidden;
}

.message-management-panel.expanded {
  width: 200px;
}

/* 展开/收起按钮 */
.panel-toggle-btn {
  position: absolute;
  top: 50%;
  left: 0;
  transform: translateY(-50%);
  width: 32px;
  height: 60px;
  background: linear-gradient(135deg, #ff6a00 0%, #ff8c00 100%);
  border: none;
  border-radius: 8px 0 0 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
  transition: all 0.2s ease;
  box-shadow: -2px 0 8px rgba(255, 106, 0, 0.2);
}

.panel-toggle-btn:hover {
  background: linear-gradient(135deg, #ff8c00 0%, #ffa500 100%);
  width: 36px;
}

.toggle-icon {
  font-size: 12px;
  color: #ffffff;
  transition: transform 0.3s ease;
}

/* 面板内容 */
.panel-content {
  padding: 16px 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 200px;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.message-management-panel.expanded .panel-content {
  opacity: 1;
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  padding-bottom: 8px;
  border-bottom: 1px solid #e8e8e8;
}

.panel-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.panel-action-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: #ffffff;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 13px;
  color: #333;
}

.panel-action-btn:hover {
  background: #fff5f0;
  border-color: #ff6a00;
  transform: translateX(2px);
}

.panel-action-btn.danger:hover {
  background: #fee2e2;
  border-color: #ef4444;
  color: #dc2626;
}

.panel-action-icon {
  font-size: 16px;
}

.panel-search {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #ffffff;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 6px 8px;
}

.panel-search-input {
  flex: 1;
  border: none;
  background: transparent;
  outline: none;
  font-size: 12px;
  padding: 2px;
}

.panel-search-close {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 16px;
  color: #999;
  padding: 0;
  line-height: 1;
}

.panel-search-close:hover {
  color: #ff6a00;
}

.panel-search-stats {
  font-size: 12px;
  color: #ff6a00;
  background: #fff5f0;
  padding: 6px 10px;
  border-radius: 6px;
  text-align: center;
}

/* AI工具面板 */
.ai-tools-panel {
  position: absolute;
  top: 60px;
  right: 12px;
  width: 200px;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  overflow: hidden;
  border: 1px solid #e8e8e8;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: linear-gradient(135deg, #ff6a00 0%, #ff8c00 100%);
  color: #ffffff;
  font-size: 14px;
  font-weight: 600;
}

.panel-close {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  color: #ffffff;
  font-size: 18px;
  cursor: pointer;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.2s ease;
}

.panel-close:hover {
  background: rgba(255, 255, 255, 0.3);
}

.panel-body {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.panel-tool-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  background: #ffffff;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 13px;
  color: #333;
}

.panel-tool-btn:hover {
  background: #fff5f0;
  border-color: #ff6a00;
  transform: translateX(4px);
}

.panel-tool-icon {
  font-size: 18px;
}

/* 工单信息卡片样式 - 参考推荐商品确认卡片 */
.ticket-info-card {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  background: linear-gradient(135deg, #fff9f0 0%, #fff5eb 100%);
  border-radius: 12px;
  border: 1px solid #ffe4c4;
  box-shadow: 0 2px 8px rgba(255, 144, 0, 0.1);
  margin-top: 8px;
}

.ticket-icon {
  font-size: 24px;
  flex-shrink: 0;
}

.ticket-content {
  flex: 1;
}

.ticket-text {
  margin-bottom: 12px;
}

.ticket-title {
  font-size: 15px;
  font-weight: 600;
  color: #ff5000;
  margin-bottom: 6px;
}

.ticket-no {
  font-size: 13px;
  color: #666;
  margin-bottom: 4px;
}

.ticket-no strong {
  color: #ff5000;
}

.ticket-desc {
  font-size: 13px;
  color: #333;
  line-height: 1.5;
}

/* 问题描述输入框 */
.ticket-description-input {
  margin-bottom: 12px;
}

.description-textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 13px;
  color: #333;
  resize: vertical;
  transition: border-color 0.3s;
  font-family: inherit;
}

.description-textarea:focus {
  outline: none;
  border-color: #ff9000;
}

.description-textarea::placeholder {
  color: #999;
}

/* 操作按钮 */
.ticket-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.ticket-btn {
  flex: 1;
  padding: 10px 16px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.ticket-btn.enter-chat {
  background: linear-gradient(135deg, #ff9000 0%, #ff5000 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(255, 144, 0, 0.3);
}

.ticket-btn.enter-chat:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 144, 0, 0.4);
}

.ticket-btn.enter-chat:active {
  transform: translateY(0);
}

/* 联系渠道列表 */
.ticket-channels {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding-top: 10px;
  border-top: 1px solid rgba(255, 144, 0, 0.15);
}

.channel-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 8px;
  font-size: 13px;
}

.channel-label {
  color: #666;
  font-weight: 500;
}

.channel-desc {
  color: #999;
}

.channel-label {
  font-weight: 600;
  color: #ff5000;
}

.channel-desc {
  color: #666;
}

/* 转人工客服卡片 - 新版（类似推荐商品确认卡片） */
.transfer-human-card {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  background: linear-gradient(135deg, #fff9f0 0%, #fff5eb 100%);
  border-radius: 12px;
  border: 1px solid #ffe4c4;
  box-shadow: 0 2px 8px rgba(255, 144, 0, 0.1);
  margin-top: 8px;
}

.transfer-icon {
  font-size: 24px;
  flex-shrink: 0;
}

.transfer-content {
  flex: 1;
}

.transfer-text {
  margin-bottom: 12px;
}

.transfer-title {
  font-size: 15px;
  font-weight: 600;
  color: #ff5000;
  margin-bottom: 6px;
}

.transfer-no {
  font-size: 13px;
  color: #666;
  margin-bottom: 4px;
}

.transfer-no strong {
  color: #ff5000;
}

.transfer-desc {
  font-size: 13px;
  color: #333;
  line-height: 1.5;
}

.transfer-description-input {
  margin-bottom: 12px;
}

.transfer-description-input .description-textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 13px;
  color: #333;
  resize: vertical;
  transition: border-color 0.3s;
  font-family: inherit;
}

.transfer-description-input .description-textarea:focus {
  outline: none;
  border-color: #ff9000;
}

.transfer-description-input .description-textarea::placeholder {
  color: #999;
}

.transfer-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.transfer-btn {
  flex: 1;
  padding: 10px 16px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.transfer-btn.enter-chat {
  background: linear-gradient(135deg, #ff9000 0%, #ff5000 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(255, 144, 0, 0.3);
}

.transfer-btn.enter-chat:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 144, 0, 0.4);
}

.transfer-btn.enter-chat:active {
  transform: translateY(0);
}

.transfer-channels {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding-top: 10px;
  border-top: 1px solid rgba(255, 144, 0, 0.15);
}

.transfer-channels .channel-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 8px;
  font-size: 13px;
}

.transfer-channels .channel-label {
  color: #666;
  font-weight: 500;
}

.transfer-channels .channel-desc {
  color: #999;
}
</style>
