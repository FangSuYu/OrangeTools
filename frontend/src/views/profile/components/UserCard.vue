<script setup>
import { ref } from 'vue'
import { useUserStore } from '@/stores/modules/user'
import { updateProfile } from '@/api/user'
import { ElMessage } from 'element-plus'
import { Camera, Iphone, Message } from '@element-plus/icons-vue'

const userStore = useUserStore()
const dialogVisible = ref(false)

const avatarList = [1, 2, 3, 4].map(i => ({
  id: i,
  name: `avatar-${i}.png`,
  url: new URL(`../../../assets/images/avatars/avatar-${i}.png`, import.meta.url).href
}))

const handleAvatarClick = () => dialogVisible.value = true

const selectAvatar = async (avatarName) => {
  try {
    await updateProfile({ nickname: userStore.nickname, avatar: avatarName })
    await userStore.getInfo()
    ElMessage.success('头像更换成功')
    dialogVisible.value = false
  } catch (e) { console.error(e) }
}
</script>

<template>
  <div class="user-card-wrapper glass-card">
    <div class="card-header-bg"></div>

    <div class="card-body">
      <div class="avatar-container" @click="handleAvatarClick">
        <div class="avatar-ring">
          <img :src="userStore.avatarUrl" class="real-avatar" />
        </div>
        <div class="camera-icon">
          <el-icon><Camera /></el-icon>
        </div>
      </div>

      <div class="identity">
        <h2 class="nickname">{{ userStore.nickname || '未设置昵称' }}</h2>
        <div class="tags">
          <span class="role-tag" :class="{ admin: userStore.role === 'admin' }">
            {{ userStore.role === 'admin' ? '管理员' : '普通用户' }}
          </span>
          <span class="id-tag">@{{ userStore.username }}</span>
        </div>
        <p class="bio">保持热爱，奔赴山海。</p> </div>

      <el-divider border-style="dashed" />

      <div class="detail-list">
        <!-- <div class="item">
          <div class="label"><el-icon><Calendar /></el-icon> 注册时间</div>
          <div class="val">2025-12-05</div>
        </div> -->
        <div class="item">
          <div class="label"><el-icon><Iphone /></el-icon> 手机绑定</div>
          <div class="val">{{ userStore.phone || '未绑定' }}</div>
        </div>
        <div class="item">
          <div class="label"><el-icon><Message /></el-icon> 邮箱绑定</div>
          <div class="val">{{ userStore.email || '未绑定' }}</div>
        </div>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" title="更换形象" width="400px" align-center append-to-body custom-class="glass-dialog">
      <div class="avatar-grid">
        <div
          v-for="item in avatarList"
          :key="item.id"
          class="avatar-option"
          :class="{ active: userStore.avatar === item.name }"
          @click="selectAvatar(item.name)"
        >
          <img :src="item.url" />
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style lang="scss" scoped>
/* 复用父组件的 glass-card 样式，这里只需补充内部结构 */
.glass-card {
  background: var(--bg-color-card);
  backdrop-filter: blur(20px);
  border: 1px solid var(--border-color-light);
  border-radius: 24px;
  overflow: hidden;
  height: 580px; /* 与右侧高度对齐 */
}

.card-header-bg {
  height: 140px;
  background: linear-gradient(120deg, #84fab0 0%, #8fd3f4 100%);
  position: relative;
  /* 暗黑模式换个深邃点的渐变 */
  :deep(html.dark) & {
    background: linear-gradient(120deg, #434343 0%, black 100%);
  }
}

.card-body {
  padding: 0 30px;
  position: relative;
  text-align: center;
}

.avatar-container {
  width: 110px; height: 110px;
  margin: -55px auto 15px; /* 负边距让头像上移一半 */
  position: relative;
  cursor: pointer;

  .avatar-ring {
    width: 100%; height: 100%;
    border-radius: 50%;
    padding: 4px;
    background: var(--bg-color-card); /* 与卡片背景一致 */
    box-shadow: 0 4px 10px rgba(0,0,0,0.1);

    .real-avatar {
      width: 100%; height: 100%; border-radius: 50%; object-fit: cover;
      transition: transform 0.3s;
    }
  }

  .camera-icon {
    position: absolute; bottom: 0; right: 0;
    width: 32px; height: 32px;
    background: var(--el-color-primary);
    color: #fff;
    border-radius: 50%;
    display: flex; align-items: center; justify-content: center;
    border: 3px solid var(--bg-color-card);
    transition: all 0.3s;
    opacity: 0; transform: scale(0.5);
  }

  &:hover {
    .real-avatar { transform: scale(1.05); }
    .camera-icon { opacity: 1; transform: scale(1); }
  }
}

.identity {
  .nickname { font-size: 24px; font-weight: 800; margin: 0 0 10px; color: var(--text-color-primary); }
  .tags {
    display: flex; justify-content: center; gap: 10px; margin-bottom: 15px;
    span { padding: 2px 10px; border-radius: 12px; font-size: 12px; font-weight: bold; }
    .role-tag { background: var(--el-fill-color); color: var(--text-color-regular); }
    .role-tag.admin { background: linear-gradient(135deg, #f6d365 0%, #fda085 100%); color: #fff; }
    .id-tag { background: var(--el-fill-color-light); color: var(--text-color-secondary); }
  }
  .bio { color: var(--text-color-secondary); font-size: 14px; margin-bottom: 0; }
}

.detail-list {
  margin-top: 10px;
  .item {
    display: flex; justify-content: space-between; align-items: center;
    padding: 18px 0;
    font-size: 14px;
    .label { display: flex; align-items: center; gap: 8px; color: var(--text-color-regular); }
    .val { font-weight: 500; color: var(--text-color-primary); }
  }
}

.avatar-grid {
  display: grid; grid-template-columns: repeat(3, 1fr); gap: 15px;
  .avatar-option {
    border-radius: 12px; overflow: hidden; cursor: pointer; border: 2px solid transparent;
    img { width: 100%; display: block; }
    &:hover { transform: translateY(-3px); box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
    &.active { border-color: var(--el-color-primary); }
  }
}
</style>
