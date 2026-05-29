<template>
<div v-if="visible" class="taobao-loading-overlay">
    <div class="loading-container">
        <div class="loading-spinner">
            <div class="spinner-circle"></div>
            <div class="spinner-circle"></div>
            <div class="spinner-circle"></div>
        </div>
        <div class="loading-text">{{ text }}</div>
    </div>
</div>
</template>

<script>
export default {
    name: 'TaobaoLoading',
    props: {
        visible: {
            type: Boolean,
            default: false
        },
        text: {
            type: String,
            default: '加载中...'
        }
    }
}
</script>

<style scoped>
.taobao-loading-overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(255, 255, 255, 0.9);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 9999;
}

.loading-container {
    text-align: center;
}

.loading-spinner {
    position: relative;
    width: 60px;
    height: 60px;
    margin: 0 auto 16px;
}

.spinner-circle {
    position: absolute;
    width: 100%;
    height: 100%;
    border: 3px solid transparent;
    border-top-color: #ff5000;
    border-radius: 50%;
    animation: spin 1.2s cubic-bezier(0.5, 0, 0.5, 1) infinite;
}

.spinner-circle:nth-child(1) {
    animation-delay: -0.45s;
}

.spinner-circle:nth-child(2) {
    animation-delay: -0.3s;
}

.spinner-circle:nth-child(3) {
    animation-delay: -0.15s;
}

@keyframes spin {
    0% {
        transform: rotate(0deg);
    }
    100% {
        transform: rotate(360deg);
    }
}

.loading-text {
    font-size: 14px;
    color: #666;
    font-weight: 500;
}
</style>
