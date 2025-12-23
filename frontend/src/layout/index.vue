<script setup>
import { computed, onMounted, onBeforeUnmount } from 'vue'
import { AppMain, Navbar, Sidebar } from './components'
import { useAppStore } from '@/stores/modules/app'

const appStore = useAppStore()

// 计算属性：动态生成 class
const classObj = computed(() => ({
  hideSidebar: !appStore.sidebar.opened,
  openSidebar: appStore.sidebar.opened,
  withoutAnimation: appStore.sidebar.withoutAnimation
}))

const handleClickOutside = () => {
  appStore.closeSidebar(false)
}

// 响应式处理：移动端自动收起侧边栏
const WIDTH = 768
const checkMobile = () => {
  const rect = document.body.getBoundingClientRect()
  const isMobile = rect.width - 1 < WIDTH
  if (isMobile && appStore.sidebar.opened) {
    appStore.closeSidebar(true)
  }
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', checkMobile)
})
</script>

<template>
  <div :class="classObj" class="app-wrapper">
    <div v-if="appStore.sidebar.opened" class="drawer-bg" @click="handleClickOutside" />
    <Sidebar class="sidebar-container" />

    <div class="main-container">
      <div class="fixed-header">
        <Navbar />
      </div>
      <AppMain />
    </div>
  </div>
</template>

<style lang="scss" scoped>
.app-wrapper {
  position: relative;
  height: 100%;
  width: 100%;
  display: flex;
}

.drawer-bg {
  background: #000;
  opacity: 0.3;
  width: 100%;
  top: 0;
  height: 100%;
  position: absolute;
  z-index: 999;
  display: none;
}

/* =========================================
   【核心逻辑】CSS 变量动态覆写
   ========================================= */

/* 默认状态 (展开): 侧边栏宽度 220px (在 theme.scss 里定义了) */

/* 收起状态 (hideSidebar): 覆写变量为 54px (ElementPlus 折叠后的宽度) */
.hideSidebar {
  --sidebar-width: 64px !important;
}

/* 侧边栏容器：宽度会自动读取上面的变量 */
.sidebar-container {
  width: var(--sidebar-width);
  height: 100%;
  background-color: var(--bg-color-card);
  border-right: 1px solid var(--border-color);
  position: fixed;
  top: 0;
  bottom: 0;
  left: 0;
  z-index: 1001;
  overflow: hidden;
  transition: width 0.28s; /* 增加宽度变化的动画 */
}

/* 主内容区：左边距也会自动读取变量 */
.main-container {
  min-height: 100%;
  transition: margin-left 0.28s;
  margin-left: var(--sidebar-width);
  width: calc(100% - var(--sidebar-width));
  position: relative;
  background-color: var(--bg-color-page);
}

.fixed-header {
  position: fixed;
  top: 0;
  right: 0;
  z-index: 9;
  width: calc(100% - var(--sidebar-width));
  transition: width 0.28s;
}

/* =========================================
   【移动端适配】
   ========================================= */
@media (max-width: 768px) {
  /* 移动端侧边栏展开时显示遮罩 */
  .openSidebar .drawer-bg {
    display: block;
  }

  /* 移动端收起侧边栏：宽度为0 (隐藏) */
  .hideSidebar {
    --sidebar-width: 0px !important;
    .sidebar-container {
      width: 0;
      transform: translateX(-100%); /* 确保彻底移出 */
    }
  }

  /* 移动端侧边栏展开：悬浮在内容之上 */
  .sidebar-container {
    transition: transform 0.28s, width 0.28s;
    width: 220px !important; /* 强制固定宽度 */
  }

  /* 移动端展开时，侧边栏正常显示，不做位移 (默认就是0) */
  .openSidebar .sidebar-container {
    transform: translateX(0);
  }
  
  /* 移动端收起时，侧边栏移出屏幕 */
  .hideSidebar .sidebar-container {
    transform: translateX(-100%);
    width: 0 !important;
  }

  /* 移动端主内容区：始终占满全屏，不留左边距 */
  .main-container {
    margin-left: 0 !important;
    width: 100% !important;
  }

  /* 移动端顶部导航：始终占满全屏 */
  .fixed-header {
    width: 100% !important;
  }
}
</style>
