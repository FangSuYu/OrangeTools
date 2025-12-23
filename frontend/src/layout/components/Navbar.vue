<script setup>
import { useAppStore } from '@/stores/modules/app'
import { useUserStore } from '@/stores/modules/user'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { CaretBottom } from '@element-plus/icons-vue'
import ThemeSwitch from '@/components/ThemeSwitch/index.vue'
import Hamburger from '@/components/Hamburger/index.vue'
import { computed } from 'vue'

const appStore = useAppStore()
const userStore = useUserStore()
const router = useRouter()
const route = useRoute()

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
    userStore.logout()
    router.push(`/login?redirect=${router.currentRoute.value.fullPath}`)
  }).catch(() => {})
}

// 面包屑逻辑：动态计算当前路径
const breadcrumbs = computed(() => {
  // 过滤出有 meta.title 的路由记录
  const matched = route.matched.filter(item => item.meta && item.meta.title)
  const first = matched[0]

  // 如果第一个不是 Dashboard，我们手动加一个 "主控台" 在前面作为根节点
  if (first && first.path !== '/dashboard') {
    return [{ title: '主控台', path: '/dashboard' }, ...matched]
  }

  return matched
})

// 判断是否是面包屑的最后一项（不可点击）
const isLast = (index) => {
  return index === breadcrumbs.value.length - 1
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

      <el-breadcrumb separator="/" class="breadcrumb-container">
        <transition-group name="breadcrumb">
          <el-breadcrumb-item v-for="(item, index) in breadcrumbs" :key="item.path">
            <span v-if="isLast(index) || !item.path" class="no-redirect">
              {{ item.meta?.title || item.title }}
            </span>
            <a v-else @click.prevent="router.push(item.path)">
              {{ item.meta?.title || item.title }}
            </a>
          </el-breadcrumb-item>
        </transition-group>
      </el-breadcrumb>
    </div>

    <div class="right-menu">
      <ThemeSwitch class="right-menu-item" />

      <el-dropdown class="avatar-container" trigger="click">
        <div class="avatar-wrapper">
          <el-avatar
            :size="30"
            :src="userStore.avatarUrl || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'"
          />
          <span class="username">{{ userStore.username || '用户' }}</span>
          <el-icon class="el-icon--right"><CaretBottom /></el-icon>
        </div>

        <template #dropdown>
          <el-dropdown-menu>
            <router-link to="/profile">
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
      background: var(--bg-color-hover)
    }
  }

  .left-menu {
    display: flex;
    align-items: center;

    /* 【新增】面包屑组件样式 */
    .breadcrumb-container {
      display: inline-block;
      font-size: 14px;
      line-height: 50px;
      margin-left: 8px;

      @media (max-width: 500px) {
        display: none;
      }

      .no-redirect {
        color: var(--text-color-secondary);
        cursor: text;
      }

      a {
        color: var(--text-color-regular);
        cursor: pointer;
        font-weight: 500;
        &:hover {
          color: var(--el-color-primary);
        }
      }
    }
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
        background: var(--bg-color-hover);
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

/* 【新增】面包屑切换动画 */
.breadcrumb-enter-active,
.breadcrumb-leave-active {
  transition: all 0.5s;
}

.breadcrumb-enter-from,
.breadcrumb-leave-active {
  opacity: 0;
  transform: translateX(20px);
}

.breadcrumb-leave-active {
  position: absolute;
}
</style>
