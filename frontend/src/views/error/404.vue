<script setup>
import { useRouter } from 'vue-router'
import { Vue3Lottie } from 'vue3-lottie'

const router = useRouter()

// Lottie 动画链接 (404 Not Found)
// 这是一个可爱的宇航员迷失太空的动画
const lottieUrl = 'src/assets/lottie/404.json'

const goHome = () => {
  router.push('/')
}
</script>

<template>
  <div class="error-container">
    <div class="lottie-box">
      <Vue3Lottie :animationLink="lottieUrl" :height="400" :width="400" />
    </div>
    <div class="content">
      <h1>404</h1>
      <p class="desc">哎呀，你访问的页面好像飞到外太空去了...</p>
      <el-button type="primary" size="large" round @click="goHome">
        返回地球 (主页)
      </el-button>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.error-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background: var(--bg-color-page);

  .content {
    text-align: center;
    margin-top: -20px; /* 稍微往上提一点 */

    h1 {
      font-size: 72px;
      margin: 0;
      color: var(--el-color-primary);
      font-family: 'Helvetica Neue', sans-serif;
    }

    .desc {
      color: var(--text-color-secondary);
      font-size: 16px;
      margin: 10px 0 30px;
    }
  }
}
</style>
