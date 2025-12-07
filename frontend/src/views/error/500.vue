<script setup>
import { useRouter } from 'vue-router'
import { Vue3Lottie } from 'vue3-lottie'

const router = useRouter()

// Lottie 动画链接 (500 Server Error)
const lottieUrl = 'src/assets/lottie/500.json'

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
      <h1>500</h1>
      <p class="desc">服务器正在开小差，我们的工程师正在修理它！</p>
      <el-button type="primary" size="large" round @click="goHome">
        返回首页重试
      </el-button>
    </div>
  </div>
</template>

<style lang="scss" scoped>
/* 复用 404 的样式即可，保持一致性 */
.error-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background: var(--bg-color-page);

  .content {
    text-align: center;
    margin-top: -20px;

    h1 {
      font-size: 72px;
      margin: 0;
      color: #f56c6c; /* 500错误用红色系 */
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
