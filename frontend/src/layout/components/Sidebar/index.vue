<script setup>
import { useRoute } from 'vue-router'
import { useAppStore } from '@/stores/modules/app'
import { Monitor, Tools , ChatLineSquare, Medal, Calendar, DataAnalysis} from '@element-plus/icons-vue'
// 引入我们强大的新 Logo 组件
import Logo from './Logo.vue'

const route = useRoute()
const appStore = useAppStore()
</script>

<template>
  <div class="sidebar-wrapper">
    <Logo :collapse="!appStore.sidebar.opened" />

    <el-scrollbar wrap-class="scrollbar-wrapper">
      <el-menu
        :default-active="route.path"
        :collapse="!appStore.sidebar.opened"
        :background-color="'transparent'"
        :text-color="'var(--text-color-regular)'"
        :active-text-color="'var(--el-color-primary)'"
        :unique-opened="false"
        :collapse-transition="false"
        mode="vertical"
        router
        class="el-menu-vertical"
      >
        <el-menu-item index="/dashboard">
          <el-icon><Monitor /></el-icon>
          <template #title>主控台</template>
        </el-menu-item>

        <el-sub-menu index="/tools">
          <template #title>
            <el-icon><Tools /></el-icon>
            <span>常用工具</span>
          </template>
          <el-menu-item index="/tools/course">
            <el-icon><DataAnalysis /></el-icon>
            课表统计助手</el-menu-item>
          <el-menu-item index="/tools/scheduler">
            <el-icon><Calendar /></el-icon>
            智能排班助手</el-menu-item>
        </el-sub-menu>
        <el-menu-item index="/community/feedback">
          <el-icon><ChatLineSquare /></el-icon>
          <template #title>需求许愿墙</template>
        </el-menu-item>
        <el-menu-item index="/community/contributors">
          <el-icon><Medal /></el-icon>
          <template #title>贡献者墙</template>
        </el-menu-item>
      </el-menu>
    </el-scrollbar>
  </div>
</template>

<style lang="scss" scoped>
.sidebar-wrapper {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: var(--bg-color-card);
  /* 右侧边框，在暗黑模式下能清晰分割侧边栏和内容区 */
  border-right: 1px solid var(--border-color);

  /* 让 Logo 区域固定在顶部，不随菜单滚动 */
  :deep(.sidebar-logo-container) {
    flex-shrink: 0; /* 禁止压缩 */
  }

  /* 让滚动区域占满剩余空间 */
  .el-scrollbar {
    flex: 1;
    height: 0; /* 这是一个 flex 布局的小技巧，确保 scrollbar 能正确计算高度 */
  }
}

/* 菜单样式 (保持不变) */
.el-menu-vertical {
  border-right: none;
  :deep(.el-menu-item), :deep(.el-sub-menu__title) {
    &:hover {
      background-color: rgba(0,0,0,0.04);
    }
  }
  :deep(.el-menu-item.is-active) {
    background-color: var(--el-color-primary-light-9);
    border-right: 3px solid var(--el-color-primary);
  }
}
/* 【新增】针对折叠模式的暴力修正 */
:deep(.el-menu--collapse) {
  .el-menu-item,
  .el-sub-menu__title {
    /* 1. 强制内边距为 0，防止 Element 默认的 padding 把图标挤偏 */
    padding: 0 !important;

    /* 2. 使用 Flex 强制居中 */
    display: flex !important;
    justify-content: center !important;
    align-items: center !important;

    /* 3. 确保图标本身没有奇怪的 margin */
    .el-icon {
      margin: 0 !important;
      text-align: center;
      vertical-align: middle;
      width: 24px;
    }

    /* 4. 隐藏折叠时的文字 span (防止某些情况文字没藏住把图标顶歪) */
    span {
      height: 0;
      width: 0;
      overflow: hidden;
      visibility: hidden;
      display: inline-block;
    }
  }
}
</style>
