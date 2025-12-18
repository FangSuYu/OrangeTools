<script setup>
import { Stamp } from '@element-plus/icons-vue' // 引入盾牌图标
</script>

<template>
  <div class="beian-container">
    <a href="https://beian.miit.gov.cn/" target="_blank" class="beian-link">
      <el-icon class="beian-icon"><Stamp /></el-icon>
      <span>渝ICP备2024040846号</span>
    </a>
  </div>
</template>

<style lang="scss" scoped>
.beian-container {
  display: inline-flex; /* 改为 inline-flex 以便在父级横向排列 */
  align-items: center;

  .beian-link {
    display: flex;
    align-items: center;
    color: inherit; /* 继承父级颜色 */
    text-decoration: none;
    font-family: Arial, sans-serif;
    transition: color 0.3s;

    .beian-icon {
      margin-right: 4px;
      font-size: 14px;
    }

    &:hover {
      color: var(--el-color-primary);
    }
  }
}
</style>
