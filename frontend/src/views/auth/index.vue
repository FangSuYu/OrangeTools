<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/modules/user'
import { User, Lock, Message, Iphone } from '@element-plus/icons-vue'
// 引入 Lottie 组件
import { Vue3Lottie } from 'vue3-lottie'
// 引入刚才下载的动画 JSON
import LoginJSON from '@/assets/login-animate.json'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

// 状态控制：true显示登录，false显示注册
const isLogin = ref(true)
const loading = ref(false)

// Lottie 配置
const lottieOptions = {
  animationData: LoginJSON,
  loop: true,
  autoplay: true
}

// ============== 登录表单 ==============
const loginFormRef = ref(null)
const loginForm = reactive({
  username: '',
  password: ''
})
const loginRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

// 登录动作
const handleLogin = () => {
  loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      // 调用 Pinia 的 action
      const success = await userStore.login(loginForm)
      loading.value = false
      if (success) {
        // 登录成功，跳转首页
        router.push('/')
      }
    }
  })
}

// ============== 注册表单 ==============
const registerFormRef = ref(null)
const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  studentId: null
})
// 自定义校验：两次密码是否一致
const validatePass2 = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== registerForm.password) {
    callback(new Error('两次输入密码不一致!'))
  } else {
    callback()
  }
}
const registerRules = {
  username: [{ required: true, message: '请输入注册账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  confirmPassword: [{ validator: validatePass2, trigger: 'blur' }]
}

// 注册动作
const handleRegister = () => {
  registerFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      // 调用 Pinia 的 action (注意：这里假设 store 里有 register action)
      const success = await userStore.register(registerForm)
      loading.value = false
      if (success) {
        // 注册成功，切换回登录面板，让用户登录
        isLogin.value = true
        ElMessage.success('注册成功，请登录')
      }
    }
  })
}
</script>

<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-left">
        <div class="brand">
          <div style="font-size: 64px; margin-bottom: 20px;">🍊</div>
          <h1>OrangeTools</h1>
          <p>让校园生活更简单的开源工具箱</p>
        </div>
        <div class="animation-wrapper">
          <Vue3Lottie :animationData="LoginJSON" :height="300" :width="300" />
        </div>
      </div>

      <div class="login-right">
        <div class="form-header">
          <h2>{{ isLogin ? '欢迎回来' : '创建账户' }}</h2>
          <p class="subtitle">
            {{ isLogin ? '请使用您的账号登录' : '填写以下信息完成注册' }}
          </p>
        </div>

        <div v-if="isLogin" class="form-content fade-in">
          <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" size="large">
            <el-form-item prop="username">
              <el-input v-model="loginForm.username" placeholder="账号 / 学号 / 手机号" :prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="loginForm.password" type="password" show-password placeholder="密码" :prefix-icon="Lock"
                @keyup.enter="handleLogin" />
            </el-form-item>
            <el-button type="primary" :loading="loading" class="submit-btn" @click="handleLogin" round>
              立即登录
            </el-button>
          </el-form>

          <div class="form-footer">
            <span>还没有账号？</span>
            <span class="link-btn" @click="isLogin = false">去注册</span>
          </div>
        </div>

        <div v-else class="form-content fade-in">
          <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" size="large">
            <el-form-item prop="username">
              <el-input v-model="registerForm.username" placeholder="设置账号" :prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="studentId">
              <el-input v-model="registerForm.studentId" placeholder="学号 (选填, 用于教务功能)" :prefix-icon="Iphone" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="registerForm.password" type="password" show-password placeholder="设置密码"
                :prefix-icon="Lock" />
            </el-form-item>
            <el-form-item prop="confirmPassword">
              <el-input v-model="registerForm.confirmPassword" type="password" show-password placeholder="确认密码"
                :prefix-icon="Lock" />
            </el-form-item>
            <el-button type="primary" :loading="loading" class="submit-btn" @click="handleRegister" round>
              立即注册
            </el-button>
          </el-form>

          <div class="form-footer">
            <span>已有账号？</span>
            <span class="link-btn" @click="isLogin = true">去登录</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  /* 使用毛玻璃质感的背景图，或者简单的渐变 */
  background: linear-gradient(135deg, var(--bg-color-page) 0%, var(--el-color-primary-light-9) 100%);
  padding: 20px;
}

.login-box {
  width: 1000px;
  height: 600px;
  background-color: var(--bg-color-card);
  border-radius: 16px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  display: flex;
  overflow: hidden;

  /* 响应式：小屏幕变竖排或隐藏左侧 */
  @media (max-width: 768px) {
    width: 100%;
    height: auto;
    flex-direction: column;

    .login-left {
      display: none;
    }

    .login-right {
      width: 100%;
      padding: 40px 20px;
    }
  }
}

.login-left {
  width: 50%;
  background: linear-gradient(135deg, var(--el-color-primary) 0%, var(--el-color-primary-dark-2) 100%);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #fff;
  position: relative;

  .brand {
    text-align: center;
    margin-bottom: 40px;

    h1 {
      font-size: 32px;
      margin: 10px 0;
      font-weight: bold;
    }

    p {
      font-size: 16px;
      opacity: 0.9;
    }
  }

  .animation-wrapper {
    /* 让动画稍微浮动一点 */
    filter: drop-shadow(0 10px 10px rgba(0, 0, 0, 0.2));
  }
}

.login-right {
  width: 50%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 0 60px;
  background-color: var(--bg-color-card);

  .form-header {
    margin-bottom: 30px;

    h2 {
      font-size: 28px;
      color: var(--text-color-primary);
      margin-bottom: 10px;
    }

    .subtitle {
      color: var(--text-color-secondary);
      font-size: 14px;
    }
  }

  .submit-btn {
    width: 100%;
    margin-top: 10px;
    font-weight: bold;
    height: 45px;
  }

  .form-footer {
    margin-top: 20px;
    text-align: center;
    font-size: 14px;
    color: var(--text-color-regular);

    .link-btn {
      color: var(--el-color-primary);
      cursor: pointer;
      margin-left: 5px;
      font-weight: 600;

      &:hover {
        text-decoration: underline;
      }
    }
  }
}

/* 简单的淡入动画 */
.fade-in {
  animation: fadeIn 0.5s ease-in-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
