<template>
<div class="taobao-cart">
  <search @search="handleSearch"></search>
  
  <div class="main-content">
    <div class="cart-header">
      <h2 class="cart-title">🛒 我的购物车</h2>
      <span class="cart-count">共 {{ carts.length }} 件商品</span>
    </div>

    <div v-if="carts.length === 0" class="empty-cart">
      <div class="empty-icon">🛒</div>
      <div class="empty-text">购物车还是空的哦</div>
      <router-link to="/" class="go-shopping-btn">去逛逛</router-link>
    </div>

    <template v-else>
      <div class="cart-list">
        <cart-item 
          v-for="cart in carts" 
          :cart="cart" 
          @delete="delItem" 
          :key="cart.id"
        ></cart-item>
      </div>

      <!-- 底部结算栏 -->
      <div class="cart-footer">
        <div class="footer-left">
          <el-checkbox v-model="selectAll" @change="toggleSelectAll">全选</el-checkbox>
          <button class="delete-btn" @click="deleteSelected">删除选中</button>
        </div>
        <div class="footer-right">
          <div class="total-info">
            <span class="total-label">合计：</span>
            <span class="total-price">¥{{ totalPrice.toFixed(2) }}</span>
          </div>
          <button class="checkout-btn" @click="checkout">
            结算({{ selectedCount }})
          </button>
        </div>
      </div>
    </template>
  </div>
</div>
</template>

<script>
import CartItem from "@/components/CartItem";
import search from "@/components/Search";

export default {
  name: "Cart",
  data() {
    return {
      userId: null,
      carts: [],
      selectAll: false,
      selectedItems: [],
    }
  },
  components: {
    'cart-item': CartItem,
    search,
  },
  computed: {
    totalPrice() {
      return this.carts
        .filter(cart => cart.checked)
        .reduce((sum, cart) => sum + (cart.price || 0) * (cart.discount || 1) * cart.count, 0);
    },
    selectedCount() {
      return this.carts.filter(cart => cart.checked).length;
    }
  },
  created() {
    this.loadCart();
    this.$root.$on('refresh-cart', () => {
      this.loadCart();
    });
  },
  beforeDestroy() {
    this.$root.$off('refresh-cart');
  },
  methods: {
    handleSearch(text) {
      this.$router.push({ path: "/goodList", query: { searchText: text } });
    },
    loadCart() {
      this.request.get("/userid").then(res => {
        this.userId = res;
        this.request.get("/api/cart/userid/" + this.userId).then(res => {
          if (res.code === '200') {
            this.carts = res.data.map(cart => ({
              ...cart,
              checked: false,
              createTime: cart.createTime?.toLocaleString().replace(/T/g, ' ').replace(/\.[\d]{3}Z/, '')
            }));
          }
        });
      });
    },
    delItem(id) {
      this.carts = this.carts.filter(item => item.id != id);
    },
    toggleSelectAll() {
      this.carts.forEach(cart => cart.checked = this.selectAll);
    },
    deleteSelected() {
      const selectedIds = this.carts.filter(cart => cart.checked).map(cart => cart.id);
      if (selectedIds.length === 0) {
        this.$message.warning('请先选择要删除的商品');
        return;
      }
      this.$confirm('确定删除选中的商品吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        selectedIds.forEach(id => {
          this.request.delete('/api/cart/' + id);
        });
        this.carts = this.carts.filter(cart => !cart.checked);
        this.$message.success('删除成功');
      });
    },
    checkout() {
      if (this.selectedCount === 0) {
        this.$message.warning('请先选择要结算的商品');
        return;
      }
      const selectedCarts = this.carts.filter(cart => cart.checked);
      this.$router.push({
        path: '/preOrder',
        query: { carts: JSON.stringify(selectedCarts) }
      });
    }
  },
}
</script>

<style scoped>
.taobao-cart {
  min-height: 100vh;
  background: linear-gradient(135deg, #fff5f0 0%, #ffffff 100%);
}

.main-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 16px 100px;
}

.cart-header {
  background: linear-gradient(135deg, #ffffff 0%, #fff5f0 100%);
  padding: 16px 24px;
  border-radius: 12px;
  margin-bottom: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 8px rgba(255, 80, 0, 0.08);
  border: 1px solid #ffe8d6;
}

.cart-title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.cart-count {
  font-size: 14px;
  color: #ff9000;
}

.cart-list {
  background: #ffffff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(255, 80, 0, 0.06);
  border: 1px solid #ffe8d6;
}

.empty-cart {
  background: linear-gradient(135deg, #ffffff 0%, #fff5f0 100%);
  border-radius: 12px;
  padding: 80px 20px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(255, 80, 0, 0.06);
  border: 1px solid #ffe8d6;
}

.empty-icon {
  font-size: 80px;
  margin-bottom: 16px;
}

.empty-text {
  font-size: 16px;
  color: #999;
  margin-bottom: 24px;
}

.go-shopping-btn {
  display: inline-block;
  padding: 12px 32px;
  background: linear-gradient(90deg, #ff9000 0%, #ff5000 100%);
  color: white;
  text-decoration: none;
  border-radius: 24px;
  font-size: 16px;
  font-weight: 600;
  transition: all 0.3s ease;
}

.go-shopping-btn:hover {
  opacity: 0.9;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 80, 0, 0.3);
}

.cart-footer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: white;
  padding: 16px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.08);
  z-index: 100;
}

.footer-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.delete-btn {
  padding: 8px 16px;
  background: white;
  border: 1px solid #ddd;
  border-radius: 4px;
  color: #666;
  cursor: pointer;
  transition: all 0.3s ease;
}

.delete-btn:hover {
  border-color: #ff5000;
  color: #ff5000;
}

.footer-right {
  display: flex;
  align-items: center;
  gap: 24px;
}

.total-info {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.total-label {
  font-size: 14px;
  color: #666;
}

.total-price {
  font-size: 24px;
  font-weight: 700;
  color: #ff5000;
}

.checkout-btn {
  padding: 12px 32px;
  background: linear-gradient(90deg, #ff9000 0%, #ff5000 100%);
  color: white;
  border: none;
  border-radius: 24px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.checkout-btn:hover {
  opacity: 0.9;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 80, 0, 0.3);
}

.checkout-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
  transform: none;
}

::v-deep .el-checkbox__label {
  font-size: 14px;
  color: #666;
}

::v-deep .el-checkbox__input.is-checked .el-checkbox__inner {
  background-color: #ff5000;
  border-color: #ff5000;
}
</style>
