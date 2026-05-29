<template>
<div class="pay-container">
    <div class="pay-header">
        <div class="header-back" @click="$router.go(-1)">←</div>
        <h2 class="header-title">确认支付</h2>
        <div class="header-placeholder"></div>
    </div>

    <div class="pay-content">
        <div class="order-info-card">
            <div class="card-header">
                <span class="card-icon">📋</span>
                <span class="card-title">订单信息</span>
            </div>
            <div class="order-details">
                <div class="detail-row">
                    <span class="detail-label">订单编号</span>
                    <span class="detail-value order-no">{{ orderNo }}</span>
                </div>
                <div class="detail-row amount-row">
                    <span class="detail-label">支付金额</span>
                    <div class="amount-display">
                        <span class="amount-symbol">¥</span>
                        <span class="amount-number">{{ money }}</span>
                    </div>
                </div>
            </div>
        </div>

        <div class="payment-method-card">
            <div class="card-header">
                <span class="card-icon">💳</span>
                <span class="card-title">选择支付方式</span>
            </div>
            <div class="payment-options">
                <div 
                    :class="['payment-option', { active: selectedMethod === 'wechat' }]"
                    @click="selectedMethod = 'wechat'"
                >
                    <div class="payment-icon wechat-icon">
                        <span class="icon-text">💚</span>
                    </div>
                    <div class="payment-info">
                        <div class="payment-name">微信支付</div>
                        <div class="payment-desc">推荐使用，即时到账</div>
                    </div>
                    <div class="payment-check">
                        <span v-if="selectedMethod === 'wechat'" class="check-icon">✓</span>
                    </div>
                </div>

                <div 
                    :class="['payment-option', { active: selectedMethod === 'alipay' }]"
                    @click="selectedMethod = 'alipay'"
                >
                    <div class="payment-icon alipay-icon">
                        <span class="icon-text">💙</span>
                    </div>
                    <div class="payment-info">
                        <div class="payment-name">支付宝</div>
                        <div class="payment-desc">安全快捷支付</div>
                    </div>
                    <div class="payment-check">
                        <span v-if="selectedMethod === 'alipay'" class="check-icon">✓</span>
                    </div>
                </div>
            </div>
        </div>

        <div class="security-tips">
            <div class="tips-header">
                <span class="tips-icon">🔒</span>
                <span class="tips-title">支付安全提示</span>
            </div>
            <ul class="tips-list">
                <li>✓ 本平台采用加密传输技术保障您的支付安全</li>
                <li>✓ 请勿向他人透露您的支付密码</li>
                <li>✓ 如遇问题请及时联系客服</li>
            </ul>
        </div>

        <div class="pay-action-section">
            <button 
                class="confirm-pay-btn" 
                @click="pay"
                :disabled="!selectedMethod"
            >
                确认支付 ¥{{ money }}
            </button>
            <div class="pay-hint">选择支付方式后点击确认支付</div>
        </div>
    </div>

    <div v-if="showPaymentDialog" class="payment-dialog-overlay" @click.self="closeDialog">
        <div class="payment-dialog">
            <div v-if="paymentStatus === 'waiting'" class="dialog-content">
                <div class="loading-spinner">
                    <div class="spinner-ring"></div>
                    <div class="spinner-ring"></div>
                    <div class="spinner-ring"></div>
                </div>
                <h3 class="dialog-title">等待支付</h3>
                <p class="dialog-message">请在新打开的支付宝页面完成支付</p>
                <div class="order-brief">
                    <span>订单号：{{ orderNo }}</span>
                    <span class="amount">¥{{ money }}</span>
                </div>
            </div>

            <div v-else-if="paymentStatus === 'success'" class="dialog-content success">
                <div class="success-animation">
                    <svg class="success-icon" viewBox="0 0 52 52">
                        <circle class="success-circle" cx="26" cy="26" r="25" fill="none"/>
                        <path class="success-check" fill="none" d="M14.1 27.2l7.1 7.2 16.7-16.8"/>
                    </svg>
                </div>
                <h3 class="dialog-title success-title">支付成功</h3>
                <p class="dialog-message">订单已支付成功，我们将尽快为您发货</p>
            </div>

            <div v-else-if="paymentStatus === 'failed'" class="dialog-content failed">
                <div class="failed-icon">✕</div>
                <h3 class="dialog-title failed-title">支付失败</h3>
                <p class="dialog-message">支付未成功，请重试或联系客服</p>
            </div>

            <div class="dialog-actions">
                <button v-if="paymentStatus === 'waiting'" class="btn-cancel" @click="closeDialog">取消支付</button>
                <button v-if="paymentStatus === 'success'" class="btn-confirm" @click="goToOrderList">查看订单</button>
                <button v-if="paymentStatus === 'failed'" class="btn-retry" @click="closeDialog">重新支付</button>
            </div>
        </div>
    </div>
</div>
</template>

<script>
export default {
    name: "Pay",
    data() {
        return {
            userId: 0,
            money1: 0,
            orderId: '',
            orderNo: '',
            money: '0.00',
            selectedMethod: '',
            showPaymentDialog: false,
            paymentStatus: 'waiting',
            pollingTimer: null
        }
    },
    created() {
        this.orderId = this.$route.query.orderNo || this.$route.params.orderId;
        console.log('Pay 页面初始化, orderId:', this.orderId);
        this.loadOrderInfo();
        this.selectedMethod = 'wechat';
    },
    beforeDestroy() {
        if (this.pollingTimer) {
            clearInterval(this.pollingTimer);
        }
    },
    methods: {
        loadOrderInfo() {
            if (!this.orderId) {
                this.$message.error('订单ID不存在');
                return;
            }
            this.request.get(`/api/order/orderNo/${this.orderId}`).then(res => {
                if (res.code === '200') {
                    const orderData = res.data;
                    // 现在返回的是数组，取第一个元素
                    const firstItem = Array.isArray(orderData) ? orderData[0] : orderData;
                    this.orderNo = firstItem.orderNo || this.orderId;
                    this.money = parseFloat(firstItem.totalPrice || 0).toFixed(2);
                } else {
                    this.$message.error('获取订单信息失败');
                }
            });
        },
        pay() {
            if (!this.selectedMethod) {
                this.$message.warning('请选择支付方式');
                return;
            }
            
            if (this.selectedMethod === 'alipay') {
                this.payWithAlipay();
            } else {
                this.payWithWechat();
            }
        },
        payWithAlipay() {
            this.request.post("/api/alipay/pay", {
                orderNo: this.orderNo,
                totalAmount: this.money,
                subject: "商城订单支付"
            }).then(res => {
                if (res.code === '200') {
                    const payForm = res.data;
                    const div = document.createElement('div');
                    div.innerHTML = payForm;
                    document.body.appendChild(div);
                    const form = div.querySelector('form');
                    form.target = '_blank';
                    form.submit();
                    document.body.removeChild(div);
                    
                    this.showPaymentDialog = true;
                    this.paymentStatus = 'waiting';
                    this.startPolling();
                } else {
                    this.$message.error(res.msg);
                }
            });
        },
        payWithWechat() {
            this.request.get("/api/order/paid/" + this.orderNo).then(res => {
                if (res.code === '200') {
                    this.showPaymentDialog = true;
                    this.paymentStatus = 'success';
                    setTimeout(() => {
                        this.$router.push('/orderlist');
                    }, 2000);
                } else {
                    this.$message.error(res.msg);
                }
            });
        },
        startPolling() {
            if (this.pollingTimer) {
                clearInterval(this.pollingTimer);
            }
            this.pollingTimer = setInterval(() => {
                this.checkPaymentStatus();
            }, 3000);
        },
        checkPaymentStatus() {
            if (!this.orderNo || this.paymentStatus !== 'waiting') {
                return;
            }
            this.request.get(`/api/alipay/query/${this.orderNo}`).then(res => {
                if (res.code === '200' && res.data === true) {
                    this.paymentStatus = 'success';
                    if (this.pollingTimer) {
                        clearInterval(this.pollingTimer);
                        this.pollingTimer = null;
                    }
                    this.request.get(`/api/order/paid/${this.orderNo}`).then(updateRes => {
                        if (updateRes.code === '200') {
                            console.log('订单状态已更新');
                        }
                    });
                }
            }).catch(() => {
            });
        },
        closeDialog() {
            if (this.pollingTimer) {
                clearInterval(this.pollingTimer);
                this.pollingTimer = null;
            }
            this.showPaymentDialog = false;
            this.paymentStatus = 'waiting';
        },
        goToOrderList() {
            this.$router.push('/orderlist');
        }
    }
}
</script>

<style scoped>
.pay-container {
    min-height: 100vh;
    background: #f4f4f4;
    padding-bottom: 100px;
}

.pay-header {
    background: linear-gradient(90deg, #ff9000 0%, #ff5000 100%);
    color: white;
    padding: 16px 20px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    position: sticky;
    top: 0;
    z-index: 100;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.header-back {
    font-size: 24px;
    cursor: pointer;
    width: 40px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    transition: background 0.3s;
}

.header-back:hover {
    background: rgba(255, 255, 255, 0.2);
}

.header-title {
    font-size: 18px;
    font-weight: 600;
    margin: 0;
}

.header-placeholder {
    width: 40px;
}

.pay-content {
    max-width: 800px;
    margin: 20px auto;
    padding: 0 16px;
}

.order-info-card,
.payment-method-card,
.security-tips {
    background: white;
    border-radius: 12px;
    margin-bottom: 16px;
    overflow: hidden;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.card-header {
    padding: 16px 20px;
    border-bottom: 1px solid #f0f0f0;
    display: flex;
    align-items: center;
    gap: 8px;
}

.card-icon {
    font-size: 20px;
}

.card-title {
    font-size: 16px;
    font-weight: 600;
    color: #333;
}

.order-details {
    padding: 20px;
}

.detail-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 0;
}

.detail-row:not(:last-child) {
    border-bottom: 1px dashed #e5e5e5;
}

.detail-label {
    color: #666;
    font-size: 14px;
}

.detail-value {
    color: #333;
    font-size: 14px;
    font-weight: 500;
}

.order-no {
    font-family: monospace;
    font-size: 13px;
    color: #999;
}

.amount-row {
    padding: 16px 0;
}

.amount-display {
    display: flex;
    align-items: baseline;
}

.amount-symbol {
    font-size: 20px;
    color: #ff5000;
    font-weight: 600;
}

.amount-number {
    font-size: 32px;
    color: #ff5000;
    font-weight: 700;
    margin-left: 4px;
}

.payment-options {
    padding: 16px 20px;
}

.payment-option {
    display: flex;
    align-items: center;
    padding: 16px;
    border: 2px solid #e5e5e5;
    border-radius: 12px;
    margin-bottom: 12px;
    cursor: pointer;
    transition: all 0.3s ease;
}

.payment-option:last-child {
    margin-bottom: 0;
}

.payment-option:hover {
    border-color: #ff9000;
    background: #fff8f0;
}

.payment-option.active {
    border-color: #ff5000;
    background: #fff5f0;
}

.payment-icon {
    width: 48px;
    height: 48px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 16px;
    flex-shrink: 0;
}

.wechat-icon {
    background: linear-gradient(135deg, #07c160 0%, #06ad56 100%);
}

.alipay-icon {
    background: linear-gradient(135deg, #1677ff 0%, #0958d9 100%);
}

.icon-text {
    font-size: 24px;
}

.payment-info {
    flex: 1;
}

.payment-name {
    font-size: 16px;
    font-weight: 600;
    color: #333;
    margin-bottom: 4px;
}

.payment-desc {
    font-size: 12px;
    color: #999;
}

.payment-check {
    width: 24px;
    height: 24px;
    border-radius: 50%;
    border: 2px solid #e5e5e5;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.3s;
}

.payment-option.active .payment-check {
    background: #ff5000;
    border-color: #ff5000;
}

.check-icon {
    color: white;
    font-size: 14px;
    font-weight: bold;
}

.security-tips {
    padding: 20px;
}

.tips-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;
}

.tips-icon {
    font-size: 18px;
}

.tips-title {
    font-size: 14px;
    font-weight: 600;
    color: #333;
}

.tips-list {
    list-style: none;
    padding: 0;
    margin: 0;
}

.tips-list li {
    font-size: 13px;
    color: #666;
    line-height: 1.8;
    padding-left: 4px;
}

.pay-action-section {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    background: white;
    padding: 16px 20px;
    box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.08);
    z-index: 100;
}

.confirm-pay-btn {
    width: 100%;
    background: linear-gradient(90deg, #ff9000 0%, #ff5000 100%);
    color: white;
    border: none;
    border-radius: 24px;
    padding: 14px;
    font-size: 16px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s;
    margin-bottom: 8px;
}

.confirm-pay-btn:hover:not(:disabled) {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(255, 80, 0, 0.3);
}

.confirm-pay-btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
}

.pay-hint {
    text-align: center;
    font-size: 12px;
    color: #999;
}

@media (max-width: 768px) {
    .pay-content {
        padding: 0 12px;
    }
    
    .amount-number {
        font-size: 28px;
    }
    
    .payment-option {
        padding: 12px;
    }
    
    .payment-icon {
        width: 40px;
        height: 40px;
        margin-right: 12px;
    }
    
    .icon-text {
        font-size: 20px;
    }
}

.payment-dialog-overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.6);
    backdrop-filter: blur(4px);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 1000;
    animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
    from { opacity: 0; }
    to { opacity: 1; }
}

.payment-dialog {
    background: white;
    border-radius: 24px;
    padding: 48px 40px;
    max-width: 480px;
    width: 90%;
    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
    animation: slideUp 0.4s ease;
    text-align: center;
}

@keyframes slideUp {
    from {
        opacity: 0;
        transform: translateY(30px);
    }
    to {
        opacity: 1;
        transform: translateY(0);
    }
}

.loading-spinner {
    position: relative;
    width: 100px;
    height: 100px;
    margin: 0 auto 32px;
}

.spinner-ring {
    position: absolute;
    border-radius: 50%;
    border: 3px solid transparent;
    animation: spin-ring 1.5s linear infinite;
}

.spinner-ring:nth-child(1) {
    width: 100px;
    height: 100px;
    border-top-color: #ff9000;
    animation-duration: 1.5s;
}

.spinner-ring:nth-child(2) {
    width: 76px;
    height: 76px;
    top: 12px;
    left: 12px;
    border-right-color: #ff5000;
    animation-duration: 1.2s;
    animation-direction: reverse;
}

.spinner-ring:nth-child(3) {
    width: 52px;
    height: 52px;
    top: 24px;
    left: 24px;
    border-bottom-color: #ff9000;
    animation-duration: 0.9s;
}

@keyframes spin-ring {
    0% { transform: rotate(0deg); }
    100% { transform: rotate(360deg); }
}

.dialog-title {
    font-size: 24px;
    font-weight: 700;
    color: #1e293b;
    margin: 0 0 12px;
}

.dialog-message {
    font-size: 15px;
    color: #64748b;
    margin: 0 0 24px;
    line-height: 1.6;
}

.order-brief {
    background: #f8fafc;
    border-radius: 12px;
    padding: 16px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;
}

.order-brief span {
    font-size: 14px;
    color: #64748b;
}

.order-brief .amount {
    font-size: 20px;
    font-weight: 700;
    color: #ff5000;
}

.success-animation {
    margin: 0 auto 32px;
    width: 100px;
    height: 100px;
}

.success-icon {
    width: 100%;
    height: 100%;
}

.success-circle {
    stroke: #10b981;
    stroke-width: 2;
    stroke-dasharray: 166;
    stroke-dashoffset: 166;
    animation: stroke-circle 0.6s cubic-bezier(0.65, 0, 0.45, 1) forwards;
}

@keyframes stroke-circle {
    100% {
        stroke-dashoffset: 0;
    }
}

.success-check {
    stroke: #10b981;
    stroke-width: 3;
    stroke-linecap: round;
    stroke-dasharray: 48;
    stroke-dashoffset: 48;
    animation: stroke-check 0.3s cubic-bezier(0.65, 0, 0.45, 1) 0.4s forwards;
}

@keyframes stroke-check {
    100% {
        stroke-dashoffset: 0;
    }
}

.success-title {
    color: #10b981;
}

.failed-icon {
    width: 80px;
    height: 80px;
    background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 auto 32px;
    font-size: 48px;
    color: white;
    font-weight: bold;
    animation: scaleIn 0.3s ease;
}

@keyframes scaleIn {
    from {
        transform: scale(0);
    }
    to {
        transform: scale(1);
    }
}

.failed-title {
    color: #ef4444;
}

.dialog-actions {
    margin-top: 32px;
}

.btn-cancel,
.btn-confirm,
.btn-retry {
    padding: 14px 40px;
    border: none;
    border-radius: 24px;
    font-size: 16px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s;
    width: 100%;
}

.btn-cancel {
    background: #f1f5f9;
    color: #64748b;
}

.btn-cancel:hover {
    background: #e2e8f0;
    transform: translateY(-2px);
}

.btn-confirm {
    background: linear-gradient(135deg, #10b981 0%, #059669 100%);
    color: white;
    box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
}

.btn-confirm:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 20px rgba(16, 185, 129, 0.4);
}

.btn-retry {
    background: linear-gradient(135deg, #ff9000 0%, #ff5000 100%);
    color: white;
    box-shadow: 0 4px 12px rgba(255, 80, 0, 0.3);
}

.btn-retry:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 20px rgba(255, 80, 0, 0.4);
}
</style>
