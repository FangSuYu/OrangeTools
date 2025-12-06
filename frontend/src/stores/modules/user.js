import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, register as registerApi } from '@/api/auth'
import { ElMessage } from 'element-plus'

export const useUserStore = defineStore('user', () => {
  // 1. State
  // 这里的初始值可以为空，因为 persist 插件会在初始化时自动从 LocalStorage 恢复数据
  const token = ref('')
  const username = ref('')
  const avatar = ref('')
  const roles = ref([])

  // 2. Action: 登录
  const login = async (loginForm) => {
    try {
      // 调用后端接口
      // 【关键修复】request.js 已经解包了 data，所以这里的 res 就是 token 字符串
      const res = await loginApi(loginForm)

      // 存入 Pinia State
      token.value = res

      // 我们稍微假装设置一下用户名 (因为现在登录接口还没返回用户信息，先用登录名代替显示)
      username.value = loginForm.username

      ElMessage.success('登录成功，欢迎回来！')
      return true
    } catch (error) {
      console.error(error)
      return false
    }
  }

  // 3. Action: 注册
  const register = async (registerForm) => {
    try {
      await registerApi(registerForm)
      ElMessage.success('注册成功，请登录')
      return true
    } catch (error) {
      console.error(error)
      return false
    }
  }

  // 4. Action: 登出
  const logout = () => {
    token.value = ''
    username.value = ''
    roles.value = []
    // Pinia 持久化插件会自动更新 LocalStorage，这里不需要手动删
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
  // 开启持久化
  // 这会将整个 store 的数据自动保存到 key 为 "user" 的 LocalStorage 中
  persist: true
})
