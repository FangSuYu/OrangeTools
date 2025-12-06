<script setup>
// 引入刚才写的组件
import ThemeSwitch from '@/components/ThemeSwitch/ThemeSwitch.vue'
</script>

<template>
  <div class="app-container">
    <h1>🎨 样式中心测试</h1>

    <div style="margin-bottom: 20px;">
      <span>切换模式：</span>
      <ThemeSwitch />
    </div>

    <div class="card-box">
      <p>1. 切换开关，观察背景色是否变黑。</p>
      <p>2. 观察下面的橙色按钮是否依然醒目。</p>
      <p>3. 观察文字颜色是否自动变白。</p>

      <el-button type="primary">活力橙按钮</el-button>
      <el-button>普通按钮</el-button>
    </div>
  </div>
</template>

<style lang="scss">
/* 必须显式指定 body 的背景色，否则 body 默认是透明的，看起来还是白的 */
body {
  background-color: var(--bg-color-page);
  color: var(--text-color-primary);
  transition: background-color 0.3s, color 0.3s; /* 增加丝滑过渡动画 */
}

.app-container {
  padding: 50px;
  text-align: center;
  h1 { margin-bottom: 30px; }
  .card-box {
    /* 引用 theme.scss 里的变量 */
    background-color: var(--bg-color-card);
    border: 1px solid var(--border-color); /* 增加边框变量，暗黑模式下更明显 */
    width: 600px;
    margin: 0 auto;
    padding: 20px;
    border-radius: 8px;
    transition: all 0.3s; /* 过渡动画 */
  }
}
</style>
