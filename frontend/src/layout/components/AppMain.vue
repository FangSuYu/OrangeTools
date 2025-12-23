<script setup>
import BeiAnFooter from '@/components/BeiAnFooter/index.vue'
import { Platform, Coffee, Sunny } from '@element-plus/icons-vue'
</script>
<template>
  <section class="app-main">
    <div class="app-content">
      <router-view v-slot="{ Component }">
        <transition name="fade-transform" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
      <div class="main-footer-area">

        <div class="footer-line">
          <span class="copyright">Copyright © 2025 OrangeTools.</span>
          <span class="divider">|</span>
          <span class="made-with">
            Made with <span class="heart">❤</span> by
            <a href="https://github.com/FangSuYu/OrangeTools" target="_blank" class="author-link">FangSuYu</a>
          </span>
        </div>

        <div class="footer-line">
          <BeiAnFooter />

          <span class="divider">|</span>

          <a href="https://github.com/FangSuYu/OrangeTools" target="_blank" class="github-link">
            <el-icon style="vertical-align: middle; margin-right: 2px">
              <Platform />
            </el-icon>
            GitHub
          </a>
        </div>

      </div>
    </div>
  </section>
</template>

<style lang="scss" scoped>
.app-main {
  /* 最小高度 = 屏幕高度 - 顶栏高度 */
  min-height: calc(100vh - var(--header-height));
  width: 100%;
  position: relative;
  overflow: hidden;

  /* 2. 【核心】使用 Flex 布局，方向为垂直列 */
  display: flex;
  flex-direction: column;

  /* 3. 设置内边距：顶部避让Header + 20px间距，左右20px */
  padding: calc(var(--header-height) + 20px) 20px 0 20px;

  background-color: var(--bg-color-page);

  /* 移动端适配：减少左右内边距 */
  @media (max-width: 768px) {
    padding: calc(var(--header-height) + 20px) 10px 0 10px;
  }
}

/* 【新增】内容包裹层样式 */
.app-content {
  /* 5. 【核心】flex: 1 让这个区域自动占据所有剩余空间 */
  /* 这样即使内容只有一行，它也会把下面的 footer 挤到屏幕最底端 */
  flex: 1;

  /* 给内容顶部留点呼吸感 */
  padding-top: 20px;
}

/* 简单的页面切换动画 */
.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.3s;
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(-30px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(30px);
}

/* 底部区域样式 (微调) */
.main-footer-area {
  padding: 20px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px; /* 两行之间的间距 */
  font-size: 12px;
  color: var(--text-color-secondary);
  background-color: transparent;

  .footer-line {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px; /* 元素间距 */
    flex-wrap: wrap;
  }

  .divider {
    color: var(--border-color);
    margin: 0 2px;
  }

  a {
    color: var(--text-color-secondary);
    text-decoration: none;
    transition: color 0.3s;
    &:hover {
      color: var(--el-color-primary);
      text-decoration: underline;
    }
  }

  /* 红心跳动动画 */
  .heart {
    color: #f56c6c;
    display: inline-block;
    animation: heartBeat 1.5s infinite;
    margin: 0 2px;
    font-size: 14px;
  }
}

@keyframes heartBeat {
  0% { transform: scale(1); }
  14% { transform: scale(1.3); }
  28% { transform: scale(1); }
  42% { transform: scale(1.3); }
  70% { transform: scale(1); }
}
</style>
