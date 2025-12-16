<template>
  <el-dialog
    v-model="visible"
    title="📊 排班分析报告"
    width="800px"
    top="10vh"
    class="result-report-dialog"
    destroy-on-close
    :close-on-click-modal="false"
  >
    <div class="report-container">

      <div class="kpi-row">
        <div class="kpi-card success">
          <div class="label">排班完成率</div>
          <div class="value">{{ stats.coverage }}%</div>
          <div class="sub">计划 {{ stats.totalNeeds }} / 实排 {{ stats.totalAssigned }}</div>
        </div>
        <div class="kpi-card" :class="stats.unassigned > 0 ? 'danger' : 'success'">
          <div class="label">未满足需求</div>
          <div class="value">{{ stats.unassigned }}</div>
          <div class="sub">个班次存在空缺</div>
        </div>
        <div class="kpi-card info">
          <div class="label">参与人数</div>
          <div class="value">{{ stats.studentCount }}</div>
          <div class="sub">人均 {{ stats.avgShift }} 班次</div>
        </div>
      </div>

      <div v-if="stats.unassigned > 0" class="section warning-section">
        <h3>⚠️ 异常检测</h3>
        <div class="warning-list">
          <div v-for="(warn, index) in resultData.warnings" :key="index" class="warning-item">
            <el-icon><Warning /></el-icon>
            <span class="text">{{ warn }}</span>
          </div>
        </div>
      </div>

      <div class="section chart-section">
        <h3>⚖️ 负载分布 (Top 10)</h3>
        <div class="bar-chart">
          <div
            v-for="item in stats.distribution"
            :key="item.name"
            class="chart-row"
          >
            <div class="name" :title="item.name">{{ item.name }}</div>
            <div class="bar-wrapper">
              <div
                class="bar-fill"
                :style="{ width: item.percent + '%' }"
                :class="getBarColor(item.count)"
              ></div>
            </div>
            <div class="count">{{ item.count }}次</div>
          </div>
        </div>
        <div class="chart-footer">
          注：系统算法已尽力平衡，方差指数: {{ stats.variance }} (越低越公平)
        </div>
      </div>

    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">放弃结果</el-button>
        <el-button @click="$emit('retry')">调整参数重算</el-button>
        <el-button type="primary" size="large" @click="handleApply">
          应用此方案
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed } from 'vue'
import { Warning } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: Boolean,
  // 后端返回的原始结果数据
  resultData: {
    type: Object,
    default: () => ({
      solution: {}, // { "1_1": [studentObj...], ... }
      warnings: [], // ["周一xx不足..."]
      totalNeeds: 0 // 总共需要填多少个坑
    })
  }
})

const emit = defineEmits(['update:modelValue', 'apply', 'retry'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

// --- 前端实时计算统计指标 ---
// 这样后端只需要传结果，不需要传一大堆统计数据，减轻后端压力
const stats = computed(() => {
  const { solution, warnings, totalNeeds } = props.resultData

  // 1. 统计每个人的班次
  const loadMap = {}
  let totalAssigned = 0

  Object.values(solution).forEach(students => {
    students.forEach(s => {
      loadMap[s.name] = (loadMap[s.name] || 0) + 1
      totalAssigned++
    })
  })

  // 2. 转换为数组并排序
  const distArray = Object.entries(loadMap)
    .map(([name, count]) => ({ name, count }))
    .sort((a, b) => b.count - a.count) // 降序

  // 3. 计算百分比条 (以最大值作为 100%)
  const maxCount = distArray.length > 0 ? distArray[0].count : 1
  const distribution = distArray.slice(0, 10).map(item => ({
    ...item,
    percent: (item.count / maxCount) * 100
  }))

  // 4. 计算覆盖率
  const coverage = totalNeeds > 0 ? Math.round((totalAssigned / totalNeeds) * 100) : 100
  // 修正：未分配数不能简单用 totalNeeds - totalAssigned，因为可能有的格子排多了。
  // 简单起见，这里假设 unassigned = warnings.length (后端会告诉我们要几个但没排进去)
  const unassigned = warnings ? warnings.length : 0

  // 5. 计算方差 (简单评估公平性)
  const counts = distArray.map(d => d.count)
  const avg = counts.reduce((a, b) => a + b, 0) / (counts.length || 1)
  const variance = (counts.reduce((a, b) => a + Math.pow(b - avg, 2), 0) / (counts.length || 1)).toFixed(2)

  return {
    coverage,
    totalNeeds,
    totalAssigned,
    unassigned,
    studentCount: distArray.length,
    avgShift: avg.toFixed(1),
    distribution,
    variance
  }
})

const getBarColor = (count) => {
  // 简单的热力色：太忙了红色，太闲了灰色，正常绿色
  if (count > stats.value.avgShift * 1.5) return 'bar-busy'
  if (count < stats.value.avgShift * 0.5) return 'bar-idle'
  return 'bar-normal'
}

const handleApply = () => {
  emit('apply', props.resultData.solution)
  visible.value = false
}
</script>

<style scoped lang="scss">
$success: #67c23a;
$warning: #e6a23c;
$danger: #f56c6c;
$info: #909399;
$primary: #409eff;
$bg-color: #f5f7fa;

.report-container {
  padding: 10px 20px;
}

/* KPI Cards */
.kpi-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 25px;

  .kpi-card {
    background: #fff;
    border-radius: 8px;
    padding: 20px;
    text-align: center;
    border: 1px solid #ebeef5;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);

    .label { color: $info; font-size: 14px; margin-bottom: 8px; }
    .value { font-size: 28px; font-weight: bold; margin-bottom: 8px; color: #303133; }
    .sub { font-size: 12px; color: #c0c4cc; }

    &.success .value { color: $success; }
    &.danger .value { color: $danger; }
    &.info .value { color: $primary; }
  }
}

.section {
  margin-bottom: 25px;
  h3 {
    font-size: 16px;
    font-weight: bold;
    margin-bottom: 15px;
    padding-left: 10px;
    border-left: 4px solid $primary;
    color: #303133;
  }
}

/* Warning List */
.warning-list {
  background: #fdf6ec;
  border-radius: 4px;
  padding: 15px;
  max-height: 120px;
  overflow-y: auto;

  .warning-item {
    display: flex;
    align-items: center;
    gap: 8px;
    color: $warning;
    font-size: 13px;
    margin-bottom: 6px;
    &:last-child { margin-bottom: 0; }
  }
}

/* Simple Bar Chart */
.chart-section {
  .bar-chart {
    border: 1px solid #ebeef5;
    border-radius: 4px;
    padding: 15px;
    background: #fff;
  }

  .chart-row {
    display: flex;
    align-items: center;
    margin-bottom: 10px;
    &:last-child { margin-bottom: 0; }

    .name { width: 60px; font-size: 13px; color: #606266; text-align: right; padding-right: 10px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;}
    .count { width: 40px; font-size: 12px; color: $info; padding-left: 10px; }

    .bar-wrapper {
      flex: 1;
      height: 10px;
      background: #f0f2f5;
      border-radius: 5px;
      overflow: hidden;

      .bar-fill {
        height: 100%;
        border-radius: 5px;
        transition: width 0.5s ease;

        &.bar-normal { background: $success; }
        &.bar-busy { background: $danger; }
        &.bar-idle { background: $info; }
      }
    }
  }

  .chart-footer {
    margin-top: 10px;
    text-align: right;
    font-size: 12px;
    color: #c0c4cc;
  }
}
</style>
