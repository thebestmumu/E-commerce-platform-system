<template>
  <div>
    <el-tabs v-model="activeName" @tab-click="handleClick">
      <div
        id="chartSum"
        style="
          display: inline-block;
          margin-left: 50px;
          margin-top: 30px;
          font-weight: bold;
          font-size: 22px;
          color: #ffb02a;
          border: 1px lightgrey solid;
          border-radius: 10px;
          padding: 20px;
        "
        
        > 总计：￥{{ total | numFilter }}</div
      >
      <!--      柱状图-->
      <el-tab-pane label="各类收入柱状图" name="bar">
        <div
          id="bar"
          style="width: 1200px; height: 500px; margin: auto auto"
        ></div>
      </el-tab-pane>
      <!--      饼图-->
      <el-tab-pane label="各类收入饼图" name="pie">
        <div
          id="pie"
          style="width: 600px; height: 600px; margin: 10px auto"
        ></div>
      </el-tab-pane>
      <!--  本周收入折线图-->
      <el-tab-pane label="本周收入" name="line1">
        <div
          id="weekLine"
          style="width: 900px; height: 500px; margin: 10px auto"
        ></div>
      </el-tab-pane>
      <!-- 本月收入折线图-->
      <el-tab-pane label="本月收入" name="line2">
        <div
          id="monthLine"
          style="width: 1500px; height: 500px; margin: 10px auto"
        ></div>
      </el-tab-pane>
      <!-- 发货地址全球分布图-->
      <el-tab-pane label="发货地址分布" name="map">
        <GlobalDeliveryMap />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import * as echarts from "echarts";
import GlobalDeliveryMap from '../../../components/GlobalDeliveryMap.vue';

export default {
  name: "IncomeChart",
  components: {
    GlobalDeliveryMap
  },
  data() {
    return {
      sumIncome: 0,
      categoryIncomes: [],
      categoryNames: [],
      incomes: [],
      activeName: "bar",
      totalAll: 0,
      totalWeek: 0,
      totalMonth: 0,
      total: 0,
      barChart: null,
      pieChart: null,
      lineChart1: null,
      lineChart2: null,
    };
  },
  methods: {
    handleClick(tab) {
      switch (tab.name) {
        case "bar":
          this.total = this.totalAll;
          break;
        case "pie":
          this.total = this.totalAll;
          break;
        case "line1":
          this.total = this.totalWeek;
          break;
        case "line2":
          this.total = this.totalMonth;
          break;
        case "map":
          this.total = 0;
          break;
      }
      this.$nextTick(() => {
        this.resizeCharts();
      });
    },
    resizeCharts() {
      if (this.barChart) this.barChart.resize();
      if (this.pieChart) this.pieChart.resize();
      if (this.lineChart1) this.lineChart1.resize();
      if (this.lineChart2) this.lineChart2.resize();
    },
  },

  mounted() {
    this.barChart = echarts.init(document.getElementById("bar"));
    this.pieChart = echarts.init(document.getElementById("pie"));
    this.lineChart1 = echarts.init(document.getElementById("weekLine"));
    this.lineChart2 = echarts.init(document.getElementById("monthLine"));
    
    var barOption = {
      tooltip: {
        trigger: "item",
      },
      title: {
        text: "收入统计柱状图",
        x: "center",
      },
      label: {
        show: true,
        position: "top",
      },
      xAxis: {
        type: "category",
        data: [],
      },
      yAxis: {
        type: "value",
      },
      series: [
        {
          data: [],
          type: "bar",
        },
      ],
    };
    var pieOption = {
      tooltip: {
        trigger: "item",
      },
      title: {
        text: "收入统计饼图",
        x: "center",
      },
      series: [
        {
          type: "pie",
          data: [],
        },
      ],
    };
    var lineOption1 = {
      tooltip: {
        trigger: "item",
      },
      label: {
        show: true,
      },
      title: {
        text: "本周收入",
        x: "center",
      },
      xAxis: {
        type: "category",
        data: ["周一", "周二", "周三", "周四", "周五", "周六", "周日"],
      },
      yAxis: {
        type: "value",
      },
      series: [
        {
          data: [],
          type: "line",
        },
      ],
    };
    var lineOption2 = {
      tooltip: {
        trigger: "item",
      },
      label: {
        show: true,
      },
      title: {
        text: "本月收入",
        x: "center",
      },
      xAxis: {
        type: "category",
        data: [],
      },
      yAxis: {
        type: "value",
      },
      series: [
        {
          data: [],
          type: "line",
        },
      ],
    };
    
    this.barChart.setOption(barOption);
    this.pieChart.setOption(pieOption);
    this.lineChart1.setOption(lineOption1);
    this.lineChart2.setOption(lineOption2);
    
    window.addEventListener('resize', this.resizeCharts);
    
    this.request.get("/api/income/chart").then((res) => {
      if (res.code === "200" && res.data && res.data.categoryIncomes) {
        let categoryIncomes = res.data.categoryIncomes;
        let categoryNames = categoryIncomes.map((item) => {
          return item.categoryName;
        });
        let incomes = categoryIncomes.map((item) => {
          return item.categoryIncome;
        });
        
        this.barChart.setOption({
          xAxis: { data: categoryNames },
          series: [{ data: incomes }]
        });

        this.pieChart.setOption({
          series: [{
            data: categoryNames.map((name, index) => ({
              value: incomes[index],
              name: name
            }))
          }]
        });
        
        let sum = 0;
        incomes.forEach((item) => {
          sum += item;
        });
        this.total = sum;
        this.totalAll = sum;
      }
    }).catch(err => {
      console.error('加载收入数据失败:', err);
    });
    
    this.request.get("/api/income/week").then((res) => {
      if (res.code === "200" && res.data) {
        let weekIncome = res.data.weekIncome || [];
        this.lineChart1.setOption({
          series: [{ data: weekIncome }]
        });
        
        let sum = 0;
        weekIncome.forEach((item) => {
          sum += item;
        });
        this.totalWeek = sum;
      }
    }).catch(err => {
      console.error('加载本周收入失败:', err);
    });
    
    this.request.get("/api/income/month").then((res) => {
      if (res.code === "200" && res.data) {
        let monthDays = res.data.monthDays || [];
        let monthIncome = res.data.monthIncome || [];
        
        this.lineChart2.setOption({
          xAxis: { data: monthDays },
          series: [{ data: monthIncome }]
        });
        
        let sum = 0;
        monthIncome.forEach((item) => {
          sum += item;
        });
        this.totalMonth = sum;
      }
    }).catch(err => {
      console.error('加载本月收入失败:', err);
    });
    
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.resizeCharts);
    if (this.barChart) this.barChart.dispose();
    if (this.pieChart) this.pieChart.dispose();
    if (this.lineChart1) this.lineChart1.dispose();
    if (this.lineChart2) this.lineChart2.dispose();
  },
  filters: {
    numFilter(value) {
      let realVal = Number(value).toFixed(2);
      return Number(realVal);
    },
  },
};
</script>

<style scoped>
</style>