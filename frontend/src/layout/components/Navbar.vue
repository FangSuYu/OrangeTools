<script setup>
import { useAppStore } from '@/stores/modules/app'
// 引入我们的主题切换组件
import ThemeSwitch from '@/components/ThemeSwitch/index.vue'
import Hamburger from '@/components/Hamburger/index.vue'

// 引入 Store
const appStore = useAppStore()

// 切换侧边栏方法
function toggleSideBar() {
  appStore.toggleSidebar()
}
</script>

<template>
  <div class="navbar">
    <div class="left-menu">
      <Hamburger id="hamburger-container" :is-active="appStore.sidebar.opened" class="hamburger-container"
        @toggleClick="toggleSideBar" />

      <span style="font-size: 14px; color: var(--text-color-secondary)">
        当前位置：首页 (Dashboard)
      </span>
    </div>

    <div class="right-menu">
      <ThemeSwitch class="right-menu-item" />

      <div class="avatar-container right-menu-item">
        <el-avatar :size="30" src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" />
        <span class="username">OrangeUser</span>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.navbar {
  height: var(--header-height);
  overflow: hidden;
  position: relative;
  background: var(--bg-color-card);
  /* 跟随主题变色 */
  box-shadow: 0 1px 4px var(--shadow-color);
  /* 跟随主题变阴影 */
  display: flex;
  justify-content: space-between;
  /* 左右两端对齐 */
  align-items: center;
  padding: 0 20px;

  /* 【新增】汉堡包容器样式 */
  .hamburger-container {
    line-height: var(--header-height);
    height: 100%;
    float: left;
    cursor: pointer;
    transition: background .3s;
    -webkit-tap-highlight-color: transparent;

    &:hover {
      background: rgba(0, 0, 0, .025)
    }
  }

  .left-menu {
    display: flex;
    align-items: center;
  }


  .right-menu {
    display: flex;
    align-items: center;

    .right-menu-item {
      display: inline-block;
      padding: 0 8px;
      height: 100%;
      vertical-align: text-bottom;
      cursor: pointer;

      &:hover {
        background: rgba(0, 0, 0, 0.025);
      }
    }

    .avatar-container {
      margin-left: 10px;
      display: flex;
      align-items: center;

      .username {
        margin-left: 8px;
        font-size: 14px;
        color: var(--text-color-regular);
      }
    }
  }
}
</style>
