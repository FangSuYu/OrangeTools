<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { UploadFilled, DataAnalysis, Delete, Document, CopyDocument, Edit, ZoomIn, Setting, ArrowRight } from '@element-plus/icons-vue'
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
const viewType = ref('list')
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

// ================== 【新增】查课增强配置 ==================
const showKeywordConfig = ref(false)
const heatmapSource = ref('default') // default | small | big
const keywordConfig = ref({
  small: '', // 小课关键词
  big: ''    // 大课关键词
})
// 辅助函数：根据数值和最大值计算颜色 (用于名单模式的文字渐变)
// type: 'free'(绿), 'small'(蓝), 'big'(橙)
const getTextColor = (val, max, type) => {
  if (!val || val === 0) return '#ccc' // 无数据显示灰色
  const ratio = max > 0 ? val / max : 0
  // 保持一定的饱和度，透明度从 0.6 到 1
  const opacity = 0.5 + (ratio * 0.5)

  if (type === 'free') return `rgba(30, 200, 80, ${opacity})`   // 绿色系
  if (type === 'small') return `rgba(64, 158, 255, ${opacity})` // 蓝色系
  if (type === 'big') return `rgba(230, 162, 60, ${opacity})`   // 橙色系
  return '#333'
}
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

  // 准备关键词
  const smallKeys = keywordConfig.value.small.split(/[,，\n]/).map(k => k.trim()).filter(k => k)
  const bigKeys = keywordConfig.value.big.split(/[,，\n]/).map(k => k.trim()).filter(k => k)

  // 记录极值用于颜色计算
  let maxFree = 0
  let maxSmall = 0
  let maxBig = 0

  // 初始化
  for (let slot = 1; slot <= 10; slot++) {
    result[slot] = {}
    for (let day = 1; day <= 7; day++) {
      result[slot][day] = {
        freeCount: 0, freeStudents: [], busyStudents: [], busyRatio: 0,
        // 新增字段
        smallCount: 0, bigCount: 0,
        smallStudents: [], bigStudents: []
      }
    }
  }

  // 筛选逻辑 (保持不变)
  const validSchedules = rawResult.value.data.filter(item => {
    const s = item.student
    if (filters.value.college && s.college !== filters.value.college) return false
    if (filters.value.major && s.major !== filters.value.major) return false
    if (filters.value.grade && s.grade !== filters.value.grade) return false
    return true
  })

  validSchedules.forEach(schedule => {
    const busyMap = new Map()
    schedule.courses.forEach(course => {
      if (course.busyWeeks.includes(targetWeek)) {
        const key = `${course.day}-${course.slot}`
        let specificDetail = null
        if (course.courseDetails && course.courseDetails.length > 0) {
          specificDetail = course.courseDetails.find(d => d.weeks && d.weeks.includes(targetWeek))
        }
        busyMap.set(key, specificDetail || { name: '未知课程', location: '', teacher: '' })
      }
    })

    for (let slot = 1; slot <= 10; slot++) {
      for (let day = 1; day <= 7; day++) {
        const key = `${day}-${slot}`
        const cell = result[slot][day]
        const studentObj = { ...schedule.student }

        if (busyMap.has(key)) {
          // === 忙碌逻辑 ===
          const courseDetail = busyMap.get(key)
          studentObj.status = 'busy'
          studentObj.currentCourse = courseDetail
          cell.busyStudents.push(studentObj)

          // === 新增：匹配小课/大课 ===
          const cName = courseDetail.name || ''

          // 匹配小课
          if (smallKeys.length > 0 && smallKeys.some(k => cName.includes(k))) {
            studentObj.isSmallMatch = true
            cell.smallCount++
            cell.smallStudents.push(studentObj)
          }
          // 匹配大课
          if (bigKeys.length > 0 && bigKeys.some(k => cName.includes(k))) {
            studentObj.isBigMatch = true
            cell.bigCount++
            cell.bigStudents.push(studentObj)
          }

        } else {
          // === 空闲逻辑 ===
          studentObj.status = 'free'
          cell.freeStudents.push(studentObj)
        }
      }
    }
  })

  // 计算统计数据 & 更新极值
  for (let slot = 1; slot <= 10; slot++) {
    for (let day = 1; day <= 7; day++) {
      const cell = result[slot][day]
      cell.freeCount = cell.freeStudents.length
      const total = cell.freeStudents.length + cell.busyStudents.length

      // 原有忙碌比例
      cell.busyRatio = total > 0 ? cell.busyStudents.length / total : 0

      // 更新最大值
      if (cell.freeCount > maxFree) maxFree = cell.freeCount
      if (cell.smallCount > maxSmall) maxSmall = cell.smallCount
      if (cell.bigCount > maxBig) maxBig = cell.bigCount
    }
  }

  // 挂载极值到 result 对象上，方便模板读取
  result.stats = { maxFree, maxSmall, maxBig }

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

  // 1. 解析输入文本 (保持原逻辑)
  const lines = inputText.value.split('\n')
  const infoMap = new Map()
  lines.forEach(line => {
    const parts = line.trim().split(/[\s\t]+/)
    if (parts.length >= 2) {
      const name = parts[0]
      let className = ''
      let code = ''
      if (parts.length === 2) {
        code = parts[1]
      } else if (parts.length >= 3) {
        className = parts[1]
        code = parts[parts.length - 1]
      }
      infoMap.set(name, { className, code })
    }
  })

  // 2. 【核心优化】深拷贝数据，解除响应式绑定
  // 如果 analysisResult 为空，直接返回
  if (!courseStore.analysisResult || !courseStore.analysisResult.data) return

  // 使用 JSON 序列化进行深拷贝，变为普通 JS 对象
  const tempResult = JSON.parse(JSON.stringify(courseStore.analysisResult))

  let count = 0

  // 3. 在普通对象上循环修改，速度极快且不会触发 Vue 更新
  tempResult.data.forEach(item => {
    const s = item.student
    if (infoMap.has(s.name)) {
      const info = infoMap.get(s.name)
      // 只有当值真正变化时才更新（可选优化）
      let changed = false
      if (info.className && s.className !== info.className) {
        s.className = info.className
        changed = true
      }
      if (info.code && s.code !== info.code) {
        s.code = info.code
        changed = true
      }
      if (changed) count++
    }
  })

  // 4. 将修改后的完整数据一次性写回 Store
  // 这只会触发一次 computed 计算和 DOM 更新
  courseStore.setAnalysisResult(tempResult)

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
const getCellColor = (cell) => {
  if (!cell) return 'transparent'

  let ratio = 0
  let hue = 120 // 默认绿色
  let alpha = 0.5

  if (heatmapSource.value === 'default') {
    // 默认：按忙碌比例 (原有逻辑)
    if (cell.busyRatio === 0) return 'rgba(0, 255, 0, 0.15)'
    hue = (1 - cell.busyRatio) * 120
    return `hsla(${hue}, 90%, 45%, 0.5)`
  }
  else if (heatmapSource.value === 'small') {
    // 小课热力：蓝色系
    const max = filteredSchedule.value.stats?.maxSmall || 1
    const val = cell.smallCount
    if (val === 0) return 'transparent'
    // 越深越蓝
    return `rgba(64, 158, 255, ${0.2 + (val / max) * 0.8})`
  }
  else if (heatmapSource.value === 'big') {
    // 大课热力：橙色系
    const max = filteredSchedule.value.stats?.maxBig || 1
    const val = cell.bigCount
    if (val === 0) return 'transparent'
    // 越深越橙
    return `rgba(230, 162, 60, ${0.2 + (val / max) * 0.8})`
  }
}

// 打开教程
const openTutorial = () => {
  window.open(TUTORIAL_LINK, '_blank')
}

// 【重构】单节课程详细复制 (通用自适应版)
const handleCopyCell = async (slot, day) => {
  const cell = filteredSchedule.value[slot]?.[day]
  if (!cell) return

  const weekMap = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  const separator = "----------------------------------------"

  // 1. 标题头
  let content = `📅 单节查课/空闲统计\n`
  content += `⏰ 时间：第${currentWeek.value}周 ${weekMap[day - 1]} 第${slot}节\n`
  content += `${separator}\n`

  // Helper: 格式化学生信息 (复用周报的逻辑)
  const formatDetail = (s) => {
    const classStr = s.className ? `【${s.className}】` : ''
    // 如果是空闲，只显示名字和班级
    if (s.status === 'free') return `${classStr}${s.name}`

    // 如果是忙碌，显示详细地点
    const loc = s.currentCourse?.location || '未知地点'
    const cName = s.currentCourse?.name || '课程'
    return `${classStr}${s.name} - ${loc} (${cName})`
  }

  // 2. 核心数据统计
  const targetCount = cell.smallCount + cell.bigCount
  const freeCount = cell.freeCount
  const otherBusyCount = cell.busyStudents.length - targetCount

  // --- A. 查课目标 (高亮显示) ---
  if (targetCount > 0) {
    content += `🎯 查课目标 (共${targetCount}处)：\n`
    cell.smallStudents.forEach(s => {
      content += `   📘 [小] ${formatDetail(s)}\n`
    })
    cell.bigStudents.forEach(s => {
      content += `   📙 [大] ${formatDetail(s)}\n`
    })
    content += `\n`
  }

  // --- B. 空闲人力 ---
  if (freeCount > 0) {
    content += `✅ 空闲人力 (${freeCount}人)：\n`
    // 空闲人员用顿号连接，节省行数
    const freeList = cell.freeStudents.map(s => formatDetail(s)).join('、')
    content += `   ${freeList}\n\n`
  } else {
    content += `🚫 空闲人力：全员忙碌\n\n`
  }

  // --- C. 其他忙碌 (兜底) ---
  if (otherBusyCount > 0) {
    // 如果有目标，其他忙碌折叠显示；如果没有目标，其他忙碌就是主要信息
    const label = targetCount > 0 ? '❌ 其他忙碌' : '❌ 忙碌人员'
    content += `${label} (${otherBusyCount}人)：\n`

    const others = cell.busyStudents.filter(s => !s.isSmallMatch && !s.isBigMatch)
    // 忙碌人员也用顿号连接，只显示简单的 "姓名(班级)" 格式，避免太长
    const otherList = others.map(s => {
      return s.className ? `${s.name}(${s.className})` : s.name
    }).join('、')

    content += `   ${otherList}\n`
  }

  try {
    await copyText(content)
    ElMessage.success('单节详情已复制！')
  } catch (err) {
    ElMessage.error('复制失败')
  }
}

// 【重构】自适应通用周报 (全场景适配版)
const handleCopyWeek = async () => {
  const weekMap = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  const separator = "----------------------------------------"

  // 1. 生成报表头
  let content = `📅 查课/空闲统计周报\n`
  content += `📍 统计范围：第 ${currentWeek.value} 周\n`
  content += `📊 关键词设定：\n`
  if (keywordConfig.value.small) content += `   📘 小课：${keywordConfig.value.small}\n`
  if (keywordConfig.value.big)   content += `   📙 大课：${keywordConfig.value.big}\n`
  content += `${separator}\n`

  // 只有配置了关键词才显示这一行，保持简洁
  if (keywordConfig.value.small || keywordConfig.value.big) {
    content += `📊 筛选模式：启用\n`
  }
  content += `${separator}\n`

  let hasContent = false

  // 2. 遍历每天
  for (let day = 1; day <= 7; day++) {
    const dayName = weekMap[day - 1]
    let dayContent = ""
    let dayHasData = false

    // 遍历每节课
    for (let slot = 1; slot <= 10; slot++) {
      const cell = filteredSchedule.value[slot]?.[day]
      if (!cell) continue

      // 数据准备
      const freeCount = cell.freeCount
      const targetCount = cell.smallCount + cell.bigCount
      const otherBusyCount = cell.busyStudents.length - targetCount
      const totalBusyCount = cell.busyStudents.length

      // 判定逻辑：只要有空闲，或者有忙碌（无论是否命中目标），都视为有效数据
      if (freeCount > 0 || totalBusyCount > 0) {
        dayHasData = true
        dayContent += `\n⏰ [${dayName} 第${slot}节]\n`

        // --- A. 空闲人力 (始终显示) ---
        if (freeCount > 0) {
          // 格式：张三(班级)、李四
          const names = cell.freeStudents.map(s => {
            return s.className ? `${s.name}(${s.className})` : s.name
          }).join('、')
          dayContent += `✅ 空闲人力 (${freeCount}人)：\n   ${names}\n`
        } else {
          dayContent += `🚫 人力状况：全员忙碌\n`
        }

        // --- B. 查课目标 (高优先级，详细展示) ---
        if (targetCount > 0) {
          dayContent += `🎯 查课目标 (共${targetCount}处)：\n`

          // Helper: 格式化详细条目
          const formatDetail = (s) => {
            const loc = s.currentCourse?.location || '未知地点'
            const cName = s.currentCourse?.name || '课程'
            const classStr = s.className ? `【${s.className}】` : ''
            return `${classStr}${s.name} - ${loc} (${cName})`
          }

          // 列出小课
          cell.smallStudents.forEach(s => {
            dayContent += `   📘 [小] ${formatDetail(s)}\n`
          })

          // 列出大课
          cell.bigStudents.forEach(s => {
            dayContent += `   📙 [大] ${formatDetail(s)}\n`
          })
        }

        // --- C. 普通忙碌 (兜底展示) ---
        // 情况1：完全没有目标 -> 显示所有忙碌者 (紧凑格式)
        // 情况2：有目标，但还有其他人忙碌 -> 显示"其他忙碌" (紧凑格式)
        if (targetCount === 0 && totalBusyCount > 0) {
          const busyNames = cell.busyStudents.map(s => {
             return s.className ? `${s.name}(${s.className})` : s.name
          }).join('、')
          dayContent += `❌ 忙碌人员 (${totalBusyCount}人)：\n   ${busyNames}\n`
        }
        else if (targetCount > 0 && otherBusyCount > 0) {
          // 过滤出非目标的忙碌学生
          const others = cell.busyStudents.filter(s => !s.isSmallMatch && !s.isBigMatch)
          const otherNames = others.map(s => s.name).join('、')
          dayContent += `❌ 其他忙碌 (${otherBusyCount}人)：${otherNames}\n`
        }
      }
    }

    // 拼接到总报表
    if (dayHasData) {
      hasContent = true
      content += `\n🗓️ ====== ${dayName} ======${dayContent}`
      content += `\n`
    }
  }

  if (!hasContent) {
    content += "\n(本周无有效数据)"
  }

  content += `\n${separator}\n`
  content += `生成时间：${new Date().toLocaleString()}`

  try {
    await copyText(content)
    ElMessage.success({
      message: '详细周报已复制！(智能自适应格式)',
      duration: 3000
    })
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
            <el-upload ref="uploadRef" v-model:file-list="fileList" class="upload-demo" drag multiple
              :auto-upload="false" accept=".xlsx, .xls">
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
            <div class="filter-container card-box">

              <div class="filter-row top-row">
                <div class="row-left">
                  <div class="week-selector">
                    <span class="label">当前统计:</span>
                    <el-select v-model="currentWeek" style="width: 110px" class="week-select">
                      <el-option v-for="i in weekOptions" :key="i" :label="`第 ${i} 周`" :value="i" />
                    </el-select>
                  </div>

                  <el-divider direction="vertical" class="filter-divider" />

                  <el-select v-model="filters.college" placeholder="全部学院" clearable style="width: 160px">
                    <el-option v-for="c in rawResult.allColleges" :key="c" :label="c" :value="c" />
                  </el-select>
                  <el-select v-model="filters.major" placeholder="全部专业" clearable style="width: 140px">
                    <el-option v-for="m in rawResult.allMajors" :key="m" :label="m" :value="m" />
                  </el-select>
                  <el-select v-model="filters.grade" placeholder="全部年级" clearable style="width: 120px">
                    <el-option v-for="g in rawResult.allGrades" :key="g" :label="g" :value="g" />
                  </el-select>
                </div>

                <div class="row-right">
                  <el-button type="danger" plain size="default" @click="handleReset">
                    <el-icon>
                      <Delete />
                    </el-icon> 重置
                  </el-button>
                </div>
              </div>

              <div class="filter-row bottom-row">
                <el-button type="primary" link @click="editInfoVisible = true" style="font-weight:bold">
                  <el-icon>
                    <Edit />
                  </el-icon> 完善班级/学号
                </el-button>
                <el-button type="primary" link @click="showKeywordConfig = true" style="font-weight:bold">
                  <el-icon>
                    <Setting />
                  </el-icon> 课程关键词
                </el-button>

                <el-divider direction="vertical" class="action-divider" />

                <el-button type="success" plain size="small" @click="handleCopyWeek">
                  <el-icon>
                    <CopyDocument />
                  </el-icon> <span>复制周报</span>
                </el-button>

                <el-radio-group v-model="viewType" size="small">
                  <el-radio-button label="list">名单</el-radio-button>
                  <el-radio-button label="heatmap">热力</el-radio-button>
                </el-radio-group>

                <transition name="el-fade-in-linear">
                  <div v-if="viewType === 'heatmap'" class="heatmap-sub-panel">
                    <el-icon class="arrow-icon">
                      <ArrowRight />
                    </el-icon>
                    <el-radio-group v-model="heatmapSource" size="small">
                      <el-radio-button label="default">默认</el-radio-button>
                      <el-radio-button label="small">小课</el-radio-button>
                      <el-radio-button label="big">大课</el-radio-button>
                    </el-radio-group>
                  </div>
                </transition>
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
                        :style="viewType === 'heatmap' ? { backgroundColor: getCellColor(filteredSchedule[slot]?.[day]) } : {}"
                        @click="handleCopyCell(slot, day)" title="点击复制该节详情">

                        <template v-if="filteredSchedule[slot] && filteredSchedule[slot][day]">

                          <transition name="mode-switch" mode="out-in">

                            <div v-if="viewType === 'list'" class="mode-list" key="list">
                              <el-tooltip placement="top" :show-after="200" :hide-after="0" transition="none"
                                :enterable="false">
                                <template #content>
                                  <div class="tooltip-list">
                                    <div>空闲: {{ filteredSchedule[slot][day].freeCount }}</div>
                                    <div>小课: {{ filteredSchedule[slot][day].smallCount }}</div>
                                    <div>大课: {{ filteredSchedule[slot][day].bigCount }}</div>
                                    <hr style="margin:5px 0; opacity:0.3" />
                                    <div v-for="s in filteredSchedule[slot][day].freeStudents" :key="s.name + s.code">
                                      {{ s.name }} <span style="opacity:0.7">({{ s.major }})</span>
                                    </div>
                                  </div>
                                </template>

                                <div class="cell-content-grid">
                                  <div class="top-row"
                                    :style="{ color: getTextColor(filteredSchedule[slot][day].freeCount, filteredSchedule.stats?.maxFree, 'free') }">
                                    空: {{ filteredSchedule[slot][day].freeCount }}
                                  </div>
                                  <div class="bottom-row">
                                    <div class="split-col left"
                                      :style="{ color: getTextColor(filteredSchedule[slot][day].smallCount, filteredSchedule.stats?.maxSmall, 'small') }">
                                      小: {{ filteredSchedule[slot][day].smallCount || '-' }}
                                    </div>
                                    <div class="split-col right"
                                      :style="{ color: getTextColor(filteredSchedule[slot][day].bigCount, filteredSchedule.stats?.maxBig, 'big') }">
                                      大: {{ filteredSchedule[slot][day].bigCount || '-' }}
                                    </div>
                                  </div>
                                </div>
                              </el-tooltip>
                            </div>

                            <div v-else class="mode-heatmap" key="heatmap">
                              <div class="ratio-text">
                                <span v-if="heatmapSource === 'default'">{{ (filteredSchedule[slot][day].busyRatio *
                                  100).toFixed(0) }}%</span>
                                <span v-else-if="heatmapSource === 'small'">{{ filteredSchedule[slot][day].smallCount
                                  }}</span>
                                <span v-else-if="heatmapSource === 'big'">{{ filteredSchedule[slot][day].bigCount
                                  }}</span>
                              </div>
                            </div>
                          </transition>

                          <div
                            v-if="filteredSchedule[slot][day].freeStudents.length + filteredSchedule[slot][day].busyStudents.length > 0"
                            class="expand-btn" @click.stop="openDetail(slot, day)">
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
        <el-dialog v-model="showKeywordConfig" title="课程类型关键词配置" width="450px" append-to-body>
          <p style="font-size:12px; color:#999; margin-bottom:15px;">输入课程名中包含的文字，多个关键词用逗号分隔。</p>
          <div style="margin-bottom:15px">
            <div style="font-weight:bold; margin-bottom:5px; color:#409eff">📘 小课关键词 (单班)</div>
            <el-input v-model="keywordConfig.small" type="textarea" :rows="2" placeholder="例: 英语, 高数, 体育" />
          </div>
          <div>
            <div style="font-weight:bold; margin-bottom:5px; color:#e6a23c">📙 大课关键词 (合班)</div>
            <el-input v-model="keywordConfig.big" type="textarea" :rows="2" placeholder="例: 形势与政策, 思想道德, 职业规划" />
          </div>
          <template #footer>
            <el-button type="primary" @click="showKeywordConfig = false">确定</el-button>
          </template>
        </el-dialog>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
/* 新版双行筛选容器 */
.filter-container {
  margin-bottom: 20px;
  display: flex;
  flex-direction: column;
  padding: 0 20px;

  /* 通用行样式 */
  .filter-row {
    display: flex;
    align-items: center;
    padding: 12px 0;
    gap: 12px;
    /* 控件之间的间距 */
  }

  /* 第一行：筛选 */
  .top-row {
    justify-content: space-between;
    /* 两端对齐 */
    border-bottom: 1px dashed var(--border-color);
    flex-wrap: wrap;
    /* 小屏自动换行 */

    .row-left,
    .row-right {
      display: flex;
      align-items: center;
      gap: 10px;
      flex-wrap: wrap;
    }
  }

  /* 第二行：操作 (关键修改) */
  .bottom-row {
    justify-content: flex-start;
    /* 强制左对齐 */
    flex-wrap: wrap;

    /* 分隔线微调 */
    .action-divider {
      height: 1.2em;
      margin: 0 5px;
      border-color: var(--border-color);
    }

    /* 热力图子面板样式 */
    .heatmap-sub-panel {
      display: flex;
      align-items: center;
      margin-left: 5px;
      padding: 2px 8px;
      background-color: var(--el-fill-color-light);
      /* 浅灰背景 */
      border-radius: 4px;

      .arrow-icon {
        font-size: 12px;
        color: var(--text-color-secondary);
        margin-right: 8px;
      }

      /* 让子选项按钮稍微小一点，区分层级 */
      :deep(.el-radio-button__inner) {
        padding: 5px 10px;
        font-size: 12px;
        border: none;
        background: transparent;
      }

      /* 选中态样式优化 */
      :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
        background-color: white;
        color: var(--el-color-primary);
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
        border-radius: 3px;
        font-weight: bold;
      }
    }
  }

  /* 周次选择器样式 */
  .week-selector {
    display: flex;
    align-items: center;

    .label {
      font-weight: bold;
      color: var(--el-color-primary);
      margin-right: 8px;
      font-size: 15px;
      white-space: nowrap;
    }

    .week-select :deep(.el-input__inner) {
      font-weight: bold;
      color: var(--el-color-primary);
    }
  }

  .filter-divider {
    height: 1.2em;
    border-color: var(--border-color);
    margin: 0 5px;
  }
}

/* 新增的单元格网格布局 */
.cell-content-grid {
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
  justify-content: center;

  .top-row {
    text-align: center;
    font-weight: 900;
    font-size: 14px;
    margin-bottom: 2px;
  }

  .bottom-row {
    display: flex;
    justify-content: space-between;
    font-size: 11px;

    .split-col {
      width: 50%;
      text-align: center;
      font-weight: bold;

      &.left {
        border-right: 1px solid rgba(0, 0, 0, 0.05);
      }
    }
  }
}

/* 移动端适配容器 */
.mobile-adapter-container {
  width: 100%;
  height: 100%;
  overflow: hidden;
  /* 禁止出现滚动条 */
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
  flex-wrap: wrap;
  /* 防止屏幕窄时布局炸裂 */
  gap: 15px;
  /* 上下行间距 */

  .left-filters {
    display: flex;
    align-items: center;
    gap: 10px;
    /* 统一左侧控件间距 */
    flex-wrap: wrap;

    .label {
      font-size: 14px;
      font-weight: bold;
      color: var(--text-color-regular);
    }
  }

  /* 【重点修改】右侧操作栏样式优化 */
  .right-actions {
    display: flex;
    align-items: center;
    /* 关键：强制不换行，防止掉到下一行 */
    flex-wrap: nowrap;

    /* 动画容器样式 */
    .heatmap-toolbar-wrapper {
      display: flex;
      align-items: center;
      margin-right: 5px;
    }

    /* 热力图子工具栏 */
    .heatmap-toolbar {
      display: flex;
      align-items: center;
      background-color: var(--el-color-primary-light-9);
      /* 使用更柔和的品牌色背景 */
      padding: 3px 8px;
      border-radius: 16px;
      /* 圆润风格 */
      border: 1px solid var(--el-color-primary-light-7);
      white-space: nowrap;

      .label {
        font-size: 12px;
        color: var(--el-color-primary);
        margin-right: 6px;
        font-weight: bold;
      }

      :deep(.el-radio-button__inner) {
        padding: 4px 8px;
        font-size: 12px;
        border: none;
        background: transparent;
        box-shadow: none !important;
      }

      :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
        background-color: var(--el-color-primary);
        color: white;
        border-radius: 12px;
        box-shadow: none;
      }
    }

    /* 常用按钮组 */
    .action-group {
      display: flex;
      align-items: center;
      gap: 8px;
      white-space: nowrap;
    }
  }

  .week-selector {
    display: flex;
    align-items: center;
    /* margin-right: 15px;  <-- 移除，改用父级 gap */

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

    transition: background-color 0.3s ease-in-out;

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

.mode-switch-enter-active,
.mode-switch-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.mode-switch-enter-from {
  opacity: 0;
  transform: scale(0.9);
  /*以此实现轻微弹出的效果*/
}

.mode-switch-leave-to {
  opacity: 0;
  transform: scale(0.9);
}
</style>
