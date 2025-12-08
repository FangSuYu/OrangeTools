<script setup>
import { ref, onMounted } from 'vue'
import audioSrc from '@/assets/muyu.mp3'

const count = ref(0)
const animate = ref(false)

// 1. 初始化音频
// 这里的链接是一个免费的木鱼音效，你也可以换成 src/assets/muyu.mp3
// const audioSrc = '@/assets/muyu.mp3'
const audio = new Audio(audioSrc)

// 2. 敲击逻辑
const knock = () => {
  // 播放声音 (重置进度，防止连点时不响)
  audio.currentTime = 0
  audio.play()

  // 数字+1
  count.value++
  localStorage.setItem('merit_count', count.value)

  // 触发动画
  animate.value = true
  setTimeout(() => {
    animate.value = false
  }, 100)

  // 生成飘字 (简单的 DOM 操作或使用 v-for 列表，这里简单处理仅做图标动画)
}

onMounted(() => {
  const saved = localStorage.getItem('merit_count')
  if (saved) count.value = parseInt(saved)
})
</script>

<template>
  <div class="wooden-fish-container" @click="knock">
    <div class="merit-counter">今日功德: {{ count }}</div>

    <div class="fish-icon" :class="{ 'active': animate }">
      <svg viewBox="0 0 1024 1024" width="150" height="150">
        <path d="M512 64C264.6 64 64 264.6 64 512s200.6 448 448 448 448-200.6 448-448S759.4 64 512 64z" fill="#FFC107"/>
        <path d="M512 128c-212 0-384 172-384 384s172 384 384 384 384-172 384-384-172-384-384-384z" fill="#FFA000"/>
        <path d="M704 512H320c-17.7 0-32-14.3-32-32s14.3-32 32-32h384c17.7 0 32 14.3 32 32s-14.3 32-32 32z" fill="#FFF"/>
      </svg>
      <div class="text-float" v-if="animate">功德 +1</div>
    </div>

    <p class="hint">点击木鱼，积攒功德</p>
  </div>
</template>

<style lang="scss" scoped>
.wooden-fish-container {
  display: flex; flex-direction: column; align-items: center; justify-content: center; height: 300px; cursor: pointer; user-select: none;
  .merit-counter { font-size: 24px; font-weight: bold; margin-bottom: 30px; color: var(--text-color-primary); }
  .fish-icon {
    position: relative; transition: transform 0.1s;
    &.active { transform: scale(0.95); }
    .text-float {
      position: absolute; top: -40px; left: 50%; transform: translateX(-50%);
      font-size: 20px; color: #f56c6c; font-weight: bold;
      animation: floatUp 0.5s ease-out forwards;
    }
  }
  .merit-counter {
    font-family: 'Helvetica Neue', sans-serif;
    background: linear-gradient(90deg, #f12711, #f5af19);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    font-size: 32px;
  }
  .hint { margin-top: 20px; color: var(--text-color-secondary); }
}

@keyframes floatUp {
  0% { opacity: 1; transform: translate(-50%, 0); }
  100% { opacity: 0; transform: translate(-50%, -50px); }
}
</style>
