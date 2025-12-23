<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useUserStore } from '@/stores/modules/user'
import { updateProfile } from '@/api/user'
import { ElMessage } from 'element-plus'
import { User, Iphone, Message, Postcard } from '@element-plus/icons-vue'

const userStore = useUserStore()
const loading = ref(false)
const form = reactive({ nickname: '', studentId: '', phone: '', email: '' })

const initData = () => {
  form.nickname = userStore.nickname
  form.studentId = userStore.studentId
  form.phone = userStore.phone
  form.email = userStore.email
}

const onSubmit = async () => {
  loading.value = true
  try {
    await updateProfile({ ...form, avatar: userStore.avatar })
    await userStore.getInfo()
    ElMessage.success('资料保存成功')
  } catch (e) { console.error(e) } finally { loading.value = false }
}

onMounted(initData)
</script>

<template>
  <div class="form-container">
    <el-form label-position="top" :model="form" class="modern-form">
      <el-form-item label="用户昵称">
        <el-input v-model="form.nickname" placeholder="请输入你的昵称" size="large">
          <template #prefix><el-icon><User /></el-icon></template>
        </el-input>
      </el-form-item>

      <el-form-item label="绑定学号 (Student ID)">
        <el-input
          v-model="form.studentId"
          placeholder="请输入你的学号 (用于后续教务功能)"
          size="large"
          clearable
        >
          <template #prefix><el-icon><Postcard /></el-icon></template>
        </el-input>
      </el-form-item>

      <div class="row-group">
        <el-form-item label="手机号码" class="half">
          <el-input v-model="form.phone" placeholder="未绑定" size="large">
            <template #prefix><el-icon><Iphone /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="电子邮箱" class="half">
          <el-input v-model="form.email" placeholder="未绑定" size="large">
            <template #prefix><el-icon><Message /></el-icon></template>
          </el-input>
        </el-form-item>
      </div>

      <div class="action-bar">
        <el-button type="primary" :loading="loading" @click="onSubmit" size="large" round class="save-btn">
          保存更改
        </el-button>
      </div>
    </el-form>
  </div>
</template>

<style lang="scss" scoped>
.form-container {
  padding: 20px 0;
  max-width: 600px;
}

.row-group {
  display: flex; gap: 20px;
  .half { flex: 1; }
}

.action-bar {
  margin-top: 40px;
  text-align: right;
  .save-btn { padding: 12px 40px; font-weight: bold; background: linear-gradient(90deg, var(--el-color-primary), #a18cd1); border: none; transition: transform 0.2s; &:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(var(--el-color-primary-rgb), 0.4); } }
}

/* 覆盖 Element 输入框样式，使其更现代 */
:deep(.el-input__wrapper) {
  background-color: var(--el-fill-color-lighter);
  box-shadow: none !important;
  border: 1px solid transparent;
  transition: all 0.3s;
  &:hover, &.is-focus {
    background-color: var(--bg-color-card);
    border-color: var(--el-color-primary);
    box-shadow: 0 0 0 3px var(--el-color-primary-light-9) !important;
  }
}
</style>
