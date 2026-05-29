<template>
  <div class="order-list-container">
    <div class="order-list-header">
      <span class="header-icon">📋</span>
      <span class="header-title">我的订单</span>
      <span class="header-count">共 {{ totalCount }} 单</span>
    </div>

    <div class="order-list-body">
      <div
        v-for="(order, index) in orders"
        :key="index"
        class="order-item"
      >
        <div class="order-item-top">
          <div class="order-item-title">
            <span class="order-no">#{{ order.orderNo || order.id }}</span>
            <span class="order-tag" :class="getStatusClass(order.state)">{{ getStatusText(order.state) }}</span>
          </div>
        </div>

        <div class="order-item-meta">
          <div class="meta-row">
            <span class="meta-label">下单时间</span>
            <span class="meta-val">{{ formatTime(order.createTime) }}</span>
          </div>
          <div class="meta-row">
            <span class="meta-label">订单金额</span>
            <span class="meta-val price">¥{{ order.totalPrice || 0 }}</span>
          </div>
          <div class="meta-row" v-if="order.linkUser">
            <span class="meta-label">收货人</span>
            <span class="meta-val">{{ order.linkUser }}</span>
          </div>
        </div>

        <div class="order-item-addr">
          <div class="addr-row">
            <span class="addr-key">收</span>
            <span class="addr-val">{{ order.linkAddress || '未填写' }}</span>
          </div>
          <div v-if="order.deliveryAddress" class="addr-row">
            <span class="addr-key">发</span>
            <span class="addr-val">{{ order.deliveryAddress }}</span>
          </div>
          <div v-if="order.expressCompany" class="addr-row">
            <span class="addr-key">快递</span>
            <span class="addr-val">{{ order.expressCompany }} {{ order.expressNo || '' }}</span>
          </div>
        </div>

        <div class="order-item-actions">
          <button class="tb-btn tb-btn-outline" @click="openMap(order)">📍 查看地址</button>
          <button class="tb-btn tb-btn-outline" @click="viewOrderDetail(order)">📄 详情</button>
          <button v-if="order.state === '0' || order.state === '待支付'" class="tb-btn tb-btn-primary" @click="payOrder(order)">💳 去支付</button>
          <button v-if="order.state === '2' || order.state === '待收货'" class="tb-btn tb-btn-primary" @click="confirmReceive(order)">📦 确认收货</button>
        </div>
      </div>
    </div>

    <div v-if="hasMore" class="load-more-wrapper">
      <button class="load-more-btn" :disabled="loadingMore" @click="loadMore">
        <span v-if="loadingMore">加载中...</span>
        <span v-else>加载更多（{{ orders.length }}/{{ totalCount }}）</span>
      </button>
    </div>

    <div v-if="showMapDialog" class="map-overlay" @click.self="closeMap">
      <div class="map-dialog">
        <AddressMap
          :address="mapAddress"
          :seller-address="mapSellerAddress"
          @close="closeMap"
        />
      </div>
    </div>
  </div>
</template>

<script>
import AddressMap from '@/components/AddressMap.vue'

export default {
  name: 'OrderList',
  components: { AddressMap },
  props: {
    orders: { type: Array, required: true, default: () => [] },
    orderCount: { type: Number, default: 0 },
    totalCount: { type: Number, default: 0 },
    hasMore: { type: Boolean, default: false },
    currentPage: { type: Number, default: 1 },
    pageSize: { type: Number, default: 10 }
  },
  data() {
    return {
      loadingMore: false,
      showMapDialog: false,
      mapAddress: { linkUser: '', linkPhone: '', linkAddress: '' },
      mapSellerAddress: ''
    }
  },
  watch: {
    orders: {
      handler() {
        this.loadingMore = false
      },
      deep: true
    }
  },
  methods: {
    openMap(order) {
      this.mapAddress = { linkUser: order.linkUser || '', linkPhone: order.linkPhone || '', linkAddress: order.linkAddress || '' }
      this.mapSellerAddress = order.deliveryAddress || ''
      this.showMapDialog = true
    },
    closeMap() { this.showMapDialog = false },
    formatTime(t) { return t || '未知时间' },
    getStatusClass(s) {
      const m = { '0':'st-pending','1':'st-paid','2':'st-shipped','3':'st-completed','4':'st-cancelled','待支付':'st-pending','已支付':'st-paid','待收货':'st-shipped','已完成':'st-completed','已取消':'st-cancelled' }
      return m[s] || 'st-default'
    },
    getStatusText(s) {
      const m = { '0':'待支付','1':'已支付','2':'待收货','3':'已完成','4':'已取消','待支付':'待支付','已支付':'已支付','待收货':'待收货','已完成':'已完成','已取消':'已取消' }
      return m[s] || '未知'
    },
    viewOrderDetail(o) { this.$emit('view-detail', o) },
    payOrder(o) { this.$emit('pay-order', o) },
    confirmReceive(o) { this.$emit('confirm-receive', o) },
    loadMore() {
      if (this.loadingMore) return
      this.loadingMore = true
      this.$emit('load-more', { page: this.currentPage + 1, limit: this.pageSize })
    }
  }
}
</script>

<style scoped>
.order-list-container {
  margin-top: 12px;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #eee;
  overflow: hidden;
  max-height: 520px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}

.order-list-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: linear-gradient(135deg, #fff5f0 0%, #ffffff 100%);
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.header-icon { font-size: 15px; }
.header-title { font-size: 13px; font-weight: 600; color: #1a1a1a; }
.header-count { font-size: 11px; color: #999; margin-left: auto; }

.order-list-body {
  flex: 1;
  overflow-y: auto;
  padding: 8px 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 0;
}

.order-list-body::-webkit-scrollbar { width: 4px; }
.order-list-body::-webkit-scrollbar-thumb { background: #ddd; border-radius: 4px; }

.order-item {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
  overflow: hidden;
  transition: all 0.25s ease;
  flex-shrink: 0;
}
.order-item:hover {
  box-shadow: 0 2px 8px rgba(255,80,0,0.08);
  border-color: #ffe0cc;
}

.order-item-top {
  padding: 8px 10px 2px;
}

.order-item-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-no {
  font-size: 11px;
  color: #666;
  font-family: monospace;
  font-weight: 500;
}

.order-tag {
  font-size: 10px;
  padding: 1px 8px;
  border-radius: 8px;
  font-weight: 500;
}

.st-pending  { background: #fff3e0; color: #f57c00; }
.st-paid     { background: #e3f2fd; color: #1976d2; }
.st-shipped  { background: #f3e5f5; color: #7b1fa2; }
.st-completed{ background: #e8f5e9; color: #388e3c; }
.st-cancelled{ background: #ffebee; color: #d32f2f; }
.st-default  { background: #f5f5f5; color: #999; }

.order-item-meta {
  padding: 4px 10px 4px;
}

.meta-row {
  display: flex;
  justify-content: space-between;
  padding: 1px 0;
}

.meta-label { font-size: 11px; color: #909399; }
.meta-val { font-size: 11px; color: #303133; font-weight: 500; }
.meta-val.price { color: #ff4400; font-weight: 600; }

.order-item-addr {
  padding: 4px 10px 6px;
  background: #fafafa;
  border-top: 1px solid #f5f5f5;
  border-bottom: 1px solid #f5f5f5;
}

.addr-row {
  display: flex;
  gap: 5px;
  padding: 1px 0;
}

.addr-key {
  font-size: 10px;
  color: #999;
  width: 16px;
  flex-shrink: 0;
  font-weight: 500;
}

.addr-val {
  font-size: 10px;
  color: #303133;
  line-height: 1.4;
}

.order-item-actions {
  display: flex;
  gap: 5px;
  padding: 6px 10px;
  flex-wrap: wrap;
}

.tb-btn {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 4px 10px;
  font-size: 10px;
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.25s ease;
  white-space: nowrap;
  font-weight: 500;
}

.tb-btn-outline {
  border: 1.5px solid #ff5000;
  color: #ff5000;
  background: #fff;
}
.tb-btn-outline:hover {
  background: #fff5f0;
  transform: translateY(-1px);
}

.tb-btn-primary {
  border: none;
  color: #fff;
  background: linear-gradient(90deg, #ff9000 0%, #ff5000 100%);
  box-shadow: 0 2px 6px rgba(255, 80, 0, 0.2);
}
.tb-btn-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 3px 10px rgba(255, 80, 0, 0.3);
}

.load-more-wrapper {
  padding: 8px 10px 10px;
  text-align: center;
  flex-shrink: 0;
}

.load-more-btn {
  width: 100%;
  padding: 8px 0;
  background: linear-gradient(135deg, #ff9000, #ff5000);
  color: #fff;
  border: none;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(255, 80, 0, 0.2);
}
.load-more-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 80, 0, 0.3);
}
.load-more-btn:active {
  transform: translateY(0);
}
.load-more-btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
  box-shadow: none;
  transform: none;
}

.map-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
}

.map-dialog {
  width: 750px;
  max-width: 90vw;
  max-height: 85vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
</style>