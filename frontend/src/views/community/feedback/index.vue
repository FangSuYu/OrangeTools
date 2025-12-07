<script setup>
import { ref, onMounted } from 'vue'
import { getPublicFeedbacks, submitFeedback } from '@/api/community'
import { Plus, ChatLineSquare } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const list = ref([])

// 表单相关
const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref(null)
const form = ref({
  type: 'suggestion',
  title: '',
  content: '',
  contact: '',
  isPublicCheck: true // 默认勾选
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入详细建议', trigger: 'blur' }]
}

// 获取列表
const fetchData = async () => {
  loading.value = true
  try {
    const res = await getPublicFeedbacks()
    list.value = res
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 提交反馈
const handleSubmit = () => {
  formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        await submitFeedback(form.value)
        ElMessage.success('提交成功！')
        dialogVisible.value = false
        // 刷新列表
        fetchData()
        // 重置表单
        form.value = { type: 'suggestion', title: '', content: '', contact: '', isPublicCheck: true }
      } catch (error) {
        // error
      } finally {
        submitLoading.value = false
      }
    }
  })
}

const getStatusTag = (status) => {
  // 既然现在默认都是公开，其实这个Tag也可以简化，不过留着也无妨，显得正式
  switch (status) {
    case 1: return { type: 'success', text: '已公开' }
    case 2: return { type: 'primary', text: '开发中' }
    case 3: return { type: 'warning', text: '已上线' }
    default: return { type: 'info', text: '待审核' }
  }
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="app-container">
    <div class="feedback-header card-box">
      <div class="left">
        <h2>🎋 需求许愿墙</h2>
        <p>你的每一个想法，都可能成为 OrangeTools 的下一个功能。</p>
      </div>
      <div class="right">
        <el-button type="primary" size="large" round icon="Plus" @click="dialogVisible = true">
          我要提建议
        </el-button>
      </div>
    </div>

    <div class="content-wrapper">
      <div v-loading="loading" class="feedback-list">
        <el-empty v-if="list.length === 0" description="暂时还没有需求，快来抢沙发！" />

        <transition-group name="list">
          <div v-for="item in list" :key="item.id" class="feedback-card card-box">
            <div class="card-header">
              <div class="badges">
                <el-tag :type="item.type === 'bug' ? 'danger' : 'success'" size="small" effect="dark">
                  {{ item.type === 'bug' ? 'BUG反馈' : '功能建议' }}
                </el-tag>
                <el-tag :type="getStatusTag(item.status).type" size="small" effect="plain" style="margin-left: 8px">
                  {{ getStatusTag(item.status).text }}
                </el-tag>
              </div>
              <span class="time">{{ item.createTime?.replace('T', ' ') }}</span>
            </div>

            <h3 class="title">{{ item.title }}</h3>
            <p class="content">{{ item.content }}</p>

            </div>
        </transition-group>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" title="提交反馈 / 建议" width="500px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="反馈类型">
          <el-radio-group v-model="form.type">
            <el-radio label="suggestion">💡 功能建议</el-radio>
            <el-radio label="bug">🐛 BUG 反馈</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="简明扼要的标题" />
        </el-form-item>
        <el-form-item label="详细描述" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="4" placeholder="详细描述..." />
        </el-form-item>
        <el-form-item label="联系方式 (选填)">
          <el-input v-model="form.contact" placeholder="QQ / 微信" />
        </el-form-item>
        <!-- <el-form-item>
          <el-checkbox v-model="form.isPublicCheck">
            我同意公开此建议 (勾选后将立即展示在列表)
          </el-checkbox>
        </el-form-item> -->
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">提交</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
.app-container { max-width: 1000px; margin: 0 auto; padding-bottom: 50px; }
.feedback-header { padding: 30px; display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; background-image: linear-gradient(120deg, var(--bg-color-card) 0%, var(--el-color-primary-light-9) 100%); h2 { margin: 0 0 10px 0; color: var(--text-color-primary); } p { margin: 0; color: var(--text-color-secondary); } }
.feedback-list { display: flex; flex-direction: column; gap: 15px; }
.feedback-card {
  padding: 20px; transition: transform 0.2s, box-shadow 0.2s;
  &:hover { transform: translateY(-2px); box-shadow: 0 4px 12px var(--shadow-color); }
  .card-header { display: flex; justify-content: space-between; margin-bottom: 12px; .time { font-size: 12px; color: var(--text-color-secondary); } }
  .title { margin: 0 0 8px 0; font-size: 18px; color: var(--text-color-primary); }
  .content { color: var(--text-color-regular); font-size: 14px; line-height: 1.6; white-space: pre-wrap; }
}
.list-enter-active, .list-leave-active { transition: all 0.5s ease; } .list-enter-from, .list-leave-to { opacity: 0; transform: translateX(30px); }
</style>
