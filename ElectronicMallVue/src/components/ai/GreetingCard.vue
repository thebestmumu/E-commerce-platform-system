<template>
  <div class="greeting-card-container">
    <div class="greeting-message">
      <div class="greeting-avatar">小皮</div>
      <div class="greeting-content">
        <div class="greeting-text">{{ greetingText }}</div>
        <div class="greeting-subtitle">{{ greetingSubtitle }}</div>
      </div>
    </div>
    
    <div v-if="isAdmin" class="admin-tools-section">
      <div class="section-header">
        <span class="section-icon">🤖</span>
        <span class="section-title">AI 智能工具箱</span>
        <span class="section-badge">管理员专属</span>
      </div>
      <div class="admin-tools-grid">
        <div class="admin-tool-item" @click="openTool('copywriting')">
          <div class="tool-icon">✍️</div>
          <div class="tool-name">AI 文案生成</div>
          <div class="tool-desc">标题 / 卖点 / 回复话术</div>
        </div>
        <div class="admin-tool-item" @click="openTool('price')">
          <div class="tool-icon">📊</div>
          <div class="tool-name">竞品价格分析</div>
          <div class="tool-desc">价格行情 + 定价建议</div>
        </div>
        <div class="admin-tool-item" @click="openTool('forecast')">
          <div class="tool-icon">📦</div>
          <div class="tool-name">库存销量预测</div>
          <div class="tool-desc">销量预测 + 备货预警</div>
        </div>
      </div>
    </div>
    
    <div v-if="!isAdmin" class="quick-actions">
      <div class="action-item" @click="handleQuickAction('催单')">
        <div class="action-icon">⚡</div>
        <div class="action-text">我要催单</div>
      </div>
      <div class="action-item" @click="handleQuickAction('价保')">
        <div class="action-icon">💰</div>
        <div class="action-text">价保申请</div>
      </div>
      <div class="action-item" @click="handleQuickAction('退换')">
        <div class="action-icon">🔄</div>
        <div class="action-text">退换/售后</div>
      </div>
      <div class="action-item" @click="handleQuickAction('活动')">
        <div class="action-icon">🎁</div>
        <div class="action-text">活动问题</div>
      </div>
      <div class="action-item" @click="handleQuickAction('安装')">
        <div class="action-icon">🔧</div>
        <div class="action-text">安装服务</div>
      </div>
    </div>
    
    <div v-if="isAdmin" class="faq-card">
      <div class="faq-header">
        <h4>管理员快捷指令</h4>
      </div>
      <div class="faq-list">
        <div 
          v-for="(question, index) in adminQuestions" 
          :key="index"
          class="faq-item"
          @click="handleQuestionClick(question)"
        >
          <span class="faq-number">{{ index + 1 }}</span>
          <span class="faq-text">{{ question.text }}</span>
          <span class="faq-arrow">›</span>
        </div>
      </div>
    </div>
    
    <div v-if="!isAdmin" class="faq-card">
      <div class="faq-header">
        <h4>猜你想问</h4>
        <button class="refresh-btn" @click="refreshQuestions">
          换一批问题 🔄
        </button>
      </div>
      
      <div class="faq-list">
        <div 
          v-for="(question, index) in currentQuestions" 
          :key="index"
          class="faq-item"
          @click="handleQuestionClick(question)"
        >
          <span class="faq-number">{{ index + 1 }}</span>
          <span class="faq-text">{{ question.text }}</span>
          <span class="faq-arrow">›</span>
        </div>
      </div>
    </div>
    
    <div v-if="showDetail" class="detail-modal" @click="closeDetail">
      <div class="detail-content" @click.stop>
        <div class="detail-header">
          <h4>{{ selectedQuestion.text }}</h4>
          <button class="close-btn" @click="closeDetail">×</button>
        </div>
        <div class="detail-body">
          <p>{{ selectedQuestion.answer }}</p>
        </div>
        <div class="detail-footer">
          <button class="ask-btn" @click="askQuestion">
            继续提问
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'GreetingCard',
  data() {
    return {
      greetingText: '',
      greetingSubtitle: '',
      isAdmin: false,
      showDetail: false,
      selectedQuestion: {},
      currentQuestions: [],
      allQuestions: [
        {
          text: '收到的商品坏了怎么办',
          answer: '如果您收到的商品有质量问题，请在签收后7天内申请退换货。您可以在订单详情页面点击"申请售后"，选择退换货原因并上传商品照片，我们会尽快为您处理。'
        },
        {
          text: '如何申请退款',
          answer: '申请退款流程：1.进入"我的订单"找到对应订单 2.点击"申请退款"按钮 3.选择退款原因 4.提交申请。退款将在1-3个工作日内原路返回您的支付账户。'
        },
        {
          text: '什么时候发货呢',
          answer: '一般情况下，订单支付成功后24小时内发货。预售商品按页面标注的发货时间为准。您可以在订单详情中查看物流信息，了解包裹实时状态。'
        },
        {
          text: '售后运费谁承担',
          answer: '因商品质量问题导致的退换货，运费由商家承担；因个人原因（如不喜欢、拍错等）导致的退换货，运费由买家承担。具体以售后审核结果为准。'
        },
        {
          text: '如何修改收货地址',
          answer: '订单未发货前，您可以在订单详情页点击"修改地址"进行更改。如订单已发货，请联系快递公司或等待包裹到达后联系派件员协商改派。'
        },
        {
          text: '商品有假货怎么办',
          answer: '我们承诺所有商品均为正品。如怀疑收到假货，请保留商品原包装和相关凭证，在订单详情页申请"假货投诉"，平台将介入调查并保障您的权益。'
        },
        {
          text: '如何查看物流信息',
          answer: '查看物流方式：1.进入"我的订单" 2.找到对应订单点击"查看物流" 3.可看到实时物流轨迹。也可复制运单号到快递公司官网查询详细信息。'
        },
        {
          text: '优惠券如何使用',
          answer: '使用优惠券：1.结算时系统会自动匹配可用优惠券 2.您也可以手动选择优惠券 3.注意查看优惠券的使用条件和有效期 4.部分商品可能不支持优惠券抵扣。'
        },
        {
          text: '如何开发票',
          answer: '申请发票：1.订单完成后进入订单详情 2.点击"申请开票" 3.填写发票抬头和税号 4.电子发票将发送至您预留的邮箱。纸质发票随商品一起寄出。'
        },
        {
          text: '会员积分怎么获得',
          answer: '获得积分方式：1.每消费1元获得1积分 2.每日签到可获得额外积分 3.参与平台活动可赢取积分 4.评价商品也可获得积分奖励。积分可在积分商城兑换商品或抵扣现金。'
        },
        {
          text: '如何联系人工客服',
          answer: '联系人工客服：1.在对话框输入"人工客服" 2.或点击页面右下角客服图标 3.工作时间：9:00-22:00 4.非工作时间可留言，我们会尽快回复您。'
        },
        {
          text: '商品保修期多久',
          answer: '保修期限：1.电子产品通常保修1年 2.服装鞋帽类7天无理由退换 3.食品类不支持退换 4.具体保修政策以商品详情页标注为准。保修期内非人为损坏可免费维修。'
        },
        {
          text: '如何取消订单',
          answer: '取消订单：1.订单未发货前，进入订单详情点击"取消订单" 2.选择取消原因 3.确认取消。如订单已发货，需申请退款或拒收包裹。取消后退款将在1-3个工作日到账。'
        },
        {
          text: '支付方式有哪些',
          answer: '支持的支付方式：1.支付宝 2.微信支付 3.银行卡支付 4.花呗分期 5.白条支付。您可以根据需要选择合适的支付方式，部分商品支持货到付款。'
        },
        {
          text: '如何评价商品',
          answer: '评价商品：1.确认收货后进入订单详情 2.点击"评价"按钮 3.选择评分星级 4.填写评价内容并可上传图片 5.提交评价。真实评价有助于其他买家了解商品，也能获得积分奖励。'
        }
      ],
      adminQuestions: [
        {
          text: '帮我生成一个手机的商品标题',
          answer: '您可以直接对我说"帮我生成一个手机的商品标题"，我会使用AI为您生成吸引人的商品标题。'
        },
        {
          text: '分析iPhone的市场价格',
          answer: '您可以对我说"分析iPhone的市场价格"，我会为您提供竞品价格行情分析和定价建议。'
        },
        {
          text: '预测下个月的库存需求',
          answer: '您可以对我说"预测下个月的库存需求"，我会为您提供库存销量预测和备货建议。'
        },
        {
          text: '帮我写一段客服回复话术',
          answer: '您可以对我说"帮我写一段客服回复话术"，我会使用AI为您生成专业的客服回复。'
        },
        {
          text: '分析某商品的竞品价格',
          answer: '您可以对我说"分析XX商品的竞品价格"，我会为您提供详细的市场价格分析报告。'
        }
      ]
    }
  },
  mounted() {
    this.checkUserRole()
    this.refreshQuestions()
  },
  methods: {
    checkUserRole() {
      const userStr = localStorage.getItem('user')
      if (userStr) {
        try {
          const user = JSON.parse(userStr)
          if (user.role === 'admin') {
            this.isAdmin = true
            this.greetingText = '管理员您好！AI 智能工具箱已就绪'
            this.greetingSubtitle = '我可以帮您生成文案、分析价格、预测库存，点击工具卡片即可开始~'
          } else {
            this.isAdmin = false
            this.greetingText = '您好，智能客服助手为您服务！'
            this.greetingSubtitle = '购物遇到问题，点击下方按钮或直接输入向我提问吧~🌸'
          }
        } catch (e) {
          this.isAdmin = false
          this.greetingText = '您好，智能客服助手为您服务！'
          this.greetingSubtitle = '购物遇到问题，点击下方按钮或直接输入向我提问吧~🌸'
        }
      } else {
        this.isAdmin = false
        this.greetingText = '您好，智能客服助手为您服务！'
        this.greetingSubtitle = '购物遇到问题，点击下方按钮或直接输入向我提问吧~🌸'
      }
    },
    refreshQuestions() {
      const shuffled = [...this.allQuestions].sort(() => 0.5 - Math.random())
      this.currentQuestions = shuffled.slice(0, 5)
    },
    handleQuickAction(action) {
      this.$emit('quick-action', action)
    },
    handleQuestionClick(question) {
      this.selectedQuestion = question
      this.showDetail = true
    },
    closeDetail() {
      this.showDetail = false
    },
    askQuestion() {
      this.$emit('ask-question', this.selectedQuestion.text)
      this.closeDetail()
    },
    openTool(tool) {
      this.$emit('open-tool', tool)
    }
  }
}
</script>

<style scoped>
.greeting-card-container {
  padding: 16px;
  background: linear-gradient(135deg, #fff5f0 0%, #ffffff 100%);
  border-radius: 12px;
  border: 1px solid #ffe4d6;
}

.greeting-message {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;
}

.greeting-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff9000, #ff5000);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: bold;
  flex-shrink: 0;
}

.greeting-content {
  flex: 1;
}

.greeting-text {
  font-size: 15px;
  color: #303133;
  font-weight: 500;
  margin-bottom: 4px;
}

.greeting-subtitle {
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}

.admin-tools-section {
  margin-bottom: 16px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.section-icon {
  font-size: 16px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.section-badge {
  background: linear-gradient(90deg, #ff9000, #ff5000);
  color: white;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 10px;
  font-weight: 600;
}

.admin-tools-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.admin-tool-item {
  background: white;
  border-radius: 12px;
  padding: 16px 12px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid #f0f0f0;
}

.admin-tool-item:hover {
  border-color: #ff5000;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 80, 0, 0.15);
}

.tool-icon {
  font-size: 28px;
  margin-bottom: 8px;
}

.tool-name {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.tool-desc {
  font-size: 11px;
  color: #909399;
}

.quick-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  overflow-x: auto;
  padding-bottom: 8px;
}

.action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 12px 16px;
  background: white;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  min-width: 70px;
  border: 1px solid #f0f0f0;
}

.action-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 80, 0, 0.15);
  border-color: #ff9000;
}

.action-icon {
  font-size: 20px;
}

.action-text {
  font-size: 11px;
  color: #606266;
  white-space: nowrap;
}

.faq-card {
  background: white;
  border-radius: 12px;
  padding: 16px;
  border: 1px solid #f0f0f0;
}

.faq-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.faq-header h4 {
  margin: 0;
  font-size: 14px;
  color: #303133;
}

.refresh-btn {
  background: none;
  border: none;
  color: #ff5000;
  font-size: 12px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.refresh-btn:hover {
  background-color: #fff5f0;
}

.faq-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.faq-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: #fafafa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.faq-item:hover {
  background: #fff5f0;
  transform: translateX(4px);
}

.faq-number {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff9000, #ff5000);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: bold;
  flex-shrink: 0;
}

.faq-text {
  flex: 1;
  font-size: 13px;
  color: #606266;
}

.faq-arrow {
  color: #c0c4cc;
  font-size: 16px;
}

.detail-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.detail-content {
  background: white;
  border-radius: 16px;
  width: 90%;
  max-width: 500px;
  max-height: 80vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.detail-header h4 {
  margin: 0;
  font-size: 16px;
  color: #303133;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: #909399;
  cursor: pointer;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: background-color 0.2s;
}

.close-btn:hover {
  background-color: #f5f5f5;
}

.detail-body {
  padding: 20px;
  overflow-y: auto;
  flex: 1;
}

.detail-body p {
  margin: 0;
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
}

.detail-footer {
  padding: 16px 20px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  justify-content: flex-end;
}

.ask-btn {
  padding: 8px 20px;
  background: linear-gradient(135deg, #ff9000, #ff5000);
  color: white;
  border: none;
  border-radius: 20px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.ask-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 80, 0, 0.3);
}
</style>
