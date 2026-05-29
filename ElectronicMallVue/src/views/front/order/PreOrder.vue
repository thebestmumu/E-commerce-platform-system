<template>
  <div class="preorder-container">
    <div class="preorder-header">
      <h2 class="preorder-title">确认订单</h2>
    </div>

    <div class="preorder-content">
      <div class="address-section">
        <div class="section-header">
          <span class="section-icon">📍</span>
          <span class="section-title">收货地址</span>
          <button class="add-address-btn" @click="addAddress">+ 新增地址</button>
        </div>
        
        <div v-if="addressData.length === 0" class="empty-address">
          <div class="empty-icon">📦</div>
          <div class="empty-text">暂无收货地址，请添加</div>
        </div>

        <div v-else class="address-list">
          <div 
            v-for="(item, index) in addressData" 
            :key="index"
            :class="['address-card', { active: index === checkedIndex }]"
            @click="select(index)"
          >
            <div class="address-info">
              <div class="address-contact">
                <span class="contact-name">{{ item.linkUser }}</span>
                <span class="contact-phone">{{ item.linkPhone }}</span>
              </div>
              <div class="address-detail">{{ item.linkAddress }}</div>
            </div>
            <div class="address-actions">
              <button class="action-btn edit-btn" @click.stop="editAddress(item)">编辑</button>
              <button class="action-btn delete-btn" @click.stop="deleteAddress(item)">删除</button>
            </div>
            <div v-if="index === checkedIndex" class="address-check">✓</div>
          </div>
        </div>
      </div>

      <div class="goods-section">
        <div class="section-header">
          <span class="section-icon">🛍️</span>
          <span class="section-title">商品信息</span>
        </div>

        <div class="goods-list">
          <div v-for="(good, index) in goods" :key="index" class="good-item">
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
              <div class="good-detail-row">
                <span class="good-price">¥{{ good.realPrice }}</span>
                <span class="good-num">x{{ good.num }}</span>
              </div>
            </div>
            <div class="good-subtotal">
              <span class="subtotal-price">¥{{ (good.realPrice * good.num).toFixed(2) }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="order-summary">
        <div class="summary-row">
          <span class="summary-label">商品总价</span>
          <span class="summary-value">¥{{ sumPrice }}</span>
        </div>
        <div class="summary-row discount-row">
          <span class="summary-label">优惠金额</span>
          <span class="summary-value discount-value">-¥{{ sumDiscount }}</span>
        </div>
        <div class="summary-row total-row">
          <span class="summary-label">应付总额</span>
          <span class="summary-value total-value">¥{{ sumPrice }}</span>
        </div>
      </div>

      <div class="submit-section">
        <button class="submit-btn" @click="submitOrder">
          提交订单
        </button>
      </div>
    </div>

    <el-dialog title="地址信息" :visible.sync="dialogFormVisible" width="500px" class="address-dialog">
      <el-form label-width="80px">
        <el-form-item label="联系人">
          <el-input v-model="address.linkUser" placeholder="请输入联系人姓名"></el-input>
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="address.linkPhone" placeholder="请输入联系电话"></el-input>
        </el-form-item>
        <el-form-item label="详细地址">
          <el-input v-model="address.linkAddress" type="textarea" :rows="3" placeholder="请输入详细地址"></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <button class="dialog-btn cancel-btn" @click="dialogFormVisible = false">取消</button>
        <button class="dialog-btn confirm-btn" @click="saveAddress">确定</button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import API from "@/utils/request";
import addressBox from "@/components/AddressBox";

export default {
  name: "PreOrder",
  data() {
    return {
      baseApi: this.$store.state.baseApi,
      userId: 0,
      addressData: [],
      address: {},
      checkedIndex: 0,
      dialogFormVisible: false,
      good: {},
      realPrice: -1,
      goods: [],
      cartId: "",
    };
  },
  components: {
    "address-box": addressBox,
  },
  created() {
    this.loadAddress();

    // 处理从购物车传来的多个商品
    if (this.$route.query.carts) {
      const carts = JSON.parse(this.$route.query.carts);
      this.goods = carts.map(cart => ({
        id: cart.goodId,
        name: cart.goodName,
        imgs: cart.img,
        discount: cart.discount,
        realPrice: cart.price * cart.discount,
        num: cart.count,
        standard: cart.standard,
        cartId: cart.id
      }));
      this.cartId = carts.map(c => c.id).join(',');
    } else {
      // 处理从商品详情页传来的单个商品
      this.good = JSON.parse(this.$route.query.good);
      this.good.realPrice = this.$route.query.realPrice;
      this.good.num = this.$route.query.num;
      this.good.standard = this.$route.query.standard;
      this.cartId = this.$route.query.cartId;
      this.goods.push(this.good);
    }
  },
  computed: {
    sumPrice: function () {
      let sum = 0;
      this.goods.forEach(function (good) {
        sum += good.realPrice * good.num;
      });
      return sum.toFixed(2);
    },
    sumDiscount: function () {
      let sum = 0;
      this.goods.forEach(function (good) {
        sum += (good.realPrice / good.discount - good.realPrice) * good.num;
      });
      return sum.toFixed(2);
    },
  },
  methods: {
    select(index) {
      this.checkedIndex = index;
    },
    addAddress() {
      this.address = {};
      this.dialogFormVisible = true;
    },
    editAddress(item) {
      this.address = JSON.parse(JSON.stringify(item));
      this.dialogFormVisible = true;
    },
    deleteAddress(item) {
      this.$confirm("您确认删除该地址吗?", "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      }).then(() => {
        API.delete("api/address/" + item.id).then((res) => {
          if (res.code === "200") {
            this.$message.success("删除地址成功");
            this.loadAddress();
          }
        });
      });
    },
    saveAddress() {
      this.address.userId = this.userId;
      API.post("/api/address", this.address).then((res) => {
        if (res.code === "200") {
          this.$message.success("保存成功");
          this.loadAddress();
          this.dialogFormVisible = false;
        } else {
          this.$message.error(res.msg);
        }
      });
    },
    loadAddress() {
      API.get("/userid").then((res) => {
        this.userId = res;
        API.get("/api/address/" + res).then((res) => {
          if (res.code === "200") {
            this.addressData = res.data;
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
    submitOrder() {
      let address = this.addressData[this.checkedIndex];
      if (!address) {
        this.$message({
          type: "warning",
          message: "请选择收货地址！",
        });
        return;
      }
      API.post("/api/order", {
        totalPrice: this.sumPrice,
        linkUser: address.linkUser,
        linkPhone: address.linkPhone,
        linkAddress: address.linkAddress,
        state: "待付款",
        goods: JSON.stringify(this.goods),
        cartId: this.cartId,
      }).then((res) => {
        if (res.code === "200") {
          let orderNo = res.data;
          this.$router.replace({
            path: "pay",
            query: { money: this.sumPrice, orderNo: orderNo },
          });
        } else {
          this.$message({
            type: "error",
            message: res.msg,
          });
        }
      });
    },
  },
};
</script>

<style scoped>
.preorder-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  background: #f4f4f4;
  min-height: 100vh;
}

.preorder-header {
  background: white;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.preorder-title {
  font-size: 24px;
  font-weight: 700;
  color: #333;
  margin: 0;
}

.preorder-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.address-section,
.goods-section {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.section-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 2px solid #f0f0f0;
}

.section-icon {
  font-size: 24px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  flex: 1;
}

.add-address-btn {
  background: linear-gradient(90deg, #ff9000 0%, #ff5000 100%);
  color: white;
  border: none;
  border-radius: 20px;
  padding: 8px 20px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.add-address-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 80, 0, 0.3);
}

.empty-address {
  text-align: center;
  padding: 40px 20px;
}

.empty-address .empty-icon {
  font-size: 60px;
  margin-bottom: 16px;
}

.empty-address .empty-text {
  font-size: 16px;
  color: #999;
}

.address-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.address-card {
  padding: 20px;
  border: 2px solid #e5e5e5;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
}

.address-card:hover {
  border-color: #ff5000;
}

.address-card.active {
  border-color: #ff5000;
  background: #fff9f5;
}

.address-info {
  margin-bottom: 12px;
}

.address-contact {
  display: flex;
  gap: 16px;
  margin-bottom: 8px;
}

.contact-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.contact-phone {
  font-size: 14px;
  color: #666;
}

.address-detail {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
}

.address-actions {
  display: flex;
  gap: 12px;
}

.action-btn {
  padding: 6px 16px;
  border-radius: 16px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s;
  border: none;
}

.edit-btn {
  background: #f5f5f5;
  color: #666;
}

.edit-btn:hover {
  background: #e5e5e5;
}

.delete-btn {
  background: #fff0f0;
  color: #ff5000;
}

.delete-btn:hover {
  background: #ffe0e0;
}

.address-check {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 24px;
  height: 24px;
  background: linear-gradient(90deg, #ff9000 0%, #ff5000 100%);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
}

.goods-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.good-item {
  display: flex;
  gap: 16px;
  padding: 16px;
  background: #f9f9f9;
  border-radius: 12px;
}

.good-image-wrapper {
  width: 120px;
  height: 120px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
  background: #f8f8f8;
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
  background: linear-gradient(135deg, #ff9000 0%, #ff5000 100%);
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
  color: #333;
  font-weight: 500;
  margin-bottom: 8px;
}

.good-spec {
  font-size: 13px;
  color: #999;
  margin-bottom: 8px;
}

.good-detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.good-price {
  font-size: 18px;
  color: #ff5000;
  font-weight: 600;
}

.good-num {
  font-size: 14px;
  color: #999;
}

.good-subtotal {
  display: flex;
  align-items: center;
}

.subtotal-price {
  font-size: 20px;
  color: #ff5000;
  font-weight: 700;
}

.order-summary {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.summary-row:last-child {
  border-bottom: none;
}

.summary-label {
  font-size: 14px;
  color: #666;
}

.summary-value {
  font-size: 16px;
  color: #333;
  font-weight: 600;
}

.discount-row .summary-value {
  color: #4caf50;
}

.total-row {
  padding-top: 16px;
  border-top: 2px solid #f0f0f0;
}

.total-value {
  font-size: 24px;
  color: #ff5000;
  font-weight: 700;
}

.submit-section {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.submit-btn {
  width: 100%;
  height: 56px;
  background: linear-gradient(90deg, #ff9000 0%, #ff5000 100%);
  color: white;
  border: none;
  border-radius: 28px;
  font-size: 20px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 4px 16px rgba(255, 80, 0, 0.3);
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(255, 80, 0, 0.4);
}

::v-deep .address-dialog {
  border-radius: 12px;
}

::v-deep .el-dialog__header {
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
}

::v-deep .el-dialog__title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

::v-deep .el-dialog__body {
  padding: 24px;
}

::v-deep .el-form-item__label {
  font-weight: 500;
  color: #333;
}

::v-deep .el-input__inner {
  border-radius: 8px;
  border: 2px solid #e5e5e5;
  transition: all 0.3s;
}

::v-deep .el-input__inner:focus {
  border-color: #ff5000;
}

.dialog-footer {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  padding: 16px 24px;
  border-top: 1px solid #f0f0f0;
}

.dialog-btn {
  padding: 10px 24px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  border: none;
}

.cancel-btn {
  background: #f5f5f5;
  color: #666;
}

.cancel-btn:hover {
  background: #e5e5e5;
}

.confirm-btn {
  background: linear-gradient(90deg, #ff9000 0%, #ff5000 100%);
  color: white;
}

.confirm-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 80, 0, 0.3);
}
</style>
