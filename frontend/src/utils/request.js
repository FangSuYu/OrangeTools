import axios from 'axios'
import { ElMessage } from 'element-plus'
// 这里我们后面会创建 User Store 来拿 Token，现在先留个注释
// import { useUserStore } from '@/stores/modules/user'

const service = axios.create({
  // 自动读取刚才定义的 .env 文件里的地址
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000 // 请求超时时间 10s
})

// =================================
// 1. 请求拦截器 (Request Interceptor)
// =================================
service.interceptors.request.use(
  (config) => {
    // 以后在这里添加 Token
    // const userStore = useUserStore()
    // if (userStore.token) {
    //   config.headers['Authorization'] = 'Bearer ' + userStore.token
    // }
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
    // 这里的 data 就是后端返回的 Result 对象 { code, msg, data }
    const res = response.data

    // 如果是二进制数据 (比如下载文件)，直接返回
    if (response.config.responseType === 'blob' || response.config.responseType === 'arraybuffer') {
      return res
    }

    // 校验业务状态码 (我们约定 200 是成功)
    if (res.code === 200) {
      return res.data // 帮调用者剥离掉外壳，直接拿 result.data
    }

    // 如果 code 不是 200，说明业务出错 (比如用户名已存在)
    ElMessage.error(res.msg || '系统未知错误')
    return Promise.reject(new Error(res.msg || 'Error'))
  },
  (error) => {
    // 处理 HTTP 状态码错误 (404, 401, 500)
    console.log('err' + error)
    let msg = error.message
    if (error.response) {
      // 这里可以细化，比如 401 就是 token 过期，强制登出
      if (error.response.status === 401) {
        msg = '登录已过期，请重新登录'
        // TODO: 执行登出逻辑
      } else if (error.response.status === 500) {
        msg = '服务器内部错误'
      }
    }
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export default service
