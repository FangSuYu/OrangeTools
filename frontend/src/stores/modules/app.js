import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  // 侧边栏状态：true = 展开，false = 收起
  // 也可以从 localStorage 读取，实现刷新后保持状态 (这里先简化)
  const sidebar = ref({
    opened: true,
    withoutAnimation: false
  })

  // 切换侧边栏状态的动作
  function toggleSidebar() {
    sidebar.value.opened = !sidebar.value.opened
  }

  // 关闭侧边栏 (比如点击遮罩层时)
  function closeSidebar(withoutAnimation) {
    sidebar.value.opened = false
    sidebar.value.withoutAnimation = withoutAnimation
  }

  return { sidebar, toggleSidebar, closeSidebar }
})
