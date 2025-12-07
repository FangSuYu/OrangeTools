<script setup>
import { ref } from 'vue'
import UserCard from './components/UserCard.vue'
import InfoTab from './components/InfoTab.vue'
import PasswordTab from './components/PasswordTab.vue'
import WoodenFish from './components/WoodenFish.vue'
import { User, Lock, Bell } from '@element-plus/icons-vue'

const activeTab = ref('info')
</script>

<template>
  <div class="profile-container">
    <div class="background-layer">
      <div class="blob blob-1"></div>
      <div class="blob blob-2"></div>
    </div>

    <div class="content-layer">
      <el-row :gutter="24" class="main-row">
        <el-col :span="8" :xs="24" class="col-left">
          <UserCard />
        </el-col>

        <el-col :span="16" :xs="24" class="col-right">
          <div class="glass-card settings-panel">
            <el-tabs v-model="activeTab" class="custom-tabs">
              <el-tab-pane name="info">
                <template #label>
                  <div class="tab-item">
                    <el-icon><User /></el-icon> <span>基础资料</span>
                  </div>
                </template>
                <InfoTab />
              </el-tab-pane>

              <el-tab-pane name="password">
                <template #label>
                  <div class="tab-item">
                    <el-icon><Lock /></el-icon> <span>安全设置</span>
                  </div>
                </template>
                <PasswordTab />
              </el-tab-pane>

              <el-tab-pane name="fish">
                <template #label>
                  <div class="tab-item">
                    <el-icon><Bell /></el-icon> <span>赛博木鱼</span>
                  </div>
                </template>
                <WoodenFish />
              </el-tab-pane>
            </el-tabs>
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.profile-container {
  position: relative;
  min-height: calc(100vh - 84px); /* 减去 navbar 高度 */
  padding: 40px;
  overflow: hidden;
  background: var(--bg-color-page);
}

/* === 炫酷背景层 === */
.background-layer {
  position: absolute;
  top: 0; left: 0; width: 100%; height: 100%;
  z-index: 0;
  overflow: hidden;

  .blob {
    position: absolute;
    filter: blur(80px);
    opacity: 0.6;
    animation: float 10s infinite ease-in-out alternate;
  }
  .blob-1 {
    top: -10%; left: -10%; width: 500px; height: 500px;
    background: linear-gradient(135deg, #ff9a9e 0%, #fad0c4 100%);
    border-radius: 40% 60% 70% 30% / 40% 50% 60% 50%;
  }
  .blob-2 {
    bottom: -10%; right: -10%; width: 600px; height: 600px;
    background: linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%);
    border-radius: 60% 40% 30% 70% / 60% 30% 70% 40%;
    animation-delay: -5s;
  }

  /* 暗黑模式背景调整 */
  :deep(html.dark) & {
    .blob-1 { background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); opacity: 0.2; }
    .blob-2 { background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%); opacity: 0.2; }
  }
}

@keyframes float {
  0% { transform: translate(0, 0) rotate(0deg); }
  100% { transform: translate(50px, 50px) rotate(20deg); }
}

/* === 内容层 === */
.content-layer {
  position: relative;
  z-index: 1;
  max-width: 1200px;
  margin: 0 auto;
}

/* 通用毛玻璃卡片样式 */
.glass-card {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.05);

  :deep(html.dark) & {
    background: rgba(30, 30, 30, 0.6);
    border: 1px solid rgba(255, 255, 255, 0.1);
  }
}

.settings-panel {
  padding: 30px 40px;
  min-height: 580px; /* 强制高度与左侧视觉平衡 */
}

/* Tab 样式重构 */
.custom-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 30px;
    .el-tabs__nav-wrap::after { display: none; /* 去掉灰色底线 */ }
    .el-tabs__active-bar {
      height: 4px; border-radius: 2px;
      background: linear-gradient(90deg, var(--el-color-primary), #a18cd1);
    }
    .el-tabs__item {
      font-size: 16px;
      font-weight: 500;
      color: var(--text-color-secondary);
      &.is-active {
        color: var(--text-color-primary);
        font-weight: bold;
        transform: scale(1.05);
      }
    }
  }
}

.tab-item {
  display: flex; align-items: center; gap: 8px;
  .el-icon { font-size: 18px; }
}
</style>
