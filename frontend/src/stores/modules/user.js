import { defineStore } from 'pinia'
import { ref } from 'vue'
// 引入我们之前封装的 API 和 Token 工具
import { login as loginApi, register as registerApi } from '@/api/auth'
import { setToken, getToken, removeToken } from '@/utils/auth'
import { ElMessage } from 'element-plus'

export const useUserStore = defineStore('user', () => {
  // 1. State: 存储 Token 和 用户信息
  const token = ref(getToken() || '') // 优先从 Cookie/LocalStorage 读取
  const username = ref('')
  const avatar = ref('')
  const roles = ref([])

  // 2. Action: 登录动作
  const login = async (loginForm) => {
    try {
      // 调用后端接口
      const res = await loginApi(loginForm)
      const accessToken = res.data // 假设后端直接返回 token 字符串

      // 存 Token
      token.value = accessToken
      setToken(accessToken)

      ElMessage.success('登录成功，欢迎回来！')
      return true
    } catch (error) {
      console.log(error)
      return false
    }
  }

  // 3. Action: 注册动作
  const register = async (registerForm) => {
    try {
      await registerApi(registerForm)
      ElMessage.success('注册成功，请登录')
      return true
    } catch (error) {
      console.log(error)
      return false
    }
  }

  // 4. Action: 登出
  const logout = () => {
    token.value = ''
    username.value = ''
    roles.value = []
    removeToken()
    // 这里以后可以加重置路由的逻辑
  }

  return {
    token,
    username,
    avatar,
    roles,
    login,
    register,
    logout
  }
}, {
  // 开启持久化 (刷新页面不丢失)
  persist: true
})
