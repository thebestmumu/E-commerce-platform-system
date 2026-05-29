<template>
<div class="order-list-container">
    <div class="order-header">
        <h2 class="order-title">我的订单</h2>
        <div class="order-tabs">
            <div 
                v-for="tab in orderTabs" 
                :key="tab.value"
                :class="['order-tab', { active: activeTab === tab.value }]"
                @click="activeTab = tab.value"
            >
                {{ tab.label }}
            </div>
        </div>
    </div>

    <!-- 订单数据分析图表 -->
    <order-analytics v-if="userId" :userId="userId"></order-analytics>

    <!-- 地图区域：全国地图 -->
    <div class="map-section">
      <div class="map-left">
        <global-delivery-map></global-delivery-map>
      </div>
    </div>

    <div v-if="!userId || filteredOrders.length === 0" class="empty-order-box">
        <div class="empty-icon">📦</div>
        <div class="empty-text">暂无订单记录</div>
        <button class="go-shopping-btn" @click="$router.push('/')">去逛逛</button>
    </div>
    
    <div v-else class="order-list">
        <div v-for="order in filteredOrders" :key="order.id" class="order-card">
            <div class="order-card-header">
                <div class="order-info">
                    <span class="order-number">订单号：{{ order.orderNo || order.id }}</span>
                    <span class="order-time">{{ formatOrderTime(order.createTime) }}</span>
                </div>
                <div class="order-status" :class="getStatusClass(order.status)">
                    {{ getStatusText(order.status) }}
                </div>
            </div>

            <div class="order-goods">
                <div v-for="(good, index) in order.goodsList" :key="index" class="order-good-item">
                    <div class="good-image-wrapper">
                        <img 
                            v-if="!isEmojiImage(good.imgs)" 
                            :src="baseApi + good.imgs" 
                            class="good-image"
                            @error="handleImageError"
                        />
                        <div v-else class="emoji-good-placeholder">
                            {{ getEmoji(good.imgs) }}
                        </div>
                    </div>
                    <div class="good-info">
                        <div class="good-name">{{ good.name }}</div>
                        <div class="good-spec" v-if="good.standard">{{ good.standard }}</div>
                        <div class="good-price-row">
                            <span class="good-price">¥{{ good.price }}</span>
                            <span class="good-count">x{{ good.count }}</span>
                        </div>
                    </div>
                    <div class="good-actions">
                        <button class="btn-good-map" @click="showAddressMap(order, good)">查看地址</button>
                    </div>
                </div>
            </div>

            <div class="order-card-footer">
                <div class="order-total">
                    <span class="total-label">共 {{ getOrderTotalCount(order) }} 件商品</span>
                    <span class="total-amount">合计：<span class="total-price">¥{{ order.totalPrice }}</span></span>
                </div>
                <div class="order-actions">
                    <button v-if="getOrderStatusNumber(order.status) === 0" class="btn-cancel" @click="cancelOrder(order.id)">取消订单</button>
                    <button v-if="getOrderStatusNumber(order.status) === 0" class="btn-pay" @click="payOrder(order.id)">立即付款</button>
                    <button v-if="getOrderStatusNumber(order.status) === 1" class="btn-receive" @click="receiveOrder(order.id)">确认收货</button>
                    <button v-if="getOrderStatusNumber(order.status) === 2" class="btn-review" @click="reviewOrder(order)">评价</button>
                    <button class="btn-delete" @click="deleteOrder(order.id)">删除</button>
                </div>
            </div>
        </div>
    </div>

    <!-- 地址地图弹窗 -->
    <el-dialog 
        title="地址地图" 
        :visible.sync="showMapDialog" 
        width="800px"
        :close-on-click-modal="false"
    >
        <address-map 
            v-if="showMapDialog"
            :address="selectedOrder.address" 
            :seller-address="selectedOrder.deliveryAddress"
            @close="showMapDialog = false"
        ></address-map>
    </el-dialog>
</div>
</template>

<script>
import API from "@/utils/request";
import OrderAnalytics from "@/components/order/OrderAnalytics.vue";
import AddressMap from "@/components/AddressMap.vue";
import GlobalDeliveryMap from "@/components/GlobalDeliveryMap.vue";

export default {
    name: "OrderList",
    components: {
        OrderAnalytics,
        AddressMap,
        GlobalDeliveryMap
    },
    data() {
        return {
            baseApi: this.$store.state.baseApi,
            orders: [],
            userId: null,
            activeTab: 'all',
            orderTabs: [
                { label: '全部订单', value: 'all' },
                { label: '待付款', value: 'pending' },
                { label: '待收货', value: 'shipped' },
                { label: '已完成', value: 'completed' }
            ],
            showMapDialog: false,
            selectedOrder: {
                address: {},
                deliveryAddress: ''
            }
        }
    },
    computed: {
        filteredOrders() {
            if (this.activeTab === 'all') {
                return this.orders;
            }
            const statusMap = {
                'pending': ['待付款', 0],
                'shipped': ['已支付', '已发货', 1],
                'completed': ['已收货', '已完成', 2]
            };
            const allowedStatuses = statusMap[this.activeTab];
            if (!allowedStatuses) return this.orders;
            
            return this.orders.filter(order => {
                const statusNum = this.getOrderStatusNumber(order.status);
                return allowedStatuses.includes(order.status) || allowedStatuses.includes(statusNum);
            });
        }
    },
    methods: {
        formatOrderTime(timeStr) {
            if (!timeStr) return '';
            const date = new Date(timeStr);
            return date.toLocaleString('zh-CN', { 
                year: 'numeric', 
                month: '2-digit', 
                day: '2-digit',
                hour: '2-digit',
                minute: '2-digit'
            });
        },
        getStatusText(status) {
            if (typeof status === 'string') {
                return status;
            }
            const statusMap = {
                0: '待付款',
                1: '待收货',
                2: '已完成',
                3: '已取消'
            };
            return statusMap[status] || '未知状态';
        },
        getStatusClass(status) {
            if (typeof status === 'string') {
                const classMap = {
                    '待付款': 'status-pending',
                    '已支付': 'status-paid',
                    '已发货': 'status-shipped',
                    '已收货': 'status-completed',
                    '已完成': 'status-completed',
                    '已取消': 'status-cancelled'
                };
                return classMap[status] || '';
            }
            const classMap = {
                0: 'status-pending',
                1: 'status-shipped',
                2: 'status-completed',
                3: 'status-cancelled'
            };
            return classMap[status] || '';
        },
        getOrderStatusNumber(status) {
            if (typeof status === 'string') {
                const statusMap = {
                    '待付款': 0,
                    '已支付': 1,
                    '已发货': 1,
                    '已收货': 2,
                    '已完成': 2,
                    '已取消': 3
                };
                return statusMap[status] || -1;
            }
            return status;
        },
        getOrderTotalCount(order) {
            if (!order.goodsList) return 0;
            return order.goodsList.reduce((sum, good) => sum + (good.count || 1), 0);
        },
        cancelOrder(orderId) {
            this.$confirm('确定取消该订单吗？', '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(() => {
                API.put(`/api/order/cancel/${orderId}`).then(res => {
                    if (res.code === '200') {
                        this.$message.success('订单已取消');
                        this.loadOrders();
                    }
                });
            });
        },
        payOrder(orderId) {
            console.log('payOrder 被调用, orderId:', orderId);
            console.log('所有订单:', this.orders);
            const order = this.orders.find(o => o.id === orderId);
            console.log('找到的订单:', order);
            if (order && order.orderNo) {
                console.log('跳转路由: /pay/' + order.orderNo);
                this.$router.push(`/pay/${order.orderNo}`);
            } else {
                this.$message.error('订单编号不存在');
            }
        },
        receiveOrder(orderId) {
            this.$confirm('确认已收到商品？', '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'info'
            }).then(() => {
                API.put(`/api/order/receive/${orderId}`).then(res => {
                    if (res.code === '200') {
                        this.$message.success('确认收货成功');
                        this.loadOrders();
                    }
                });
            });
        },
        reviewOrder(order) {
            this.$router.push({
                path: '/review',
                query: { orderId: order.id }
            });
        },
        deleteOrder(orderId) {
            this.$confirm('确定删除该订单吗？', '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(() => {
                API.delete(`/api/order/${orderId}`).then(res => {
                    if (res.code === '200') {
                        this.$message.success('订单已删除');
                        this.loadOrders();
                    }
                });
            });
        },
        isEmojiImage(imgs) {
            return imgs && imgs.startsWith('emoji:');
        },
        getEmoji(imgs) {
            if (!imgs || !imgs.startsWith('emoji:')) return '📦';
            const parts = imgs.split(':');
            return parts.length >= 2 ? parts[1] : '📦';
        },
        handleImageError(e) {
            e.target.style.display = 'none';
        },
        showAddressMap(order, good) {
            this.selectedOrder = {
                address: {
                    linkUser: order.linkUser,
                    linkPhone: order.linkPhone,
                    linkAddress: order.linkAddress
                },
                deliveryAddress: good ? (good.deliveryAddress || '') : (order.deliveryAddress || ''),
                goodName: good ? good.name : ''
            };
            this.showMapDialog = true;
        },
        loadOrders() {
            if (!this.userId) return;
            API.get(`/api/order/userid/${this.userId}`).then(res => {
                console.log('订单原始数据:', res.data);
                if (res.code === '200') {
                    const rawData = res.data || [];
                    const orderMap = {};
                    
                    rawData.forEach(item => {
                        const orderId = item.id;
                        if (!orderMap[orderId]) {
                            orderMap[orderId] = {
                                id: item.id,
                                orderNo: item.orderno || item.orderNo || '',
                                totalPrice: item.totalprice || item.totalPrice || 0,
                                userId: item.userid || item.userId || 0,
                                linkUser: item.linkuser || item.linkUser || '',
                                linkPhone: item.linkphone || item.linkPhone || '',
                                linkAddress: item.linkaddress || item.linkAddress || '',
                                status: item.status || '',
                                createTime: item.createtime || item.createTime || '',
                                deliveryAddress: item.deliveryaddress || item.deliveryAddress || '',
                                expressCompany: item.expresscompany || item.expressCompany || '',
                                expressNo: item.expressno || item.expressNo || '',
                                goodsList: []
                            };
                        }
                        
                        if (item.goodid || item.goodId) {
                            orderMap[orderId].goodsList.push({
                                id: item.goodid || item.goodId,
                                name: item.goodname || item.goodName || '',
                                count: item.count || 1,
                                standard: item.standard || '',
                                imgs: item.imgs || '',
                                price: item.price || 0,
                                discount: item.discount || 0,
                                deliveryAddress: item.gooddeliveryaddress || item.goodDeliveryAddress || ''
                            });
                        }
                    });
                    
                    // 对每个订单的商品列表进行倒序排序
                    Object.values(orderMap).forEach(order => {
                        order.goodsList.reverse();
                    });
                    
                    // 按创建时间倒序排序，最新的订单在最前面
                    this.orders = Object.values(orderMap).sort((a, b) => {
                        const timeA = new Date(a.createTime).getTime();
                        const timeB = new Date(b.createTime).getTime();
                        return timeB - timeA;
                    });
                    console.log('处理后的订单数据:', this.orders);
                    // 调试：检查每个订单的 orderNo 和 status
                    this.orders.forEach(order => {
                        console.log(`订单ID: ${order.id}, orderNo: ${order.orderNo}, status: ${order.status}, getOrderStatusNumber: ${this.getOrderStatusNumber(order.status)}`);
                    });
                }
            });
        }
    },
    created() {
        API.get("/userid").then(userId => {
            this.userId = userId;
            this.loadOrders();
        });
    }
}
</script>

<style scoped>
.order-list-container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 24px;
    background: linear-gradient(135deg, #fff5f0 0%, #ffffff 100%);
    min-height: 100vh;
}

.order-header {
    background: linear-gradient(135deg, #ffffff 0%, #fff5f0 100%);
    border-radius: 16px;
    padding: 28px;
    margin-bottom: 24px;
    box-shadow: 0 4px 16px rgba(255, 80, 0, 0.06), 0 2px 6px rgba(255, 80, 0, 0.04);
    border: 1px solid rgba(255, 224, 204, 0.8);
}

.order-title {
    font-size: 26px;
    font-weight: 700;
    color: #1e293b;
    margin: 0 0 24px 0;
    letter-spacing: 0.5px;
}

.order-tabs {
    display: flex;
    gap: 12px;
    border-bottom: 2px solid #e2e8f0;
    padding-bottom: 0;
}

.order-tab {
    padding: 14px 28px;
    font-size: 15px;
    color: #64748b;
    cursor: pointer;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    position: relative;
    border-bottom: 3px solid transparent;
    margin-bottom: -2px;
    border-radius: 8px 8px 0 0;
    font-weight: 500;
}

.order-tab:hover {
    color: #ff9000;
    background: linear-gradient(135deg, #fff5f0 0%, #ffe8d6 100%);
}

.order-tab.active {
    color: #ff5000;
    font-weight: 600;
    border-bottom-color: #ff5000;
    background: linear-gradient(135deg, #fff5f0 0%, #ffe8d6 100%);
}

.empty-order-box {
    background: linear-gradient(135deg, #ffffff 0%, #fafbfc 100%);
    border-radius: 16px;
    padding: 80px 20px;
    text-align: center;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06), 0 2px 6px rgba(0, 0, 0, 0.04);
    border: 1px solid rgba(226, 232, 240, 0.8);
}

.empty-icon {
    font-size: 80px;
    margin-bottom: 20px;
}

.empty-text {
    font-size: 18px;
    color: #64748b;
    margin-bottom: 30px;
    font-weight: 500;
}

.go-shopping-btn {
    background: linear-gradient(135deg, #ff9000 0%, #ff5000 100%);
    color: white;
    border: none;
    border-radius: 24px;
    padding: 14px 40px;
    font-size: 16px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    box-shadow: 0 4px 12px rgba(255, 80, 0, 0.25);
}

.go-shopping-btn:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 20px rgba(255, 80, 0, 0.35);
    background: linear-gradient(135deg, #ff5000 0%, #e04400 100%);
}

.order-list {
    display: flex;
    flex-direction: column;
    gap: 20px;
}

.map-section {
    display: flex;
    gap: 20px;
    margin: 20px 0;
    align-items: flex-start;
}

.map-left {
    flex: 1;
    min-width: 0;
}

.map-right {
    flex: 0 0 420px;
    min-width: 420px;
}

.order-card {
    background: linear-gradient(135deg, #ffffff 0%, #fafbfc 100%);
    border-radius: 16px;
    overflow: hidden;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06), 0 2px 6px rgba(0, 0, 0, 0.04);
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    border: 1px solid rgba(226, 232, 240, 0.8);
}

.order-card:hover {
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1), 0 4px 12px rgba(0, 0, 0, 0.06);
    transform: translateY(-2px);
    border-color: #3b82f6;
}

.order-card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 18px 24px;
    background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
    border-bottom: 1px solid #e2e8f0;
}

.order-info {
    display: flex;
    flex-direction: column;
    gap: 6px;
}

.order-number {
    font-size: 15px;
    color: #1e293b;
    font-weight: 600;
}

.order-time {
    font-size: 13px;
    color: #64748b;
}

.order-status {
    padding: 8px 20px;
    border-radius: 20px;
    font-size: 14px;
    font-weight: 600;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04);
}

.status-pending {
    background: linear-gradient(135deg, #fff7ed 0%, #ffedd5 100%);
    color: #ea580c;
    border: 1px solid #fed7aa;
}

.status-paid {
    background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
    color: #d97706;
    border: 1px solid #fcd34d;
}

.status-shipped {
    background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
    color: #2563eb;
    border: 1px solid #bfdbfe;
}

.status-completed {
    background: linear-gradient(135deg, #f0fdf4 0%, #dcfce7 100%);
    color: #16a34a;
    border: 1px solid #bbf7d0;
}

.status-cancelled {
    background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
    color: #64748b;
    border: 1px solid #e2e8f0;
}

.order-goods {
    padding: 24px;
}

.order-good-item {
    display: flex;
    gap: 20px;
    padding: 16px 0;
    border-bottom: 1px solid #e2e8f0;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    align-items: center;
}

.order-good-item:hover {
    background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
    margin: 0 -24px;
    padding-left: 24px;
    padding-right: 24px;
    border-radius: 8px;
}

.order-good-item:last-child {
    border-bottom: none;
}

.good-image-wrapper {
    width: 120px;
    height: 120px;
    border-radius: 12px;
    overflow: hidden;
    flex-shrink: 0;
    background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.good-image {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.emoji-good-placeholder {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
    font-size: 48px;
}

.good-info {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
}

.good-name {
    font-size: 16px;
    color: #1e293b;
    font-weight: 600;
    margin-bottom: 8px;
    line-height: 1.4;
}

.good-spec {
    font-size: 13px;
    color: #64748b;
    margin-bottom: 8px;
}

.good-price-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.good-price {
    font-size: 20px;
    color: #ef4444;
    font-weight: 700;
}

.good-count {
    font-size: 14px;
    color: #64748b;
    font-weight: 500;
}

.good-actions {
    display: flex;
    align-items: center;
    flex-shrink: 0;
}

.btn-good-map {
    padding: 6px 16px;
    border: 1px solid #10b981;
    border-radius: 16px;
    background: linear-gradient(135deg, #ffffff 0%, #fafbfc 100%);
    color: #10b981;
    font-size: 12px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    white-space: nowrap;
}

.btn-good-map:hover {
    background: linear-gradient(135deg, #f0fdf4 0%, #dcfce7 100%);
    box-shadow: 0 2px 8px rgba(16, 185, 129, 0.2);
    transform: translateY(-1px);
}

.order-address {
    padding: 16px 24px;
    background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
    border-top: 1px solid #bae6fd;
    border-bottom: 1px solid #bae6fd;
    display: flex;
    gap: 12px;
    align-items: flex-start;
}

.address-icon {
    font-size: 20px;
    flex-shrink: 0;
    margin-top: 2px;
}

.address-info {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.address-row {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
    line-height: 1.6;
}

.address-label {
    color: #0369a1;
    font-weight: 600;
    flex-shrink: 0;
}

.address-value {
    color: #1e293b;
    font-weight: 500;
}

.address-phone {
    color: #64748b;
    font-size: 13px;
}

.order-card-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 20px 24px;
    border-top: 1px solid #e2e8f0;
    background: linear-gradient(135deg, #fafbfc 0%, #f8fafc 100%);
}

.order-total {
    display: flex;
    align-items: center;
    gap: 20px;
}

.total-label {
    font-size: 14px;
    color: #64748b;
    font-weight: 500;
}

.total-amount {
    font-size: 15px;
    color: #1e293b;
    font-weight: 600;
}

.total-price {
    font-size: 22px;
    color: #ef4444;
    font-weight: 700;
}

.order-actions {
    display: flex;
    gap: 12px;
}

.btn-cancel,
.btn-delete {
    padding: 10px 24px;
    border: 1px solid #e2e8f0;
    border-radius: 24px;
    background: linear-gradient(135deg, #ffffff 0%, #fafbfc 100%);
    color: #64748b;
    font-size: 14px;
    cursor: pointer;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    font-weight: 500;
}

.btn-cancel:hover,
.btn-delete:hover {
    border-color: #ef4444;
    color: #ef4444;
    background: linear-gradient(135deg, #fef2f2 0%, #fee2e2 100%);
    box-shadow: 0 2px 8px rgba(239, 68, 68, 0.15);
}

.btn-pay,
.btn-receive {
    padding: 10px 24px;
    border: none;
    border-radius: 24px;
    background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
    color: white;
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    box-shadow: 0 2px 8px rgba(59, 130, 246, 0.25);
}

.btn-pay:hover,
.btn-receive:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(59, 130, 246, 0.35);
    background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
}

.btn-review {
    padding: 10px 24px;
    border: 1px solid #3b82f6;
    border-radius: 24px;
    background: linear-gradient(135deg, #ffffff 0%, #fafbfc 100%);
    color: #3b82f6;
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.btn-review:hover {
    background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
    box-shadow: 0 2px 8px rgba(59, 130, 246, 0.15);
    transform: translateY(-1px);
}

.btn-map {
    padding: 10px 24px;
    border: 1px solid #10b981;
    border-radius: 24px;
    background: linear-gradient(135deg, #ffffff 0%, #fafbfc 100%);
    color: #10b981;
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.btn-map:hover {
    background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%);
    box-shadow: 0 2px 8px rgba(16, 185, 129, 0.15);
    transform: translateY(-1px);
}
</style>
