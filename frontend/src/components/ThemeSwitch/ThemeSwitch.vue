<script setup>
import { useDark, useToggle } from '@vueuse/core'
import { Sunny, Moon } from '@element-plus/icons-vue'
import { nextTick } from 'vue'

const isDark = useDark()
const toggleDark = useToggle(isDark)

const switchTheme = (event) => {
  const x = event.clientX
  const y = event.clientY

  const endRadius = Math.hypot(
    Math.max(x, innerWidth - x),
    Math.max(y, innerHeight - y),
  )

  // 兼容性处理
  if (!document.startViewTransition) {
    toggleDark()
    return
  }

  const transition = document.startViewTransition(async () => {
    toggleDark()
    await nextTick()
  })

  transition.ready.then(() => {
    const clipPath = [
      `circle(0px at ${x}px ${y}px)`,
      `circle(${endRadius}px at ${x}px ${y}px)`,
    ]

    // 【核心优化点】
    // 不再区分是从黑变白还是从白变黑，也不再反转 clipPath。
    // 永远执行：新视图(View-New) 从 0 扩散到 Full。
    // 这样避免了 Z-Index 切换带来的闪烁。
    document.documentElement.animate(
      {
        clipPath: clipPath,
      },
      {
        duration: 400,
        easing: 'ease-in',
        // 永远只操作“新视图”
        pseudoElement: '::view-transition-new(root)',
      },
    )
  })
}
</script>

<template>
  <div class="theme-switch-wrapper" @click="switchTheme">
    <el-switch
      v-model="isDark"
      inline-prompt
      :active-icon="Moon"
      :inactive-icon="Sunny"
      class="theme-switch"
      style="--el-switch-on-color: #2C2D2E; --el-switch-off-color: #f2f2f2"
    />
  </div>
</template>

<style scoped lang="scss">
.theme-switch-wrapper {
  display: inline-block;
  cursor: pointer;
  :deep(.el-switch) {
    pointer-events: none;
  }
}

.theme-switch {
  :deep(.el-switch__core) {
    border-color: var(--border-color);
    .el-icon {
      color: var(--text-color-regular);
    }
  }
}
</style>

<style>
::view-transition-old(root),
::view-transition-new(root) {
  animation: none;
  mix-blend-mode: normal;
}

/* 【核心优化点】永远让“新视图”在最上面 */
/* 这样不管是黑变白，还是白变黑，都是新颜色盖在旧颜色上面 */
::view-transition-new(root) {
  z-index: 9999;
}
::view-transition-old(root) {
  z-index: 1;
}
</style>
