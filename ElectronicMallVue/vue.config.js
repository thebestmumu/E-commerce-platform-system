/*
 * @Description: 
 * @Author: Rabbiter
 * @Date: 2023-03-26 15:27:05
 */
const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
  transpileDependencies: true,
  devServer: {
    port: 9193, // 端口
    proxy: {
      '/api': {
        target: 'http://localhost:9191',
        changeOrigin: true,
        pathRewrite: {
          '^/api': '/api'
        },
        // ✅ 关键配置：让 SSE 实时推送，禁用缓存
        xfwd: false,
        headers: {
          'Cache-Control': 'no-cache',
          'Connection': 'keep-alive',
        }
      }
    }
  },
})
