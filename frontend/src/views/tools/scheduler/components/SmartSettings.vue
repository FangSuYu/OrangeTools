<template>
  <el-dialog v-model="visible" title="🤖 智能排班配置" width="1100px" top="5vh" :close-on-click-modal="false"
    class="smart-settings-dialog" destroy-on-close>
    <div class="settings-container">

      <aside class="settings-sidebar">
        <div class="sidebar-section fixed-top">
          <h3>1. 全局约束</h3>
          <el-form label-position="top">
            <el-form-item label="默认每班次人数（每个节次默认2人）">
              <el-input-number v-model="config.defaultCount" :min="1" :max="10" class="w-100" />
            </el-form-item>
            <el-form-item label="每人每周最大班次（每人每周最多值班次数）">
              <el-input-number v-model="config.maxPerWeek" :min="1" :max="50" class="w-100" />
            </el-form-item>
          </el-form>
        </div>

        <div class="sidebar-section scrollable-list">
          <h3>2. 排班策略</h3>
          <div class="strategy-list">
            <div v-for="algo in algorithms" :key="algo.value" class="strategy-card"
              :class="{ active: config.algorithm === algo.value }" @click="config.algorithm = algo.value">
              <div class="card-icon">{{ algo.icon }}</div>
              <div class="card-content">
                <div class="card-title">{{ algo.label }}</div>
                <div class="card-desc">{{ algo.desc }}</div>
              </div>
              <div class="card-check" v-if="config.algorithm === algo.value">
                <el-icon>
                  <Check />
                </el-icon>
              </div>
            </div>
          </div>
        </div>

        <div class="sidebar-footer">
          <el-button type="danger" link @click="resetConfig">重置所有配置</el-button>
        </div>
      </aside>

      <main class="settings-main" @mouseup="endPaint" @mouseleave="endPaint">
        <div class="main-header">
          <div class="header-left">
            <div class="title-row">
              <h3>3. 需求明细调整</h3>
              <div class="legend">
                <span class="dot zero"></span> 0人
                <span class="dot normal"></span> {{ config.defaultCount }}人
                <span class="dot high"></span> >{{ config.defaultCount }}人
              </div>
            </div>
            <div class="sub-text">
              左键点击格子修改，或切换刷子后<b>按住左键拖动</b>批量涂抹。
            </div>
          </div>

          <div class="header-right">
            <div class="brush-tools">
              <el-radio-group v-model="paintMode" size="small">
                <el-radio-button value="edit">
                  <el-icon style="vertical-align: middle; margin-right: 3px">
                    <Edit />
                  </el-icon>
                  修改框
                </el-radio-button>
                <el-radio-button value="zero">橡皮(0)</el-radio-button>
                <el-radio-button value="default">默认({{ config.defaultCount }})</el-radio-button>
                <el-radio-button value="custom">自定义</el-radio-button>
              </el-radio-group>

              <div v-if="paintMode === 'custom'" class="custom-input-wrapper">
                <el-input-number v-model="customBrushNum" :min="1" :max="20" size="small" controls-position="right"
                  style="width: 80px;" />
                <span class="unit">人</span>
              </div>
            </div>
          </div>
        </div>

        <div class="heatmap-container">
          <div class="grid-row header-row sticky-top">
            <div class="corner-cell">节次</div>
            <div v-for="day in 7" :key="day" class="day-header" @click="batchSetCol(day)" title="点击可设置整列">
              {{ ['周一', '周二', '周三', '周四', '周五', '周六', '周日'][day - 1] }}
            </div>
          </div>

          <div v-for="row in timeLayout" :key="row.id" class="grid-row body-row"
            :class="{ 'special-row': row.type === 'special' }">
            <div class="row-header" @click="batchSetRow(row.id)" title="点击可设置整行">
              {{ row.alias || row.label }}
            </div>

            <div v-for="day in 7" :key="day" class="req-cell" :class="getCellClass(day, row.id)"
              @mousedown="(e) => startPaint(day, row.id, e)" @mouseenter="onPaintHover(day, row.id)">
              {{ getReqCount(day, row.id) }}
            </div>
          </div>
        </div>
      </main>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <div class="footer-left">
          <span v-if="lastSavedTime" class="save-tip">
            <el-icon>
              <Clock />
            </el-icon> 上次自动保存: {{ lastSavedTime }}
          </span>
        </div>
        <div class="footer-right">
          <el-button @click="visible = false">取消</el-button>
          <el-button type="primary" size="large" @click="handleConfirm">
            开始计算
          </el-button>
        </div>
      </div>
    </template>

    <el-popover :visible="popoverVisible" :virtual-ref="popoverRef" virtual-triggering trigger="manual" width="200px"
      placement="top">
      <div class="popover-content">
        <div class="popover-title">设置需求人数</div>
        <el-input-number v-model="currentEditValue" :min="0" :max="50" size="small" @change="saveCurrentEdit" />
        <div class="quick-btns">
          <el-button size="small" text bg @click="quickSet(0)">0</el-button>
          <el-button size="small" text bg @click="quickSet(config.defaultCount)">默认</el-button>
        </div>
        <div style="margin-top: 10px; text-align: right;">
          <el-button type="primary" size="small" link @click="closePopover">完成</el-button>
        </div>
      </div>
    </el-popover>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { Check, Clock, Edit } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const props = defineProps({
  modelValue: Boolean,
  studentPool: Array
})

const emit = defineEmits(['update:modelValue', 'confirm'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

// --- 常量定义 ---
const STORAGE_KEY = 'orange_scheduler_config_draft'

const timeLayout = [
  { id: 0, label: '早', alias: '早自习', type: 'special' },
  { id: 1, label: '1', type: 'normal' },
  { id: 2, label: '2', type: 'normal' },
  { id: 3, label: '3', type: 'normal' },
  { id: 4, label: '4', type: 'normal' },
  { id: 11, label: '午', alias: '午休', type: 'special' },
  { id: 5, label: '5', type: 'normal' },
  { id: 6, label: '6', type: 'normal' },
  { id: 7, label: '7', type: 'normal' },
  { id: 8, label: '8', type: 'normal' },
  { id: 12, label: '晚', alias: '傍晚', type: 'special' },
  { id: 9, label: '9', type: 'normal' },
  { id: 10, label: '10', type: 'normal' },
  { id: 13, label: '夜', alias: '晚间', type: 'special' }
]

const algorithms = [
  { value: 'GREEDY_BALANCE', label: '⚖️ 负载均衡优先', desc: '优先安排工时最少的人，确保大家忙闲均匀。', icon: '⚖️' },
  { value: 'CONSECUTIVE', label: '🔗 连班优先', desc: '尽量让人连续工作(如1-2节)，减少碎片时间。', icon: '🔗' },
  { value: 'HUNGARIAN', label: '💎 全局最优解', desc: '使用匈牙利算法/费用流，寻找数学上的理论最优匹配。', icon: '💎' },
  { value: 'RANDOM', label: '🎲 随机分配', desc: '完全随机分配，仅满足硬性冲突条件。', icon: '🎲' }
]

// --- 核心配置 ---
const config = ref({
  defaultCount: 2,
  maxPerWeek: 5,
  algorithm: 'GREEDY_BALANCE',
  requirements: {}
})

const lastSavedTime = ref('')

// --- 涂抹（刷子）工具状态 ---
const isPainting = ref(false)
// 模式: 'edit'(修改框), 'zero'(橡皮), 'default'(默认), 'custom'(自定义)
const paintMode = ref('edit')
const customBrushNum = ref(3) // 自定义刷子的数值

// 辅助函数：获取当前刷子的实际数值
const getPaintValue = () => {
  if (paintMode.value === 'edit') return -1
  if (paintMode.value === 'zero') return 0
  if (paintMode.value === 'default') return config.value.defaultCount
  if (paintMode.value === 'custom') return customBrushNum.value
  return -1
}

// --- 本地存储 (不变) ---
let saveTimer = null
watch(config, () => {
  if (saveTimer) clearTimeout(saveTimer)
  saveTimer = setTimeout(() => {
    const dataToSave = { config: config.value, timestamp: new Date().toLocaleString() }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(dataToSave))
    lastSavedTime.value = dataToSave.timestamp
  }, 1000)
}, { deep: true })

onMounted(() => {
  const saved = localStorage.getItem(STORAGE_KEY)
  if (saved) {
    try {
      const parsed = JSON.parse(saved)
      if (parsed.config) {
        config.value = { ...config.value, ...parsed.config }
        lastSavedTime.value = parsed.timestamp
      }
    } catch (e) { console.error(e) }
  }
})

const resetConfig = () => { /* ...保持原样... */
  ElMessageBox.confirm('重置配置?', '提示', { type: 'warning' }).then(() => {
    config.value = { defaultCount: 2, maxPerWeek: 5, algorithm: 'GREEDY_BALANCE', requirements: {} }
    localStorage.removeItem(STORAGE_KEY)
  })
}

// --- 核心交互逻辑 ---
const getReqCount = (day, section) => {
  const key = `${day}_${section}`
  return config.value.requirements[key] !== undefined ? config.value.requirements[key] : config.value.defaultCount
}

const getCellClass = (day, section) => {
  const count = getReqCount(day, section)
  if (count === 0) return 'cell-zero'
  if (count > config.value.defaultCount) return 'cell-high'
  return 'cell-normal'
}

// Popover 逻辑修复
const popoverVisible = ref(false)
const popoverRef = ref()
const currentEditKey = ref('')
const currentEditValue = ref(0)

const openCellEdit = async (day, section, e) => {
  // 关键修复：关闭画笔模式
  isPainting.value = false

  const key = `${day}_${section}`
  currentEditKey.value = key
  currentEditValue.value = getReqCount(day, section)

  // 关键修复：使用 unrefElement 或者确保 DOM 存在
  // 这里直接保存 e.currentTarget
  popoverRef.value = e.currentTarget

  // 关键修复：等待 DOM 更新
  await nextTick()
  popoverVisible.value = true
}

const closePopover = () => {
  popoverVisible.value = false
}

const saveCurrentEdit = () => {
  updateRequirement(currentEditKey.value, currentEditValue.value)
}

const quickSet = (val) => {
  currentEditValue.value = val
  saveCurrentEdit()
}

// 统一更新方法
const updateRequirement = (key, val) => {
  if (!key) return
  if (val === config.value.defaultCount) {
    const newReq = { ...config.value.requirements }
    delete newReq[key]
    config.value.requirements = newReq
  } else {
    config.value.requirements = { ...config.value.requirements, [key]: val }
  }
}

// --- 涂抹（刷子）逻辑 ---
const startPaint = (day, section, e) => {
  if (e.button !== 0) return

  const val = getPaintValue()

  // 如果是修改框模式，则打开弹窗
  if (val === -1) {
    openCellEdit(day, section, e)
    return
  }

  // 否则开始涂抹
  isPainting.value = true
  const key = `${day}_${section}`
  updateRequirement(key, val) // 使用计算出来的 val
  popoverVisible.value = false
}

const onPaintHover = (day, section) => {
  if (!isPainting.value) return
  const val = getPaintValue()
  if (val === -1) return

  const key = `${day}_${section}`
  updateRequirement(key, val) // 使用计算出来的 val
}

const endPaint = () => {
  isPainting.value = false
}

// 批量设置 (保持原样)
const batchSetRow = (sectionId) => { /* ...保持原样... */
  ElMessageBox.prompt('输入人数', '行设置', { inputPattern: /^\d+$/ }).then(({ value }) => {
    const num = parseInt(value)
    const newReq = { ...config.value.requirements }
    for (let d = 1; d <= 7; d++) {
      const key = `${d}_${sectionId}`
      if (num === config.value.defaultCount) delete newReq[key]
      else newReq[key] = num
    }
    config.value.requirements = newReq
  })
}
const batchSetCol = (day) => { /* ...保持原样... */
  ElMessageBox.prompt('输入人数', '列设置', { inputPattern: /^\d+$/ }).then(({ value }) => {
    const num = parseInt(value)
    const newReq = { ...config.value.requirements }
    timeLayout.forEach(row => {
      const key = `${day}_${row.id}`
      if (num === config.value.defaultCount) delete newReq[key]
      else newReq[key] = num
    })
    config.value.requirements = newReq
  })
}

const handleConfirm = () => {
  const payload = {
    strategy: config.value.algorithm,
    maxPerWeek: config.value.maxPerWeek,
    defaultCount: config.value.defaultCount,
    requirements: []
  }
  for (let d = 1; d <= 7; d++) {
    timeLayout.forEach(row => {
      const count = getReqCount(d, row.id)
      if (count > 0) {
        payload.requirements.push({ day: d, section: row.id, count: count })
      }
    })
  }
  emit('confirm', payload)
  visible.value = false
}
</script>

<style scoped lang="scss">
$primary-color: #ff9c00;
$border-color: #e4e7ed;

.settings-container {
  display: flex;
  height: 650px;
  border: 1px solid $border-color;
  border-radius: 8px;
  overflow: hidden;
}

/* 左侧侧边栏 - Flex布局修复滚动 */
.settings-sidebar {
  width: 320px;
  background: #f9fafe;
  border-right: 1px solid $border-color;
  padding: 20px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  /* 防止撑开 */

  .sidebar-section {
    margin-bottom: 20px;

    h3 {
      font-size: 16px;
      font-weight: bold;
      margin-bottom: 15px;
      color: #303133;
      border-left: 4px solid $primary-color;
      padding-left: 10px;
    }
  }

  /* 策略列表可滚动 */
  .scrollable-list {
    flex: 1;
    overflow-y: auto;
    min-height: 0;
    /* 关键：允许flex子项收缩 */
    padding-right: 5px;
    /* 防止滚动条遮挡 */

    .strategy-list {
      display: flex;
      flex-direction: column;
      gap: 10px;
    }
  }

  .strategy-card {
    background: #fff;
    border: 1px solid $border-color;
    border-radius: 8px;
    padding: 12px;
    cursor: pointer;
    display: flex;
    align-items: flex-start;
    gap: 10px;
    transition: all 0.2s;
    position: relative;
    flex-shrink: 0;
    /* 防止被压缩 */

    &:hover {
      border-color: $primary-color;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
    }

    &.active {
      border-color: $primary-color;
      background: #fffcf0;
    }

    .card-icon {
      font-size: 24px;
      margin-top: 2px;
    }

    .card-content {
      flex: 1;
    }

    .card-title {
      font-weight: bold;
      font-size: 14px;
      color: #303133;
    }

    .card-desc {
      font-size: 12px;
      color: #909399;
      margin-top: 4px;
      line-height: 1.4;
    }

    .card-check {
      position: absolute;
      right: 8px;
      top: 8px;
      color: $primary-color;
    }
  }

  .sidebar-footer {
    margin-top: 10px;
    text-align: center;
    flex-shrink: 0;
  }
}

/* 右侧网格区域 */
.settings-main {
  flex: 1;
  padding: 20px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  user-select: none;
  /* 防止涂抹时选中文本 */

  .main-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    /* 底部对齐，视觉更整齐 */
    margin-bottom: 15px;
    flex-shrink: 0;
    gap: 20px;
    /* 防止左右太挤 */

    .header-left {
      display: flex;
      flex-direction: column;
      gap: 5px;

      .title-row {
        display: flex;
        align-items: center;
        gap: 15px;

        h3 {
          font-size: 16px;
          font-weight: bold;
          color: #303133;
          margin: 0;
        }
      }

      .sub-text {
        font-size: 12px;
        color: #909399;
      }

      .legend {
        display: flex;
        gap: 10px;
        font-size: 12px;
        color: #606266;
        align-items: center;

        .dot {
          width: 8px;
          height: 8px;
          display: inline-block;
          border-radius: 2px;
          margin-right: 2px;
        }

        .zero {
          background: #f0f2f5;
          border: 1px solid #dcdfe6;
        }

        .normal {
          background: #e1f3d8;
          border: 1px solid #67c23a;
        }

        .high {
          background: #fde2e2;
          border: 1px solid #f56c6c;
        }
      }
    }

    .header-right {
      /* 让工具栏靠右 */
    }

    .brush-tools {
      display: flex;
      align-items: center;
      gap: 8px;
      background: #fff;
      padding: 4px;
      border-radius: 4px;
      border: 1px solid #dcdfe6;
      /* 给工具栏加个框，看起来像个整体 */

      .custom-input-wrapper {
        display: flex;
        align-items: center;
        gap: 5px;
        margin-left: 5px;
        padding-left: 5px;
        border-left: 1px solid #ebeef5;

        .unit {
          font-size: 12px;
          color: #606266;
        }
      }
    }
  }

  /* * 修复对齐：将 header 和 body 放在同一个容器
   * 关键样式：position: relative; overflow: auto;
   */
  .heatmap-container {
    flex: 1;
    overflow: auto;
    border: 1px solid $border-color;
    border-radius: 4px;
    position: relative;

    /* 每一行的通用样式 */
    .grid-row {
      display: flex;
      min-width: 800px;
      /* 防止过窄 */

      .corner-cell,
      .row-header {
        width: 80px;
        flex-shrink: 0;
        display: flex;
        align-items: center;
        justify-content: center;
        background: #fff;
        border-right: 1px solid $border-color;
        font-size: 13px;
        font-weight: bold;
        color: #909399;
        position: sticky;
        left: 0;
        z-index: 5;
        /* 行头也吸左 */
      }

      .day-header,
      .req-cell {
        flex: 1;
        text-align: center;
        display: flex;
        align-items: center;
        justify-content: center;
        border-right: 1px solid #ebeef5;
      }
    }

    /* 表头吸顶 */
    .header-row {
      position: sticky;
      top: 0;
      z-index: 10;
      background: #f9fafe;
      border-bottom: 1px solid $border-color;
      height: 40px;

      .corner-cell {
        background: #f9fafe;
        z-index: 15;
        /* 角落层级最高 */
      }

      .day-header {
        font-weight: bold;
        color: #606266;
        cursor: pointer;

        &:hover {
          background: #eef1f6;
          color: $primary-color;
        }
      }
    }

    /* 内容行 */
    .body-row {
      border-bottom: 1px solid #ebeef5;
      height: 40px;

      &.special-row {
        border-bottom: 2px solid $border-color;

        .row-header {
          background: #fdf6ec;
          color: $primary-color;
        }
      }

      .row-header {
        cursor: pointer;

        &:hover {
          background: #eef1f6;
          color: $primary-color;
        }
      }

      .req-cell {
        font-size: 14px;
        cursor: pointer;
        transition: background 0.1s;

        &:hover {
          outline: 2px solid $primary-color;
          z-index: 2;
        }

        &.cell-zero {
          background: #f5f7fa;
          color: #c0c4cc;
        }

        &.cell-normal {
          background: #f0f9eb;
          color: #67c23a;
          font-weight: bold;
        }

        &.cell-high {
          background: #fef0f0;
          color: #f56c6c;
          font-weight: bold;
        }
      }
    }
  }
}

.dialog-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .save-tip {
    font-size: 12px;
    color: #909399;
    display: flex;
    align-items: center;
    gap: 4px;
  }
}

.popover-content {
  text-align: center;

  .popover-title {
    font-size: 12px;
    color: #909399;
    margin-bottom: 8px;
  }

  .quick-btns {
    margin-top: 8px;
    display: flex;
    justify-content: center;
    gap: 5px;
  }
}

.w-100 {
  width: 100%;
}
</style>
