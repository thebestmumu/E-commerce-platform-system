<template>
<div class="pay-success-container">
    <div class="pay-success-card">
        <div class="success-icon">✓</div>
        <h2 class="success-title">支付成功</h2>
        <p class="success-message">您的订单已支付成功，我们将尽快为您发货</p>
        
        <div class="order-info-section">
            <h3 class="section-title">订单信息</h3>
            <div class="info-row">
                <span class="info-label">订单编号</span>
                <span class="info-value">{{ orderInfo.orderNo }}</span>
            </div>
            <div class="info-row">
                <span class="info-label">支付金额</span>
                <span class="info-value amount">¥{{ orderInfo.totalAmount }}</span>
            </div>
            <div class="info-row">
                <span class="info-label">支付时间</span>
                <span class="info-value">{{ orderInfo.payTime }}</span>
            </div>
        </div>
        
        <div class="action-buttons">
            <button class="btn-primary" @click="$router.push('/orderlist')">查看订单</button>
            <button class="btn-secondary" @click="$router.push('/')">继续购物</button>
        </div>
    </div>
</div>
</template>

<script>
export default {
    name: "PaySuccess",
    data() {
        return {
            orderInfo: {
                orderNo: '',
                totalAmount: '0.00',
                payTime: ''
            }
        }
    },
    created() {
        this.orderInfo.orderNo = this.$route.query.orderNo || '';
        this.orderInfo.totalAmount = this.$route.query.totalAmount || '0.00';
        this.orderInfo.payTime = new Date().toLocaleString('zh-CN');
    }
}
</script>

<style scoped>
.pay-success-container {
    min-height: 100vh;
    background: linear-gradient(135deg, #f0fdf4 0%, #ffffff 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 20px;
}

.pay-success-card {
    background: white;
    border-radius: 20px;
    padding: 40px;
    max-width: 600px;
    width: 100%;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
    text-align: center;
}

.success-icon {
    width: 80px;
    height: 80px;
    background: linear-gradient(135deg, #10b981 0%, #059669 100%);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 auto 24px;
    font-size: 40px;
    color: white;
    font-weight: bold;
    box-shadow: 0 4px 16px rgba(16, 185, 129, 0.3);
}

.success-title {
    font-size: 28px;
    font-weight: 700;
    color: #1e293b;
    margin: 0 0 12px;
}

.success-message {
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

.action-buttons {
    display: flex;
    gap: 16px;
    justify-content: center;
}

.btn-primary {
    padding: 14px 32px;
    background: linear-gradient(135deg, #ff9000 0%, #ff5000 100%);
    color: white;
    border: none;
    border-radius: 24px;
    font-size: 16px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s;
    box-shadow: 0 4px 12px rgba(255, 80, 0, 0.25);
}

.btn-primary:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 20px rgba(255, 80, 0, 0.35);
}

.btn-secondary {
    padding: 14px 32px;
    background: white;
    color: #ff5000;
    border: 2px solid #ff5000;
    border-radius: 24px;
    font-size: 16px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s;
}

.btn-secondary:hover {
    background: #fff5f0;
    transform: translateY(-2px);
}

@media (max-width: 768px) {
    .pay-success-card {
        padding: 30px 20px;
    }
    
    .success-title {
        font-size: 24px;
    }
    
    .action-buttons {
        flex-direction: column;
    }
    
    .btn-primary,
    .btn-secondary {
        width: 100%;
    }
}
</style>
