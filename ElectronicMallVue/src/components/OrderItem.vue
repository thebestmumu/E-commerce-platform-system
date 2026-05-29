<template>
<div>
  <div class="header" style="padding-left: 25px;">
    <span style="line-height: 40px"><b>{{order.create_time}}</b></span>
    <span style="line-height: 40px;margin-left: 30px"><b>订单编号： {{order.order_no}}</b></span>
  </div>
  <div class="body">
    <div style="display: inline-block;">
      <router-link :to="'goodView/'+order.good_id">
        <img :src="baseApi + order.imgs" style="width: 100px;height:100px">
      </router-link>
    </div>
    <div style="display: inline-block;line-height: 40px" >
      <table>
        <tr>
          <th>商品</th>
          <th>规格</th>
          <th>数量</th>
          <th>总价</th>
          <th>收货人</th>
          <th>订单状态</th>
        </tr>
        <tr>
          <a :href="'goodView/'+order.good_id">
            <td>{{order.good_name}}</td>
          </a>
          <td>{{order.standard}}</td>
          <td>{{order.count}}</td>
          <td>{{order.total_price}}</td>
          <el-popover
              placement="bottom-start"
              width="200"
              trigger="hover"
              :content=address>
            <td slot="reference" style="color: #42b983">{{ order.link_user }}</td>
          </el-popover>
<!--          订单状态-->
          <template v-if="order.state==='已发货'">
            <td style="color: #42b983">{{order.state}}</td>
            <td>
              <el-button style="margin-left: 20px;font-size: 15px;" type="primary" @click="receive">确认收货</el-button>
            </td>
          </template>

          <template v-else-if="order.state==='已收货'">
            <td style="color: #42b983"><a class="el-icon-check"></a>{{order.state}}</td>
            <td>
              <el-button 
                v-if="!order.hasReviewed" 
                style="margin-left: 20px;font-size: 15px;" 
                type="success" 
                @click="openReviewDialog"
              >
                评价
              </el-button>
              <el-tag v-else type="success" effect="plain">已评价</el-tag>
            </td>
          </template>

          <template v-else-if="order.state==='已支付'">
            <td style="color: #3b62f8"> {{order.state}}</td>
            <td>
              <el-button style="font-size: 15px;" type="info" plain disabled>等待发货</el-button>
            </td>
          </template>

          <template v-else>
            <td>{{order.state}}</td>
            <td>
              <el-button style="margin-left: 20px;font-size: 15px;" type="success" @click="pay">去支付</el-button>
            </td>
          </template>

        </tr>
      </table>
    </div>
  </div>
  
  <!-- 评论对话框 -->
  <review-dialog
    :visible.sync="reviewDialogVisible"
    :good-info="goodInfo"
    :order-id="order.id"
    :standard="order.standard"
    @review-submitted="handleReviewSubmitted"
  ></review-dialog>
</div>
</template>

<script>
import ReviewDialog from '@/components/review/ReviewDialog';

export default {
  name: "OrderItem",
  components: {
    'review-dialog': ReviewDialog
  },
  props:{
    order: Object,
  },
  created() {
    console.log(this.order)
  },
  data(){
    return{
      address: '电话:'+this.order.link_phone+' 地址:'+this.order.link_address,
      baseApi: this.$store.state.baseApi,
      reviewDialogVisible: false,
      goodInfo: null
    }
  },
  methods:{
    //跳转到支付页面
    pay(){
      this.$router.push({path: 'pay',query:{money: this.order.total_price,orderNo: this.order.order_no}})
    },
    //确认收货
    receive(){

      this.$confirm('是否确认收货？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }).then(() => {

        this.request.get("/api/order/received/"+this.order.order_no).then(res=>{
          if(res.code==='200'){
            this.$message.success("收货成功");
            this.order.state='已收货'
          }
        })
      })

    },
    // 打开评论对话框
    openReviewDialog() {
      // 获取商品信息
      this.request.get(`/api/good/${this.order.good_id}`)
        .then(res => {
          if (res.code === '200') {
            this.goodInfo = res.data;
            this.reviewDialogVisible = true;
          }
        });
    },
    // 评论提交成功后的处理
    handleReviewSubmitted() {
      this.$message.success('评价成功！');
      this.order.hasReviewed = true;
    }
  }
}
</script>

<style scoped>
.header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  height: 40px;
  border-radius: 16px 16px 0 0;
  padding-left: 25px;
  display: flex;
  align-items: center;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.2);
}

.header b {
  color: white;
  font-weight: 600;
  font-size: 14px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.body {
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  padding: 20px;
  border-radius: 0 0 16px 16px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(0, 0, 0, 0.05);
  margin-bottom: 10px;
  transition: all 0.3s ease;
}

.body:hover {
  box-shadow: 0 8px 25px rgba(99, 102, 241, 0.15);
  transform: translateY(-2px);
}

.body img {
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.body img:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 16px rgba(99, 102, 241, 0.2);
}

th, td {
  width: 120px;
  text-align: center;
  padding: 12px 8px;
}

th {
  font-size: 15px;
  color: #6366f1;
  font-weight: 600;
  background: linear-gradient(135deg, #f3f4f6 0%, #e5e7eb 100%);
  border-radius: 8px;
}

td {
  color: #374151;
  font-size: 14px;
  border-bottom: 1px solid #f3f4f6;
}

::v-deep .el-button {
  border-radius: 8px !important;
  border: none !important;
  font-weight: 600 !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1) !important;
}

::v-deep .el-button--primary {
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%) !important;
}

::v-deep .el-button--success {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%) !important;
}

::v-deep .el-button--info {
  background: linear-gradient(135deg, #6b7280 0%, #4b5563 100%) !important;
}

::v-deep .el-button:hover {
  transform: translateY(-2px) !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15) !important;
}

a {
  color: #6366f1;
  text-decoration: none;
  transition: all 0.3s ease;
  font-weight: 500;
}

a:hover {
  color: #8b5cf6;
  text-decoration: underline;
}
</style>