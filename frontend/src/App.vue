<script setup>
// 这里以后写逻辑
</script>

<template>
  <div class="test-container">
    <h1>🍊 橙子工具箱前端环境</h1>

    <div class="card">
      <p>如果你看到了下面的蓝色按钮，说明 Element Plus 配置成功！</p>
      <el-button type="primary" size="large" round>
        点我测试
      </el-button>

      <el-button type="success" plain>
        Vue 3.5 + Vite 7
      </el-button>
    </div>
  </div>
</template>

<style scoped>
/* 简单的居中样式 */
.test-container {
  text-align: center;
  margin-top: 60px;
}
.card {
  margin-top: 20px;
  padding: 20px;
  border: 1px solid #eee;
  border-radius: 8px;
}
</style>
