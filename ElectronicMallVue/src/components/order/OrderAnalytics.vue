<!--
 * @Description: 订单数据分析组件
 * @Author: Assistant
 * @Date: 2026-04-14
-->
<template>
  <div class="order-analytics">
    <div class="analytics-header">
      <h3>订单数据分析</h3>
      <div class="filter-controls">
        <el-select v-model="selectedYear" placeholder="选择年份" @change="loadData">
          <el-option
            v-for="year in availableYears"
            :key="year"
            :label="year + '年'"
            :value="year"
          />
        </el-select>
        <el-select v-model="selectedMonth" placeholder="选择月份" @change="loadData" :disabled="!selectedYear">
          <el-option
            label="全部"
            :value="0"
          />
          <el-option
            v-for="month in 12"
            :key="month"
            :label="month + '月'"
            :value="month"
          />
        </el-select>
      </div>
    </div>
    
    <div class="charts-container">
      <!-- 年份消费趋势图 -->
      <div class="chart-section">
        <h4>年度消费趋势</h4>
        <div ref="yearChart" class="chart" style="width: 100%; height: 300px;"></div>
      </div>
      
      <!-- 月份消费详情图 -->
      <div class="chart-section">
        <h4>月度消费详情</h4>
        <div ref="monthChart" class="chart" style="width: 100%; height: 300px;"></div>
      </div>
      
      <!-- 分类占比饼图 -->
      <div class="chart-section">
        <h4>消费分类占比</h4>
        <div ref="categoryChart" class="chart" style="width: 100%; height: 300px;"></div>
      </div>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import request from '@/utils/request'

export default {
  name: 'OrderAnalytics',
  props: {
    userId: {
      type: Number,
      required: true
    }
  },
  data() {
    return {
      selectedYear: new Date().getFullYear(),
      selectedMonth: new Date().getMonth() + 1,
      availableYears: [],
      orders: [],
      analyticsData: null,
      yearChart: null,
      monthChart: null,
      categoryChart: null
    }
  },
  mounted() {
    this.initYears()
    this.loadData()
  },
  beforeDestroy() {
    if (this.yearChart) {
      this.yearChart.dispose()
    }
    if (this.monthChart) {
      this.monthChart.dispose()
    }
    if (this.categoryChart) {
      this.categoryChart.dispose()
    }
  },
  methods: {
    // 初始化可用年份（最近5年）
    initYears() {
      const currentYear = new Date().getFullYear()
      this.availableYears = []
      for (let i = 0; i < 5; i++) {
        this.availableYears.push(currentYear - i)
      }
    },
    
    // 加载订单数据
    async loadData() {
      try {
        // 获取订单分析数据
        const params = {}
        if (this.selectedYear) {
          params.year = this.selectedYear
        }
        if (this.selectedMonth && this.selectedMonth !== 0) {
          params.month = this.selectedMonth
        }
        
        const res = await request.get(`/api/order/analytics/${this.userId}`, { params })
        if (res.code === '200') {
          this.analyticsData = res.data
          this.renderCharts()
        } else {
          // 如果分析API不可用，回退到原始数据
          await this.loadFallbackData()
        }
      } catch (error) {
        console.error('加载订单分析数据失败:', error)
        // 回退到原始数据
        await this.loadFallbackData()
      }
    },
    
    // 加载回退数据（原始订单数据）
    async loadFallbackData() {
      try {
        const res = await request.get(`/api/order/userid/${this.userId}`)
        if (res.code === '200') {
          this.orders = res.data
          this.processData()
          this.renderCharts()
        }
      } catch (error) {
        console.error('加载回退数据失败:', error)
      }
    },
    
    // 处理数据
    processData() {
      // 这里的数据处理将在后续步骤中实现
      console.log('处理订单数据:', this.orders)
    },
    
    // 渲染图表
    renderCharts() {
      this.renderYearChart()
      this.renderMonthChart()
      this.renderCategoryChart()
    },
    
    // 渲染年份图表
    renderYearChart() {
      if (!this.yearChart) {
        this.yearChart = echarts.init(this.$refs.yearChart)
      }
      
      let years = []
      let amounts = []
      
      // 优先使用分析 API 数据
      if (this.analyticsData && this.analyticsData.yearlyConsumption && Object.keys(this.analyticsData.yearlyConsumption).length > 0) {
        console.log('使用 API 返回的年度消费数据')
        const yearData = this.analyticsData.yearlyConsumption
        years = Object.keys(yearData).sort((a, b) => parseInt(a) - parseInt(b))
        amounts = years.map(year => yearData[year] || 0)
      } else {
        console.log('API 没有返回年度数据，使用回退逻辑从订单列表提取')
        // 使用回退数据：从订单列表中提取
        const yearData = {}
        if (this.orders && this.orders.length > 0) {
          this.orders.forEach(order => {
            const createTime = order.createTime || order.create_time
            if (createTime) {
              const date = new Date(createTime)
              const year = date.getFullYear()
              // 修复：使用正确的字段名 total_price
              const amount = parseFloat(order.totalPrice || order.total_price || order.totalMoney || order.total) || 0
              
              console.log(`订单时间：${createTime}, 年份：${year}, 金额：${amount}`)
              
              if (!yearData[year]) {
                yearData[year] = 0
              }
              yearData[year] += amount
            }
          })
          
          years = Object.keys(yearData).sort((a, b) => parseInt(a) - parseInt(b))
          amounts = years.map(year => yearData[year])
          
          console.log(`回退逻辑提取的年度数据：年份=${years}, 金额=${amounts}`)
        }
      }
      
      // 如果没有数据，显示提示
      if (years.length === 0) {
        console.warn('没有任何年度消费数据')
        const option = {
          title: {
            text: '年度消费趋势',
            left: 'center'
          },
          tooltip: {
            formatter: '{b}: ¥{c}'
          },
          xAxis: {
            type: 'category',
            data: [],
            axisLabel: {
              rotate: 45
            }
          },
          yAxis: {
            type: 'value',
            name: '消费金额 (元)'
          },
          series: [
            {
              name: '消费金额',
              type: 'bar',
              data: [],
              itemStyle: {
                color: '#5470c6'
              }
            }
          ]
        }
        
        this.yearChart.setOption(option)
        return
      }
      
      const option = {
        title: {
          text: '年度消费趋势',
          left: 'center'
        },
        tooltip: {
          trigger: 'axis',
          formatter: '{b}: ¥{c}'
        },
        xAxis: {
          type: 'category',
          data: years.map(year => `${year}年`),
          axisLabel: {
            rotate: 45
          }
        },
        yAxis: {
          type: 'value',
          name: '消费金额 (元)'
        },
        series: [
          {
            name: '消费金额',
            type: 'bar',
            data: amounts,
            itemStyle: {
              color: '#5470c6'
            }
          }
        ]
      }
      
      this.yearChart.setOption(option)
    },
    
    // 渲染月份图表
    renderMonthChart() {
      if (!this.monthChart) {
        this.monthChart = echarts.init(this.$refs.monthChart)
      }
      
      let monthData = new Array(12).fill(0)
      let chartTitle = ''
      
      if (this.analyticsData && this.analyticsData.monthlyConsumption) {
        // 使用分析API数据
        const monthConsumption = this.analyticsData.monthlyConsumption
        monthData = Array.from({length: 12}, (_, i) => monthConsumption[i + 1] || 0) // 月份从1开始
        
        if (this.selectedMonth === 0) {
          chartTitle = `${this.selectedYear}年全年月度消费详情`
        } else {
          chartTitle = `${this.selectedYear}年${this.selectedMonth}月消费详情`
        }
      } else {
        // 使用回退数据
        this.orders.forEach(order => {
          const createTime = order.createTime || order.create_time
          if (createTime) {
            const date = new Date(createTime)
            const year = date.getFullYear()
            const month = date.getMonth() + 1 // 转换为1-12
            // 修复：使用正确的字段名 total_price
            const amount = parseFloat(order.totalPrice || order.total_price || order.totalMoney || order.total) || 0
            
            if (year === this.selectedYear) {
              monthData[month - 1] += amount
            }
          }
        })
        
        if (this.selectedMonth === 0) {
          chartTitle = `${this.selectedYear}年全年月度消费详情`
        } else {
          chartTitle = `${this.selectedYear}年${this.selectedMonth}月消费详情`
        }
      }
      
      const option = {
        title: {
          text: chartTitle,
          left: 'center'
        },
        tooltip: {
          trigger: 'axis',
          formatter: '{b}: ¥{c}'
        },
        xAxis: {
          type: 'category',
          data: ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月']
        },
        yAxis: {
          type: 'value',
          name: '消费金额(元)'
        },
        series: [
          {
            name: '消费金额',
            type: 'bar',
            data: monthData,
            itemStyle: {
              color: '#91cc75'
            }
          }
        ]
      }
      
      this.monthChart.setOption(option)
    },
    
    // 渲染分类饼图
    renderCategoryChart() {
      if (!this.categoryChart) {
        this.categoryChart = echarts.init(this.$refs.categoryChart)
      }
      
      // 这里需要获取订单商品的分类信息
      // 由于当前数据结构限制，先显示一个示例饼图
      const option = {
        title: {
          text: '消费分类占比',
          left: 'center'
        },
        tooltip: {
          trigger: 'item',
          formatter: '{a} <br/>{b}: {c}元 ({d}%)'
        },
        legend: {
          orient: 'vertical',
          left: 'left'
        },
        series: [
          {
            name: '消费分类',
            type: 'pie',
            radius: '50%',
            data: [
              { value: 335, name: '电子产品' },
              { value: 310, name: '服装服饰' },
              { value: 234, name: '家居用品' },
              { value: 135, name: '食品饮料' },
              { value: 154, name: '图书文具' }
            ],
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: 'rgba(0, 0, 0, 0.5)'
              }
            }
          }
        ]
      }
      
      this.categoryChart.setOption(option)
    }
  }
}
</script>

<style scoped>
.order-analytics {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.analytics-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.analytics-header h3 {
  margin: 0;
  color: #333;
}

.filter-controls {
  display: flex;
  gap: 10px;
}

.charts-container {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
}

.chart-section {
  background: #f9f9f9;
  border-radius: 6px;
  padding: 15px;
}

.chart-section h4 {
  margin: 0 0 15px 0;
  color: #666;
  text-align: center;
}

.chart {
  background: #fff;
  border-radius: 4px;
}
</style>