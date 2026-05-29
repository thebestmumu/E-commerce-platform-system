<template>
    <div>
        <div>
            <el-select
                v-model="searchMode"
                placeholder="请选择订单类型"
                style="width: 150px; margin-right: 10px"
            >
                <el-option value="已支付" label="已支付"></el-option>
                <el-option value="已发货" label="已发货"></el-option>
                <el-option value="已收货" label="已收货"></el-option>
            </el-select>
            <el-input
                v-model="searchText"
                @keyup.enter.native="load"
                style="width: 200px"
            >
                <i slot="prefix" class="el-input__icon iconfont icon-r-find"></i
            ></el-input>
            <el-button @click="reset" type="warning" style="margin: 10px">
                
                重置
            </el-button>
            <el-button @click="load" type="primary" style="margin: 10px">
                
                搜索
            </el-button>
        </div>
        <el-table :data="tableData" border stripe style="width: 100%">
            <el-table-column prop="id" label="ID" width="50" sortable>
            </el-table-column>
            <el-table-column
                prop="orderNo"
                label="订单编号"
                width="200"
            ></el-table-column>
            <el-table-column
                prop="totalPrice"
                label="总价"
                width="100"
            ></el-table-column>
            <el-table-column
                prop="userId"
                label="下单人id"
                width="100"
            ></el-table-column>
            <el-table-column
                prop="linkUser"
                label="联系人"
                width="150"
            ></el-table-column>
            <el-table-column
                prop="linkPhone"
                label="联系电话"
            ></el-table-column>
            <el-table-column
                prop="linkAddress"
                label="送货地址"
                width="300"
            ></el-table-column>
            <el-table-column prop="state" label="状态" width="100">
                <template slot-scope="scope">
                    <el-tag
                        type="warning"
                        v-if="scope.row.state === '待付款'"
                        >{{ scope.row.state }}</el-tag
                    >
                    <el-tag
                        type="success"
                        v-if="scope.row.state === '已支付'"
                        >{{ scope.row.state }}</el-tag
                    >
                    <el-tag
                        type="primary"
                        v-if="scope.row.state === '已发货'"
                        >{{ scope.row.state }}</el-tag
                    >
                    <el-tag type="info" v-if="scope.row.state === '已收货'">{{
                        scope.row.state
                    }}</el-tag>
                </template>
            </el-table-column>
            <el-table-column
                prop="createTime"
                label="下单时间"
            ></el-table-column>
            <el-table-column fixed="right" label="操作" width="250">
                <template slot-scope="scope">
                    <el-button
                        type="primary"
                        @click="showDetail(scope.row)"
                    >
                        详情
                    </el-button>
                    <el-popconfirm
                        @confirm="showDeliveryDialog(scope.row)"
                        title="确定发货吗？"
                        v-if="scope.row.state === '已支付'"
                    >
                        <el-button
                            type="primary"
                            style="margin-left: 10px"
                            slot="reference"
                        >
                            发货
                        </el-button>
                    </el-popconfirm>
                </template>
            </el-table-column>
        </el-table>
        <!--    发货弹窗-->
        <el-dialog title="确认发货" :visible.sync="deliveryDialogVisible" width="500px">
            <el-form :model="deliveryForm" label-width="100px">
                <el-form-item label="订单编号">
                    <el-input v-model="deliveryForm.orderNo" disabled></el-input>
                </el-form-item>
                <el-form-item label="收货地址">
                    <div class="address-display">{{ deliveryForm.linkAddress }}</div>
                </el-form-item>
                <el-form-item label="发货地址">
                    <el-input 
                        v-model="deliveryForm.deliveryAddress" 
                        type="textarea" 
                        :rows="3" 
                        placeholder="请输入发货地址（卖家地址）"
                    ></el-input>
                </el-form-item>
                <el-form-item label="快递公司">
                    <el-select v-model="deliveryForm.expressCompany" placeholder="请选择快递公司" style="width: 100%">
                        <el-option label="顺丰快递" value="顺丰"></el-option>
                        <el-option label="圆通快递" value="圆通"></el-option>
                        <el-option label="中通快递" value="中通"></el-option>
                        <el-option label="韵达快递" value="韵达"></el-option>
                        <el-option label="EMS" value="EMS"></el-option>
                    </el-select>
                </el-form-item>
                <el-form-item label="快递单号">
                    <el-input v-model="deliveryForm.expressNo" placeholder="请输入快递单号"></el-input>
                </el-form-item>
            </el-form>
            <div slot="footer" class="dialog-footer">
                <el-button @click="deliveryDialogVisible = false">取消</el-button>
                <el-button type="primary" @click="confirmDelivery">确认发货</el-button>
            </div>
        </el-dialog>
        <!--    分页-->
        <div style="margin-top: 10px">
            <el-pagination
                @size-change="handleSizeChange"
                @current-change="handleCurrentChange"
                :current-page="pageNum"
                :page-size="pageSize"
                :page-sizes="[3, 5, 8, 10]"
                layout="total, sizes, prev, pager, next, jumper"
                :total="total"
            >
            </el-pagination>
        </div>
        <!--    详情弹窗-->
        <el-dialog :visible.sync="dialogFormVisible">
            <el-table :data="detail" background-color="black">
                <el-table-column label="图片" width="150">
                    <template slot-scope="scope">
                        <img
                            :src="baseApi + scope.row.img"
                            min-width="100"
                            height="100"
                        />
                    </template>
                </el-table-column>

                <el-table-column prop="goodId" label="商品id"></el-table-column>
                <el-table-column
                    prop="goodName"
                    label="商品名称"
                ></el-table-column>
                <el-table-column
                    prop="standard"
                    label="商品规格"
                ></el-table-column>
                <el-table-column prop="price" label="单价"></el-table-column>
                <el-table-column prop="discount" label="折扣"></el-table-column>
                <el-table-column prop="price" label="实价"></el-table-column>
                <el-table-column prop="count" label="数量"></el-table-column>
                <el-table-column label="总价">
                    <template slot-scope="scope">
                        {{ scope.row.price * scope.row.count }}
                    </template>
                </el-table-column>
            </el-table>
        </el-dialog>
    </div>
</template>

<script>
import API from "../../utils/request";
const url = "/api/order/";


export default {
    name: "Order",
    data() {
        return {
            options: [],
            searchMode: "",
            searchText: "",
            user: {},
            tableData: [],
            pageNum: 1,
            pageSize: 8,
            entity: {},
            total: 0,
            dialogFormVisible: false,
            detail: [],
            baseApi: this.$store.state.baseApi,
            deliveryDialogVisible: false,
            deliveryForm: {
                orderNo: '',
                linkAddress: '',
                deliveryAddress: '',
                expressCompany: '',
                expressNo: ''
            },
        };
    },
    created() {
        this.load();
    },
    methods: {
        handleSizeChange(pageSize) {
            this.pageSize = pageSize;
            this.load();
        },
        handleCurrentChange(pageNum) {
            this.pageNum = pageNum;
            this.load();
        },
        reset() {
            this.searchMode = "";
            this.searchText = "";
            this.load();
        },
        load() {
            API.get(url + "/page", {
                params: {
                    pageNum: this.pageNum,
                    pageSize: this.pageSize,
                    orderNo: this.searchText,
                    state: this.searchMode,
                },
            })
                .then((res) => {
                    this.tableData = res.data.records || [];
                    this.total = res.data.total;
                    
                })
                .catch((e) => {
                    if (e.response.data == undefined) {
                        this.$message({
                            showClose: true,
                            message: e,
                            type: "error",
                            duration: 5000,
                        });
                    } else {
                        this.$message({
                            showClose: true,
                            message: e.response.data,
                            type: "error",
                            duration: 5000,
                        });
                    }
                    
                });
        },
        showDetail(row) {
            this.request
                .get("/api/order/orderNo/" + row.orderNo)
                .then((res) => {
                    if (res.code === "200") {
                        this.detail = [];
                        this.detail.push(res.data);
                        this.dialogFormVisible = true;
                    }
                });
        },
        showDeliveryDialog(order) {
            this.deliveryForm = {
                orderNo: order.orderNo,
                linkAddress: order.linkAddress,
                deliveryAddress: '',
                expressCompany: '',
                expressNo: ''
            };
            this.deliveryDialogVisible = true;
        },
        confirmDelivery() {
            if (!this.deliveryForm.deliveryAddress) {
                this.$message.warning('请输入发货地址');
                return;
            }
            if (!this.deliveryForm.expressCompany) {
                this.$message.warning('请选择快递公司');
                return;
            }
            if (!this.deliveryForm.expressNo) {
                this.$message.warning('请输入快递单号');
                return;
            }
            this.request
                .get("/api/order/delivery/" + this.deliveryForm.orderNo, {
                    params: {
                        deliveryAddress: this.deliveryForm.deliveryAddress,
                        expressCompany: this.deliveryForm.expressCompany,
                        expressNo: this.deliveryForm.expressNo
                    }
                })
                .then((res) => {
                    if (res.code === "200") {
                        this.$message.success("成功发货");
                        this.deliveryDialogVisible = false;
                        this.load();
                    }
                });
        },
        delivery(order) {
            this.request
                .get("/api/order/delivery/" + order.orderNo)
                .then((res) => {
                    if (res.code === "200") {
                        this.$message.success("成功发货");
                        order.state = "已发货";
                    }
                });
        },
    },
};
</script>

<style scoped>
.address-display {
    padding: 10px;
    background: #f5f7fa;
    border-radius: 4px;
    color: #606266;
    font-size: 14px;
    line-height: 1.6;
}
</style>
