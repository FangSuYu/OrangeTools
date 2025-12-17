<script setup>
import { ref, computed } from 'vue'
import { CopyDocument, Location, User, School, Monitor } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { copyText } from '@/utils/clipboard' // 复用你现有的工具

const props = defineProps({
  visible: Boolean,
  slotData: Object, // { freeStudents: [], busyStudents: [], detailsMap: {} }
  weekInfo: Object  // { week: 15, day: 2, slot: 3 }
})

const emit = defineEmits(['update:visible'])

const activeTab = ref('status') // status | location | class

// 辅助：周几转换
const weekMap = ['周一','周二','周三','周四','周五','周六','周日']

// 核心计算：分组逻辑
const groupedData = computed(() => {
  const { freeStudents, busyStudents } = props.slotData || { freeStudents: [], busyStudents: [] }
  const result = {}

  if (activeTab.value === 'status') {
    result['❌ 忙碌 (' + busyStudents.length + ')'] = busyStudents
    result['✅ 空闲 (' + freeStudents.length + ')'] = freeStudents
  }
  else if (activeTab.value === 'location') {
    // 按地点分组
    busyStudents.forEach(s => {
      // 尝试提取地点前缀 (中文)
      let locName = '其他地点'
      // 从学生对应的课程详情中找地点
      // 注意：这里需要 index.vue 传进来带有 details 的数据
      // 简化处理：假设 busyStudents 里的对象已经带了当节课的 courseDetail
      if (s.currentCourse && s.currentCourse.location) {
        // 提取中文前缀，例如 "文华楼"
        const match = s.currentCourse.location.match(/^[\u4e00-\u9fa5]+/)
        if (match) locName = match[0]
      }
      if (!result[locName]) result[locName] = []
      result[locName].push(s)
    })
    // 空闲的一组
    result['无课/空闲'] = freeStudents
  }
  else if (activeTab.value === 'class') {
    // 按班级分组
    const all = [...freeStudents, ...busyStudents]
    all.forEach(s => {
      const cls = s.className || '未分配班级'
      if (!result[cls]) result[cls] = []
      result[cls].push(s)
    })
  }

  return result
})

const handleClose = () => {
  emit('update:visible', false)
}

const handleCopy = async () => {
  // 生成文本报表
  let content = `📅 ${weekMap[props.weekInfo.day-1]} 第${props.weekInfo.slot}节 (第${props.weekInfo.week}周)\n`
  for (const group in groupedData.value) {
    content += `\n【${group}】\n`
    groupedData.value[group].forEach(s => {
      content += `${s.name}`
      if (s.status === 'busy') content += ` [${s.currentCourse?.name}]`
      content += `\n`
    })
  }
  try {
    await copyText(content)
    ElMessage.success('详情已复制')
  } catch (e) {
    ElMessage.error('复制失败')
  }
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="课程详情深度分析"
    width="600px"
    @close="handleClose"
    append-to-body
    destroy-on-close
  >
    <div class="dialog-header">
      <div class="info-tag">
        <span class="highlight">第 {{ weekInfo.week }} 周</span>
        <span class="sep">|</span>
        <span>{{ weekMap[weekInfo.day-1] }}</span>
        <span class="sep">|</span>
        <span>第 {{ weekInfo.slot }} 节</span>
      </div>

      <el-radio-group v-model="activeTab" size="small">
        <el-radio-button label="status">按状态</el-radio-button>
        <el-radio-button label="location">按地点</el-radio-button>
        <el-radio-button label="class">按班级</el-radio-button>
      </el-radio-group>
    </div>

    <div class="scroll-container">
      <div v-for="(list, groupName) in groupedData" :key="groupName" class="group-block">
        <div class="group-title">{{ groupName }}</div>
        <div class="card-grid">
          <div
            v-for="s in list"
            :key="s.name"
            class="student-card"
            :class="{ 'is-free': !s.currentCourse }"
          >
            <div class="card-top">
              <span class="name">{{ s.name }}</span>
              <span class="major">{{ s.major }}</span>
            </div>

            <div v-if="s.currentCourse" class="course-info">
              <div class="row"><el-icon><School /></el-icon> {{ s.currentCourse.name }}</div>
              <div class="row"><el-icon><User /></el-icon> {{ s.currentCourse.teacher }}</div>
              <div class="row"><el-icon><Location /></el-icon> {{ s.currentCourse.location }}</div>
              <div class="row"><el-icon><Monitor /></el-icon> {{ s.className || '暂无班级' }}</div>
              <div class="row" v-if="s.code"><el-icon><User /></el-icon> {{ s.code }}</div>
            </div>

            <div v-else class="basic-info">
              <div class="row"><el-icon><Monitor /></el-icon> {{ s.className || '暂无班级' }}</div>
              <div class="row" v-if="s.code"><el-icon><User /></el-icon> {{ s.code }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="handleClose">关闭</el-button>
      <el-button type="primary" @click="handleCopy">
        <el-icon><CopyDocument /></el-icon> 复制本页统计
      </el-button>
    </template>
  </el-dialog>
</template>

<style lang="scss" scoped>
.dialog-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; padding-bottom: 10px; border-bottom: 1px solid var(--border-color); }
.info-tag { font-size: 16px; font-weight: bold; .highlight { color: var(--el-color-primary); } .sep { margin: 0 8px; color: #ddd; } }
.scroll-container { max-height: 500px; overflow-y: auto; padding-right: 5px; }
.group-title { font-size: 14px; font-weight: bold; margin: 15px 0 10px; padding-left: 8px; border-left: 3px solid var(--el-color-primary); }
.card-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 10px; }
.student-card {
  border: 1px solid var(--border-color); border-radius: 4px; padding: 10px; font-size: 12px; background: var(--bg-color-overlay);
  &.is-free { border-left: 3px solid var(--el-color-success); }
  .card-top { display: flex; justify-content: space-between; margin-bottom: 8px; font-weight: bold; font-size: 13px; .major { font-weight: normal; color: #999; font-size: 12px; } }
  .row { display: flex; align-items: center; gap: 4px; margin-bottom: 2px; color: var(--text-color-secondary); text-overflow: ellipsis; white-space: nowrap; overflow: hidden; }
}
</style>
