<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/modules/user'
import CountUp from 'vue-countup-v3'
import { getToolStats, getPublicFeedbacks } from '@/api/community'
import {
  DataAnalysis, ChatLineSquare, Medal, User,
  Sunny, Moon, Coffee, Timer, StarFilled, ArrowRight,
  Menu,Connection, Refresh
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

// ================== 1. 状态定义 ==================

const stats = ref({ toolUsage: 0, feedbackCount: 0, runDays: 0 })
const activities = ref([])
const loading = ref(true)
const hitokoto = ref({ text: '正在加载每日一言...', from: 'OrangeTools', link: '#' })

// ================== 2. 快捷导航配置 (Data-Driven) ==================
// 以后想调整顺序，或者加新工具，直接改这个数组即可！
const shortcuts = ref([
  {
    id: 'course',
    title: '课表助手',
    desc: '全自动空闲统计',
    path: '/tools/course',
    icon: DataAnalysis,
    colorClass: 'course'
  },
  {
    id: 'feedback',
    title: '许愿墙',
    desc: '听见你的声音',
    path: '/community/feedback',
    icon: ChatLineSquare,
    colorClass: 'feedback'
  },
  {
    id: 'contributors',
    title: '荣誉墙',
    desc: '致敬贡献者',
    path: '/community/contributors',
    icon: Medal,
    colorClass: 'thanks'
  },
  {
    id: 'profile',
    title: '个人中心',
    desc: '资料与设置',
    path: '/profile', // 暂时跳首页
    icon: User,
    colorClass: 'profile',
    // tip: '立即进入'
  }
])

// ================== 3. 业务逻辑 ==================

const PROJECT_START_DATE = '2025-12-08'

const calcRunDays = () => {
  const start = new Date(PROJECT_START_DATE).getTime()
  const now = new Date().getTime()
  stats.value.runDays = Math.floor((now - start) / (1000 * 60 * 60 * 24))
}

const fetchHitokoto = () => {
  fetch('https://v1.hitokoto.cn?c=i&c=k')
    .then(r => r.json())
    .then(data => {
      hitokoto.value = { text: data.hitokoto, from: data.from, link: `https://hitokoto.cn/?uuid=${data.uuid}` }
    })
    .catch(() => {
      hitokoto.value = { text: '保持热爱，奔赴山海。', from: 'Developers', link: '#' }
    })
}

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了，注意休息'
  if (hour < 12) return '早上好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const loadData = async () => {
  try {
    const [toolRes, feedbackRes] = await Promise.all([getToolStats(), getPublicFeedbacks('new')])

    if (toolRes) {
      let total = 0
      toolRes.forEach(t => total += (t.usageCount || 0))
      stats.value.toolUsage = total
    }

    if (feedbackRes) {
      stats.value.feedbackCount = feedbackRes.length
      activities.value = feedbackRes.slice(0, 5)
    }
  } catch (error) {
    console.error('主控台数据加载失败', error)
  } finally {
    loading.value = false
  }
}

const go = (path) => router.push(path)

// ================== 4. GitHub CDN 版本逻辑 (CDN版) ==================
const GITHUB_USER = 'FangSuYu'
const GITHUB_REPO = 'OrangeTools'

const releaseInfo = ref({
  version: 'Checking...', // 初始状态
  status: 'Loading',      // 状态标签
  url: `https://github.com/${GITHUB_USER}/${GITHUB_REPO}/releases`,
  desc: '正在连接 CDN 获取最新版本...',
  loading: false
})

const fetchLatestRelease = async () => {
  releaseInfo.value.loading = true
  try {
    // 【核心】使用 jsDelivr CDN 读取 package.json
    // 加时间戳 ?t=... 防止缓存
    const timestamp = new Date().getTime()
    const res = await fetch(`https://cdn.jsdelivr.net/gh/${GITHUB_USER}/${GITHUB_REPO}@main/package.json?t=${timestamp}`)

    if (res.ok) {
      const data = await res.json()
      releaseInfo.value = {
        version: `v${data.version}`, // 读取 package.json 的 version
        status: 'Stable', // 既然能读取到，就是稳定版
        url: `https://github.com/${GITHUB_USER}/${GITHUB_REPO}/releases`,
        desc: '🚀 最新版本已发布！点击下方按钮前往 GitHub 查看详细日志与下载。',
        loading: false
      }
    } else {
      throw new Error('CDN fetch failed')
    }
  } catch (e) {
    console.warn('版本检查失败', e)
    // 失败兜底显示
    releaseInfo.value.version = 'v1.0.0'
    releaseInfo.value.status= 'Local'
    releaseInfo.value.desc = '网络连接不稳定，显示本地版本。'
    releaseInfo.value.loading = false
  }
}
onMounted(() => {
  calcRunDays()
  fetchHitokoto()
  loadData()
  fetchLatestRelease()
})
</script>

<template>
  <div class="dashboard-container">

    <div class="welcome-card">
      <div class="welcome-left">
        <div class="avatar-box">
          <el-avatar :size="70" :src="userStore.avatarUrl" icon="UserFilled" class="user-avatar" />
        </div>
        <div class="welcome-text">
          <h2 class="greet">
            {{ greeting }}，{{ userStore.nickname || userStore.username }}
            <span class="wave">👋</span>
          </h2>
          <p class="quote">
            <el-icon><Coffee /></el-icon>
            <a :href="hitokoto.link" target="_blank" class="hitokoto-link">
              {{ hitokoto.text }} —— 「{{ hitokoto.from }}」
            </a>
          </p>
        </div>
      </div>
      <div class="welcome-right">
        <div class="stat-item">
          <div class="label">工具调用</div>
          <div class="value"><count-up :end-val="stats.toolUsage" :duration="2" /></div>
        </div>
        <div class="stat-item">
          <div class="label">社区许愿</div>
          <div class="value"><count-up :end-val="stats.feedbackCount" :duration="2" /></div>
        </div>
        <div class="stat-item">
          <div class="label">平稳运行</div>
          <div class="value"><count-up :end-val="stats.runDays" :duration="2" /><span class="unit">天</span></div>
        </div>
      </div>
    </div>

    <div class="section-title"><el-icon><Menu /></el-icon> 快捷导航</div>
    <el-row :gutter="20" class="quick-nav">
      <el-col
        v-for="item in shortcuts"
        :key="item.id"
        :xs="24" :sm="12" :md="6"
      >
        <div class="nav-card" :class="item.colorClass" @click="go(item.path)">
          <div class="icon-layer">
            <el-icon><component :is="item.icon" /></el-icon>
          </div>

          <div class="info">
            <h3>{{ item.title }}</h3>
            <p>{{ item.desc }}</p>
          </div>

          <div class="hover-tip">
            {{ item.tip || '立即进入' }} <el-icon v-if="!item.tip"><ArrowRight /></el-icon>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="bottom-section">
      <el-col :xs="24" :lg="16" class="col-left">
        <el-card class="box-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span><el-icon><Timer /></el-icon> 社区最新动态</span>
              <el-button link type="primary" @click="go('/community/feedback')">全部</el-button>
            </div>
          </template>
          <div v-loading="loading" class="activity-list">
            <el-empty v-if="activities.length === 0" description="暂无动态" :image-size="80" />
            <el-timeline v-else>
              <el-timeline-item
                v-for="item in activities"
                :key="item.id"
                :type="item.type === 'bug' ? 'danger' : 'primary'"
                :timestamp="item.createTime?.replace('T', ' ')"
                placement="top"
              >
                <div class="timeline-content">
                  <span class="tag">{{ item.type === 'bug' ? '🐛 BUG' : '💡 建议' }}</span>
                  <span class="title">{{ item.title }}</span>
                  <el-tag v-if="item.status===1" size="small" type="success" effect="plain" class="status-tag">已公开</el-tag>
                </div>
              </el-timeline-item>
            </el-timeline>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="8" class="col-right">
        <el-card class="box-card project-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span><el-icon><Connection /></el-icon> 项目状态</span>
              <el-button link :icon="Refresh" :loading="releaseInfo.loading" @click="fetchLatestRelease"></el-button>
            </div>
          </template>

          <div class="repo-info">
            <div class="repo-header">
              <div class="version-badge">
                <span class="label">Current</span>
                <span class="ver">{{ releaseInfo.version }}</span>
              </div>
              <el-tag size="small" :type="releaseInfo.status === 'Stable' ? 'success' : 'info'" effect="dark">
                {{ releaseInfo.status }}
              </el-tag>
            </div>

            <div class="repo-desc">
              <p>{{ releaseInfo.desc }}</p>
            </div>

            <div class="repo-actions">
              <a :href="releaseInfo.url" target="_blank" class="github-btn">
                <img src="https://img.icons8.com/ios-glyphs/30/ffffff/github.png" alt="git">
                GitHub Release
              </a>
            </div>

            <div class="bg-decoration">
              <img src="@/assets/favicon.png" alt="bg" />
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

  </div>
</template>

<style lang="scss" scoped>
/* 右侧项目卡片样式 */
.project-card {
  position: relative;
  overflow: hidden;

  .repo-info {
    position: relative;
    z-index: 2;
  }

  .repo-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    .version-badge {
      display: flex;
      align-items: center;
      background: var(--el-color-primary-light-9);
      padding: 4px 6px;
      border-radius: 8px;
      border: 1px solid var(--el-color-primary-light-5);

      .label {
        background: var(--el-color-primary);
        color: #fff;
        font-size: 12px;
        padding: 2px 6px;
        border-radius: 4px;
        margin-right: 8px;
        font-weight: bold;
      }
      .ver {
        font-weight: 800;
        color: var(--el-color-primary);
        font-size: 16px;
        font-family: 'Helvetica Neue', sans-serif;
      }
    }

    .date {
      font-size: 12px;
      color: var(--text-color-secondary);
    }
  }

  .repo-desc {
    background: var(--el-fill-color-light);
    padding: 15px;
    border-radius: 8px;
    font-size: 13px;
    color: var(--text-color-regular);
    line-height: 1.6;
    margin-bottom: 20px;
    min-height: 80px;
    border: 1px dashed var(--border-color);
  }

  .repo-actions {
    text-align: center;
    .github-btn {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      width: 100%;
      padding: 10px 0;
      background: #24292e; /* GitHub 黑 */
      color: #fff;
      border-radius: 8px;
      text-decoration: none;
      font-weight: bold;
      transition: all 0.3s;

      img { width: 20px; height: 20px; }

      &:hover {
        background: #000;
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(0,0,0,0.3);
      }
    }
  }

  /* 暗黑模式适配 */
  :deep(html.dark) & .github-btn {
    background: #373e47;
    &:hover { background: #505a66; }
  }

  /* 背景装饰水印 */
  .bg-decoration {
    position: absolute;
    bottom: -20px;
    right: -20px;
    opacity: 0.05;
    z-index: 1;
    img { width: 150px; height: 150px; transform: rotate(-15deg); }
  }
}
/* 样式与之前完全一致，直接复用即可 */
.dashboard-container { padding: 20px; max-width: 1400px; margin: 0 auto; }
.welcome-card { background: var(--bg-color-card); border-radius: 12px; padding: 30px; display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; box-shadow: 0 4px 20px var(--shadow-color); margin-bottom: 30px; border: 1px solid var(--border-color); background-image: linear-gradient(120deg, var(--bg-color-card) 60%, var(--el-color-primary-light-9) 100%);
  .welcome-left { display: flex; align-items: center; gap: 20px; .user-avatar { border: 4px solid var(--bg-color-page); box-shadow: 0 4px 12px rgba(0,0,0,0.1); } .greet { margin: 0 0 10px 0; font-size: 24px; color: var(--text-color-primary); .wave { display: inline-block; animation: wave 2.5s infinite; transform-origin: 70% 70%; } } .quote { margin: 0; color: var(--text-color-secondary); font-size: 14px; display: flex; align-items: center; gap: 6px; .hitokoto-link { color: var(--text-color-secondary); text-decoration: none; transition: color 0.3s; &:hover { color: var(--el-color-primary); } } } }
  .welcome-right { display: flex; gap: 40px; text-align: right; .stat-item { .label { font-size: 13px; color: var(--text-color-secondary); margin-bottom: 5px; } .value { font-size: 28px; font-weight: bold; color: var(--text-color-primary); font-family: 'Helvetica Neue', sans-serif; .unit { font-size: 14px; margin-left: 4px; font-weight: normal; color: var(--text-color-secondary); } } } }
}
.section-title { font-size: 18px; font-weight: bold; color: var(--text-color-primary); margin-bottom: 15px; display: flex; align-items: center; gap: 8px; }
.nav-card { background: var(--bg-color-card); border-radius: 12px; padding: 20px; margin-bottom: 20px; display: flex; align-items: center; gap: 15px; cursor: pointer; position: relative; overflow: hidden; border: 1px solid var(--border-color); transition: all 0.3s;
  .icon-layer { width: 50px; height: 50px; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 24px; transition: all 0.3s; }
  &.course .icon-layer { background: #e8f3ff; color: #409eff; }
  &.feedback .icon-layer { background: #ecf5ff; color: #36cfc9; }
  &.thanks .icon-layer { background: #fff7e6; color: #fa8c16; }
  &.profile .icon-layer { background: #f9f0ff; color: #b37feb; }
  /* 暗黑模式 */
  :deep(html.dark) & { &.course .icon-layer { background: #1a2a3c; color: #409eff; } &.feedback .icon-layer { background: #162e2d; color: #36cfc9; } &.thanks .icon-layer { background: #2b2111; color: #fa8c16; } &.profile .icon-layer { background: #221a29; color: #b37feb; } }
  .info { h3 { margin: 0 0 5px 0; font-size: 16px; color: var(--text-color-primary); } p { margin: 0; font-size: 12px; color: var(--text-color-secondary); } }
  .hover-tip { position: absolute; right: 20px; font-size: 12px; color: var(--text-color-secondary); opacity: 0; transform: translateX(10px); transition: all 0.3s; display: flex; align-items: center; gap: 4px; }
  &:hover { transform: translateY(-5px); box-shadow: 0 8px 20px var(--shadow-color); .icon-layer { transform: scale(1.1); } .hover-tip { opacity: 1; transform: translateX(0); } }
}
.box-card { border-radius: 12px; border: 1px solid var(--border-color); background: var(--bg-color-card); margin-bottom: 20px; :deep(.el-card__header) { padding: 15px 20px; border-bottom: 1px solid var(--border-color); } .card-header { display: flex; justify-content: space-between; align-items: center; font-weight: bold; } }
.timeline-content { .tag { font-weight: bold; margin-right: 8px; font-size: 13px; } .title { color: var(--text-color-regular); } .status-tag { margin-left: 10px; transform: scale(0.8); } }
.version-list { .ver-item { position: relative; padding-left: 20px; padding-bottom: 25px; border-left: 2px solid var(--border-color); &::before { content: ''; position: absolute; left: -6px; top: 0; width: 10px; height: 10px; border-radius: 50%; background: var(--border-color); } &.active { border-left-color: var(--el-color-primary); &::before { background: var(--el-color-primary); box-shadow: 0 0 0 4px var(--el-color-primary-light-9); } .ver-num { color: var(--el-color-primary); } } .ver-num { font-weight: bold; font-size: 16px; margin-bottom: 8px; } .ver-desc ul { padding-left: 18px; margin: 0; font-size: 13px; color: var(--text-color-regular); li { margin-bottom: 4px; } } .ver-date { margin-top: 8px; font-size: 12px; color: var(--text-color-secondary); } &:last-child { border-left: 2px solid transparent; padding-bottom: 0; } } }
@media (max-width: 768px) { .welcome-card { flex-direction: column; align-items: flex-start; .welcome-right { margin-top: 20px; width: 100%; justify-content: space-between; text-align: left; } } }
@keyframes wave { 0% { transform: rotate(0deg); } 10% { transform: rotate(14deg); } 20% { transform: rotate(-8deg); } 30% { transform: rotate(14deg); } 40% { transform: rotate(-4deg); } 50% { transform: rotate(10deg); } 60% { transform: rotate(0deg); } 100% { transform: rotate(0deg); } }
</style>
