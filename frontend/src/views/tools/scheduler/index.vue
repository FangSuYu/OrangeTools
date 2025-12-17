<template>
  <div class="app-container">

    <div v-if="!hasData" class="tool-upload-page">
      <div class="tool-header">
        <div class="header-content">
          <h1>📅 智能排班助手</h1>
          <p class="desc">
            批量上传 Excel 课表，一键生成智能排班方案，支持手动调整。
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
        <div v-if="!hasData" class="upload-section card-box">
          <el-upload ref="uploadRef" v-model:file-list="fileList" class="upload-demo" drag multiple :auto-upload="false"
            accept=".xlsx, .xls">
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">将 Excel 课表拖到此处，或 <em>点击上传</em></div>
            <template #tip>
              <div class="el-upload__tip">支持 .xlsx / .xls 文件，可批量上传</div>
            </template>
          </el-upload>

          <div class="actions">
            <el-button type="primary" size="large" :loading="loading" @click="startAnalysis" round>
              <el-icon>
                <DataAnalysis />
              </el-icon>
              <span>开始智能排班</span>
            </el-button>
          </div>
        </div>
      </transition>
    </div>

    <div v-else class="workspace-wrapper">

      <div class="workspace-toolbar">
        <div class="left-section">
          <div class="week-selector">
            <span class="label">当前周次:</span>
            <el-input-number v-model="currentWeek" :min="1" :max="25" size="default" />
          </div>
        </div>

        <div class="right-section">
          <el-button type="primary" color="#626aef" :icon="Cpu" @click="handleOpenSmartSettings"
            style="margin-right: 15px; font-weight: bold;">
            智能排班
          </el-button>
          <input type="file" ref="importInputRef" style="display: none" accept=".json" @change="handleImportJSON" />

          <el-button-group>
            <el-tooltip content="导入之前的排班数据(.json)" placement="bottom">
              <el-button type="warning" plain :icon="FolderOpened" @click="triggerImport">导入</el-button>
            </el-tooltip>
            <el-tooltip content="保存当前进度为文件(.json)" placement="bottom">
              <el-button type="success" plain :icon="FolderChecked" @click="handleExportJSON">存档</el-button>
            </el-tooltip>
          </el-button-group>

          <el-divider direction="vertical" class="custom-divider" />

          <el-button type="danger" plain :icon="Delete" @click="handleClear">重置</el-button>

          <el-tooltip content="截图分享" placement="bottom">
            <el-button type="primary" plain :icon="Camera" @click="handleScreenshot" :loading="screenshotLoading"
              style="margin-right: 20px;">
            </el-button>
          </el-tooltip>
        </div>
      </div>

      <div class="workspace-body">

        <aside class="sidebar">
          <div class="sidebar-header">
            <h3>待选人员 ({{ filteredStudents.length }})</h3>

            <div class="filter-group">
              <el-input v-model="searchQuery" placeholder="搜索姓名..." prefix-icon="Search" clearable
                class="filter-item" />
              <el-select v-model="filterCollege" placeholder="选择学院" clearable class="filter-item">
                <el-option v-for="c in collegeOptions" :key="c" :label="c" :value="c" />
              </el-select>
              <div class="filter-row">
                <el-select v-model="filterGrade" placeholder="年级" clearable class="filter-half">
                  <el-option v-for="g in gradeOptions" :key="g" :label="g" :value="g" />
                </el-select>
                <el-select v-model="filterMajor" placeholder="专业" clearable class="filter-half">
                  <el-option v-for="m in majorOptions" :key="m" :label="m" :value="m" />
                </el-select>
              </div>
            </div>
          </div>

          <div class="sidebar-content">
            <VueDraggable v-model="filteredStudents" :group="{ name: 'people', pull: 'clone', put: false }"
              :sort="false" item-key="id" ghost-class="ghost-card" class="student-list" :force-fallback="true"
              :fallback-on-body="true" :scroll-sensitivity="150" :scroll-speed="20" drag-class="dragging-card-fallback"
              :bubble-scroll="false" @start="onDragStart" @end="onDragEnd">
              <template #item="{ element }">
                <div class="student-card">
                  <div class="card-avatar">{{ element.name.charAt(0) }}</div>
                  <div class="card-info">
                    <div class="name">{{ element.name }}</div>
                    <div class="meta">{{ element.major }}</div>
                  </div>
                  <el-icon class="drag-icon">
                    <Rank />
                  </el-icon>
                </div>
              </template>
            </VueDraggable>
          </div>
        </aside>

        <main class="schedule-grid" ref="scheduleGridRef">
          <div class="grid-header">
            <div class="idx-col">节次</div>
            <div v-for="day in ['周一', '周二', '周三', '周四', '周五', '周六', '周日']" :key="day" class="day-col">
              {{ day }}
            </div>
          </div>

          <div class="grid-rows">
            <div v-for="row in timeLayout" :key="row.id" class="grid-row"
              :class="{ 'special-row': row.type === 'special' }">
              <div class="idx-cell">
                <span v-if="row.type === 'special'" class="special-label">{{ row.alias }}</span>
                <span v-else>{{ row.label }}</span>
              </div>

              <div v-for="day in 7" :key="day" class="task-cell" :class="getCellHintClass(day, row.id)"
                @click.self="openSelectDialog(day, row.id)">
                <VueDraggable :list="getSlotList(day, row.id)" group="people" item-key="id" class="cell-draggable"
                  ghost-class="ghost-tag" @change="(evt) => onSlotChange(evt, day, row.id)" :force-fallback="true"
                  :fallback-on-body="true" :scroll-sensitivity="150" :scroll-speed="20" :bubble-scroll="false">
                  <template #item="{ element }">
                    <el-tooltip :content="getConflictInfo(element, day, row.id).tooltip" placement="top"
                      :disabled="!getConflictInfo(element, day, row.id).isConflict" :teleported="false">
                      <el-tag :type="getConflictInfo(element, day, row.id).type" closable class="student-tag"
                        @close="removeStudent(day, row.id, element.id)" @click.stop>
                        {{ element.name }}
                        <el-icon v-if="getConflictInfo(element, day, row.id).isConflict" class="warn-icon">
                          <Warning />
                        </el-icon>
                      </el-tag>
                    </el-tooltip>
                  </template>
                </VueDraggable>

                <div class="cell-action-overlay">
                  <el-button :icon="Plus" circle size="small" class="add-btn"
                    @click.stop="openSelectDialog(day, row.id)" />
                </div>
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" title="添加人员" width="550px" append-to-body top="5vh">
      <div class="dialog-header-custom">
        <span class="info">
          正在安排: <span class="highlight">周{{ currentSelectDay }} {{ getSectionName(currentSelectSection) }}</span>
        </span>
        <el-input v-model="dialogSearch" placeholder="搜索姓名..." style="width: 200px;" size="small"
          prefix-icon="Search" />
      </div>
      <div class="dialog-list">
        <div v-for="student in dialogStudentList" :key="student.id" class="dialog-item"
          :class="{ 'is-conflict': student.conflictInfo.isConflict, 'is-added': student.isAdded }"
          @click="!student.isAdded && selectStudentFromDialog(student)">
          <div class="item-left">
            <div class="avatar">{{ student.name.charAt(0) }}</div>
            <div class="text">
              <div class="name">{{ student.name }}</div>

              <div class="meta-info">
                <span class="meta-item grade">{{ student.grade }}级</span>
                <span class="divider">|</span>
                <span class="meta-item college" :title="student.college">{{ student.college }}</span>
                <span class="divider">|</span>
                <span class="meta-item major" :title="student.major">{{ student.major }}</span>
              </div>

              <div class="desc" v-if="student.isAdded">
                <el-tag size="small" type="info" effect="plain">已添加</el-tag>
              </div>
              <div class="desc" v-else-if="student.conflictInfo.isConflict">
                <el-tag size="small" type="danger" effect="plain">{{ student.conflictInfo.reason }}</el-tag>
              </div>
              <div class="desc" v-else>
                <el-tag size="small" type="success" effect="plain">空闲</el-tag>
              </div>
            </div>
          </div>
          <el-button size="small"
            :type="student.isAdded ? 'info' : (student.conflictInfo.isConflict ? 'default' : 'primary')"
            :icon="student.isAdded ? Check : Plus" :disabled="student.isAdded" circle />
        </div>
      </div>
    </el-dialog>

    <SmartSettings v-model="showSmartSettings" :student-pool="studentPool" @confirm="handleAutoSchedule" />
    <ResultReport v-model="showResultReport" :result-data="analysisResult" @apply="applySchedule"
      @retry="handleRetry" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { storeToRefs } from 'pinia'
import { useSchedulerStore } from '@/stores/modules/scheduler'
import VueDraggable from 'vuedraggable'
import html2canvas from 'html2canvas'
import CountUp from 'vue-countup-v3'
import { getToolStats, reportToolUsage } from '@/api/community'
import { autoSchedule } from '@/api/tools/scheduler'
import { Calendar, UploadFilled, Delete, Download, Search, Rank, Plus, Warning, Document, Check, Camera, DataAnalysis, Close, FolderOpened, FolderChecked, Cpu } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, ElLoading } from 'element-plus'
import SmartSettings from './components/SmartSettings.vue'
import ResultReport from './components/ResultReport.vue'

const store = useSchedulerStore()
const { studentPool, scheduleSolution, currentWeek } = storeToRefs(store)

const hasData = computed(() => studentPool.value && studentPool.value.length > 0)

// --- 统计与教程 ---
const usageCount = ref(0)
// 教程链接
const TUTORIAL_LINK = 'https://ai.feishu.cn/docx/InhNdj8dPooPGWxAAEXcGP9Fndf?from=from_copylink' // 示例链接，可按需修改

const openTutorial = () => {
  window.open(TUTORIAL_LINK, '_blank')
}

onMounted(async () => {
  // 获取工具统计数据 (code: scheduler_tool)
  try {
    const res = await getToolStats()
    // 后端返回的是 List<Tool>，找到 scheduler_tool 这一项
    const tool = res.find(t => t.code === 'scheduler_tool')
    if (tool) {
      usageCount.value = tool.usageCount
    }
  } catch (e) {
    console.error('获取统计失败', e)
  }
})

// --- 上传逻辑 ---
const fileList = ref([])
const loading = ref(false)
const uploadRef = ref(null)

const startAnalysis = async () => {
  if (fileList.value.length === 0) {
    ElMessage.warning('请先选择 Excel 文件')
    return
  }
  loading.value = true

  // 注意：store.uploadAndParse 需要根据你实际 store 的实现，可能需要适配 raw file
  // 假设 store.uploadAndParse 接受 file objects 数组
  const rawFiles = fileList.value.map(f => f.raw)
  const success = await store.uploadAndParse(rawFiles)

  if (success) {
    reportToolUsage('scheduler_tool').then(() => {
      usageCount.value++ // 前端手动+1
    })
  }

  loading.value = false
}

// ... 以下逻辑保持不变 ...
const handleClear = () => {
  ElMessageBox.confirm('确定要重置所有数据吗？', '提示', { type: 'warning' }).then(() => {
    store.clearAll()
    fileList.value = []
  })
}

// 2. 新增 导入/导出 逻辑
const importInputRef = ref(null)

// 触发导入点击
const triggerImport = () => {
  importInputRef.value.click()
}

// 处理导入 JSON
const handleImportJSON = (event) => {
  const file = event.target.files[0]
  if (!file) return

  const reader = new FileReader()
  reader.onload = (e) => {
    try {
      const data = JSON.parse(e.target.result)

      // 简单的格式校验
      if (!data.version || !data.pool || !data.solution) {
        ElMessage.error('文件格式不正确，不是橙子排班助手的数据文件')
        return
      }

      // 恢复数据到 Pinia
      ElMessageBox.confirm('导入将覆盖当前所有数据，确定继续吗？', '警告', { type: 'warning' })
        .then(() => {
          store.studentPool = data.pool
          store.scheduleSolution = data.solution
          store.currentWeek = data.currentWeek || 1
          ElMessage.success('方案已恢复')
        })
        .catch(() => {
          // 取消导入，清空 input 防止下次无法触发 change
          event.target.value = ''
        })
    } catch (err) {
      ElMessage.error('文件解析失败')
    }
    // 清空 input
    event.target.value = ''
  }
  reader.readAsText(file)
}

// 处理导出 JSON
const handleExportJSON = () => {
  if (!hasData.value) {
    ElMessage.warning('当前没有数据可导出')
    return
  }

  // 构造数据包
  const exportData = {
    version: '1.0',
    timestamp: new Date().getTime(),
    currentWeek: currentWeek.value,
    pool: studentPool.value,       // 人员库
    solution: scheduleSolution.value // 排班结果
  }

  const jsonStr = JSON.stringify(exportData, null, 2)
  const blob = new Blob([jsonStr], { type: 'application/json' })
  const url = URL.createObjectURL(blob)

  const link = document.createElement('a')
  link.href = url
  link.download = `橙子排班存档_${new Date().toLocaleDateString()}.json`
  link.click()

  URL.revokeObjectURL(url)
  ElMessage.success('存档文件已下载')
}

const scheduleGridRef = ref(null)
const screenshotLoading = ref(false)

const handleScreenshot = async () => {
  if (!scheduleGridRef.value) return
  screenshotLoading.value = true

  try {
    const element = scheduleGridRef.value

    // 1. 保存原始样式
    const originalStyles = {
      overflow: element.style.overflow,
      maxWidth: element.style.maxWidth,
      width: element.style.width
    }

    // 2. 临时调整样式
    element.style.overflow = 'visible'
    element.style.maxWidth = 'none'
    element.style.width = 'fit-content'

    // 3. 等待样式生效
    await nextTick()
    await new Promise(resolve => setTimeout(resolve, 100))

    // 4. 计算实际内容宽度（表头的实际宽度）
    const header = element.querySelector('.schedule-header')
    const actualContentWidth = header ? header.scrollWidth : element.scrollWidth

    // 5. 截图
    const canvas = await html2canvas(element, {
      scale: 2,
      useCORS: true,
      backgroundColor: '#ffffff',
      width: actualContentWidth,
      height: element.scrollHeight,
      scrollX: 0,
      scrollY: -window.scrollY,
      onclone: function (clonedDoc) {
        const clonedElement = clonedDoc.querySelector('.schedule-grid')
        if (clonedElement) {
          clonedElement.style.overflow = 'visible'
          clonedElement.style.maxWidth = 'none'
          clonedElement.style.width = actualContentWidth + 'px'
        }
      },
      ignoreElements: function (el) {
        return el.classList.contains('cell-action-overlay')
      }
    })

    // 6. 恢复原始样式
    element.style.overflow = originalStyles.overflow
    element.style.maxWidth = originalStyles.maxWidth
    element.style.width = originalStyles.width

    // 7. 下载
    const link = document.createElement('a')
    link.download = '橙子排班方案_第' + currentWeek.value + '周.png'
    link.href = canvas.toDataURL('image/png', 1.0)
    link.click()
    ElMessage.success('截图已生成并下载')
  } catch (error) {
    console.error(error)
    ElMessage.error('截图生成失败')
  } finally {
    screenshotLoading.value = false
  }
}



// --- 筛选、拖拽、弹窗逻辑 ---
const searchQuery = ref('')
const filterCollege = ref('')
const filterGrade = ref('')
const filterMajor = ref('')
const collegeOptions = computed(() => [...new Set(studentPool.value.map(s => s.college).filter(Boolean))])
const gradeOptions = computed(() => [...new Set(studentPool.value.map(s => s.grade).filter(Boolean))])
const majorOptions = computed(() => [...new Set(studentPool.value.map(s => s.major).filter(Boolean))])
const filteredStudents = computed(() => {
  return studentPool.value.filter(s => {
    const matchName = s.name.includes(searchQuery.value)
    const matchCollege = filterCollege.value ? s.college === filterCollege.value : true
    const matchGrade = filterGrade.value ? s.grade === filterGrade.value : true
    const matchMajor = filterMajor.value ? s.major === filterMajor.value : true
    return matchName && matchCollege && matchGrade && matchMajor
  })
})
const draggingStudent = ref(null)
const onDragStart = (evt) => {
  if (filteredStudents.value[evt.oldIndex]) {
    draggingStudent.value = filteredStudents.value[evt.oldIndex]
  }
  // 【新增】黑科技：拖拽开始时，强制锁死页面的文字选择能力
  // 这样无论你怎么乱晃鼠标，都不会出现那种难看的蓝色文字选中块
  document.body.style.userSelect = 'none'
  document.body.style.cursor = 'grabbing' // 强制显示“抓紧”的手势
}
const onDragEnd = () => {
  draggingStudent.value = null
  // 【新增】拖拽结束，恢复正常
  document.body.style.userSelect = ''
  document.body.style.cursor = ''
}
const getCellHintClass = (day, section) => {
  if (!draggingStudent.value) return ''
  const check = store.checkConflict(draggingStudent.value, day, section, currentWeek.value)
  return check.conflict ? 'hint-busy' : 'hint-free'
}
const getSlotList = (day, section) => {
  const key = `${day}_${section}`
  if (!scheduleSolution.value[key]) scheduleSolution.value[key] = []
  return scheduleSolution.value[key]
}
const onSlotChange = (evt, day, section) => {
  if (evt.added) {
    const key = `${day}_${section}`
    const list = scheduleSolution.value[key]
    const unique = []
    const ids = new Set()
    for (const s of list) {
      if (!ids.has(s.id)) { ids.add(s.id); unique.push(s); }
      else { ElMessage.warning(`${s.name} 已存在`); }
    }
    scheduleSolution.value[key] = unique
  }
}
const removeStudent = (day, section, id) => { store.removeStudentFromSlot(day, section, id) }
const getConflictInfo = (student, day, section) => {
  const check = store.checkConflict(student, day, section, currentWeek.value)
  if (check.conflict) return { isConflict: true, type: 'danger', tooltip: `冲突: ${check.reason}` }
  return { isConflict: false, type: 'success', tooltip: '空闲' }
}
const dialogVisible = ref(false)
const currentSelectDay = ref(1)
const currentSelectSection = ref(1)
const dialogSearch = ref('')
const openSelectDialog = (day, section) => {
  currentSelectDay.value = day
  currentSelectSection.value = section
  dialogVisible.value = true
  dialogSearch.value = ''
}
const dialogStudentList = computed(() => {
  const currentSlotStudents = getSlotList(currentSelectDay.value, currentSelectSection.value)
  const currentIds = currentSlotStudents.map(s => s.id)

  return studentPool.value
    .filter(s => s.name.includes(dialogSearch.value))
    .map(s => {
      // 1. 获取原始的 Store 检查结果 { conflict: boolean, reason: string }
      const rawCheck = store.checkConflict(s, currentSelectDay.value, currentSelectSection.value, currentWeek.value)

      return {
        ...s,
        // 2. 这里的 conflictInfo 必须手动构造，确保模板里的 .isConflict 能读到值
        conflictInfo: {
          isConflict: rawCheck.conflict, // 映射 conflict -> isConflict
          reason: rawCheck.reason
        },
        isAdded: currentIds.includes(s.id)
      }
    })
    .sort((a, b) => {
      if (a.isAdded !== b.isAdded) return a.isAdded ? 1 : -1
      if (a.conflictInfo.isConflict !== b.conflictInfo.isConflict) return a.conflictInfo.isConflict ? 1 : -1
      return 0
    })
})
const selectStudentFromDialog = (student) => {
  store.addStudentToSlot(currentSelectDay.value, currentSelectSection.value, student)
}

// --- 【新增】排班时间轴配置 (配置驱动视图) ---
// 0:早间, 1-4:上午, 11:午间, 5-8:下午, 12:傍晚, 9-10:晚课, 13:深夜
const timeLayout = [
  { id: 0, label: '早', alias: '醒了', type: 'special' },
  { id: 1, label: '1', type: 'normal' },
  { id: 2, label: '2', type: 'normal' },
  { id: 3, label: '3', type: 'normal' },
  { id: 4, label: '4', type: 'normal' },
  { id: 11, label: '午', alias: '困了', type: 'special' },
  { id: 5, label: '5', type: 'normal' },
  { id: 6, label: '6', type: 'normal' },
  { id: 7, label: '7', type: 'normal' },
  { id: 8, label: '8', type: 'normal' },
  { id: 12, label: '晚', alias: '饿了', type: 'special' },
  { id: 9, label: '9', type: 'normal' },
  { id: 10, label: '10', type: 'normal' },
  { id: 13, label: '夜', alias: '乏了', type: 'special' }
]

// 【新增】辅助函数：根据 section ID 获取显示名称 (用于弹窗标题)
const getSectionName = (sectionId) => {
  const target = timeLayout.find(t => t.id === sectionId)
  return target ? (target.alias || `第${target.id}节`) : `${sectionId}`
}

// 以下为智能排班相关功能代码
const showSmartSettings = ref(false)
const showResultReport = ref(false) // <--- 新增
const analysisResult = ref({})      // <--- 新增：存储后端返回的结果
const handleOpenSmartSettings = () => {
  showSmartSettings.value = true
}

// 处理：配置提交 -> (模拟后端计算) -> 打开报告
const handleAutoSchedule = async (configPayload) => {
  // 1. 组装完整请求包
  // 我们需要把 studentPool 里的对象转换一下，只传算法需要的字段（id, name, busySlots）
  // 假设 studentPool 里的每个学生对象已经有 scheduleRaw (原始课表数据)
  const studentsForBackend = studentPool.value.map(s => ({
    id: s.id,
    name: s.name,
    // 将课表转换为字符串数组 ["1_1", "1_2"]
    busySlots: s.scheduleRaw.map(item => `${item.day}_${item.section}`)
  }))

  const requestData = {
    ...configPayload, // 包含 strategy, requirements, maxPerWeek 等
    students: studentsForBackend
  }

  console.log('发送给后端的完整数据:', requestData)

  const loadingInstance = ElLoading.service({
    text: '正在进行智能运算...',
    background: 'rgba(0, 0, 0, 0.7)'
  })

  try {
    // 2. 调用真实后端 (取消注释)
    const res = await autoSchedule(requestData)

    // 3. 赋值结果 (适配后端返回结构)
    analysisResult.value = res
    showResultReport.value = true

    // --- 暂时保留 Mock 用于测试 UI ---
    setTimeout(() => {
        // ... 原来的 Mock 代码 ...
        loadingInstance.close()
    }, 1000)
    // -------------------------------

  } catch (e) {
    console.error(e)
    ElMessage.error('排班计算失败')
    loadingInstance.close()
  }
}

// 处理：应用结果 (报告页点击“应用”后触发)
const applySchedule = (solution) => {
  store.scheduleSolution = solution // 直接覆盖 Store
  ElMessage.success('排班方案已应用！')
}

// 处理：重试 (报告页点击“调整参数”后触发)
const handleRetry = () => {
  showResultReport.value = false
  showSmartSettings.value = true // 回到配置页
}
</script>

<style scoped lang="scss">
$primary-color: #ff9c00;
$bg-color: #f5f7fa;
$border-color: #e4e7ed;

/* 引入淡入淡出动画 (适配 transition name="el-zoom-in-center") */
.app-container {
  height: 100vh;
  background-color: $bg-color;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

// ================= UI 重构：上传页 (1:1 复刻 Course 风格) =================
.tool-upload-page {
  max-width: 1200px;
  margin: 0 auto;
  padding-bottom: 50px;
  width: 100%;
}

.tool-header {
  text-align: center;
  margin-bottom: 30px;
  margin-top: 20px;

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
      align-items: center;
      justify-content: center;
      gap: 5px;

      background: var(--el-fill-color);
      color: var(--text-color-regular);

      padding: 6px 16px;
      border-radius: 20px;
      font-size: 13px;
      font-weight: bold;
      border: 1px solid var(--border-color);

      span {
        font-size: 16px;
        font-family: 'Helvetica Neue', sans-serif;
        color: var(--el-color-primary);
        margin: 0 2px;
      }
    }

    /* 暗黑模式微调 */
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

.card-box {
  background: var(--bg-color-card);
  border-radius: 12px;
  border: 1px solid var(--border-color);
}

.upload-section {
  padding: 40px;
  text-align: center;

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

  /* 复刻 Course 的文件列表样式 */
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

    .el-upload-list__item {
      background-color: var(--bg-color-page);
      color: var(--text-color-regular);

      &:hover {
        background-color: var(--bg-color-overlay);
      }
    }
  }
}

.upload-demo {
  width: 100%;
}

// === 工作台样式 (保持原有) ===
.workspace-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;

  .workspace-toolbar {
    height: 60px;
    background: #fff;
    border-bottom: 1px solid $border-color;
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 20px;
    flex-shrink: 0;

    .left-section {
      display: flex;
      align-items: center;
      gap: 20px;

      .week-selector {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 14px;
        color: #606266;
      }
    }

    .right-section {
      display: flex;
      gap: 10px;
    }
  }

  .workspace-body {
    flex: 1;
    display: flex;
    overflow: hidden;

    .sidebar {
      width: 320px;
      background: #fff;
      border-right: 1px solid $border-color;
      display: flex;
      flex-direction: column;

      .sidebar-header {
        padding: 15px;
        border-bottom: 1px solid #f0f0f0;

        h3 {
          margin-bottom: 15px;
          color: #303133;
        }

        .filter-group {
          .filter-item {
            margin-bottom: 8px;
            width: 100%;
          }

          .filter-row {
            display: flex;
            gap: 8px;
          }

          .filter-half {
            flex: 1;
          }
        }
      }

      .sidebar-content {
        flex: 1;
        overflow-y: auto;
        padding: 10px;
        background: #f9f9f9;
        overscroll-behavior: contain;
        /* 阻止滚动事件冒泡到父容器 */

        .student-card {
          background: #fff;
          padding: 12px;
          border-radius: 6px;
          border: 1px solid #ebeef5;
          display: flex;
          align-items: center;
          margin-bottom: 8px;
          cursor: grab;
          transition: all 0.2s;

          &:hover {
            border-color: $primary-color;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
          }

          .card-avatar {
            width: 36px;
            height: 36px;
            background: #f2f6fc;
            color: #909399;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            margin-right: 12px;
            font-weight: bold;
          }

          .card-info {
            flex: 1;

            .name {
              font-weight: 600;
              color: #303133;
            }

            .meta {
              font-size: 12px;
              color: #909399;
              margin-top: 2px;
            }
          }

          .drag-icon {
            color: #c0c4cc;
          }
        }
      }
    }

    .schedule-grid {
      flex: 1;
      display: flex;
      flex-direction: column;
      padding: 20px 20px 80px 20px;
      overflow: auto;
      overscroll-behavior: contain;
      /* 阻止滚动事件冒泡到父容器 */

      .schedule-grid.screenshot-mode {
        display: inline-block !important;
        width: auto !important;
        max-width: none !important;
        overflow: visible !important;
      }

      .grid-header {
        display: flex;
        flex-shrink: 0;
        margin-bottom: 5px;
        position: sticky; // 添加这行
        top: 0; // 添加这行
        // background: #fff; // 添加这行，防止内容透出
        z-index: 10; // 添加这行，保证在内容之上
        padding-top: 20px; // 可选：与整体padding一致
        padding-bottom: 15px; // 可选：增加一些底部间隙

        .idx-col {
          width: 40px;
        }

        .day-col {
          flex: 1;
          text-align: center;
          background: #eef1f6;
          padding: 10px;
          margin: 0 2px;
          border-radius: 4px;
          font-weight: bold;
          color: #606266;
        }
      }

      .grid-rows {
        .grid-row {
          display: flex;
          margin-bottom: 4px;

          /* 【新增】特殊时段行样式 */
          &.special-row {
            margin-bottom: 8px;
            /* 特殊时段和下一节课之间多留点缝隙，增加区分度 */

            .idx-cell {
              background-color: #f2f6fc;
              /* 稍微深一点的背景 */
              color: $primary-color;
              /* 橙色字体 */
              font-size: 13px;
              writing-mode: vertical-lr;
              /* 竖排文字 (可选，如果文字是'早自习'可能放不下) */
              /* 或者如果不竖排，可以用小一点的字 */
              writing-mode: horizontal-tb;
              font-weight: 800;
              padding: 0 4px;
            }

            .task-cell {
              background-color: #fafafa;
              /* 格子背景稍微灰一点，表示非正课时间 */
              border-style: dashed;
              /* 边框用虚线，表示特殊性质 */
              background: #fff;
              /* 确保普通行背景 */
            }
          }

          .idx-cell {
            width: 40px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-weight: bold;
            color: #909399;
          }

          .task-cell {
            flex: 1;
            background: #fff;
            margin: 0 2px;
            border: 1px solid #ebeef5;
            border-radius: 4px;
            min-height: 100px;
            padding: 4px;
            position: relative;
            display: flex;
            flex-direction: column;
            cursor: pointer;
            transition: background 0.2s;

            &:hover {
              border-color: $primary-color;
              background-color: #fffcf0;

              .cell-action-overlay {
                opacity: 1;
                pointer-events: auto;
              }
            }

            &.hint-free {
              background-color: #f0f9eb !important;
              border-color: #67c23a !important;
            }

            &.hint-busy {
              background-color: #fef0f0 !important;
              border-color: #f56c6c !important;
            }

            .cell-draggable {
              flex: 1;
              display: flex;
              flex-wrap: wrap;
              gap: 4px;
              align-content: flex-start;
            }

            .student-tag {
              position: relative;
              z-index: 10;
              cursor: pointer;

              .warn-icon {
                margin-left: 2px;
              }
            }

            .cell-action-overlay {
              position: absolute;
              top: 0;
              left: 0;
              right: 0;
              bottom: 0;
              display: flex;
              align-items: flex-end;
              justify-content: flex-end;
              padding: 5px;
              pointer-events: none;
              opacity: 0;
              transition: opacity 0.2s;
              z-index: 5;

              .add-btn {
                pointer-events: auto;
                box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
              }
            }
          }
        }
      }
    }
  }
}

.ghost-card {
  opacity: 0.5;
  border: 1px dashed $primary-color !important;
  background: #ecf5ff !important;
}

.ghost-tag {
  opacity: 0.5;
}

.dialog-header-custom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  border-bottom: 1px solid #eee;
  padding-bottom: 10px;

  .highlight {
    color: $primary-color;
  }
}

.dialog-list {
  max-height: 400px;
  overflow-y: auto;

  .dialog-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px;
    border-bottom: 1px solid #f5f7fa;
    cursor: pointer;
    border-radius: 4px;

    &:hover {
      background: #f0f2f5;
    }

    &.is-conflict {
      background: #fef0f0;
    }

    &.is-added {
      opacity: 0.6;
      cursor: not-allowed;
    }

    .item-left {
      display: flex;
      align-items: center;
      gap: 10px;

      .avatar {
        width: 32px;
        height: 32px;
        background: #eee;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
      }

      .text {
        display: flex;
        /* 新增：改为 flex 布局 */
        flex-direction: column;
        /* 新增：垂直排列 */
        overflow: hidden;
        /* 新增：防止溢出 */

        .name {
          font-weight: 600;
          font-size: 15px;
          /* 微调：稍微加大名字字号 */
          color: #303133;
        }

        /* 【新增】元数据样式 */
        .meta-info {
          display: flex;
          /* 关键：改为 Flex 布局，让大家排排坐 */
          align-items: center;
          /* 垂直居中 */
          font-size: 12px;
          color: #909399;
          margin: 4px 0;

          /* 注意：删掉父级原来的 white-space, overflow, text-overflow */
          /* 这些属性要移到子元素里去 */

          .divider {
            margin: 0 4px;
            color: #e0e0e0;
            flex-shrink: 0;
            /* 分割线永远不许缩小 */
          }

          /* 通用的截断逻辑 */
          .meta-item {
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
          }

          /* --- 关键：分别为三者设置最大宽度 --- */
          /* 你可以根据实际弹窗宽度微调这些 px 值 */

          .grade {
            max-width: 50px;
            /* 年级一般很短，50px 够了 */
            flex-shrink: 0;
            /* 年级尽量不要被压缩 */
          }

          .college {
            max-width: 140px;
            /* 学院给最大的空间，超过显示 ... */
          }

          .major {
            max-width: 100px;
            /* 专业给中等空间，超过显示 ... */
          }
        }

        .desc {
          margin-top: 0;
          /* 修改：去掉之前的 margin-top，由 meta-info 控制间距 */
        }
      }
    }
  }
}

.custom-divider {
  height: 24px;
  /* 调整为你期望的高度 */
  align-self: center;
  /* 在 flex 容器中垂直居中 */
}
</style>
<style>
.dragging-card-fallback {
  /* 强制设置背景色和边框，防止变成透明 */
  background: #fff !important;
  border: 1px solid #ff9c00 !important;
  /* 拖拽时给个橙色边框提示 */
  border-radius: 6px;
  padding: 12px;
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.15);
  /* 加点阴影，更有立体感 */

  /* 保持原有的 Flex 布局，防止内容塌陷 */
  display: flex !important;
  align-items: center;
  width: 280px;
  /* 强制固定宽度，防止脱离父级后宽度变样 */
  opacity: 1 !important;
  /* 确保不透明 */
  z-index: 9999 !important;
  /* 确保在最上层 */
}

/* 修复拖拽时内部元素的布局 */
.dragging-card-fallback .card-avatar {
  width: 36px;
  height: 36px;
  background: #f2f6fc;
  color: #909399;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  font-weight: bold;
}

.dragging-card-fallback .card-info {
  flex: 1;
}

.dragging-card-fallback .name {
  font-weight: 600;
  color: #303133;
}

.dragging-card-fallback .meta {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.dragging-card-fallback .drag-icon {
  color: #ff9c00;
  /* 拖拽时图标变色 */
}
</style>
