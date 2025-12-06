import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useCourseStore = defineStore('course', () => {
  // 1. 核心数据：后端返回的那个大 JSON (AnalysisResult)
  const analysisResult = ref(null)

  // 2. 上次分析时间 (用于判断是否过期，比如超过 2 小时就清空)
  const lastAnalyzeTime = ref(0)

  // 设置数据
  function setAnalysisResult(data) {
    analysisResult.value = data
    lastAnalyzeTime.value = Date.now()
  }

  // 清空数据
  function clearData() {
    analysisResult.value = null
    lastAnalyzeTime.value = 0
  }

  // 检查是否过期 (例如 2 小时 = 2 * 60 * 60 * 1000 毫秒)
  // 你可以在调用前检查这个方法
  function isExpired() {
    const EXPIRE_TIME = 2 * 60 * 60 * 1000
    return (Date.now() - lastAnalyzeTime.value) > EXPIRE_TIME
  }

  return {
    analysisResult,
    lastAnalyzeTime,
    setAnalysisResult,
    clearData,
    isExpired
  }
}, {
  persist: true // 开启持久化，刷新不丢失
})
