<template>
<div class="address-map-container">
    <div class="map-header">
        <h3 class="map-title">📍 物流路线追踪</h3>
        <button class="close-btn" @click="$emit('close')">×</button>
    </div>
    
    <div class="address-info">
        <div class="info-row">
            <span class="info-label">收货人：</span>
            <span class="info-value">{{ address.linkUser }}</span>
        </div>
        <div class="info-row">
            <span class="info-label">联系电话：</span>
            <span class="info-value">{{ address.linkPhone }}</span>
        </div>
        <div class="info-row">
            <span class="info-label">详细地址：</span>
            <span class="info-value">{{ address.linkAddress }}</span>
        </div>
    </div>
    
    <div class="map-content">
        <div id="baiduMap" class="baidu-map"></div>
        <div class="map-controls">
            <button class="map-btn" @click="showRoute">� 查看物流路线</button>
            <button class="map-btn secondary" @click="showSellerAddress">📦 查看发货地址</button>
        </div>
    </div>
    
    <div v-if="routeInfo" class="route-info">
        <h4>🚚 物流路线信息</h4>
        <div class="route-detail">
            <p><strong>发货地：</strong>{{ routeInfo.start }}</p>
            <p><strong>收货地：</strong>{{ routeInfo.end }}</p>
            <p><strong>总距离：</strong>{{ routeInfo.distance }}</p>
            <p><strong>预计时间：</strong>{{ routeInfo.duration }}</p>
        </div>
    </div>
    
    <div v-if="sellerAddress" class="seller-address-info">
        <h4>🏭 发货地址信息</h4>
        <p>{{ sellerAddress }}</p>
    </div>
</div>
</template>

<script>
export default {
    name: "AddressMap",
    props: {
        address: {
            type: Object,
            required: true
        },
        sellerAddress: {
            type: String,
            default: ''
        }
    },
    data() {
        return {
            map: null,
            routeInfo: null,
            startMarker: null,
            endMarker: null
        }
    },
    mounted() {
        this.initMap();
    },
    methods: {
        initMap() {
            const script = document.createElement('script');
            script.src = `https://api.map.baidu.com/api?v=3.0&ak=RNsH2PuOKazm6ifsOG6LrJ8Ir95uX5JT&callback=initBaiduMap`;
            window.initBaiduMap = () => {
                this.createMap();
            };
            document.head.appendChild(script);
        },
        createMap() {
            this.map = new BMap.Map("baiduMap");
            this.map.centerAndZoom("中国", 5);
            this.map.enableScrollWheelZoom(true);
            
            if (this.sellerAddress && this.address && this.address.linkAddress) {
                this.showRoute();
            } else if (this.address && this.address.linkAddress) {
                this.geocodeAddress();
            }
        },
        showRoute() {
            if (!this.map) return;
            
            if (!this.sellerAddress) {
                this.$message.info('暂无发货地址信息，将只显示收货地址');
                this.geocodeAddress();
                return;
            }
            
            this.map.clearOverlays();
            this.routeInfo = null;
            
            const geoc = new BMap.Geocoder();
            let startPoint = null;
            let endPoint = null;
            
            geoc.getPoint(this.sellerAddress, (start) => {
                if (start) {
                    startPoint = start;
                    
                    geoc.getPoint(this.address.linkAddress, (end) => {
                        if (end) {
                            endPoint = end;
                            
                            this.drawRoute(startPoint, endPoint);
                            this.calculateRoute(startPoint, endPoint);
                        } else {
                            this.$message.warning('无法定位收货地址');
                        }
                    });
                } else {
                    this.$message.warning('无法定位发货地址');
                }
            });
        },
        drawRoute(start, end) {
            this.map.centerAndZoom(start, 6);
            
            this.startMarker = new BMap.Marker(start);
            this.startMarker.setIcon(new BMap.Icon(
                'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAADgdz34AAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAApgAAAKYB3X3/OAAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAANCSURBVEiJtZZPbBtFFMZ/M7ubXdtdb1xSFyeilBapySVU8h8OoFaooFSqiihIVIpQBKci6KEg9Q6H9kovIHoCIVQJJCKE1ENFjnAgcaSGC6rEnxBwA04Tx43t2FnvDAfjkNibxgHxnWb2e/u992bee7tCa00YFsffekFY+nUzFtjW0LrvjRXrCDIAaPLlW0nHL0SsZtVoaF98mLrx3pdhOqLtYPHChahZcYYO7KvPFxvRl5XPp1sN3adWiD1ZAqD6XYK1b/dvE5IWryTt2udLFedwc1+9kLp+vbbpoDh+6TklxBeAi9TL0taeWpdmZzQDry0AcO+jQ12RyohqqoYoo8RDwJrU+qXkjWtfi8Xxt58BdQuwQs9qC/afLwCw8tnQbqYAPsgxE1S6F3EAIXux2oQFKm0ihMsOF71dHYx+f3NND68ghCu1YIoePPQN1pGRABkJ6Bus96CutRZMydTl+TvuiRW1m3n0eDl0vRPcEysqdXn+jsQPsrHMquGeXEaY4Yk4wxWcY5V/9scqOMOVUFthatyTy8QyqwZ+kDURKoMWxNKr2EeqVKcTNOajqKoBgOE28U4tdQl5p5bwCw7BWquaZSzAPlwjlithJtp3pTImSqQRrb2Z8PHGigD4RZuNX6JYj6wj7O4TFLbCO/Mn/m8R+h6rYSUb3ekokRY6f/YukArN979jcW+V/S8g0eT/N3VN3kTqWbQ428m9/8k0P/1aIhF36PccEl6EhOcAUCrXKZXXWS3XKd2vc/TRBG9O5ELC17MmWubD2nKhUKZa26Ba2+D3P+4/MNCFwg59oWVeYhkzgN/JDR8deKBoD7Y+ljEjGZ0sosXVTvbc6RHirr2reNy1OXd6pJsQ+gqjk8VWFYmHrwBzW/n+uMPFiRwHB2I7ih8ciHFxIkd/3Omk5tCDV1t+2nNu5sxxpDFNx+huNhVT3/zMDz8usXC3ddaHBj1GHj/As08fwTS7Kt1HBTmyN29vdwAw+/wbwLVOJ3uAD1wi/dUH7Qei66PfyuRj4Ik9is+hglfbkbfR3cnZm7chlUWLdwmprtCohX4HUtlOcQjLYCu+fzGJH2QRKvP3UNz8bWk1qMxjGTOMThZ3kvgLI5AzFfo379UAAAAASUVORK5CYII=',
                new BMap.Size(20, 30)
            ));
            this.map.addOverlay(this.startMarker);
            
            const startLabel = new BMap.Label('发货地', {
                offset: new BMap.Size(20, -10)
            });
            this.startMarker.setLabel(startLabel);
            
            this.endMarker = new BMap.Marker(end);
            this.map.addOverlay(this.endMarker);
            
            const endLabel = new BMap.Label('收货地', {
                offset: new BMap.Size(20, -10)
            });
            this.endMarker.setLabel(endLabel);
            
            const driving = new BMap.DrivingRoute(this.map, {
                renderOptions: {
                    map: this.map,
                    autoViewport: true
                },
                onSearchComplete: (results) => {
                    if (driving.getStatus() === BMAP_STATUS_SUCCESS) {
                        const plan = results.getPlan(0);
                        console.log('路线规划成功');
                    }
                }
            });
            
            driving.search(start, end);
        },
        calculateRoute(start, end) {
            const driving = new BMap.DrivingRoute(this.map, {
                onSearchComplete: (results) => {
                    if (driving.getStatus() === BMAP_STATUS_SUCCESS) {
                        const plan = results.getPlan(0);
                        const distance = plan.getDistance(false);
                        const duration = plan.getDuration(false);
                        
                        this.routeInfo = {
                            start: this.sellerAddress,
                            end: this.address.linkAddress,
                            distance: distance,
                            duration: duration
                        };
                    }
                }
            });
            
            driving.search(start, end);
        },
        geocodeAddress() {
            if (!this.map || !this.address.linkAddress) return;
            
            this.map.clearOverlays();
            this.routeInfo = null;
            
            const geoc = new BMap.Geocoder();
            geoc.getPoint(this.address.linkAddress, (point) => {
                if (point) {
                    this.map.centerAndZoom(point, 15);
                    
                    const marker = new BMap.Marker(point);
                    this.map.addOverlay(marker);
                    
                    const label = new BMap.Label(this.address.linkUser, {
                        offset: new BMap.Size(20, -10)
                    });
                    marker.setLabel(label);
                    
                    const infoWindow = new BMap.InfoWindow(
                        `<div style="padding: 10px;">
                            <p><strong>收货人：</strong>${this.address.linkUser}</p>
                            <p><strong>电话：</strong>${this.address.linkPhone}</p>
                            <p><strong>地址：</strong>${this.address.linkAddress}</p>
                        </div>`
                    );
                    marker.addEventListener("click", () => {
                        this.map.openInfoWindow(infoWindow, point);
                    });
                } else {
                    this.$message.warning('无法定位该地址，请检查地址是否正确');
                }
            });
        },
        showSellerAddress() {
            if (!this.map || !this.sellerAddress) {
                this.$message.info('暂无发货地址信息');
                return;
            }
            
            const geoc = new BMap.Geocoder();
            geoc.getPoint(this.sellerAddress, (point) => {
                if (point) {
                    this.map.centerAndZoom(point, 15);
                    this.map.clearOverlays();
                    
                    const marker = new BMap.Marker(point);
                    marker.setIcon(new BMap.Icon(
                        'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAADgdz34AAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAApgAAAKYB3X3/OAAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAANCSURBVEiJtZZPbBtFFMZ/M7ubXdtdb1xSFyeilBapySVU8h8OoFaooFSqiihIVIpQBKci6KEg9Q6H9kovIHoCIVQJJCKE1ENFjnAgcaSGC6rEnxBwA04Tx43t2FnvDAfjkNibxgHxnWb2e/u992bee7tCa00YFsffekFY+nUzFtjW0LrvjRXrCDIAaPLlW0nHL0SsZtVoaF98mLrx3pdhOqLtYPHChahZcYYO7KvPFxvRl5XPp1sN3adWiD1ZAqD6XYK1b/dvE5IWryTt2udLFedwc1+9kLp+vbbpoDh+6TklxBeAi9TL0taeWpdmZzQDry0AcO+jQ12RyohqqoYoo8RDwJrU+qXkjWtfi8Xxt58BdQuwQs9qC/afLwCw8tnQbqYAPsgxE1S6F3EAIXux2oQFKm0ihMsOF71dHYx+f3NND68ghCu1YIoePPQN1pGRABkJ6Bus96CutRZMydTl+TvuiRW1m3n0eDl0vRPcEysqdXn+jsQPsrHMquGeXEaY4Yk4wxWcY5V/9scqOMOVUFthatyTy8QyqwZ+kDURKoMWxNKr2EeqVKcTNOajqKoBgOE28U4tdQl5p5bwCw7BWquaZSzAPlwjlithJtp3pTImSqQRrb2Z8PHGigD4RZuNX6JYj6wj7O4TFLbCO/Mn/m8R+h6rYSUb3ekokRY6f/YukArN979jcW+V/S8g0eT/N3VN3kTqWbQ428m9/8k0P/1aIhF36PccEl6EhOcAUCrXKZXXWS3XKd2vc/TRBG9O5ELC17MmWubD2nKhUKZa26Ba2+D3P+4/MNCFwg59oWVeYhkzgN/JDR8deKBoD7Y+ljEjGZ0sosXVTvbc6RHirr2reNy1OXd6pJsQ+gqjk8VWFYmHrwBzW/n+uMPFiRwHB2I7ih8ciHFxIkd/3Omk5tCDV1t+2nNu5sxxpDFNx+huNhVT3/zMDz8usXC3ddaHBj1GHj/As08fwTS7Kt1HBTmyN29vdwAw+/wbwLVOJ3uAD1wi/dUH7Qei66PfyuRj4Ik9is+hglfbkbfR3cnZm7chlUWLdwmprtCohX4HUtlOcQjLYCu+fzGJH2QRKvP3UNz8bWk1qMxjGTOMThZ3kvgLI5AzFfo379UAAAAASUVORK5CYII=',
                        new BMap.Size(20, 30)
                    ));
                    this.map.addOverlay(marker);
                    
                    const infoWindow = new BMap.InfoWindow(
                        `<div style="padding: 10px;">
                            <p><strong>发货地址：</strong>${this.sellerAddress}</p>
                        </div>`
                    );
                    marker.addEventListener("click", () => {
                        this.map.openInfoWindow(infoWindow, point);
                    });
                } else {
                    this.$message.warning('无法定位发货地址，请检查地址是否正确');
                }
            });
        }
    }
};
</script>

<style scoped>
.address-map-container {
    background: white;
    border-radius: 12px;
    padding: 20px;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.map-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    padding-bottom: 15px;
    border-bottom: 2px solid #f0f0f0;
}

.map-title {
    font-size: 18px;
    font-weight: 600;
    color: #333;
    margin: 0;
}

.close-btn {
    background: none;
    border: none;
    font-size: 24px;
    color: #999;
    cursor: pointer;
    width: 32px;
    height: 32px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    transition: all 0.3s;
}

.close-btn:hover {
    background: #f5f5f5;
    color: #333;
}

.address-info {
    background: #f8f9fa;
    border-radius: 8px;
    padding: 15px;
    margin-bottom: 20px;
}

.info-row {
    display: flex;
    margin-bottom: 8px;
}

.info-row:last-child {
    margin-bottom: 0;
}

.info-label {
    font-weight: 600;
    color: #666;
    min-width: 80px;
}

.info-value {
    color: #333;
    flex: 1;
}

.map-content {
    margin-bottom: 20px;
}

.baidu-map {
    width: 100%;
    height: 400px;
    border-radius: 8px;
    border: 1px solid #e0e0e0;
    margin-bottom: 15px;
}

.map-controls {
    display: flex;
    gap: 10px;
}

.map-btn {
    flex: 1;
    padding: 10px 16px;
    background: linear-gradient(135deg, #ff9000 0%, #ff5000 100%);
    color: white;
    border: none;
    border-radius: 20px;
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s;
}

.map-btn:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(255, 80, 0, 0.3);
}

.seller-address-info {
    background: #fff5f0;
    border: 1px solid #ffe8d6;
    border-radius: 8px;
    padding: 15px;
}

.seller-address-info h4 {
    margin: 0 0 10px 0;
    color: #ff5000;
    font-size: 14px;
}

.seller-address-info p {
    margin: 0;
    color: #666;
    font-size: 13px;
    line-height: 1.6;
}
</style>
