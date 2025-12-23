<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { UploadFilled, DataAnalysis, Delete, Document, CopyDocument, Edit, ZoomIn } from '@element-plus/icons-vue'
import { ElIcon, ElMessage } from 'element-plus'
import { analyzeCourse } from '@/api/tools/course'
import { useCourseStore } from '@/stores/modules/course'
import { getToolStats, reportToolUsage } from '@/api/community'
import CountUp from 'vue-countup-v3'
import { copyText } from '@/utils/clipboard'
import CourseDetailDialog from './components/CourseDetailDialog.vue'

const courseStore = useCourseStore()

// ================== 移动端缩放适配逻辑 ==================
const adapterContainer = ref(null)
const scale = ref(1)
const isMobile = ref(false)

const contentStyle = computed(() => {
  if (!isMobile.value) return {}
  return {
    width: '1200px', // 强制桌面宽度
    transform: `scale(${scale.value})`,
    transformOrigin: 'top left',
    height: `${100 / scale.value}%` // 补偿缩放导致的高度减少，确保占满容器
  }
})

const updateScale = () => {
  const width = window.innerWidth
  // AppMain 左右有 padding (移动端 10px * 2 = 20px)
  // 实际上在这个组件里，我们能拿到的宽度是 container 的宽度
  // 为了保险，我们直接用 window.innerWidth 减去预估的 padding
  const availableWidth = width - 20 
  if (availableWidth < 1200) {
    isMobile.value = true
    scale.value = availableWidth / 1200
  } else {
    isMobile.value = false
    scale.value = 1
  }
}

onMounted(() => {
  updateScale()
  window.addEventListener('resize', updateScale)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateScale)
})

// ================== 1. 状态定义 ==================
const loading = ref(false)
const uploadRef = ref(null)
const fileList = ref([])
const viewMode = ref('list')
const usageCount = ref(0)// 默认为 0，等待加载
const currentWeek = ref(1)
const editInfoVisible = ref(false) // 控制"完善信息"弹窗
const detailVisible = ref(false)   // 控制"课程详情"弹窗
const detailData = ref({})         // 传给详情弹窗的数据
const detailWeekInfo = ref({})     // 传给详情弹窗的周次信息
const inputText = ref('')          // 粘贴的名单文本

// 【自定义】教程链接 (你可以替换成你真实的飞书文档链接)
const TUTORIAL_LINK = 'https://ai.feishu.cn/docx/WYp1dER7AoWzCox9viDcXaCYnAb?from=from_copylink'

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

  // 初始化空壳
  for (let slot = 1; slot <= 10; slot++) {
    result[slot] = {}
    for (let day = 1; day <= 7; day++) {
      result[slot][day] = { freeCount: 0, freeStudents: [], busyStudents: [], busyRatio: 0 }
    }
  }

  // 筛选学生
  const validSchedules = rawResult.value.data.filter(item => {
    const s = item.student
    if (filters.value.college && s.college !== filters.value.college) return false
    if (filters.value.major && s.major !== filters.value.major) return false
    if (filters.value.grade && s.grade !== filters.value.grade) return false
    return true
  })

  validSchedules.forEach(schedule => {
    // 【重要】我们不直接把 schedule.student 塞进去，而是克隆一份
    // 这样我们可以给这个临时对象加一个 'currentCourse' 属性，而不污染原始数据
    // 如果不克隆，你在第3节课加了属性，第4节课该学生身上还会有这个属性

    // 这里先预处理：找出该学生本周所有忙碌的课程
    // map结构：key="day-slot", value=CourseDetail
    const busyMap = new Map()

    schedule.courses.forEach(course => {
      // 如果这门大课包含当前周
      if (course.busyWeeks.includes(targetWeek)) {
        const key = `${course.day}-${course.slot}`
        // 【新增】尝试从 courseDetails 里找到具体是哪一个小课 (根据周次匹配)
        let specificDetail = null
        if (course.courseDetails && course.courseDetails.length > 0) {
          specificDetail = course.courseDetails.find(d => d.weeks && d.weeks.includes(targetWeek))
        }
        // 存入 map，如果没有具体 detail，存个占位符也好，或者就依赖外层的 busyWeeks
        busyMap.set(key, specificDetail || { name: '未知课程', location: '', teacher: '' })
      }
    })

    // 填充 7x10 网格
    for (let slot = 1; slot <= 10; slot++) {
      for (let day = 1; day <= 7; day++) {
        const key = `${day}-${slot}`
        const cell = result[slot][day]

        // 克隆学生基础信息
        const studentObj = { ...schedule.student }

        if (busyMap.has(key)) {
          // 忙碌
          studentObj.status = 'busy'
          studentObj.currentCourse = busyMap.get(key) // 挂载具体课程信息！
          cell.busyStudents.push(studentObj)
        } else {
          // 空闲
          studentObj.status = 'free'
          cell.freeStudents.push(studentObj)
        }
      }
    }
  })

  // 计算统计数据
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

// 打开详情弹窗
const openDetail = (slot, day) => {
  const cell = filteredSchedule.value[slot]?.[day]
  if (!cell) return
  // 传入当前格子里的数据 (包含已经挂载好 currentCourse 的学生列表)
  detailData.value = cell
  detailWeekInfo.value = { week: currentWeek.value, day, slot }
  detailVisible.value = true
}

// 处理"完善信息"的文本匹配
const handleMatch = () => {
  if (!inputText.value) return
  const lines = inputText.value.split('\n')
  let count = 0
  const infoMap = new Map()

  // 解析输入文本
  lines.forEach(line => {
    // 兼容空格或Tab分隔
    const parts = line.trim().split(/[\s\t]+/)
    if (parts.length >= 2) {
      const name = parts[0]
      // 简单策略：如果是3段(姓名 班级 学号)，中间是班级，最后是学号
      let className = ''
      let code = ''
      if (parts.length === 2) {
        code = parts[1] // 只有两段，假设是学号
      } else if (parts.length >= 3) {
        className = parts[1]
        code = parts[parts.length - 1]
      }
      infoMap.set(name, { className, code })
    }
  })

  // 更新 Pinia Store 中的原始数据
  if (courseStore.analysisResult && courseStore.analysisResult.data) {
    courseStore.analysisResult.data.forEach(item => {
      const s = item.student
      if (infoMap.has(s.name)) {
        const info = infoMap.get(s.name)
        if (info.className) s.className = info.className
        if (info.code) s.code = info.code
        count++
      }
    })
  }

  ElMessage.success(`匹配完成！已更新 ${count} 位同学的信息`)
  editInfoVisible.value = false
  inputText.value = ''
}

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
    reportToolUsage('course_tool').then(() => {
      usageCount.value++ // 前端手动+1，给用户即时反馈
    })
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

  const weekMap = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  const title = `【${weekMap[day - 1]} 第${slot}节 空闲统计 (第${currentWeek.value}周)】`

  const freeNames = cell.freeStudents.map(s => s.name).join(', ') || '无'
  const busyNames = cell.busyStudents.map(s => s.name).join(', ') || '无'

  const content = `${title}
✅ 空闲(${cell.freeCount}人): ${freeNames}
❌ 忙碌(${cell.busyStudents.length}人): ${busyNames}`

  try {
    await copyText(content)
    ElMessage.success('已复制该节课详情到剪贴板')
  } catch (err) {
    ElMessage.error('复制失败，请手动复制', err)
  }
}

// 【新增】复制整周详情
const handleCopyWeek = async () => {
  const weekMap = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  let content = `📅 第${currentWeek.value}周 空闲统计报表\n------------------------\n`

  // 遍历每一天
  for (let day = 1; day <= 7; day++) {
    content += `\n【${weekMap[day - 1]}】\n`
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
    await copyText(content)
    ElMessage.success('已复制本周完整报表！')
  } catch (err) {
    ElMessage.error('复制失败', err)
  }
}

onMounted(async () => {
  // 原有的获取统计逻辑
  try {
    const res = await getToolStats()
    const tool = res.find(t => t.code === 'course_tool')
    if (tool) {
      usageCount.value = tool.usageCount
    }
  } catch (e) {
    console.error('获取统计失败', e)
  }
})

</script>

<template>
  <div class="mobile-adapter-container" ref="adapterContainer">
    <div class="mobile-adapter-content" :style="contentStyle">
      <div class="app-container">
        <div class="tool-header">
          <div class="header-content">
            <h1>📅 课表空闲统计助手</h1>
            <p class="desc">
              批量上传 Excel 课表，一键分析全员空闲时间。
              <el-link type="primary" class="tutorial-link" @click="openTutorial">
                <el-icon>
                  <Document />
                </el-icon> 查看使用教程
              </el-link>
            </p>
            <div class="stats-badge">🔥 已累计服务 <span><count-up :end-val="usageCount" :duration="2.5" /></span> 人次</div>
          </div>
        </div>

        <transition name="el-zoom-in-center">
          <div v-if="!hasResult" class="upload-section card-box">
            <el-upload ref="uploadRef" v-model:file-list="fileList" class="upload-demo" drag multiple :auto-upload="false"
              accept=".xlsx, .xls">
              <el-icon class="el-icon--upload"><upload-filled /></el-icon>
              <div class="el-upload__text">将 Excel 课表拖到此处，或 <em>点击上传</em></div>
              <template #tip>
                <div class="el-upload__tip">支持批量上传，自动识别全学期课程</div>
              </template>
            </el-upload>
            <div class="actions">
              <el-button type="primary" size="large" :loading="loading" @click="handleAnalyze" round>
                <el-icon>
                  <DataAnalysis />
                </el-icon>
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
                <el-button type="primary" link @click="editInfoVisible = true" style="margin-left: 10px; font-weight:bold">
                  <el-icon>
                    <Edit />
                  </el-icon> 完善班级/学号
                </el-button>
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
                  <el-icon>
                    <Delete />
                  </el-icon>
                  <span>重置</span>
                </el-button>
              </div>
            </div>

            <div class="schedule-container card-box">
              <table class="course-table">
                <thead>
                  <tr>
                    <th style="width: 60px">节次</th>
                    <th v-for="day in ['周一', '周二', '周三', '周四', '周五', '周六', '周日']" :key="day">{{ day }}</th>
                  </tr>
                </thead>

                <transition name="fade" mode="out-in">
                  <tbody :key="currentWeek">
                    <tr v-for="slot in 10" :key="slot">
                      <td class="slot-idx">第 {{ slot }} 节</td>
                      <td v-for="day in 7" :key="day" class="cell-wrapper"
                        :style="viewMode === 'heatmap' ? { backgroundColor: getCellColor(filteredSchedule[slot]?.[day]?.busyRatio) } : {}"
                        @click="handleCopyCell(slot, day)" title="点击复制该节详情">
                        <template v-if="filteredSchedule[slot] && filteredSchedule[slot][day]">

                          <div v-if="viewMode === 'list'" class="mode-list">
                            <el-tooltip placement="top" :show-after="200" :hide-after="0" transition="none"
                              :enterable="false">
                              <template #content>
                                <div class="tooltip-list">
                                  <div v-for="s in filteredSchedule[slot][day].freeStudents" :key="s.name + s.code">
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
                            <div class="ratio-text"
                              :style="{ opacity: filteredSchedule[slot][day].busyRatio > 0.5 ? 1 : 0.6 }">
                              {{ (filteredSchedule[slot][day].busyRatio * 100).toFixed(0) }}% 忙
                            </div>
                          </div>
                          <div
                            v-if="filteredSchedule[slot] && filteredSchedule[slot][day] && (filteredSchedule[slot][day].freeStudents.length + filteredSchedule[slot][day].busyStudents.length > 0)"
                            class="expand-btn" @click.stop="openDetail(slot, day)" title="查看详细课程与分组">
                            <el-icon>
                              <ZoomIn />
                            </el-icon>
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
        <course-detail-dialog v-model:visible="detailVisible" :slot-data="detailData" :week-info="detailWeekInfo" />

        <el-dialog v-model="editInfoVisible" title="批量完善学生信息" width="500px" append-to-body>
          <p style="margin-bottom:10px; color:#666; line-height:1.5">
            请粘贴名单，每行一位同学。<br />
            固定格式：<b>姓名 班级 学号</b> （中间用空格或Tab分隔）<br />
            如果无学号，请用数字0代替。<br />
            如果重复更新，会替换掉对应姓名的对应信息。
          </p>
          <el-input v-model="inputText" type="textarea" :rows="10" placeholder="例如：
    方苏渝 23级大数据0034班 2350203170
    肖静宜 24级大数据专本贯通班 2450223033" />
          <template #footer>
            <el-button @click="editInfoVisible = false">取消</el-button>
            <el-button type="primary" @click="handleMatch">开始匹配并更新</el-button>
          </template>
        </el-dialog>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
/* 移动端适配容器 */
.mobile-adapter-container {
  width: 100%;
  height: 100%;
  overflow: hidden; /* 禁止出现滚动条 */
}

/* 内部内容容器 */
.mobile-adapter-content {
  width: 100%;
  height: 100%;
  /* 默认无缩放 */
}

/* 定义淡入淡出动画 (0.5s) */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.app-container {
  max-width: 1200px;
  margin: 0 auto;
  padding-bottom: 50px;
}

.tool-header {
  text-align: center;
  margin-bottom: 30px;

  .header-content {
    h1 {
      margin-bottom: 10px;
      color: var(--text-color-primary);
    }

    .desc {
      color: var(--text-color-secondary);
      margin-bottom: 15px;
    }

    .stats-badge {
      display: inline-flex;
      /* 强制在一行 */
      align-items: center;
      justify-content: center;
      gap: 5px;

      /* 【修复重点】 */
      /* 1. 默认(亮色): 使用填充色(浅灰)，适配性最好 */
      background: var(--el-fill-color);
      color: var(--text-color-regular);
      /* 文字用常规色，不要太亮 */

      padding: 6px 16px;
      border-radius: 20px;
      font-size: 13px;
      font-weight: bold;
      border: 1px solid var(--border-color);
      /* 边框也跟随主题 */

      span {
        font-size: 16px;
        font-family: 'Helvetica Neue', sans-serif;
        color: var(--el-color-primary);
        /* 数字保持品牌色高亮 */
        margin: 0 2px;
      }
    }

    /* 单独针对暗黑模式微调 (保险起见) */
    /* 当 html 有 dark 类时，强制背景为深色 */
    :deep(html.dark) & .stats-badge {
      background: #262727;
      border-color: #4c4d4f;
    }

    .tutorial-link {
      font-size: 14px;
      margin-left: 10px;
      vertical-align: baseline;
      cursor: pointer;

      &:hover {
        text-decoration: underline;
      }
    }
  }
}

/* 【暗黑模式修复】上传区域 */
.upload-section {
  padding: 40px;
  text-align: center;

  /* 确保上传框背景在暗黑模式下也是卡片背景 */
  :deep(.el-upload-dragger) {
    background-color: var(--bg-color-overlay);
    border-color: var(--border-color);

    &:hover {
      border-color: var(--el-color-primary);
    }
  }

  .actions {
    margin-top: 20px;
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 15px;

    :deep(.el-button) {
      display: inline-flex !important;
      justify-content: center !important;
      align-items: center !important;

      span {
        margin-left: 5px;
        display: inline-flex;
        align-items: center;
      }

      .el-icon {
        margin-right: 0;
      }
    }
  }

  :deep(.el-upload-list) {
    max-height: 200px;
    overflow-y: auto;
    margin-top: 10px;
    text-align: left;
    border: 1px solid var(--border-color);
    border-radius: 4px;
    padding: 5px;
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: 5px;

    /* 列表项暗黑模式适配 */
    .el-upload-list__item {
      background-color: var(--bg-color-page);
      color: var(--text-color-regular);

      &:hover {
        background-color: var(--bg-color-overlay);
      }
    }
  }
}

.filter-bar {
  margin-bottom: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;

  .left-filters {
    display: flex;
    align-items: center;

    .label {
      font-size: 14px;
      font-weight: bold;
      color: var(--text-color-regular);
      margin-right: 10px;
    }
  }

  .right-actions {
    display: flex;
    align-items: center;
  }

  .week-selector {
    display: flex;
    align-items: center;
    margin-right: 15px;

    .label {
      font-weight: bold;
      color: var(--el-color-primary);
      margin-right: 8px;
      font-size: 15px;
    }

    .week-select {
      :deep(.el-input__inner) {
        font-weight: bold;
        color: var(--el-color-primary);
      }
    }
  }
}

.course-table {
  width: 100%;
  border-collapse: collapse;

  th,
  td {
    border: 1px solid var(--border-color);
    text-align: center;
    padding: 8px;
  }

  th {
    background-color: var(--bg-color-page);
    color: var(--text-color-primary);
    font-weight: bold;
    height: 40px;
  }

  .slot-idx {
    background-color: var(--bg-color-page);
    color: var(--text-color-secondary);
    font-size: 12px;
  }

  .cell-wrapper {
    position: relative;
    /* 确保绝对定位是相对于格子的 */
    height: 60px;
    vertical-align: middle;
    cursor: pointer;
    transition: none;
    /* 【暗黑模式修复】热力图文字颜色自适应 */
    color: var(--text-color-primary);

    &:hover {
      outline: 2px solid var(--el-color-primary);
      z-index: 1;
      position: relative;
    }

    &:active {
      opacity: 0.8;
    }

    /* 点击反馈 */

    /* 【新增】放大镜按钮样式 */
    .expand-btn {
      position: absolute;
      right: 2px;
      bottom: 2px;
      width: 20px;
      height: 20px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 4px;
      background-color: rgba(255, 255, 255, 0.9);
      color: var(--el-color-primary);
      box-shadow: 0 0 4px rgba(0, 0, 0, 0.1);
      opacity: 0;
      /* 默认隐藏 */
      transition: all 0.2s;
      font-size: 14px;
      z-index: 10;
      /* 保证在文字上面 */

      &:hover {
        background-color: var(--el-color-primary);
        color: white;
        transform: scale(1.1);
      }
    }

    /* 只有鼠标悬停在格子上时，才显示放大镜 */
    &:hover .expand-btn {
      opacity: 1;
    }
  }
}

.mode-list {
  cursor: help;

  .count {
    color: var(--el-color-success);
    font-weight: bold;

    &.zero {
      color: var(--text-color-secondary);
      font-weight: normal;
    }
  }
}

.mode-heatmap {
  .ratio-text {
    font-size: 12px;
    font-weight: bold;
    /* 文字描边，确保在深色或浅色热力图上都能看清 */
    text-shadow: 0 0 2px var(--bg-color-card);
    color: var(--text-color-primary);
  }
}

.tooltip-list {
  max-height: 300px;
  overflow-y: auto;
  line-height: 1.8;
}
</style>
