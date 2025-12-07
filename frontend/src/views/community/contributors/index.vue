<script setup>
import { ref, computed, onMounted } from 'vue'
import { contributors } from './data'
import { Medal, UserFilled, Star } from '@element-plus/icons-vue'

// 1. 分类管理
const activeTab = ref('core')
const tabs = [
  { label: '核心团队', value: 'core', icon: Medal },
  { label: '社区贡献', value: 'community', icon: UserFilled },
  { label: '全部英雄', value: 'all', icon: Star }
]

// 状态标记
const isFirstLoad = ref(true)

// 2. 过滤数据
const filteredList = computed(() => {
  if (activeTab.value === 'all') return contributors
  return contributors.filter(item => item.category === activeTab.value)
})

// 3. 3D 倾斜交互逻辑 (【核心修复】)
const handleMouseMove = (e) => {
  const wrapper = e.currentTarget // 获取外层容器
  const card = wrapper.querySelector('.card-3d') // 【修复】获取真正需要变形的内层元素
  if (!card) return

  const rect = wrapper.getBoundingClientRect()
  const x = e.clientX - rect.left
  const y = e.clientY - rect.top
  const centerX = rect.width / 2
  const centerY = rect.height / 2

  // 旋转角度
  const rotateX = ((y - centerY) / centerY) * -12
  const rotateY = ((x - centerX) / centerX) * 12

  // 【修复】直接设置在 .card-3d 上，强制覆盖 CSS 类中的默认值
  card.style.setProperty('--rx', `${rotateX}deg`)
  card.style.setProperty('--ry', `${rotateY}deg`)
  card.style.setProperty('--mx', `${x}px`)
  card.style.setProperty('--my', `${y}px`)
}

const handleMouseLeave = (e) => {
  const wrapper = e.currentTarget
  const card = wrapper.querySelector('.card-3d') // 【修复】同样获取内层元素复位
  if (!card) return

  // 复位
  card.style.setProperty('--rx', `0deg`)
  card.style.setProperty('--ry', `0deg`)
  card.style.setProperty('--mx', `-100%`) // 把光照移出去
  card.style.setProperty('--my', `-100%`)
}

// 4. 生成随机飞行轨迹 (仅用于首屏动画)
const getRandomEnterStyle = (index) => {
  if (!isFirstLoad.value) {
    return { '--index': index }
  }

  const randomX = (Math.random() - 0.5) * 2000
  const randomY = (Math.random() - 0.5) * 2000
  const randomRotate = (Math.random() - 0.5) * 180

  return {
    '--index': index,
    '--enter-x': `${randomX}px`,
    '--enter-y': `${randomY}px`,
    '--enter-r': `${randomRotate}deg`
  }
}

// 5. 生命周期
onMounted(() => {
  setTimeout(() => {
    isFirstLoad.value = false
  }, 1500)
})
</script>

<template>
  <div class="app-container">
    <div class="header">
      <h1 class="glitch-title" data-text="🏆 贡献者荣誉墙">🏆 贡献者荣誉墙</h1>
      <p class="subtitle">Code with ❤️ by OrangeTools Team</p>
    </div>

    <div class="tabs-wrapper">
      <div
        v-for="(tab, i) in tabs"
        :key="tab.value"
        class="tab-item"
        :class="{ active: activeTab === tab.value }"
        :style="{ '--delay': i * 0.1 + 's' }"
        @click="activeTab = tab.value"
      >
        <el-icon><component :is="tab.icon" /></el-icon>
        {{ tab.label }}
      </div>
    </div>

    <transition-group
      :name="isFirstLoad ? 'big-bang' : 'list'"
      tag="div"
      class="grid"
      appear
    >
      <div
        v-for="(item, index) in filteredList"
        :key="item.name"
        class="card-wrapper"
        :style="getRandomEnterStyle(index)"
        @mousemove="handleMouseMove"
        @mouseleave="handleMouseLeave"
      >
        <div class="card-3d">
          <div class="card-content">
            <div class="glow" />

            <div class="avatar-wrapper">
              <img :src="item.avatar" :alt="item.name">
            </div>

            <div class="info">
              <div class="role-badge">{{ item.role }}</div>
              <h3 class="name">{{ item.name }}</h3>
              <p class="desc">{{ item.desc }}</p>
              <div class="tags">
                <span v-for="tag in item.tags" :key="tag" class="tag">#{{ tag }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </transition-group>
  </div>
</template>

<style lang="scss" scoped>
.app-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 60px 20px;
  perspective: 2000px;
}

/* 头部样式 */
.header {
  text-align: center; margin-bottom: 50px;
  .glitch-title {
    font-size: 48px; font-weight: 900; position: relative; display: inline-block; color: var(--text-color-primary); margin-bottom: 10px;
    &::before, &::after { content: attr(data-text); position: absolute; top: 0; left: 0; width: 100%; height: 100%; background: var(--bg-color-page); }
    &::before { left: 2px; text-shadow: -1px 0 #ff00c1; clip-path: inset(44% 0 61% 0); animation: glitch-anim-1 2.5s infinite linear alternate-reverse; }
    &::after { left: -2px; text-shadow: -1px 0 #00fff9; clip-path: inset(54% 0 20% 0); animation: glitch-anim-2 3s infinite linear alternate-reverse; }
  }
  .subtitle { font-size: 18px; color: var(--text-color-secondary); letter-spacing: 2px; opacity: 0; animation: fadeInUp 0.8s 0.5s forwards; }
}

/* Tab 样式 */
.tabs-wrapper {
  display: flex; justify-content: center; gap: 15px; margin-bottom: 50px;
  .tab-item {
    display: flex; align-items: center; gap: 6px; padding: 10px 24px; border-radius: 30px; background: var(--bg-color-card); border: 1px solid var(--border-color); cursor: pointer; font-weight: 500; transition: all 0.3s;
    opacity: 0; transform: translateY(20px); animation: fadeInUp 0.6s cubic-bezier(0.2, 0.8, 0.2, 1) forwards; animation-delay: var(--delay);
    &:hover { transform: translateY(-2px); box-shadow: 0 4px 12px var(--shadow-color); color: var(--el-color-primary); }
    &.active { background: var(--el-color-primary); color: #fff; border-color: var(--el-color-primary); box-shadow: 0 4px 12px rgba(var(--el-color-primary-rgb), 0.4); }
  }
}

.grid {
  display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 40px; padding: 20px; position: relative;
}

/* === 核心 3D 卡片样式 === */
.card-wrapper {
  height: 100%;
  transform-style: preserve-3d;
  perspective: 1000px;
}

.card-3d {
  position: relative; height: 100%;
  /* 默认值，JS 会通过 style 属性覆盖这些值 */
  --rx: 0deg; --ry: 0deg; --mx: 50%; --my: 50%;

  transform-style: preserve-3d;
  transform: rotateX(var(--rx)) rotateY(var(--ry));
  transition: transform 0.1s ease-out;
  will-change: transform;
}

.card-content {
  background: var(--bg-color-card); border-radius: 20px; padding: 40px 30px; text-align: center; border: 1px solid var(--border-color); box-shadow: 0 10px 30px -10px var(--shadow-color); position: relative; overflow: hidden; height: 100%; display: flex; flex-direction: column; align-items: center; backdrop-filter: blur(10px);
  /* 修复部分浏览器渲染瑕疵 */
  backface-visibility: hidden;

  /* 光照层 */
  .glow {
    position: absolute; top: 0; left: 0; right: 0; bottom: 0;
    background: radial-gradient(circle at var(--mx) var(--my), rgba(255, 255, 255, 0.15) 0%, transparent 60%);
    pointer-events: none; z-index: 2; mix-blend-mode: overlay;
  }
}

.avatar-wrapper {
  width: 110px; height: 110px; margin-bottom: 20px; border-radius: 50%; border: 4px solid var(--bg-color-page); box-shadow: 0 8px 20px rgba(0,0,0,0.1); overflow: hidden; z-index: 3; transition: transform 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  img { width: 100%; height: 100%; object-fit: cover; }
}
.card-wrapper:hover .avatar-wrapper { transform: translateZ(50px) scale(1.15); }

/* 文字样式 */
.role-badge { display: inline-block; background: var(--el-color-primary-light-9); color: var(--el-color-primary); padding: 4px 12px; border-radius: 12px; font-size: 12px; font-weight: bold; text-transform: uppercase; margin-bottom: 10px; }
.name { font-size: 26px; font-weight: 800; margin: 0 0 15px; color: var(--text-color-primary); }
.desc { color: var(--text-color-regular); font-size: 15px; line-height: 1.6; margin-bottom: 25px; flex-grow: 1; }
.tags { display: flex; flex-wrap: wrap; justify-content: center; gap: 8px; z-index: 3; .tag { background: var(--el-fill-color); color: var(--text-color-secondary); padding: 6px 12px; border-radius: 8px; font-size: 12px; transition: all 0.2s; &:hover { background: var(--el-color-primary); color: #fff; transform: translateY(-2px); } } }

/* === 动画系统 === */
.big-bang-enter-active { transition: all 1s cubic-bezier(0.34, 1.56, 0.64, 1); }
.big-bang-enter-from { opacity: 0; transform: translate(var(--enter-x), var(--enter-y)) rotate(var(--enter-r)) scale(0.1); }

.list-move, .list-enter-active, .list-leave-active { transition: all 0.5s cubic-bezier(0.55, 0, 0.1, 1); }
.list-enter-from, .list-leave-to { opacity: 0; transform: scale(0.9); }
.list-leave-active { position: absolute; width: calc((100% - 40px) / 1); z-index: -1; }

@keyframes glitch-anim-1 { 0% { clip-path: inset(20% 0 80% 0); } 20% { clip-path: inset(60% 0 10% 0); } 40% { clip-path: inset(40% 0 50% 0); } 60% { clip-path: inset(80% 0 5% 0); } 80% { clip-path: inset(10% 0 70% 0); } 100% { clip-path: inset(30% 0 20% 0); } }
@keyframes glitch-anim-2 { 0% { clip-path: inset(10% 0 60% 0); } 20% { clip-path: inset(80% 0 5% 0); } 40% { clip-path: inset(30% 0 20% 0); } 60% { clip-path: inset(10% 0 80% 0); } 80% { clip-path: inset(50% 0 30% 0); } 100% { clip-path: inset(70% 0 10% 0); } }
@keyframes fadeInUp { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }
</style>
