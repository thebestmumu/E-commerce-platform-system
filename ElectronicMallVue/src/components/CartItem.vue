<template>
  <div>
    <div class="header" style="padding-left: 25px;">
      <el-checkbox v-model="cart.checked" style="margin-right: 15px;"></el-checkbox>
      <span style="line-height: 40px"><b>订单时间：{{ cart.createTime }}</b></span>
    </div>
    <div class="body">
<!--      图片-->
      <div style="display: inline-block;margin-right: 20px">
        <router-link :to="'goodView/'+cart.goodId">
          <img :src="baseApi + cart.img" style="width: 100px;height:100px">
        </router-link>
      </div>
<!--      商品信息-->
      <div style="display: inline-block;line-height: 40px" >
        <table>
          <tr>
            <th>商品</th>
            <th>规格</th>
            <th>价格</th>
            <th>数量</th>
            <th>总价</th>
            <th>操作</th>
          </tr>
          <tr>
            <a :href="'goodView/'+cart.goodId">
              <td>{{ cart.goodName }}</td>
            </a>
            <td>{{cart.standard}}</td>
            <td>{{realPrice.toFixed(2)}}</td>
            <td>
              <el-button style="font-size: 15px;" @click="countChangeFlag=true" v-if="!countChangeFlag">
                {{cart.count}}
              </el-button>
              <el-input-number v-model="cart.count" :min="1" :max="cart.store" v-if="countChangeFlag" style="width: 120px" ></el-input-number>
              </td>
            <td>{{totalPrice}}</td>
            <td>
              <el-button type="success" @click="pay" style="font-size: 18px;">
                
                支付
              </el-button>
              <el-popconfirm
                  @confirm="del"
                  title="确定删除？"
              >
                <el-button type="danger" slot="reference" style="font-size: 18px;">
                  
                    移除
                </el-button>
              </el-popconfirm>
            </td>
          </tr>
        </table>
      </div>
    </div>



  </div>
</template>

<script>

export default {
  name: "CartItem",
  props:{
    cart: Object,
    countChangeFlag: false,
  },
  created() {
    
  },
  mounted() {
    
  },
  data(){
    return{

      baseApi: this.$store.state.baseApi,
    }
  },
  computed:{
    totalPrice:function () {
      return (this.realPrice * this.cart.count).toFixed(2)
    },
    realPrice: function (){
      return (this.cart.price * this.cart.discount)
    }
  },
  methods:{
    //从购物车移除
    del(id){
      this.request.delete("/api/cart/"+this.cart.id).then(res=>{
        if(res.code==='200'){
          this.$message.success("删除成功")
          this.$emit('delete',this.cart.id)
        }
      })
    },
    //跳转到支付页面
    pay(){
      let good = {id: this.cart.goodId,name: this.cart.goodName,imgs: this.cart.img,discount: this.cart.discount}
      this.$router.push({name: 'preOrder',query: {good: JSON.stringify(good), realPrice: this.realPrice, num: this.cart.count, standard: this.cart.standard, cartId: this.cart.id}})
    },
  }
}
</script>

<style scoped>
.header {
  background: linear-gradient(135deg, #ff9000 0%, #ff5000 100%);
  height: 40px;
  border-radius: 12px 12px 0 0;
  padding-left: 25px;
  display: flex;
  align-items: center;
  box-shadow: 0 2px 8px rgba(255, 80, 0, 0.2);
}

.header b {
  color: white;
  font-weight: 600;
  font-size: 14px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.body {
  background: linear-gradient(135deg, #ffffff 0%, #fff5f0 100%);
  padding: 20px;
  border-radius: 0 0 12px 12px;
  box-shadow: 0 4px 15px rgba(255, 80, 0, 0.08);
  border: 1px solid #ffe8d6;
  transition: all 0.3s ease;
}

.body:hover {
  box-shadow: 0 8px 25px rgba(255, 80, 0, 0.15);
  transform: translateY(-2px);
}

.body img {
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(255, 80, 0, 0.1);
  transition: all 0.3s ease;
}

.body img:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 16px rgba(255, 80, 0, 0.2);
}

th, td {
  width: 120px;
  text-align: center;
  padding: 12px 8px;
}

th {
  font-size: 15px;
  color: #ff5000;
  font-weight: 600;
  background: linear-gradient(135deg, #fff5f0 0%, #ffe8d6 100%);
  border-radius: 8px;
}

td {
  color: #374151;
  font-size: 14px;
  border-bottom: 1px solid #ffe8d6;
}

::v-deep .el-button {
  border-radius: 8px !important;
  border: none !important;
  font-weight: 600 !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1) !important;
}

::v-deep .el-button--success {
  background: linear-gradient(135deg, #ff9000 0%, #ff5000 100%) !important;
}

::v-deep .el-button--danger {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%) !important;
}

::v-deep .el-button:hover {
  transform: translateY(-2px) !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15) !important;
}

::v-deep .el-input-number {
  border-radius: 8px !important;
  border: 2px solid #ffe8d6 !important;
  transition: all 0.3s ease !important;
}

::v-deep .el-input-number:hover {
  border-color: #ff9000 !important;
}

::v-deep .el-input-number__increase,
::v-deep .el-input-number__decrease {
  background: linear-gradient(135deg, #ff9000 0%, #ff5000 100%) !important;
  color: white !important;
  border: none !important;
}

a {
  color: #ff5000;
  text-decoration: none;
  transition: all 0.3s ease;
  font-weight: 500;
}

a:hover {
  color: #ff9000;
  text-decoration: underline;
}
</style>