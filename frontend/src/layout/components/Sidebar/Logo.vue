<script setup>
import { ref, computed } from 'vue'
// 引入一个默认图标，万一没配图片时用它兜底
import { Shop } from '@element-plus/icons-vue'

const props = defineProps({
  collapse: {
    type: Boolean,
    required: true
  }
})

// ================== 配置区 (以后可以移到全局配置文件) ==================
const title = ref('OrangeTools') // 网站标题
// 模式控制开关：
// true  = 显示“图片+文字” (展开时) / “只显示图片” (收缩时)
// false = 显示“纯文字” (展开时) / “显示兜底图标” (收缩时)
const showLogoImage = ref(true)

// Logo 图片地址 (先用网图代替，你有了本地图片后改成 import 引入)
// 建议：大图用长方形，小图用正方形
const logoUrl = ref('https://element-plus.org/images/element-plus-logo.svg')
const logoSmallUrl = ref('https://element-plus.org/images/element-plus-logo-small.svg')

// 计算当前应该显示哪个图片
const currentLogo = computed(() => {
  return props.collapse ? (logoSmallUrl.value || logoUrl.value) : logoUrl.value
})
// ====================================================================
</script>

<template>
  <div class="sidebar-logo-container" :class="{'collapse': collapse}">
    <transition name="sidebarLogoFade">
      <router-link key="expand" class="sidebar-logo-link" to="/">

        <template v-if="showLogoImage">
          <img v-if="currentLogo" :src="currentLogo" class="sidebar-logo">
          <h1 v-show="!collapse" class="sidebar-title">{{ title }}</h1>
        </template>

        <template v-else>
          <el-icon v-show="collapse" class="sidebar-logo-icon" size="24"><Shop /></el-icon>
          <h1 v-show="!collapse" class="sidebar-title text-center">{{ title }}</h1>
        </template>

      </router-link>
    </transition>
  </div>
</template>

<style lang="scss" scoped>
/* 定义过渡动画 */
.sidebarLogoFade-enter-active {
  transition: opacity 0.3s;
}
.sidebarLogoFade-enter-from,
.sidebarLogoFade-leave-to {
  opacity: 0;
}

.sidebar-logo-container {
  position: relative;
  width: 100%;
  height: var(--header-height);
  line-height: var(--header-height);
  background: var(--bg-color-card);
  text-align: center;
  overflow: hidden;
  border-bottom: 1px solid var(--border-color);
  box-sizing: border-box;

  & .sidebar-logo-link {
    height: 100%;
    width: 100%;

    /* 【核心修复】强制 Flex 居中，确保水平和垂直都在正中间 */
    display: flex;
    align-items: center;
    justify-content: center;

    text-decoration: none;

    /* 图片样式 */
    & .sidebar-logo {
      width: 32px;
      height: 32px;
      vertical-align: middle;
      margin-right: 12px; /* 展开时：保留右边距 */
      transition: all 0.3s;

      /* 防止图片被挤压 */
      flex-shrink: 0;
    }

    & .sidebar-logo-icon {
      color: var(--el-color-primary);
    }

    /* 标题样式 */
    & .sidebar-title {
      display: inline-block;
      margin: 0;
      color: var(--text-color-primary);
      font-weight: 600;
      line-height: 50px;
      font-size: 16px;
      font-family: Avenir, Helvetica Neue, Arial, Helvetica, sans-serif;
      vertical-align: middle;
      white-space: nowrap;
      transition: all 0.3s;

      &.text-center {
        margin-right: 0;
        font-size: 18px;
        color: var(--el-color-primary);
      }
    }
  }

  /* 【收缩状态下的终极修正】 */
  &.collapse {
    /* 1. 强制容器内无内边距 */
    padding: 0;

    .sidebar-logo {
      /* 2. 彻底清除边距，确保它就在 Flex 盒子的正中心 */
      margin-right: 0px !important;
      margin-left: 0px !important;

      /* 3. 调整大小与 Element Plus 菜单图标(24px) 视觉一致 */
      width: 24px;
      height: 24px;
    }

    /* 4. 如果是纯文字图标模式，也确保居中 */
    .sidebar-logo-icon {
        margin: 0 !important;
    }
  }
}
</style>
