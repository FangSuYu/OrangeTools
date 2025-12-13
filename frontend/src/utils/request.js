import axios from 'axios'
import { ElMessage } from 'element-plus'
// 【1. 解封引用】引入 User Store
import { useUserStore } from '@/stores/modules/user'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 30000
})

// =================================
// 1. 请求拦截器 (Request Interceptor)
// =================================
service.interceptors.request.use(
  (config) => {
    // 【2. 解封逻辑】每次发送请求前，自动带上 Token
    const userStore = useUserStore()
    if (userStore.token) {
      // 注意：后端 JwtFilter 里判断的是 "Bearer " 前缀 (有个空格)
      config.headers['Authorization'] = 'Bearer ' + userStore.token
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// =================================
// 2. 响应拦截器 (Response Interceptor)
// =================================
service.interceptors.response.use(
  (response) => {
    const res = response.data

    if (response.config.responseType === 'blob' || response.config.responseType === 'arraybuffer') {
      return res
    }

    if (res.code === 200) {
      return res.data
    }

    ElMessage.error(res.msg || '系统未知错误')
    return Promise.reject(new Error(res.msg || 'Error'))
  },
  (error) => {
    console.log('err' + error)
    let msg = error.message
    if (error.response) {
      if (error.response.status === 401) {
        msg = '登录已过期，请重新登录'
        // 【建议】这里可以加一行：自动登出并跳回登录页
        const userStore = useUserStore()
        userStore.logout()
        location.reload() // 可选：刷新页面
      } else if (error.response.status === 403) {
        msg = '无权访问 (403)'
      } else if (error.response.status === 500) {
        msg = '服务器内部错误'
      }
    }
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export default service
