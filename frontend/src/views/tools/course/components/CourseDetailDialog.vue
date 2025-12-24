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

const activeTab = ref('status') // status | grade | course | location | class
const sortType = ref('priority')

// 辅助：周几转换
const weekMap = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

// 核心计算：分组逻辑
const groupedData = computed(() => {
  const { freeStudents, busyStudents } = props.slotData || { freeStudents: [], busyStudents: [] }
  const result = {}

  // 1. 定义排序函数 (权重：小课 > 大课 > 空闲 > 普通忙碌)
  const sortFunc = (a, b) => {
    if (sortType.value === 'default') return 0 // 保持原序

    // 计算权重
    const getWeight = (s) => {
      if (s.isSmallMatch) return 4
      if (s.isBigMatch) return 3
      if (!s.currentCourse) return 2 // 空闲
      return 1 // 普通忙碌
    }
    return getWeight(b) - getWeight(a) // 降序排列
  }

  // 2. 准备全量数据并应用排序 (如果是按年级、班级等分组，需要先合并再排序)
  // 注意：按状态分组时，组内已经固定了性质，排序意义不大，主要用于 年级/班级/地点 分组

  if (activeTab.value === 'status') {
    const smalls = busyStudents.filter(s => s.isSmallMatch).sort(sortFunc)
    const bigs = busyStudents.filter(s => s.isBigMatch).sort(sortFunc)
    const others = busyStudents.filter(s => !s.isSmallMatch && !s.isBigMatch).sort(sortFunc)
    const frees = [...freeStudents].sort(sortFunc)

    if (smalls.length) result['📘 小课匹配 (' + smalls.length + ')'] = smalls
    if (bigs.length) result['📙 大课匹配 (' + bigs.length + ')'] = bigs
    result['✅ 空闲 (' + frees.length + ')'] = frees
    if (others.length) result['❌ 其他忙碌 (' + others.length + ')'] = others
  }
  else {
    // 对于 Grade, Course, Location, Class，我们先合并所有学生
    let all = [...freeStudents, ...busyStudents]

    // 先排序，确保分组后的数组是有序的
    all.sort(sortFunc)

    all.forEach(s => {
      let key = '其他'
      if (activeTab.value === 'grade') key = s.grade ? s.grade + '级' : '未知年级'
      else if (activeTab.value === 'course') key = s.currentCourse ? s.currentCourse.name : '[空闲人员]'
      else if (activeTab.value === 'location') {
        key = s.currentCourse && s.currentCourse.location ? s.currentCourse.location : '无课/空闲'
      }
      else if (activeTab.value === 'class') key = s.className || '未分配班级'

      if (!result[key]) result[key] = []
      result[key].push(s)
    })

    // 对分组的 Key 进行排序 (可选，比如按年级从大到小)
    return Object.keys(result).sort().reduce((obj, k) => {
      obj[k] = result[k]; return obj
    }, {})
  }

  return result
})

const handleClose = () => {
  emit('update:visible', false)
}

// 【重构】弹窗详情复制 (支持分组 + 状态图标)
const handleCopy = async () => {
  const separator = "----------------------------------------"

  // 1. 标题头
  let content = `📊 课程详情深度统计\n`
  content += `⏰ 时间：第${props.weekInfo.week}周 ${weekMap[props.weekInfo.day - 1]} 第${props.weekInfo.slot}节\n`

  // 获取当前的分组模式名称
  const tabNameMap = {
    'status': '按状态归类',
    'grade': '按年级归类',
    'course': '按课程归类',
    'location': '按地点归类',
    'class': '按班级归类'
  }
  content += `📍 统计维度：${tabNameMap[activeTab.value] || '默认'}\n`
  content += `${separator}\n`

  // 2. 遍历分组数据
  for (const group in groupedData.value) {
    const list = groupedData.value[group]
    // 分组标题
    content += `\n📦 【${group}】 (共${list.length}人)\n`

    // 遍历组内学生
    list.forEach(s => {
      // 智能判断前缀图标
      let icon = '❌' // 默认忙碌
      if (!s.currentCourse) icon = '✅' // 空闲
      else if (s.isSmallMatch) icon = '📘' // 小课
      else if (s.isBigMatch) icon = '📙' // 大课

      // 班级信息
      const classStr = s.className ? `【${s.className}】` : ''

      // 核心信息
      let detailStr = `${classStr}${s.name}`

      // 如果有课，追加课程详情
      if (s.currentCourse) {
        detailStr += ` - ${s.currentCourse.location} (${s.currentCourse.name})`
      } else {
        detailStr += ` (空闲)`
      }

      content += `   ${icon} ${detailStr}\n`
    })
  }

  content += `\n${separator}\n生成时间：${new Date().toLocaleString()}`

  try {
    await copyText(content)
    ElMessage.success('当前视图详情已复制！')
  } catch (e) {
    ElMessage.error('复制失败')
  }
}
</script>

<template>
  <el-dialog :model-value="visible" title="课程详情深度分析" width="600px" @close="handleClose" append-to-body destroy-on-close>
<div class="dialog-header">
      <div class="header-left">
        <div class="info-tag">
          <span class="highlight">第 {{ weekInfo.week }} 周</span>
          <span class="sep">|</span>
          <span>{{ weekMap[weekInfo.day - 1] }}</span>
          <span class="sep">|</span>
          <span>第 {{ weekInfo.slot }} 节</span>
        </div>
      </div>

      <div class="header-right">
        <el-radio-group v-model="activeTab" size="small" style="margin-right: 15px">
          <el-radio-button label="status">状态</el-radio-button>
          <el-radio-button label="grade">年级</el-radio-button>
          <el-radio-button label="course">课程</el-radio-button>
          <el-radio-button label="location">地点</el-radio-button>
          <el-radio-button label="class">班级</el-radio-button>
        </el-radio-group>

        <div class="sort-control">
          <span class="label">排序:</span>
          <el-radio-group v-model="sortType" size="small">
            <el-radio label="default">默认</el-radio>
            <el-radio label="priority">优先级</el-radio>
          </el-radio-group>
        </div>
      </div>
    </div>
    <div class="scroll-container">
      <div v-for="(list, groupName) in groupedData" :key="groupName" class="group-block">
        <div class="group-title">{{ groupName }}</div>
        <div class="card-grid">
          <div v-for="s in list" :key="s.name" class="student-card" :class="{
              'is-free': !s.currentCourse,
              'is-small': s.isSmallMatch,
              'is-big': s.isBigMatch
            }">
            <div class="card-top">
              <span class="name">{{ s.name }}</span>
              <span class="major">{{ s.major }}</span>
            </div>

            <div v-if="s.currentCourse" class="course-info">
              <div class="row"><el-icon>
                  <School />
                </el-icon> {{ s.currentCourse.name }}</div>
              <div class="row"><el-icon>
                  <User />
                </el-icon> {{ s.currentCourse.teacher }}</div>
              <div class="row"><el-icon>
                  <Location />
                </el-icon> {{ s.currentCourse.location }}</div>
              <div class="row"><el-icon>
                  <Monitor />
                </el-icon> {{ s.className || '暂无班级' }}</div>
              <div class="row" v-if="s.code"><el-icon>
                  <User />
                </el-icon> {{ s.code }}</div>
            </div>

            <div v-else class="basic-info">
              <div class="row"><el-icon>
                  <Monitor />
                </el-icon> {{ s.className || '暂无班级' }}</div>
              <div class="row" v-if="s.code"><el-icon>
                  <User />
                </el-icon> {{ s.code }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="handleClose">关闭</el-button>
      <el-button type="primary" @click="handleCopy">
        <el-icon>
          <CopyDocument />
        </el-icon> 复制本页统计
      </el-button>
    </template>
  </el-dialog>
</template>

<style lang="scss" scoped>
.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding-bottom: 15px;
  border-bottom: 1px solid var(--border-color);
  flex-wrap: wrap; /* 防止小屏挤压 */
  gap: 10px;
}

.header-right {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.sort-control {
  display: flex;
  align-items: center;
  background: var(--el-fill-color);
  padding: 2px 10px;
  border-radius: 12px;

  .label {
    font-size: 12px;
    color: var(--text-color-secondary);
    margin-right: 8px;
  }
}

/* 保持原有的 info-tag 样式 */
.info-tag {
  font-size: 16px;
  font-weight: bold;
  .highlight { color: var(--el-color-primary); }
  .sep { margin: 0 8px; color: #ddd; }
}

.scroll-container {
  max-height: 500px;
  overflow-y: auto;
  padding-right: 5px;
}

.group-title {
  font-size: 14px;
  font-weight: bold;
  margin: 15px 0 10px;
  padding-left: 8px;
  border-left: 3px solid var(--el-color-primary);
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 10px;
}

.student-card {
  border: 1px solid var(--border-color);
  border-radius: 4px;
  padding: 10px;
  font-size: 12px;
  background: var(--bg-color-overlay);

  &.is-free { border-left: 3px solid var(--el-color-success); }
  &.is-small { border-left: 3px solid #409eff; background: rgba(64,158,255,0.05); } /* 蓝色标记 */
  &.is-big { border-left: 3px solid #e6a23c; background: rgba(230,162,60,0.05); } /* 橙色标记 */

  .card-top {
    display: flex;
    justify-content: space-between;
    margin-bottom: 8px;
    font-weight: bold;
    font-size: 13px;

    .major {
      font-weight: normal;
      color: #999;
      font-size: 12px;
    }
  }

  .row {
    display: flex;
    align-items: center;
    gap: 4px;
    margin-bottom: 2px;
    color: var(--text-color-secondary);
    text-overflow: ellipsis;
    white-space: nowrap;
    overflow: hidden;
  }
}
</style>
