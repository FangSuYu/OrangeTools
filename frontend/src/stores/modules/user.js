import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, getUserInfo as getUserInfoApi } from '@/api/auth' // 确保你api里有这个
import { ElMessage } from 'element-plus'

export const useUserStore = defineStore('user', () => {
  const token = ref('')
  const username = ref('')
  const avatar = ref('')
  const role = ref('') // 用户角色

  // 登录 Action
  const login = async (loginForm) => {
    try {
      const res = await loginApi(loginForm)
      token.value = res // 存 Token

      // 【关键】登录成功后，立刻获取用户信息(包含角色)
      await getInfo()

      return true
    } catch (error) {
      return false
    }
  }

  // 获取用户信息 Action
  const getInfo = async () => {
    try {
      const res = await getUserInfoApi()
      username.value = res.username
      avatar.value = res.avatar
      // 【核心修复】强制转为小写，防止后端返回 ADMIN 导致前端判断失效
      role.value = res.role ? res.role.toLowerCase() : 'user'
    } catch (error) {
      console.error('获取用户信息失败', error)
    }
  }

  // 登出 Action
  const logout = () => {
    token.value = ''
    username.value = ''
    role.value = ''
    // 持久化插件会自动清理 LocalStorage
  }

  return {
    token,
    username,
    avatar,
    role,
    login,
    getInfo, // 导出这个方法，以便在页面刷新时也可以调用
    logout
  }
}, {
  persist: true
})
