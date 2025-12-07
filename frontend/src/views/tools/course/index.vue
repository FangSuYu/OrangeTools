<script setup>
import { ref, computed } from 'vue'
import { UploadFilled, DataAnalysis, Delete, Search, Document, CopyDocument } from '@element-plus/icons-vue'
import { ElIcon, ElMessage, ElMessageBox } from 'element-plus'
import { analyzeCourse } from '@/api/tools/course'
import { useCourseStore } from '@/stores/modules/course'

const courseStore = useCourseStore()

// ================== 1. 状态定义 ==================
const loading = ref(false)
const uploadRef = ref(null)
const fileList = ref([])
const viewMode = ref('list')
const usageCount = ref(1284)
const currentWeek = ref(1)

// 【自定义】教程链接 (你可以替换成你真实的飞书文档链接)
const TUTORIAL_LINK = 'https://www.feishu.cn/'

const filters = ref({
  college: '',
  major: '',
  grade: ''
})

// ================== 2. 计算属性 ==================

// 防御性读取数据
const hasResult = computed(() => !!courseStore.analysisResult)

const rawResult = computed(() => {
  const res = courseStore.analysisResult
  if (res && Array.isArray(res.data)) {
    return res
  }
  return { allColleges: [], allMajors: [], allGrades: [], data: [], maxWeek: 20 }
})

const weekOptions = computed(() => {
  const max = rawResult.value.maxWeek || 20
  return Array.from({ length: max }, (_, i) => i + 1)
})

const filteredSchedule = computed(() => {
  if (!hasResult.value) return {}

  const result = {}
  const targetWeek = currentWeek.value

  for (let slot = 1; slot <= 10; slot++) {
    result[slot] = {}
    for (let day = 1; day <= 7; day++) {
      result[slot][day] = { freeCount: 0, freeStudents: [], busyStudents: [], busyRatio: 0 }
    }
  }

  const validSchedules = rawResult.value.data.filter(item => {
    const s = item.student
    if (filters.value.college && s.college !== filters.value.college) return false
    if (filters.value.major && s.major !== filters.value.major) return false
    if (filters.value.grade && s.grade !== filters.value.grade) return false
    return true
  })

  validSchedules.forEach(schedule => {
    const student = schedule.student
    const busySlots = new Set()
    schedule.courses.forEach(course => {
      if (course.busyWeeks.includes(targetWeek)) {
        busySlots.add(`${course.day}-${course.slot}`)
      }
    })

    for (let slot = 1; slot <= 10; slot++) {
      for (let day = 1; day <= 7; day++) {
        const key = `${day}-${slot}`
        const cell = result[slot][day]
        if (busySlots.has(key)) {
          cell.busyStudents.push(student)
        } else {
          cell.freeStudents.push(student)
        }
      }
    }
  })

  for (let slot = 1; slot <= 10; slot++) {
    for (let day = 1; day <= 7; day++) {
      const cell = result[slot][day]
      cell.freeCount = cell.freeStudents.length
      const total = cell.freeStudents.length + cell.busyStudents.length
      cell.busyRatio = total > 0 ? cell.busyStudents.length / total : 0
    }
  }

  return result
})

// ================== 3. 交互逻辑 ==================

const handleAnalyze = async () => {
  if (fileList.value.length === 0) {
    ElMessage.warning('请先选择 Excel 文件')
    return
  }
  loading.value = true
  try {
    const formData = new FormData()
    fileList.value.forEach(file => formData.append('files', file.raw))
    const res = await analyzeCourse(formData)
    courseStore.setAnalysisResult(res)
    ElMessage.success(`分析完成！已加载全学期数据`)
    fileList.value = []
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  courseStore.clearData()
  filters.value = { college: '', major: '', grade: '' }
  currentWeek.value = 1
}

// 颜色生成 (暗黑模式下，文字会自动反色，这里只控制背景)
const getCellColor = (ratio) => {
  if (ratio === 0) return 'rgba(0, 255, 0, 0.15)' // 降低一点不透明度，暗黑模式下更舒服
  const hue = (1 - ratio) * 120
  // 使用 HSLA，保持较高饱和度但适当透明度
  return `hsla(${hue}, 90%, 45%, 0.5)`
}

// 打开教程
const openTutorial = () => {
  window.open(TUTORIAL_LINK, '_blank')
}

// 【新增】复制单节课详情
const handleCopyCell = async (slot, day) => {
  const cell = filteredSchedule.value[slot]?.[day]
  if (!cell) return

  const weekMap = ['周一','周二','周三','周四','周五','周六','周日']
  const title = `【${weekMap[day-1]} 第${slot}节 空闲统计 (第${currentWeek.value}周)】`

  const freeNames = cell.freeStudents.map(s => s.name).join(', ') || '无'
  const busyNames = cell.busyStudents.map(s => s.name).join(', ') || '无'

  const content = `${title}
✅ 空闲(${cell.freeCount}人): ${freeNames}
❌ 忙碌(${cell.busyStudents.length}人): ${busyNames}`

  try {
    await navigator.clipboard.writeText(content)
    ElMessage.success('已复制该节课详情到剪贴板')
  } catch (err) {
    ElMessage.error('复制失败，请手动复制',err)
  }
}

// 【新增】复制整周详情
const handleCopyWeek = async () => {
  const weekMap = ['周一','周二','周三','周四','周五','周六','周日']
  let content = `📅 第${currentWeek.value}周 空闲统计报表\n------------------------\n`

  // 遍历每一天
  for (let day = 1; day <= 7; day++) {
    content += `\n【${weekMap[day-1]}】\n`
    let hasFree = false
    // 遍历每一节
    for (let slot = 1; slot <= 10; slot++) {
      const cell = filteredSchedule.value[slot]?.[day]
      // 只复制有空闲的时间段，防止刷屏
      if (cell && cell.freeCount > 0) {
        hasFree = true
        const names = cell.freeStudents.map(s => s.name).join(',')
        content += `第${slot}节(${cell.freeCount}人空闲): ${names}\n`
      }
    }
    if (!hasFree) content += `(全天全员忙碌)\n`
  }

  try {
    await navigator.clipboard.writeText(content)
    ElMessage.success('已复制本周完整报表！')
  } catch (err) {
    ElMessage.error('复制失败',err)
  }
}
</script>

<template>
  <div class="app-container">
    <div class="tool-header">
      <div class="header-content">
        <h1>📅 课表空闲统计助手</h1>
        <p class="desc">
          批量上传 Excel 课表，一键分析全员空闲时间。
          <el-link type="primary" class="tutorial-link" @click="openTutorial">
            <el-icon><Document /></el-icon> 查看使用教程
          </el-link>
        </p>
        <div class="stats-badge">🔥 已累计服务 <span>{{ usageCount }}</span> 人次</div>
      </div>
    </div>

    <transition name="el-zoom-in-center">
      <div v-if="!hasResult" class="upload-section card-box">
        <el-upload
          ref="uploadRef"
          v-model:file-list="fileList"
          class="upload-demo"
          drag
          multiple
          :auto-upload="false"
          accept=".xlsx, .xls"
        >
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">将 Excel 课表拖到此处，或 <em>点击上传</em></div>
          <template #tip><div class="el-upload__tip">支持批量上传，自动识别全学期课程</div></template>
        </el-upload>
        <div class="actions">
          <el-button type="primary" size="large" :loading="loading" @click="handleAnalyze" round>
            <el-icon><DataAnalysis /></el-icon>
            <span>开始全量分析</span>
          </el-button>
        </div>
      </div>
    </transition>

    <transition name="el-fade-in-linear">
      <div v-if="hasResult" class="result-section">
        <div class="filter-bar card-box">
          <div class="left-filters">
            <div class="week-selector">
              <span class="label">当前统计:</span>
              <el-select v-model="currentWeek" style="width: 110px" class="week-select">
                <el-option v-for="i in weekOptions" :key="i" :label="`第 ${i} 周`" :value="i" />
              </el-select>
            </div>

            <el-divider direction="vertical" />

            <el-select v-model="filters.college" placeholder="全部学院" clearable style="width: 140px">
              <el-option v-for="c in rawResult.allColleges" :key="c" :label="c" :value="c" />
            </el-select>
            <el-select v-model="filters.major" placeholder="全部专业" clearable style="width: 140px; margin-left: 10px">
              <el-option v-for="m in rawResult.allMajors" :key="m" :label="m" :value="m" />
            </el-select>
            <el-select v-model="filters.grade" placeholder="全部年级" clearable style="width: 120px; margin-left: 10px">
              <el-option v-for="g in rawResult.allGrades" :key="g" :label="g" :value="g" />
            </el-select>
          </div>

          <div class="right-actions">
            <el-button type="success" plain size="small" @click="handleCopyWeek" style="margin-right: 15px">
              <el-icon>
                <CopyDocument />
              </el-icon>
              <span>复制周报</span>
            </el-button>

            <el-radio-group v-model="viewMode" size="small">
              <el-radio-button label="list">名单</el-radio-button>
              <el-radio-button label="heatmap">热力</el-radio-button>
            </el-radio-group>
            <el-button type="danger" plain style="margin-left: 15px" @click="handleReset">
              <el-icon><Delete /></el-icon>
              <span>重置</span>
            </el-button>
          </div>
        </div>

        <div class="schedule-container card-box">
          <table class="course-table">
            <thead>
              <tr>
                <th style="width: 60px">节次</th>
                <th v-for="day in ['周一','周二','周三','周四','周五','周六','周日']" :key="day">{{ day }}</th>
              </tr>
            </thead>

            <transition name="fade" mode="out-in">
              <tbody :key="currentWeek">
                <tr v-for="slot in 10" :key="slot">
                  <td class="slot-idx">第 {{ slot }} 节</td>
                  <td v-for="day in 7" :key="day"
                      class="cell-wrapper"
                      :style="viewMode === 'heatmap' ? { backgroundColor: getCellColor(filteredSchedule[slot]?.[day]?.busyRatio) } : {}"
                      @click="handleCopyCell(slot, day)"
                      title="点击复制该节详情"
                  >
                    <template v-if="filteredSchedule[slot] && filteredSchedule[slot][day]">

                      <div v-if="viewMode === 'list'" class="mode-list">
                        <el-tooltip placement="top" :show-after="200" :hide-after="0" transition="none" :enterable="false">
                          <template #content>
                            <div class="tooltip-list">
                              <div v-for="s in filteredSchedule[slot][day].freeStudents" :key="s.name+s.code">
                                {{ s.name }} <span style="opacity:0.7">({{ s.major }})</span>
                              </div>
                              <div v-if="filteredSchedule[slot][day].freeCount === 0">全员有课</div>
                            </div>
                          </template>
                          <div class="cell-content">
                            <div class="count" :class="{ 'zero': filteredSchedule[slot][day].freeCount === 0 }">
                              空闲: {{ filteredSchedule[slot][day].freeCount }}人
                            </div>
                          </div>
                        </el-tooltip>
                      </div>

                      <div v-else class="mode-heatmap">
                        <div class="ratio-text" :style="{ opacity: filteredSchedule[slot][day].busyRatio > 0.5 ? 1 : 0.6 }">
                          {{ (filteredSchedule[slot][day].busyRatio * 100).toFixed(0) }}% 忙
                        </div>
                      </div>

                    </template>
                  </td>
                </tr>
              </tbody>
            </transition>
          </table>
        </div>
      </div>
    </transition>
  </div>
</template>

<style lang="scss" scoped>
/* 定义淡入淡出动画 (0.5s) */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.app-container { max-width: 1200px; margin: 0 auto; padding-bottom: 50px; }

.tool-header {
  text-align: center; margin-bottom: 30px;
  .header-content {
    h1 { margin-bottom: 10px; color: var(--text-color-primary); }
    .desc { color: var(--text-color-secondary); margin-bottom: 15px; }
    .tutorial-link { font-size: 14px; margin-left: 10px; vertical-align: baseline; }
    .stats-badge { display: inline-block; background: var(--el-color-primary-light-9); color: var(--el-color-primary); padding: 4px 12px; border-radius: 20px; font-size: 13px; font-weight: bold; span { font-size: 16px; } }
  }
}

/* 【暗黑模式修复】上传区域 */
.upload-section {
  padding: 40px; text-align: center;
  /* 确保上传框背景在暗黑模式下也是卡片背景 */
  :deep(.el-upload-dragger) {
    background-color: var(--bg-color-overlay);
    border-color: var(--border-color);
    &:hover { border-color: var(--el-color-primary); }
  }

  .actions {
    margin-top: 20px; display: flex; justify-content: center; align-items: center; gap: 15px;
    :deep(.el-button) { display: inline-flex !important; justify-content: center !important; align-items: center !important; span { margin-left: 5px; display: inline-flex; align-items: center; } .el-icon { margin-right: 0; } }
  }

  :deep(.el-upload-list) {
    max-height: 200px; overflow-y: auto; margin-top: 10px; text-align: left;
    border: 1px solid var(--border-color); border-radius: 4px; padding: 5px;
    display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 5px;

    /* 列表项暗黑模式适配 */
    .el-upload-list__item {
      background-color: var(--bg-color-page);
      color: var(--text-color-regular);
      &:hover { background-color: var(--bg-color-overlay); }
    }
  }
}

.filter-bar {
  margin-bottom: 20px; display: flex; justify-content: space-between; align-items: center;
  .left-filters { display: flex; align-items: center; .label { font-size: 14px; font-weight: bold; color: var(--text-color-regular); margin-right: 10px; } }
  .right-actions { display: flex; align-items: center; }
  .week-selector { display: flex; align-items: center; margin-right: 15px; .label { font-weight: bold; color: var(--el-color-primary); margin-right: 8px; font-size: 15px; } .week-select { :deep(.el-input__inner) { font-weight: bold; color: var(--el-color-primary); } } }
}

.course-table {
  width: 100%; border-collapse: collapse;
  th, td { border: 1px solid var(--border-color); text-align: center; padding: 8px; }
  th { background-color: var(--bg-color-page); color: var(--text-color-primary); font-weight: bold; height: 40px; }
  .slot-idx { background-color: var(--bg-color-page); color: var(--text-color-secondary); font-size: 12px; }

  .cell-wrapper {
    height: 60px; vertical-align: middle; cursor: pointer; transition: none;
    /* 【暗黑模式修复】热力图文字颜色自适应 */
    color: var(--text-color-primary);

    &:hover { outline: 2px solid var(--el-color-primary); z-index: 1; position: relative; }
    &:active { opacity: 0.8; } /* 点击反馈 */
  }

  .mode-list { cursor: help; .count { color: var(--el-color-success); font-weight: bold; &.zero { color: var(--text-color-secondary); font-weight: normal; } } }

  .mode-heatmap {
    .ratio-text {
      font-size: 12px; font-weight: bold;
      /* 文字描边，确保在深色或浅色热力图上都能看清 */
      text-shadow: 0 0 2px var(--bg-color-card);
      color: var(--text-color-primary);
    }
  }
}
.tooltip-list { max-height: 300px; overflow-y: auto; line-height: 1.8; }
</style>
