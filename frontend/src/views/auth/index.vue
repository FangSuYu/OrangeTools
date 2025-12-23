<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/modules/user'
import { User, Lock, Message, Iphone, Key } from '@element-plus/icons-vue'
import { Vue3Lottie } from 'vue3-lottie'
import LoginJSON from '@/assets/login-animate.json'
import { ElMessage } from 'element-plus'
import { sendCode, resetPassword } from '@/api/auth'
import BeiAnFooter from '@/components/BeiAnFooter/index.vue'

const router = useRouter()
const userStore = useUserStore()

// 模式控制：login, register, forgot
const authMode = ref('login')
const loginType = ref('account') // account, email
const loading = ref(false)
const codeLoading = ref(false)

// Lottie 配置
const lottieOptions = {
  animationData: LoginJSON,
  loop: true,
  autoplay: true
}

// ============== 倒计时逻辑 ==============
const countdown = reactive({
  register: 0,
  login: 0,
  forgot: 0
})
let timer = null

const startCountdown = (type) => {
  countdown[type] = 60
  timer = setInterval(() => {
    countdown[type]--
    if (countdown[type] <= 0) {
      clearInterval(timer)
      countdown[type] = 0
    }
  }, 1000)
}

const handleSendCode = async (type, email) => {
  if (!email) {
    ElMessage.warning('请先输入邮箱')
    return
  }
  // 简单校验邮箱格式
  if (!/^\w+([-+.]\w+)*@\w+([-.]\w+)*\.\w+([-.]\w+)*$/.test(email)) {
    ElMessage.warning('邮箱格式不正确')
    return
  }

  try {
    codeLoading.value = true
    // type: REGISTER, LOGIN, RESET (后端需要的大写)
    await sendCode({ email, type: type.toUpperCase() })
    ElMessage.success('验证码已发送')
    startCountdown(type)
  } catch (error) {
    // 错误处理交由拦截器
  } finally {
    codeLoading.value = false
  }
}

// ============== 登录表单 (账号) ==============
const loginFormRef = ref(null)
const loginForm = reactive({
  username: '',
  password: ''
})
const loginRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

// ============== 登录表单 (邮箱) ==============
const emailLoginFormRef = ref(null)
const emailLoginForm = reactive({
  email: '',
  code: ''
})
const emailLoginRules = {
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

// 登录动作
const handleLogin = () => {
  const isAccount = loginType.value === 'account'
  const formRef = isAccount ? loginFormRef.value : emailLoginFormRef.value
  const action = isAccount ? userStore.login : userStore.loginEmail
  const data = isAccount ? loginForm : emailLoginForm

  formRef.validate(async (valid) => {
    if (valid) {
      loading.value = true
      const success = await action(data)
      loading.value = false
      if (success) {
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
  studentId: null,
  email: '',
  code: ''
})
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
  confirmPassword: [{ validator: validatePass2, trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] }
  ],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

const handleRegister = () => {
  registerFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      const success = await userStore.register(registerForm)
      loading.value = false
      if (success) {
        authMode.value = 'login'
        ElMessage.success('注册成功，请登录')
      }
    }
  })
}

// ============== 忘记密码表单 ==============
const forgotFormRef = ref(null)
const forgotForm = reactive({
  email: '',
  code: '',
  newPassword: '',
  confirmNewPassword: ''
})
const validateNewPass2 = (rule, value, callback) => {
  if (value !== forgotForm.newPassword) {
    callback(new Error('两次输入密码不一致!'))
  } else {
    callback()
  }
}
const forgotRules = {
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }],
  confirmNewPassword: [{ validator: validateNewPass2, trigger: 'blur' }]
}

const handleReset = () => {
  forgotFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await resetPassword(forgotForm)
        ElMessage.success('密码重置成功，请重新登录')
        authMode.value = 'login'
      } catch (e) {
        // error
      } finally {
        loading.value = false
      }
    }
  })
}

// 标题和副标题
const titleText = computed(() => {
  if (authMode.value === 'login') return '欢迎回来'
  if (authMode.value === 'register') return '创建账户'
  return '重置密码'
})
const subtitleText = computed(() => {
  if (authMode.value === 'login') return '请登录您的账号'
  if (authMode.value === 'register') return '填写以下信息完成注册'
  return '通过邮箱验证找回密码'
})

// 未开发功能提示
const showNotDevelopedMessage = () => {
  ElMessage.warning('该功能还未开发')
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
          <h2>{{ titleText }}</h2>
          <p class="subtitle">{{ subtitleText }}</p>
        </div>

        <!-- 登录模块 -->
        <div v-if="authMode === 'login'" class="form-content fade-in">
          <el-tabs v-model="loginType" class="login-tabs">
            <el-tab-pane label="账号登录" name="account">
              <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" size="large">
                <el-form-item prop="username">
                  <el-input v-model="loginForm.username" placeholder="账号" :prefix-icon="User" />
                </el-form-item>
                <el-form-item prop="password">
                  <el-input v-model="loginForm.password" type="password" show-password placeholder="密码" :prefix-icon="Lock"
                    @keyup.enter="handleLogin" />
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <el-tab-pane label="邮箱登录" name="email">
              <el-form ref="emailLoginFormRef" :model="emailLoginForm" :rules="emailLoginRules" size="large">
                <el-form-item prop="email">
                  <el-input v-model="emailLoginForm.email" placeholder="请输入邮箱" :prefix-icon="Message" />
                </el-form-item>
                <el-form-item prop="code">
                  <el-input v-model="emailLoginForm.code" placeholder="验证码" :prefix-icon="Key" @keyup.enter="handleLogin">
                    <template #append>
                      <el-button :loading="codeLoading" :disabled="countdown.login > 0" @click="handleSendCode('login', emailLoginForm.email)">
                        {{ countdown.login > 0 ? `${countdown.login}s后重试` : '获取验证码' }}
                      </el-button>
                    </template>
                  </el-input>
                </el-form-item>
              </el-form>
            </el-tab-pane>
          </el-tabs>

          <div class="actions-row">
            <span class="link-btn" @click="authMode = 'forgot'">忘记密码？</span>
          </div>

          <el-button type="primary" :loading="loading" class="submit-btn" @click="handleLogin" round>
            立即登录
          </el-button>

          <!-- 第三方登录预留 (仅展示，无功能) -->
          <div class="social-login">
            <div class="divider"><span>其他登录方式</span></div>
            <div class="icons">
              <!-- 这里预留QQ、微信、手机号图标，目前仅用文字或Emoji代替，或者用Element图标 -->
              <div class="icon-item" title="微信登录 (待开发)" @click="showNotDevelopedMessage">
                <span style="color: #07c160; font-weight: bold;">WeChat</span>
              </div>
              <div class="icon-item" title="QQ登录 (待开发)" @click="showNotDevelopedMessage">
                <span style="color: #1296db; font-weight: bold;">QQ</span>
              </div>
              <div class="icon-item" title="手机登录 (待开发)" @click="showNotDevelopedMessage">
                <el-icon><Iphone /></el-icon>
              </div>
            </div>
          </div>

          <div class="form-footer">
            <span>还没有账号？</span>
            <span class="link-btn" @click="authMode = 'register'">去注册</span>
          </div>
        </div>

        <!-- 注册模块 -->
        <div v-else-if="authMode === 'register'" class="form-content fade-in">
          <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" size="large">
            <el-form-item prop="username">
              <el-input v-model="registerForm.username" placeholder="设置账号" :prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="studentId">
              <el-input v-model="registerForm.studentId" placeholder="学号 (选填)" :prefix-icon="Iphone" />
            </el-form-item>
            <el-form-item prop="email">
              <el-input v-model="registerForm.email" placeholder="邮箱" :prefix-icon="Message" />
            </el-form-item>
            <el-form-item prop="code">
              <el-input v-model="registerForm.code" placeholder="验证码" :prefix-icon="Key">
                <template #append>
                  <el-button :loading="codeLoading" :disabled="countdown.register > 0" @click="handleSendCode('register', registerForm.email)">
                    {{ countdown.register > 0 ? `${countdown.register}s后重试` : '获取验证码' }}
                  </el-button>
                </template>
              </el-input>
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
            <span class="link-btn" @click="authMode = 'login'">去登录</span>
          </div>
        </div>

        <!-- 忘记密码模块 -->
        <div v-else-if="authMode === 'forgot'" class="form-content fade-in">
          <el-form ref="forgotFormRef" :model="forgotForm" :rules="forgotRules" size="large">
            <el-form-item prop="email">
              <el-input v-model="forgotForm.email" placeholder="请输入注册邮箱" :prefix-icon="Message" />
            </el-form-item>
            <el-form-item prop="code">
              <el-input v-model="forgotForm.code" placeholder="验证码" :prefix-icon="Key">
                <template #append>
                  <el-button :loading="codeLoading" :disabled="countdown.forgot > 0" @click="handleSendCode('reset', forgotForm.email)">
                    {{ countdown.forgot > 0 ? `${countdown.forgot}s后重试` : '获取验证码' }}
                  </el-button>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item prop="newPassword">
              <el-input v-model="forgotForm.newPassword" type="password" show-password placeholder="新密码"
                :prefix-icon="Lock" />
            </el-form-item>
            <el-form-item prop="confirmNewPassword">
              <el-input v-model="forgotForm.confirmNewPassword" type="password" show-password placeholder="确认新密码"
                :prefix-icon="Lock" />
            </el-form-item>
            <el-button type="primary" :loading="loading" class="submit-btn" @click="handleReset" round>
              重置密码
            </el-button>
          </el-form>

          <div class="form-footer">
            <span class="link-btn" @click="authMode = 'login'">返回登录</span>
          </div>
        </div>

      </div>
    </div>
    <BeiAnFooter class="login-beian" />
  </div>
</template>

<style lang="scss" scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: var(--bg-gradient-login);
  padding: 20px;
  position: relative;
}

/* 针对登录页的特殊定位和颜色重写 */
.login-beian {
  position: absolute; /* 绝对定位到底部 */
  bottom: 10px;
  z-index: 10;

  /* 样式穿透：修改组件内部 a 标签颜色 */
  :deep(a) {
    color: #909399; /* 或者 #eee 如果背景很深 */
    text-shadow: 0 1px 1px rgba(0,0,0,0.1);

    &:hover {
      color: #fff; /* 登录页悬停变白 */
    }
  }
}

.login-box {
  width: 1000px;
  // height: 600px; // remove fixed height to adapt content
  min-height: 600px;
  background-color: var(--bg-color-card);
  border-radius: 16px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  display: flex;
  overflow: hidden;

  @media (max-width: 768px) {
    width: 100%;
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
    filter: drop-shadow(0 10px 10px rgba(0, 0, 0, 0.2));
  }
}

.login-right {
  width: 50%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 40px 60px; // Increased padding
  background-color: var(--bg-color-card);

  .form-header {
    margin-bottom: 20px;

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

  .login-tabs {
    margin-bottom: 10px;
    :deep(.el-tabs__nav-wrap::after) {
      height: 1px;
    }
  }

  .actions-row {
    display: flex;
    justify-content: flex-end;
    margin-bottom: 15px;
    font-size: 14px;

    .link-btn {
      color: var(--text-color-secondary);
      cursor: pointer;
      &:hover {
        color: var(--el-color-primary);
      }
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

  .social-login {
    margin-top: 30px;
    .divider {
      display: flex;
      align-items: center;
      color: var(--text-color-placeholder);
      font-size: 12px;
      margin-bottom: 15px;
      &::before, &::after {
        content: '';
        flex: 1;
        height: 1px;
        background: var(--border-color-light);
      }
      span {
        padding: 0 10px;
      }
    }
    .icons {
      display: flex;
      justify-content: center;
      gap: 20px;
      .icon-item {
        cursor: pointer;
        opacity: 0.6;
        transition: all 0.3s;
        display: flex;
        align-items: center;
        &:hover {
          opacity: 1;
          transform: scale(1.1);
        }
      }
    }
  }
}

.fade-in {
  animation: fadeIn 0.4s ease-in-out;
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
