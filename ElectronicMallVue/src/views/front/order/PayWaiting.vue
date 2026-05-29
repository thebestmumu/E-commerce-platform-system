<template>
<div class="pay-waiting-container">
    <div class="pay-waiting-card">
        <div class="loading-animation">
            <div class="spinner"></div>
        </div>
        <h2 class="waiting-title">等待支付中...</h2>
        <p class="waiting-message">请在新打开的支付宝页面完成支付</p>
        
        <div class="order-info-section">
            <h3 class="section-title">订单信息</h3>
            <div class="info-row">
                <span class="info-label">订单编号</span>
                <span class="info-value">{{ orderNo }}</span>
            </div>
            <div class="info-row">
                <span class="info-label">支付金额</span>
                <span class="info-value amount">¥{{ totalAmount }}</span>
            </div>
        </div>
        
        <div class="status-tips">
            <div class="tip-item" :class="{ active: status === 'waiting' }">
                <span class="tip-icon">⏳</span>
                <span class="tip-text">等待支付</span>
            </div>
            <div class="tip-item" :class="{ active: status === 'success' }">
                <span class="tip-icon">✅</span>
                <span class="tip-text">支付成功</span>
            </div>
            <div class="tip-item" :class="{ active: status === 'failed' }">
                <span class="tip-icon">❌</span>
                <span class="tip-text">支付失败</span>
            </div>
        </div>
        
        <div class="action-buttons">
            <button class="btn-check" @click="checkStatus">刷新状态</button>
            <button class="btn-back" @click="$router.push('/orderlist')">返回订单</button>
        </div>
    </div>
</div>
</template>

<script>
export default {
    name: "PayWaiting",
    data() {
        return {
            orderNo: '',
            totalAmount: '0.00',
            status: 'waiting',
            pollingTimer: null
        }
    },
    created() {
        this.orderNo = this.$route.query.orderNo || '';
        this.totalAmount = this.$route.query.totalAmount || '0.00';
        this.startPolling();
    },
    beforeDestroy() {
        if (this.pollingTimer) {
            clearInterval(this.pollingTimer);
        }
    },
    methods: {
        startPolling() {
            this.pollingTimer = setInterval(() => {
                this.checkStatus();
            }, 3000);
        },
        checkStatus() {
            if (!this.orderNo) {
                this.$message.error('订单编号不存在');
                return;
            }
            this.request.get(`/api/alipay/query/${this.orderNo}`).then(res => {
                if (res.code === '200') {
                    if (res.data === true) {
                        this.status = 'success';
                        if (this.pollingTimer) {
                            clearInterval(this.pollingTimer);
                        }
                        this.$message.success('支付成功！');
                        setTimeout(() => {
                            this.$router.push({
                                path: '/pay-success',
                                query: {
                                    orderNo: this.orderNo,
                                    totalAmount: this.totalAmount
                                }
                            });
                        }, 1500);
                    } else {
                        this.status = 'waiting';
                    }
                } else {
                    this.status = 'failed';
                    if (this.pollingTimer) {
                        clearInterval(this.pollingTimer);
                    }
                    this.$message.error('支付查询失败');
                }
            }).catch(() => {
                this.status = 'failed';
                if (this.pollingTimer) {
                    clearInterval(this.pollingTimer);
                }
            });
        }
    }
}
</script>

<style scoped>
.pay-waiting-container {
    min-height: 100vh;
    background: linear-gradient(135deg, #fef3c7 0%, #ffffff 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 20px;
}

.pay-waiting-card {
    background: white;
    border-radius: 20px;
    padding: 40px;
    max-width: 600px;
    width: 100%;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
    text-align: center;
}

.loading-animation {
    margin-bottom: 24px;
}

.spinner {
    width: 80px;
    height: 80px;
    border: 6px solid #f3f4f6;
    border-top: 6px solid #f59e0b;
    border-radius: 50%;
    animation: spin 1s linear infinite;
    margin: 0 auto;
}

@keyframes spin {
    0% { transform: rotate(0deg); }
    100% { transform: rotate(360deg); }
}

.waiting-title {
    font-size: 28px;
    font-weight: 700;
    color: #1e293b;
    margin: 0 0 12px;
}

.waiting-message {
    font-size: 16px;
    color: #64748b;
    margin: 0 0 32px;
    line-height: 1.6;
}

.order-info-section {
    background: #f8fafc;
    border-radius: 12px;
    padding: 24px;
    margin-bottom: 32px;
    text-align: left;
}

.section-title {
    font-size: 18px;
    font-weight: 600;
    color: #1e293b;
    margin: 0 0 16px;
    padding-bottom: 12px;
    border-bottom: 2px solid #e2e8f0;
}

.info-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 0;
}

.info-row:not(:last-child) {
    border-bottom: 1px dashed #e2e8f0;
}

.info-label {
    color: #64748b;
    font-size: 14px;
}

.info-value {
    color: #1e293b;
    font-size: 14px;
    font-weight: 500;
}

.info-value.amount {
    color: #ef4444;
    font-size: 20px;
    font-weight: 700;
}

.status-tips {
    display: flex;
    justify-content: space-around;
    margin-bottom: 32px;
    padding: 20px;
    background: #f8fafc;
    border-radius: 12px;
}

.tip-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    opacity: 0.4;
    transition: all 0.3s;
}

.tip-item.active {
    opacity: 1;
}

.tip-icon {
    font-size: 32px;
}

.tip-text {
    font-size: 14px;
    color: #64748b;
    font-weight: 500;
}

.action-buttons {
    display: flex;
    gap: 16px;
    justify-content: center;
}

.btn-check {
    padding: 14px 32px;
    background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
    color: white;
    border: none;
    border-radius: 24px;
    font-size: 16px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s;
    box-shadow: 0 4px 12px rgba(245, 158, 11, 0.25);
}

.btn-check:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 20px rgba(245, 158, 11, 0.35);
}

.btn-back {
    padding: 14px 32px;
    background: white;
    color: #64748b;
    border: 2px solid #e2e8f0;
    border-radius: 24px;
    font-size: 16px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s;
}

.btn-back:hover {
    background: #f8fafc;
    transform: translateY(-2px);
}

@media (max-width: 768px) {
    .pay-waiting-card {
        padding: 30px 20px;
    }
    
    .waiting-title {
        font-size: 24px;
    }
    
    .action-buttons {
        flex-direction: column;
    }
    
    .btn-check,
    .btn-back {
        width: 100%;
    }
}
</style>
