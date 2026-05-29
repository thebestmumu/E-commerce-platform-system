<template>
  <div class="global-delivery-map">
    <h3 class="map-title">📦 商品发货地址全国分布</h3>
    <div id="globalMap" style="width: 100%; height: 500px;"></div>
    <div class="map-stats">
      <div class="stat-item">
        <span class="stat-label">总发货地址数</span>
        <span class="stat-value">{{ totalAddresses }}</span>
      </div>
      <div class="stat-item">
        <span class="stat-label">覆盖城市数</span>
        <span class="stat-value">{{ uniqueCities }}</span>
      </div>
      <div class="stat-item">
        <span class="stat-label">覆盖省份数</span>
        <span class="stat-value">{{ uniqueProvinces }}</span>
      </div>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts';

export default {
  name: 'GlobalDeliveryMap',
  data() {
    return {
      mapChart: null,
      addressData: [],
      totalAddresses: 0,
      uniqueCities: 0,
      uniqueProvinces: 0
    };
  },
  mounted() {
    this.loadDeliveryAddresses();
  },
  beforeDestroy() {
    if (this.mapChart) {
      this.mapChart.dispose();
    }
  },
  methods: {
    async loadDeliveryAddresses() {
      try {
        const res = await this.request.get('/api/good/delivery-addresses');
        if (res.code === '200' && res.data) {
          this.addressData = res.data;
          this.totalAddresses = this.addressData.length;
          const cities = new Set(this.addressData.map(item => item.city));
          this.uniqueCities = cities.size;
          
          // 统计省份
          const provinces = new Set(this.addressData.map(item => item.province).filter(Boolean));
          this.uniqueProvinces = provinces.size;
          
          await this.loadChinaMap();
          this.initMap();
        }
      } catch (error) {
        console.error('加载发货地址数据失败:', error);
      }
    },
    async loadChinaMap() {
      try {
        const response = await fetch('https://geo.datav.aliyun.com/areas_v3/bound/100000_full.json');
        const chinaMap = await response.json();
        echarts.registerMap('china', chinaMap);
      } catch (error) {
        console.error('加载中国地图数据失败，使用备用数据源:', error);
        try {
          const response = await fetch('https://raw.githubusercontent.com/apache/echarts/master/test/data/map/json/china.json');
          const chinaMap = await response.json();
          echarts.registerMap('china', chinaMap);
        } catch (err) {
          console.error('备用数据源也失败:', err);
        }
      }
    },
    initMap() {
      const chartDom = document.getElementById('globalMap');
      if (!chartDom) return;
      
      this.mapChart = echarts.init(chartDom);
      
      // 统计每个省份的商品数量
      const provinceData = {};
      this.addressData.forEach(item => {
        const province = item.province || this.getProvinceByCity(item.city);
        if (province) {
          if (!provinceData[province]) {
            provinceData[province] = 0;
          }
          provinceData[province] += (item.count || 1);
        }
      });
      
      // 转换为地图数据格式
      const mapData = Object.keys(provinceData).map(province => ({
        name: province,
        value: provinceData[province]
      }));
      
      const maxValue = Math.max(...Object.values(provinceData), 1);
      
      const option = {
        tooltip: {
          trigger: 'item',
          formatter: function(params) {
            if (params.value && params.value > 0) {
              return `${params.name}<br/>已购商品数量：<strong style="color:#ff6600">${params.value}</strong>`;
            }
            return `${params.name}<br/><span style="color:#999">暂无购买记录</span>`;
          }
        },
        visualMap: {
          min: 0,
          max: maxValue,
          left: 'left',
          top: 'bottom',
          text: ['购买多', '购买少'],
          calculable: true,
          inRange: {
            color: ['#ffffff', '#ffe0b2', '#ffb74d', '#ff9800', '#f57c00', '#e65100']
          },
          textStyle: {
            color: '#333'
          }
        },
        geo: {
          map: 'china',
          roam: false,
          label: {
            show: true,
            color: '#666',
            fontSize: 9
          },
          itemStyle: {
            areaColor: '#f5f5f5',
            borderColor: '#999',
            borderWidth: 1
          },
          emphasis: {
            itemStyle: {
              areaColor: '#ffcc00',
              borderColor: '#ff6600',
              borderWidth: 2,
              shadowColor: 'rgba(255, 102, 0, 0.5)',
              shadowBlur: 10
            },
            label: {
              show: true,
              color: '#fff',
              fontWeight: 'bold'
            }
          }
        },
        series: [
          {
            name: '商品分布',
            type: 'map',
            geoIndex: 0,
            data: mapData
          }
        ]
      };
      
      this.mapChart.setOption(option);
    },
    getProvinceByCity(city) {
      const cityProvinceMap = {
        '北京': '北京市', '上海': '上海市', '天津': '天津市', '重庆': '重庆市',
        '广州': '广东省', '深圳': '广东省', '东莞': '广东省', '佛山': '广东省',
        '杭州': '浙江省', '宁波': '浙江省', '温州': '浙江省',
        '南京': '江苏省', '苏州': '江苏省', '无锡': '江苏省',
        '成都': '四川省', '武汉': '湖北省', '西安': '陕西省',
        '长沙': '湖南省', '郑州': '河南省', '济南': '山东省',
        '青岛': '山东省', '大连': '辽宁省', '沈阳': '辽宁省',
        '厦门': '福建省', '福州': '福建省', '合肥': '安徽省',
        '南昌': '江西省', '昆明': '云南省', '贵阳': '贵州省',
        '南宁': '广西壮族自治区', '哈尔滨': '黑龙江省', '长春': '吉林省',
        '石家庄': '河北省', '太原': '山西省', '兰州': '甘肃省',
        '银川': '宁夏回族自治区', '西宁': '青海省', '乌鲁木齐': '新疆维吾尔自治区',
        '拉萨': '西藏自治区', '呼和浩特': '内蒙古自治区'
      };
      return cityProvinceMap[city] || null;
    }
  }
};
</script>

<style scoped>
.global-delivery-map {
  margin: 0;
  padding: 15px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  height: 100%;
}

.map-title {
  text-align: center;
  margin-bottom: 15px;
  color: #333;
  font-size: 16px;
}

.map-stats {
  display: flex;
  justify-content: center;
  gap: 30px;
  margin-top: 15px;
  padding: 10px;
  background: #f5f7fa;
  border-radius: 8px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-label {
  font-size: 12px;
  color: #666;
  margin-bottom: 3px;
}

.stat-value {
  font-size: 20px;
  font-weight: bold;
  color: #409eff;
}
</style>
