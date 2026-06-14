import axios from 'axios'
import ElementUI from "element-ui";
import router from '../router'

const request = axios.create({
    baseURL: 'http://localhost:9191',  // 注意！！ 这里是全局统一加上了 '/api' 前缀，也就是说所有接口都会加上'/api'前缀在，页面里面写接口的时候就不要加 '/api'了，否则会出现2个'/api'，类似 '/api/api/user'这样的报错，切记！！！
    timeout: 50000
})

// request 拦截器
// 可以自请求发送前对请求做一些处理
// 比如统一加 token，对请求参数统一加密
request.interceptors.request.use(config => {
    config.headers['Content-Type'] = 'application/json;charset=utf-8';
    
    // 优先检查客服登录信息
    const serviceUserStr = localStorage.getItem("serviceUser")
    if(serviceUserStr){
        try {
            const serviceUser = JSON.parse(serviceUserStr)
            if(serviceUser && serviceUser.token){
                config.headers['token'] = serviceUser.token;  // 设置客服 token
                config.headers['serviceId'] = serviceUser.userId;  // 设置 serviceId
            }
        } catch(e) {
            console.error('解析客服信息失败:', e)
        }
    } else {
        // 否则检查普通用户登录信息
        const userStr = localStorage.getItem("user")
        if(userStr){
            try {
                const user = JSON.parse(userStr)
                if(user && user.token){
                    config.headers['token'] = user.token;  // 设置用户 token
                    config.headers['userId'] = user.id;    // 设置 userId
                }
            } catch(e) {
                console.error('解析用户信息失败:', e)
            }
        }
    }
    
    return config
}, error => {
    return Promise.reject(error)
});

// response 拦截器
// 可以在接口响应后统一处理结果
import "@/utils/initialize"
request.interceptors.response.use(
    response => {
        let res = response.data;
        // 如果是返回的文件
        if (response.config.responseType === 'blob') {
            return res
        }
        // 兼容服务端返回的字符串数据
        if (typeof res === 'string') {
            res = res ? JSON.parse(res) : res
        }
        // 发生错误，如 token 失效，则返回登录
        if(res.code === '401' || res.code === '402'){
            // 检查是否是客服账号
            const serviceUser = localStorage.getItem('serviceUser')
            if(serviceUser){
                ElementUI.MessageBox({
                    title: '错误',
                    message: res.msg
                }).then(() =>{
                    router.push('/service-login')
                })
            } else {
                ElementUI.MessageBox({
                    title: '错误',
                    message: res.msg
                }).then(() =>{
                    router.push('/login')
                })
            }
        }
        return res;
    },
    error => {
        console.log('err' + error) // for debug
        return Promise.reject(error)
    }
)


export default request

