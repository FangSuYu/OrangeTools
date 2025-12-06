<script setup>
import { computed } from 'vue'
import { AppMain, Navbar, Sidebar } from './components'
import { useAppStore } from '@/stores/modules/app'

const appStore = useAppStore()

// 计算属性：动态生成 class
const classObj = computed(() => ({
  hideSidebar: !appStore.sidebar.opened,
  openSidebar: appStore.sidebar.opened,
  withoutAnimation: appStore.sidebar.withoutAnimation
}))
</script>

<template>
  <div :class="classObj" class="app-wrapper">
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
</style>
