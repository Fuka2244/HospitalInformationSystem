<template>
  <el-container class="main-layout">
    <!-- 顶部导航 -->
    <el-header class="header">
      <div class="header-inner">
        <div class="brand" @click="router.push('/home')">
          <div class="brand-mark">
            <el-icon><FirstAidKit /></el-icon>
          </div>
          <div class="brand-text">
            <div class="brand-title">医院信息系统</div>
          </div>
        </div>

        <el-menu :default-active="route.path" router mode="horizontal" class="top-menu" :ellipsis="false">
          <el-menu-item index="/home">
            <el-icon><HomeFilled /></el-icon>
            <span>首页</span>
          </el-menu-item>
          <el-menu-item index="/appointment">
            <el-icon><Calendar /></el-icon>
            <span>预约挂号</span>
          </el-menu-item>
          <el-menu-item index="/records">
            <el-icon><Document /></el-icon>
            <span>病历与就诊</span>
          </el-menu-item>
          <el-menu-item index="/report">
            <el-icon><Notebook /></el-icon>
            <span>医疗报告</span>
          </el-menu-item>
          <el-menu-item index="/billing">
            <el-icon><Wallet /></el-icon>
            <span>费用查询</span>
          </el-menu-item>
          <el-menu-item index="/medicine">
            <el-icon><FirstAidKit /></el-icon>
            <span>药品查询</span>
          </el-menu-item>
          <el-menu-item index="/department">
            <el-icon><OfficeBuilding /></el-icon>
            <span>科室信息</span>
          </el-menu-item>
          <el-menu-item index="/about">
            <el-icon><InfoFilled /></el-icon>
            <span>系统概况</span>
          </el-menu-item>
        </el-menu>

        <div class="header-right">
          <span class="username">{{ userStore.userInfo?.username || '用户' }}</span>
          <el-dropdown @command="handleCommand">
            <el-avatar :size="34" icon="User" class="avatar" />
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人信息</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </el-header>

    <!-- 主内容 -->
    <el-main class="main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { watch } from 'vue'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { Calendar, Document, FirstAidKit, HomeFilled, InfoFilled, Notebook, OfficeBuilding, User, Wallet } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 路由切换时滚动到页面顶部
watch(() => route.path, () => {
  const main = document.querySelector('.main')
  if (main) main.scrollTop = 0
})

async function handleCommand(cmd: string) {
  if (cmd === 'profile') {
    router.push('/profile')
  } else if (cmd === 'logout') {
    try {
      await ElMessageBox.confirm('确定退出登录？', '提示', { type: 'warning' })
      await userStore.logout()
      router.push('/login')
    } catch { /* cancelled */ }
  }
}
</script>

<style scoped>
.main-layout { height: 100vh; }

.header {
  height: 60px;
  padding: 0;
  border-bottom: 1px solid rgba(15, 23, 42, 0.08);
  background: #fff;
  box-shadow: 0 1px 8px rgba(15, 23, 42, 0.06);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-inner {
  width: 100%;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 20px;
  padding: 0 32px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  user-select: none;
  flex-shrink: 0;
}

.brand-mark {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, #409eff 0%, #2f80ed 100%);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

.brand-mark :deep(svg) { width: 18px; height: 18px; }

.brand-text { display: flex; flex-direction: column; line-height: 1; }

.brand-title {
  font-weight: 700;
  letter-spacing: 0.5px;
  font-size: 15px;
  color: #1a2a4a;
}

.top-menu {
  flex: 1;
  border-bottom: none;
  background: transparent;
  padding-left: 8px;
  min-width: 520px;
}

:deep(.top-menu.el-menu--horizontal) {
  background: transparent;
}

:deep(.top-menu.el-menu--horizontal > .el-menu-item) {
  border-bottom: none;
  border-radius: 8px;
  margin: 0 4px;
  height: 38px;
  line-height: 38px;
  color: #5a6a85;
  font-size: 14px;
  padding: 0 16px;
  transition: all 0.2s ease;
}

:deep(.top-menu.el-menu--horizontal > .el-menu-item:hover) {
  background: rgba(64, 158, 255, 0.06);
  color: #409eff;
}

:deep(.top-menu.el-menu--horizontal > .el-menu-item.is-active) {
  background: rgba(64, 158, 255, 0.10);
  color: #409eff;
  font-weight: 600;
}

:deep(.top-menu .el-icon) {
  color: currentColor;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
  margin-left: auto;
  padding-left: 24px;
}

.username {
  color: #5a6a85;
  font-size: 14px;
}

.avatar {
  border: 2px solid rgba(64, 158, 255, 0.2);
  background: rgba(64, 158, 255, 0.08);
  cursor: pointer;
  transition: border-color 0.2s ease;
}

.avatar:hover {
  border-color: rgba(64, 158, 255, 0.4);
}

.main {
  background: transparent;
  overflow-y: auto;
  padding: 0;
}

@media (max-width: 1100px) {
  .brand-title { display: none; }
  .top-menu { min-width: 0; }
  .header-inner { padding: 0 12px; gap: 8px; }
  :deep(.top-menu.el-menu--horizontal > .el-menu-item) {
    padding: 0 10px;
    font-size: 13px;
  }
}

@media (max-width: 768px) {
  .username { display: none; }
  :deep(.top-menu.el-menu--horizontal > .el-menu-item span) {
    display: none;
  }
  :deep(.top-menu.el-menu--horizontal > .el-menu-item) {
    padding: 0 8px;
  }
}
</style>
