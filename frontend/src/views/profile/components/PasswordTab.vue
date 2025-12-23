<script setup>
import { ref, reactive } from 'vue'
import { updatePassword } from '@/api/user'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/modules/user'
import { useRouter } from 'vue-router'

const userStore = useUserStore()
const router = useRouter()
const loading = ref(false)
const formRef = ref(null)

const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 自定义校验规则：确认密码必须与新密码一致
const validateConfirm = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入新密码'))
  } else if (value !== form.newPassword) {
    callback(new Error('两次输入密码不一致!'))
  } else {
    callback()
  }
}

const rules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [{ validator: validateConfirm, trigger: 'blur' }]
}

const onSubmit = () => {
  formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await updatePassword({
          oldPassword: form.oldPassword,
          newPassword: form.newPassword
        })
        ElMessage.success('密码修改成功，请重新登录')

        // 密码修改后强制登出
        userStore.logout()
        router.push('/login')

      } catch (e) {
        // 错误已在 request.js 中处理
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<template>
  <div class="form-container">
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="modern-form">
      <el-form-item label="当前密码" prop="oldPassword">
        <el-input v-model="form.oldPassword" type="password" show-password placeholder="验证身份" size="large" />
      </el-form-item>

      <el-form-item label="新密码" prop="newPassword">
        <el-input v-model="form.newPassword" type="password" show-password placeholder="6-20位字符" size="large" />
      </el-form-item>

      <el-form-item label="确认新密码" prop="confirmPassword">
        <el-input v-model="form.confirmPassword" type="password" show-password placeholder="再次输入" size="large" />
      </el-form-item>

      <div class="action-bar">
        <el-button type="primary" :loading="loading" @click="onSubmit" size="large" round class="save-btn">确认修改</el-button>
      </div>
    </el-form>
  </div>
</template>

<style lang="scss" scoped>
/* 同样复制 InfoTab 里的 style */
.form-container { padding: 20px 0; max-width: 600px; }
.action-bar { margin-top: 40px; text-align: right; .save-btn { padding: 12px 40px; font-weight: bold; background: linear-gradient(90deg, #ff9a9e, #fad0c4); border: none; color: #fff; &:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(255, 154, 158, 0.4); } } }
:deep(.el-input__wrapper) { background-color: var(--el-fill-color-lighter); box-shadow: none !important; border: 1px solid transparent; &:hover, &.is-focus { background-color: var(--bg-color-card); border-color: var(--el-color-primary); box-shadow: 0 0 0 3px var(--el-color-primary-light-9) !important; } }
</style>
