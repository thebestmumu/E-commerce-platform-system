<template>
<div class="bottom-nav">
    <div 
        v-for="item in navItems" 
        :key="item.path"
        :class="['nav-item', { active: isActive(item.path) }]"
        @click="navigate(item)"
    >
        <div class="nav-icon">
            <span class="icon-text" v-html="item.icon"></span>
            <span v-if="item.badge && item.badge > 0" class="nav-badge">{{ item.badge > 99 ? '99+' : item.badge }}</span>
        </div>
        <div class="nav-label">{{ item.label }}</div>
    </div>
</div>
</template>

<script>
export default {
    name: 'BottomNav',
    data() {
        return {
            navItems: [
                { path: '/', icon: '🏠', label: '首页' },
                { path: '/goodlist', icon: '📦', label: '分类' },
                { path: '/aichat', icon: '🤖', label: 'AI助手' },
                { path: '/cart', icon: '🛒', label: '购物车', badge: 0 },
                { path: '/person', icon: '👤', label: '我的' }
            ]
        }
    },
    computed: {
        cartCount() {
            const count = localStorage.getItem('cartCount');
            return count ? parseInt(count) : 0;
        }
    },
    watch: {
        cartCount: {
            handler(val) {
                this.navItems[3].badge = val;
            },
            immediate: true
        }
    },
    methods: {
        isActive(path) {
            if (path === '/') {
                return this.$route.path === '/';
            }
            return this.$route.path.startsWith(path);
        },
        navigate(item) {
            if (item.path === '/aichat') {
                window.open('/aichat', '_blank');
            } else {
                this.$router.push(item.path);
            }
        }
    }
}
</script>

<style scoped>
.bottom-nav {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    height: 60px;
    background: white;
    display: flex;
    justify-content: space-around;
    align-items: center;
    box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.08);
    z-index: 1000;
    padding-bottom: env(safe-area-inset-bottom);
}

.nav-item {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: all 0.3s;
    position: relative;
    padding: 8px 0;
}

.nav-item:hover {
    background: #f8f8f8;
}

.nav-item.active {
    color: #ff5000;
}

.nav-icon {
    position: relative;
    font-size: 22px;
    margin-bottom: 4px;
    line-height: 1;
}

.icon-text {
    display: block;
}

.nav-badge {
    position: absolute;
    top: -6px;
    right: -10px;
    min-width: 16px;
    height: 16px;
    padding: 0 4px;
    background: #ff0036;
    color: white;
    font-size: 10px;
    line-height: 16px;
    text-align: center;
    border-radius: 8px;
    font-weight: 600;
}

.nav-label {
    font-size: 11px;
    color: #666;
    line-height: 1;
}

.nav-item.active .nav-label {
    color: #ff5000;
    font-weight: 600;
}

@media (min-width: 769px) {
    .bottom-nav {
        display: none;
    }
}
</style>
