<script setup>
import { useAppStore } from '@/stores/modules/app'
import { useUserStore } from '@/stores/modules/user'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { CaretBottom } from '@element-plus/icons-vue' // 引入下箭头图标
import ThemeSwitch from '@/components/ThemeSwitch/index.vue'
import Hamburger from '@/components/Hamburger/index.vue'

const appStore = useAppStore()
const userStore = useUserStore()
const router = useRouter()

function toggleSideBar() {
  appStore.toggleSidebar()
}

// 退出登录逻辑
const handleLogout = () => {
  ElMessageBox.confirm('确定要注销并退出系统吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    // 1. 清理 Pinia 和 LocalStorage
    userStore.logout()
    // 2. 跳转回登录页 (带上当前页面的路径，方便登录后跳回来，虽然现在是 dashboard)
    router.push(`/login?redirect=${router.currentRoute.value.fullPath}`)
  }).catch(() => {})
}
</script>

<template>
  <div class="navbar">
    <div class="left-menu">
      <Hamburger
        id="hamburger-container"
        :is-active="appStore.sidebar.opened"
        class="hamburger-container"
        @toggleClick="toggleSideBar"
      />

      <span style="font-size: 14px; color: var(--text-color-secondary); margin-left: 10px;">
        首页 (Dashboard)
      </span>
    </div>

    <div class="right-menu">
      <ThemeSwitch class="right-menu-item" />

      <el-dropdown class="avatar-container" trigger="click">
        <div class="avatar-wrapper">
          <el-avatar
            :size="30"
            :src="userStore.avatar || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'"
          />
          <span class="username">{{ userStore.username || '用户' }}</span>
          <el-icon class="el-icon--right"><CaretBottom /></el-icon>
        </div>

        <template #dropdown>
          <el-dropdown-menu>
            <router-link to="/">
              <el-dropdown-item>个人中心</el-dropdown-item>
            </router-link>
            <a target="_blank" href="https://github.com/FangSuYu/OrangeTools">
              <el-dropdown-item>项目地址</el-dropdown-item>
            </a>
            <el-dropdown-item divided @click="handleLogout">
              <span style="display:block;">退出登录</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.navbar {
  height: var(--header-height);
  overflow: hidden;
  position: relative;
  background: var(--bg-color-card);
  box-shadow: 0 1px 4px var(--shadow-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;

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
      cursor: pointer;

      .avatar-wrapper {
        display: flex;
        align-items: center;

        .username {
          margin-left: 8px;
          font-size: 14px;
          color: var(--text-color-regular);
          font-weight: 500;
        }

        .el-icon--right {
          margin-left: 4px;
          color: var(--text-color-secondary);
        }
      }
    }
  }
}
</style>
