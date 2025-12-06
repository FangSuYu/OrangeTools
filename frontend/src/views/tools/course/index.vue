<script setup>
import { ref, computed, onMounted } from 'vue'
import { UploadFilled, DataAnalysis, Delete, User, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { analyzeCourse } from '@/api/tools/course'
import { useCourseStore } from '@/stores/modules/course'

// 使用 Store (持久化存储)
const courseStore = useCourseStore()

// ================== 1. 状态定义 ==================
const loading = ref(false)
const uploadRef = ref(null)
const fileList = ref([])
// 视图模式: 'list' (名单模式) | 'heatmap' (热力图模式)
const viewMode = ref('list')
// 占位符数据：累计服务次数
const usageCount = ref(1284)

// 筛选表单
const filters = ref({
  college: '',
  major: '',
  grade: ''
})

// ================== 2. 计算属性 (核心逻辑) ==================

// 判断是否有分析结果
const hasResult = computed(() => !!courseStore.analysisResult)

// 获取原始结果
const rawResult = computed(() => courseStore.analysisResult || {
  allColleges: [], allMajors: [], allGrades: [], schedule: {}
})

/**
 * 【前端核心算法】根据筛选条件，动态计算当前的课表状态
 * 这样用户切换筛选时，不需要请求后端，瞬间完成渲染
 */
const filteredSchedule = computed(() => {
  if (!hasResult.value) return {}

  const rawSchedule = rawResult.value.schedule
  const result = {}

  // 遍历 1-10 节课
  for (let slot = 1; slot <= 10; slot++) {
    result[slot] = {}
    // 遍历 周一到周日
    for (let day = 1; day <= 7; day++) {
      const cell = rawSchedule[slot]?.[day]
      if (!cell) continue

      // 过滤空闲学生
      const validFree = cell.freeStudents.filter(s => matchFilter(s))
      // 过滤忙碌学生 (用于热力图计算)
      const validBusy = cell.busyStudents.filter(s => matchFilter(s))

      result[slot][day] = {
        freeCount: validFree.length,
        freeStudents: validFree,
        busyCount: validBusy.length,
        busyStudents: validBusy,
        // 计算忙碌比例 (0.0 - 1.0)，用于热力图颜色深浅
        busyRatio: (validBusy.length + validFree.length) > 0
          ? validBusy.length / (validBusy.length + validFree.length)
          : 0
      }
    }
  }
  return result
})

// 辅助函数：判断学生是否符合当前筛选条件
function matchFilter(student) {
  if (filters.value.college && student.college !== filters.value.college) return false
  if (filters.value.major && student.major !== filters.value.major) return false
  if (filters.value.grade && student.grade !== filters.value.grade) return false
  return true
}

// ================== 3. 交互逻辑 ==================

// 上传并分析
const handleAnalyze = async () => {
  if (fileList.value.length === 0) {
    ElMessage.warning('请先选择 Excel 文件')
    return
  }

  loading.value = true
  try {
    // 构建 FormData
    const formData = new FormData()
    fileList.value.forEach(file => {
      formData.append('files', file.raw)
    })

    // 调用接口
    const res = await analyzeCourse(formData)

    // 存入 Pinia (自动持久化)
    courseStore.setAnalysisResult(res)
    ElMessage.success(`分析完成！共处理 ${res.totalPeople} 人`)

    // 清空上传列表，准备查看结果
    fileList.value = []
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 重置所有数据 (重新上传)
const handleReset = () => {
  courseStore.clearData()
  filters.value = { college: '', major: '', grade: '' }
}

// 颜色生成器 (热力图)
// 根据忙碌比例返回颜色：绿(空闲) -> 黄 -> 红(忙碌)
const getCellColor = (ratio) => {
  if (ratio === 0) return 'rgba(0, 255, 0, 0.2)' // 纯绿，20%透明度 (看着清爽)

  // 红色是 0度，绿色是 120度
  // 随着 ratio 增大 (0 -> 1)，hue 从 120 -> 0
  const hue = (1 - ratio) * 120

  // 使用 hsla，设置 60% 的透明度，让底下的网格线若隐若现，更有质感
  return `hsla(${hue}, 100%, 50%, 0.6)`
}
</script>

<template>
  <div class="app-container">

    <div class="tool-header">
      <div class="header-content">
        <h1>📅 课表空闲统计助手</h1>
        <p class="desc">批量上传 Excel 课表，一键分析全员空闲时间，学生会/班委必备神器。</p>
        <div class="stats-badge">
          🔥 已累计服务 <span>{{ usageCount }}</span> 人次
        </div>
      </div>
    </div>

    <transition name="el-zoom-in-center">
      <div v-if="!hasResult" class="upload-section card-box">
        <el-upload ref="uploadRef" v-model:file-list="fileList" class="upload-demo" drag multiple :auto-upload="false"
          accept=".xlsx, .xls">
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">
            将 Excel 课表文件拖到此处，或 <em>点击上传</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              支持批量上传，只能上传 xlsx/xls 文件
            </div>
          </template>
        </el-upload>

        <div class="actions">
          <el-button type="primary" size="large" :loading="loading" @click="handleAnalyze" round>
            <el-icon><DataAnalysis /></el-icon>
            <span>开始智能分析</span>
          </el-button>
        </div>
      </div>
    </transition>

    <transition name="el-fade-in-linear">
      <div v-if="hasResult" class="result-section">

        <div class="filter-bar card-box">
          <div class="left-filters">
            <span class="label"><el-icon>
                <Search />
              </el-icon> 筛选维度：</span>

            <el-select v-model="filters.college" placeholder="全部学院" clearable style="width: 160px">
              <el-option v-for="c in rawResult.allColleges" :key="c" :label="c" :value="c" />
            </el-select>

            <el-select v-model="filters.major" placeholder="全部专业" clearable style="width: 160px; margin-left: 10px">
              <el-option v-for="m in rawResult.allMajors" :key="m" :label="m" :value="m" />
            </el-select>

            <el-select v-model="filters.grade" placeholder="全部年级" clearable style="width: 140px; margin-left: 10px">
              <el-option v-for="g in rawResult.allGrades" :key="g" :label="g" :value="g" />
            </el-select>
          </div>

          <div class="right-actions">
            <el-radio-group v-model="viewMode" size="small">
              <el-radio-button label="list">名单模式</el-radio-button>
              <el-radio-button label="heatmap">热力图模式</el-radio-button>
            </el-radio-group>

            <el-button type="danger" plain style="margin-left: 15px" @click="handleReset">
              <el-icon><Delete /></el-icon>
              <span>清空重置</span>
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
            <tbody>
              <tr v-for="slot in 10" :key="slot">
                <td class="slot-idx">第 {{ slot }} 节</td>

                <td v-for="day in 7" :key="day" class="cell-wrapper"
                  :style="viewMode === 'heatmap' ? { backgroundColor: getCellColor(filteredSchedule[slot]?.[day]?.busyRatio) } : {}">
                  <template v-if="filteredSchedule[slot] && filteredSchedule[slot][day]">

                    <div v-if="viewMode === 'list'" class="mode-list">
                      <el-tooltip placement="top" :show-after="200" :hide-after="0" transition="none"
                        :enterable="false">
                        <template #content>
                          <div class="tooltip-list">
                            <div v-for="s in filteredSchedule[slot][day].freeStudents" :key="s.code + s.name">
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

                  </template>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

      </div>
    </transition>
  </div>
</template>

<style lang="scss" scoped>
.app-container {
  max-width: 1200px;
  margin: 0 auto;
  padding-bottom: 50px;
}

/* 头部样式 */
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
      display: inline-block;
      background: var(--el-color-primary-light-9);
      color: var(--el-color-primary);
      padding: 4px 12px;
      border-radius: 20px;
      font-size: 13px;
      font-weight: bold;

      span {
        font-size: 16px;
      }
    }
  }
}

/* 上传区样式 */
.upload-section {
  padding: 40px;
  text-align: center;

  /* 【Issue 1 修复】强制按钮内容居中 */
.actions {
  margin-top: 20px;
  /* 确保按钮容器本身居中 */
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 15px;

  /* 【修复】强制穿透修改 Element Plus 按钮内部布局 */
  :deep(.el-button) {
    /* 强制内部使用 Flex 居中 */
    display: inline-flex !important;
    justify-content: center !important;
    align-items: center !important;

    /* 修复图标和文字的间距 */
    span {
      margin-left: 5px;
      display: inline-flex;
      align-items: center;
    }

    /* 针对图标 */
    .el-icon {
      margin-right: 0;
    }
  }
}

  /* 【Issue 2 修复】限制上传列表高度，防止撑爆页面 */
  :deep(.el-upload-list) {
    max-height: 200px;
    overflow-y: auto;
    margin-top: 10px;
    text-align: left;
    /* 文件名靠左 */
    border: 1px solid var(--border-color);
    border-radius: 4px;
    padding: 5px;

    /* 变成两列布局 (可选，如果文件名不长) */
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: 5px;
  }
}

/* 筛选栏样式 */
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
    /* 【Issue 1 修复】确保右侧按钮内容也居中 */
    display: flex;
    align-items: center;
  }
}

/* 课表样式 (自定义 Table) */
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
  height: 60px;
  vertical-align: middle;
  cursor: default; /* 改为默认光标，减少浏览器的交互计算 */

  /* 【性能优化】移除 z-index 和 position 变化，这会触发重排 */
  /* outline 性能很好，可以保留，但颜色不要带透明度 */
  &:hover {
    outline: 2px solid var(--el-color-primary);
  }
}
}

/* 列表模式 */
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

/* 热力图模式 */
.mode-heatmap {
  .ratio-text {
    font-size: 12px;
    font-weight: bold;
    color: #333;
  }
}


.tooltip-list {
  max-height: 300px;
  overflow-y: auto;
  line-height: 1.8;
}
</style>
