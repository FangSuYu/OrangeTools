import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, getUserInfo as getUserInfoApi, register as registerApi, loginEmail as loginEmailApi } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref('')
  const username = ref('')
  const nickname = ref('')
  const studentId = ref('')
  const avatar = ref('') // 这里存的是数据库里的文件名，如 "avatar-1.png"
  const role = ref('')
  const phone = ref('')
  const email = ref('')

  // 【核心修复】计算属性：将文件名转换为真实路径
  const avatarUrl = computed(() => {
    // 1. 如果没头像，给个默认的
    if (!avatar.value) return 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'

    // 2. 如果是网络图片 (http开头)，直接返回
    if (avatar.value.startsWith('http')) return avatar.value

    // 3. 如果是本地预设图片，动态解析路径
    // 注意：这里假设你的图片都在 src/assets/images/avatars/ 下
    try {
      return new URL(`../../assets/images/avatars/${avatar.value}`, import.meta.url).href
    } catch (e) {
      return 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'
    }
  })

  const login = async (loginForm) => {
    try {
      const res = await loginApi(loginForm)
      token.value = res
      await getInfo()
      return true
    } catch (error) {
      return false
    }
  }

  const loginEmail = async (loginForm) => {
    try {
      const res = await loginEmailApi(loginForm)
      token.value = res
      await getInfo()
      return true
    } catch (error) {
      return false
    }
  }


  const register = async (registerForm) => {
    try {
      await registerApi(registerForm)
      return true
    } catch (error) {
      // 错误已经在 request.js 拦截器处理了，这里不需要弹窗
      return false
    }
  }

  const getInfo = async () => {
    try {
      const res = await getUserInfoApi()
      username.value = res.username
      nickname.value = res.nickname
      studentId.value = res.studentId
      avatar.value = res.avatar
      role.value = res.role ? res.role.toLowerCase() : 'user'
      phone.value = res.phone
      email.value = res.email
    } catch (error) {
      console.error('获取用户信息失败', error)
    }
  }

  const logout = () => {
    token.value = ''
    username.value = ''
    studentId.value = ''
    nickname.value = ''
    avatar.value = ''
    role.value = ''
  }

  return {
    token, username, nickname, studentId, avatar, role, phone, email,
    avatarUrl, // 【导出这个计算属性】
    login, loginEmail, register, getInfo, logout
  }
}, {
  persist: true
})
