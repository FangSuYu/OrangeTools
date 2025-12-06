<template>
  <section class="app-main">
    <router-view v-slot="{ Component }">
      <transition name="fade-transform" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
  </section>
</template>

<style lang="scss" scoped>
.app-main {
  /* 最小高度 = 屏幕高度 - 顶栏高度 */
  min-height: calc(100vh - var(--header-height));
  width: 100%;
  position: relative;
  overflow: hidden;
  padding-top: var(--header-height); /* 给固定的顶栏让出位置 */
  padding: calc(var(--header-height) + 20px) 20px 20px 20px;
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
</style>
