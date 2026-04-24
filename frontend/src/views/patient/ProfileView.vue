<template>
  <div class="page profile-page">
    <div class="page-header profile-header">
      <div>
        <div class="page-title">个人信息</div>
        <div class="page-subtitle">管理账号资料、联系方式与隐私信息</div>
      </div>
    </div>

    <div class="profile-layout" v-loading="infoLoading">
      <el-card class="profile-main-card" shadow="never">
        <div class="profile-banner"></div>

        <div class="profile-main">
          <div class="avatar-shell">
            <span class="avatar-aura"></span>
            <div class="avatar-wrapper" @click="triggerUpload">
              <el-avatar :size="110" :icon="User" :src="avatarUrl" />
              <div class="avatar-overlay">
                <el-icon><Camera /></el-icon>
                <span>更换头像</span>
              </div>
            </div>
          </div>

          <input
            ref="fileInputRef"
            type="file"
            accept="image/jpg,image/jpeg,image/png,image/gif,image/webp,image/bmp"
            style="display: none"
            @change="handleFileChange"
          />

          <h2 class="profile-name">{{ profile?.name || profile?.username || '未登录用户' }}</h2>
          <p class="profile-account">账户 ID：{{ profile?.account || '--' }}</p>

          <div class="meta-row">
            <el-tag effect="light">{{ profile?.gender || '未填写性别' }}</el-tag>
            <el-tag effect="light" type="info">{{ profile?.age ? profile.age + ' 岁' : '年龄未填' }}</el-tag>
          </div>

          <div class="status-pill" :class="{ online: !!profile }">
            {{ profile ? '在线状态正常' : '未登录' }}
          </div>
        </div>

        <div class="metric-grid">
          <div class="metric-card">
            <span class="metric-label">就诊次数</span>
            <strong class="metric-value">{{ profile?.totalVisits ?? 0 }}</strong>
          </div>
          <div class="metric-card">
            <span class="metric-label">最近就诊</span>
            <strong class="metric-value metric-value--small">{{ profile?.lastVisitDate || '暂无记录' }}</strong>
          </div>
        </div>
      </el-card>

      <el-card class="profile-detail-card" shadow="never">
        <div class="detail-head">
          <h3>资料详情</h3>
          <p>在手机号和地址右侧可直接发起修改</p>
        </div>

        <div class="detail-list">
          <div class="detail-row">
            <span class="detail-label">用户名</span>
            <span class="detail-value">{{ profile?.username || '--' }}</span>
          </div>

          <div class="detail-row">
            <span class="detail-label">账户ID</span>
            <span class="detail-value">{{ profile?.account || '--' }}</span>
          </div>

          <div class="detail-row detail-row--action">
            <span class="detail-label">手机号</span>
            <div class="detail-main">
              <span class="detail-value">{{ profile?.phone || '未填写' }}</span>
              <el-button type="primary" link size="small" @click="openEditDrawer('phone')">换绑手机号</el-button>
            </div>
          </div>

          <div class="detail-row detail-row--action">
            <span class="detail-label">身份证</span>
            <div class="detail-main">
              <span class="detail-value">{{ displayIdCard || '未填写' }}</span>
              <el-button v-if="!idCardRevealed" type="primary" link size="small" @click="showIdCardDialog">查看详细</el-button>
              <el-button v-else type="warning" link size="small" @click="idCardRevealed = false">隐藏</el-button>
            </div>
          </div>

          <div class="detail-row detail-row--action">
            <span class="detail-label">地址</span>
            <div class="detail-main">
              <span class="detail-value">{{ profile?.address || '未填写' }}</span>
              <el-button type="primary" link size="small" @click="openEditDrawer('address')">修改地址</el-button>
            </div>
          </div>

          <div class="detail-row">
            <span class="detail-label">年龄</span>
            <span class="detail-value">{{ profile?.age ? profile.age + ' 岁' : '未填写' }}</span>
          </div>
        </div>
      </el-card>
    </div>

    <el-dialog
      v-model="editDrawerVisible"
      class="profile-edit-dialog"
      width="560px"
      :close-on-click-modal="false"
      @closed="handleEditDrawerClosed"
    >
      <template #header>
        <div class="dialog-head">
          <span>{{ editDrawerTitle }}</span>
          <el-tag effect="light" type="info" size="small">需验证密码</el-tag>
        </div>
      </template>

      <el-form
        ref="formRef"
        class="edit-form"
        :model="updateForm"
        :rules="updateRules"
        label-width="100px"
        v-loading="updateLoading"
      >
        <el-form-item v-if="editTarget === 'all'" label="用户名" prop="username">
          <el-input v-model="updateForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item v-if="editTarget !== 'address'" label="手机号" prop="phone">
          <el-input v-model="updateForm.phone" placeholder="请输入新手机号" maxlength="11" />
        </el-form-item>
        <el-form-item v-if="editTarget !== 'phone'" label="地址" prop="address">
          <el-input v-model="updateForm.address" placeholder="请输入地址" />
        </el-form-item>
        <el-form-item label="当前密码" prop="password" required>
          <el-input v-model="updateForm.password" type="password" :placeholder="passwordPlaceholder" show-password />
        </el-form-item>
        <el-form-item class="edit-form-actions">
          <el-button @click="editDrawerVisible = false">取消</el-button>
          <el-button type="primary" @click="handleUpdate">保存修改</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>

    <el-dialog
      v-model="idCardDialogVisible"
      class="idcard-dialog"
      title="身份验证"
      width="420px"
      :close-on-click-modal="false"
      @closed="idCardPassword = ''"
    >
      <p class="idcard-tip">查看完整身份证号需要验证您的登录密码</p>
      <el-input
        v-model="idCardPassword"
        type="password"
        placeholder="请输入登录密码"
        show-password
        @keyup.enter="handleVerifyIdCard"
      />
      <template #footer>
        <el-button @click="idCardDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="idCardLoading" @click="handleVerifyIdCard">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { User, Camera } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getPatientInfo, updateProfile, uploadAvatar, getIdCard } from '@/api/patient'
import type { PatientInfo, UpdateProfileDto } from '@/types'

const userStore = useUserStore()
const infoLoading = ref(false)
const updateLoading = ref(false)
const editDrawerVisible = ref(false)
const editTarget = ref<'all' | 'phone' | 'address'>('all')
const profile = ref<PatientInfo | null>(null)
const formRef = ref<FormInstance>()
const fileInputRef = ref<HTMLInputElement>()

const idCardRevealed = ref(false)
const fullIdCard = ref('')
const idCardDialogVisible = ref(false)
const idCardPassword = ref('')
const idCardLoading = ref(false)

const BASE_URL = '/HIS'
const avatarTimestamp = ref(Date.now())

const avatarUrl = computed(() => {
  if (profile.value?.avatar) {
    return BASE_URL + profile.value.avatar + '?t=' + avatarTimestamp.value
  }
  return undefined
})

const displayIdCard = computed(() => {
  if (idCardRevealed.value && fullIdCard.value) {
    return fullIdCard.value
  }
  return profile.value?.idCard || ''
})

const editDrawerTitle = computed(() => {
  if (editTarget.value === 'phone') return '换绑手机号'
  if (editTarget.value === 'address') return '修改地址'
  return '资料编辑'
})

const passwordPlaceholder = computed(() => {
  if (editTarget.value === 'phone') return '换绑手机号需要验证当前密码'
  if (editTarget.value === 'address') return '修改地址需要验证当前密码'
  return '修改信息需要验证当前密码'
})

const updateForm = reactive<UpdateProfileDto>({
  username: '',
  phone: '',
  address: '',
  password: '',
})

const phoneValidator = (_rule: any, value: string, callback: any) => {
  if (value && !/^1[3-9]\d{9}$/.test(value)) {
    callback(new Error('请输入正确的11位手机号'))
  } else {
    callback()
  }
}

const updateRules: FormRules<UpdateProfileDto> = {
  password: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  phone: [{ validator: phoneValidator, trigger: 'blur' }],
}

async function loadProfile() {
  infoLoading.value = true
  try {
    const res = await getPatientInfo()
    profile.value = res.data
    updateForm.username = res.data.username
    updateForm.phone = res.data.phone
    updateForm.address = res.data.address
    updateForm.password = ''

    idCardRevealed.value = false
    fullIdCard.value = ''
  } catch {
    profile.value = null
  } finally {
    infoLoading.value = false
  }
}

function openEditDrawer(target: 'all' | 'phone' | 'address' = 'all') {
  editTarget.value = target
  if (!profile.value) {
    const cached = userStore.userInfo
    if (cached) {
      updateForm.username = cached.username || ''
      updateForm.phone = cached.phone || ''
      updateForm.address = cached.address || ''
    }
    loadProfile()
  }
  updateForm.password = ''
  formRef.value?.clearValidate()
  editDrawerVisible.value = true
}

function handleEditDrawerClosed() {
  formRef.value?.clearValidate()
  updateForm.password = ''
  editTarget.value = 'all'
}

function triggerUpload() {
  fileInputRef.value?.click()
}

async function handleFileChange(e: Event) {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('头像文件大小不能超过5MB')
    target.value = ''
    return
  }

  const allowedTypes = ['image/jpg', 'image/jpeg', 'image/png', 'image/gif', 'image/webp', 'image/bmp']
  if (!allowedTypes.includes(file.type)) {
    ElMessage.error('仅支持 jpg、jpeg、png、gif、webp、bmp 格式的图片')
    target.value = ''
    return
  }

  try {
    infoLoading.value = true
    const res = await uploadAvatar(file)
    ElMessage.success('头像上传成功')
    if (profile.value) {
      profile.value.avatar = res.data
    }
    avatarTimestamp.value = Date.now()
    await userStore.fetchProfile()
  } catch {
    // 错误已在拦截器中处理
  } finally {
    infoLoading.value = false
    target.value = ''
  }
}

function showIdCardDialog() {
  idCardPassword.value = ''
  idCardDialogVisible.value = true
}

async function handleVerifyIdCard() {
  if (!idCardPassword.value) {
    ElMessage.warning('请输入密码')
    return
  }
  idCardLoading.value = true
  try {
    const res = await getIdCard(idCardPassword.value)
    fullIdCard.value = res.data.idCard
    idCardRevealed.value = true
    idCardDialogVisible.value = false
    ElMessage.success('验证成功')
  } catch {
    // 错误已在拦截器中处理
  } finally {
    idCardLoading.value = false
  }
}

async function handleUpdate() {
  await formRef.value?.validate()
  updateLoading.value = true
  try {
    await updateProfile(updateForm)
    ElMessage.success('修改成功')
    await loadProfile()
    await userStore.fetchProfile()
    editDrawerVisible.value = false
  } finally {
    updateLoading.value = false
  }
}

onMounted(loadProfile)
</script>

<style scoped>
.profile-page {
  position: relative;
  overflow: hidden;
  min-height: calc(100vh - var(--his-header-height));
  background:
    radial-gradient(1200px 520px at -10% -10%, rgba(var(--his-primary-rgb), 0.2), transparent 62%),
    radial-gradient(980px 460px at 110% -5%, rgba(52, 185, 124, 0.14), transparent 66%),
    linear-gradient(160deg, #f4fdf7 0%, #f0fbf4 45%, #f8fffb 100%);
}

.profile-page::before,
.profile-page::after {
  content: "";
  position: absolute;
  pointer-events: none;
  border-radius: 999px;
  filter: blur(2px);
}

.profile-page::before {
  width: 540px;
  height: 540px;
  top: -210px;
  left: -170px;
  background: radial-gradient(circle at 35% 35%, rgba(var(--his-primary-rgb), 0.24), rgba(var(--his-primary-rgb), 0));
  animation: orb-float-a 11s ease-in-out infinite;
}

.profile-page::after {
  width: 660px;
  height: 660px;
  right: -300px;
  bottom: -350px;
  background: radial-gradient(circle at 40% 35%, rgba(var(--his-primary-rgb), 0.17), rgba(var(--his-primary-rgb), 0));
  animation: orb-float-b 13s ease-in-out infinite;
}

.profile-header {
  margin-bottom: 18px;
}

.profile-layout {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 24px;
  align-items: stretch;
  min-height: calc(100vh - var(--his-header-height) - 128px);
}

.profile-main-card,
.profile-detail-card {
  position: relative;
  isolation: isolate;
  display: flex;
  flex-direction: column;
  height: 100%;
  border-radius: 22px;
  border: 1px solid rgba(var(--his-primary-rgb), 0.2);
  box-shadow:
    0 22px 46px rgba(18, 56, 38, 0.1),
    0 8px 20px rgba(var(--his-primary-rgb), 0.12);
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(12px) saturate(130%);
  overflow: hidden;
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;
}

.profile-main-card:hover,
.profile-detail-card:hover {
  transform: translateY(-2px);
  border-color: rgba(var(--his-primary-rgb), 0.34);
  box-shadow:
    0 26px 54px rgba(18, 56, 38, 0.12),
    0 12px 26px rgba(var(--his-primary-rgb), 0.16);
}

.profile-main-card::before,
.profile-detail-card::before {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: linear-gradient(
    130deg,
    rgba(255, 255, 255, 0.42) 0%,
    rgba(255, 255, 255, 0.08) 22%,
    rgba(255, 255, 255, 0.02) 58%,
    rgba(255, 255, 255, 0.28) 100%
  );
  z-index: 0;
}

:deep(.profile-main-card > .el-card__body),
:deep(.profile-detail-card > .el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
  z-index: 1;
  padding: 0;
}

.profile-banner {
  position: relative;
  height: 132px;
  overflow: hidden;
  background:
    linear-gradient(125deg, rgba(var(--his-primary-rgb), 0.96), rgba(40, 165, 112, 0.94)),
    radial-gradient(circle at 86% 18%, rgba(255, 255, 255, 0.34), rgba(255, 255, 255, 0));
}

.profile-banner::before {
  content: "";
  position: absolute;
  inset: 0;
  opacity: 0.22;
  background: repeating-linear-gradient(
    -25deg,
    rgba(255, 255, 255, 0.8) 0,
    rgba(255, 255, 255, 0.8) 2px,
    rgba(255, 255, 255, 0) 2px,
    rgba(255, 255, 255, 0) 16px
  );
}

.profile-banner::after {
  content: "";
  position: absolute;
  top: -30%;
  left: -45%;
  width: 36%;
  height: 170%;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0), rgba(255, 255, 255, 0.45), rgba(255, 255, 255, 0));
  transform: rotate(14deg);
  animation: banner-sweep 5.5s ease-in-out infinite;
}

.profile-main {
  flex: 1;
  margin-top: -62px;
  padding: 0 24px 18px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.avatar-shell {
  position: relative;
  width: 126px;
  height: 126px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-aura {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: conic-gradient(
    from 120deg,
    rgba(255, 255, 255, 0.05),
    rgba(255, 255, 255, 0.95),
    rgba(var(--his-primary-rgb), 0.45),
    rgba(255, 255, 255, 0.05)
  );
  filter: blur(0.3px);
  animation: aura-spin 7.5s linear infinite;
}

.avatar-wrapper {
  position: relative;
  z-index: 1;
  cursor: pointer;
  border-radius: 50%;
  overflow: hidden;
  box-shadow:
    0 0 0 5px rgba(255, 255, 255, 0.96),
    0 12px 26px rgba(var(--his-primary-rgb), 0.24);
  transition: transform 0.2s ease;
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  opacity: 0;
  transition: opacity 0.2s ease;
  font-size: 12px;
}

.avatar-shell:hover .avatar-wrapper {
  transform: translateY(-1px);
}

.avatar-wrapper:hover .avatar-overlay {
  opacity: 1;
}

.profile-name {
  margin: 14px 0 4px;
  font-size: 24px;
  font-weight: 800;
  color: var(--his-text);
  letter-spacing: 0.3px;
}

.profile-account {
  margin: 0;
  color: var(--his-text-2);
  font-size: 13px;
  opacity: 0.95;
}

.meta-row {
  margin-top: 12px;
  display: flex;
  justify-content: center;
  gap: 10px;
  flex-wrap: wrap;
}

.status-pill {
  margin-top: 14px;
  height: 30px;
  padding: 0 13px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  color: var(--his-text-2);
  background: rgba(148, 163, 184, 0.12);
  font-size: 12px;
  font-weight: 700;
}

.status-pill::before {
  content: "";
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: currentColor;
  box-shadow: 0 0 0 0 rgba(var(--his-primary-rgb), 0.35);
  animation: state-pulse 1.9s ease-in-out infinite;
}

.status-pill.online {
  border-color: rgba(var(--his-primary-rgb), 0.26);
  color: var(--his-primary);
  background: rgba(var(--his-primary-rgb), 0.12);
}

.metric-grid {
  margin-top: auto;
  border-top: 1px solid rgba(var(--his-primary-rgb), 0.2);
  display: grid;
  grid-template-columns: 1fr 1fr;
  background:
    linear-gradient(180deg, rgba(var(--his-primary-rgb), 0.08), rgba(var(--his-primary-rgb), 0.02)),
    rgba(255, 255, 255, 0.5);
}

.metric-card {
  padding: 18px 18px;
  display: flex;
  flex-direction: column;
  gap: 7px;
  transition: transform 0.22s ease, background-color 0.22s ease;
}

.metric-card:hover {
  transform: translateY(-2px);
  background: rgba(255, 255, 255, 0.5);
}

.metric-card + .metric-card {
  border-left: 1px solid rgba(var(--his-primary-rgb), 0.18);
}

.metric-label {
  font-size: 12px;
  color: var(--his-text-2);
  letter-spacing: 0.2px;
}

.metric-value {
  font-size: 20px;
  line-height: 1.2;
  font-weight: 800;
  color: var(--his-text);
}

.metric-value--small {
  font-size: 14px;
}

.profile-detail-card {
  padding: 20px 20px 18px;
}

.detail-head {
  margin-bottom: 14px;
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid rgba(var(--his-primary-rgb), 0.18);
  background:
    linear-gradient(120deg, rgba(var(--his-primary-rgb), 0.13), rgba(var(--his-primary-rgb), 0.04)),
    rgba(255, 255, 255, 0.62);
}

.detail-head h3 {
  margin: 0;
  font-size: 20px;
  color: var(--his-text);
  line-height: 1.1;
}

.detail-head p {
  margin: 6px 0 0;
  color: var(--his-text-2);
  font-size: 12px;
}

.detail-list {
  flex: 1;
  display: grid;
  grid-template-rows: repeat(6, minmax(64px, 1fr));
  gap: 10px;
}

.detail-row {
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(var(--his-primary-rgb), 0.14);
  border-radius: 14px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.9), rgba(255, 255, 255, 0.76)),
    rgba(255, 255, 255, 0.84);
  padding: 12px 14px;
  display: grid;
  grid-template-columns: 84px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.detail-row::before {
  content: "";
  position: absolute;
  left: 0;
  top: 10px;
  bottom: 10px;
  width: 3px;
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(var(--his-primary-rgb), 0.82), rgba(var(--his-primary-rgb), 0.2));
  opacity: 0.6;
}

.detail-row:hover {
  transform: translateX(2px);
  border-color: rgba(var(--his-primary-rgb), 0.26);
  box-shadow: 0 10px 18px rgba(var(--his-primary-rgb), 0.11);
}

.detail-row--action {
  grid-template-columns: 84px minmax(0, 1fr);
}

.detail-label {
  color: var(--his-text-2);
  font-size: 13px;
  font-weight: 600;
}

.detail-main {
  min-width: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.detail-value {
  min-width: 0;
  color: var(--his-text);
  font-size: 14px;
  font-weight: 600;
  word-break: break-word;
}

.detail-row :deep(.el-button) {
  flex-shrink: 0;
  height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid rgba(var(--his-primary-rgb), 0.24);
  background: rgba(var(--his-primary-rgb), 0.1);
  font-weight: 700;
  transition: transform 0.18s ease, background-color 0.18s ease;
}

.detail-row :deep(.el-button:hover) {
  transform: translateY(-1px);
  background: rgba(var(--his-primary-rgb), 0.16);
}

.dialog-head {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--his-text);
  font-weight: 700;
}

.edit-form {
  margin-top: 6px;
}

.edit-form-actions :deep(.el-form-item__content) {
  justify-content: flex-end;
  gap: 10px;
}

.idcard-tip {
  margin-bottom: 12px;
  color: var(--his-text-2);
  font-size: 13px;
}

:deep(.profile-edit-dialog .el-dialog),
:deep(.idcard-dialog .el-dialog) {
  border-radius: 18px;
  border: 1px solid rgba(var(--his-primary-rgb), 0.22);
  box-shadow:
    0 26px 56px rgba(18, 56, 38, 0.16),
    0 10px 24px rgba(var(--his-primary-rgb), 0.14);
  backdrop-filter: blur(8px);
}

:deep(.profile-edit-dialog .el-dialog__header),
:deep(.idcard-dialog .el-dialog__header) {
  margin-bottom: 0;
  padding: 16px 20px 12px;
  background: linear-gradient(
    180deg,
    rgba(var(--his-primary-rgb), 0.14),
    rgba(255, 255, 255, 0.02)
  );
}

:deep(.profile-edit-dialog .el-dialog__body),
:deep(.idcard-dialog .el-dialog__body) {
  padding: 12px 20px 24px;
}

@keyframes orb-float-a {
  0%, 100% { transform: translate3d(0, 0, 0); }
  50% { transform: translate3d(18px, 14px, 0); }
}

@keyframes orb-float-b {
  0%, 100% { transform: translate3d(0, 0, 0); }
  50% { transform: translate3d(-20px, -16px, 0); }
}

@keyframes banner-sweep {
  0% { left: -45%; opacity: 0; }
  18% { opacity: 0.75; }
  46% { opacity: 0.1; }
  100% { left: 128%; opacity: 0; }
}

@keyframes aura-spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes state-pulse {
  0% { box-shadow: 0 0 0 0 rgba(var(--his-primary-rgb), 0.38); }
  65% { box-shadow: 0 0 0 7px rgba(var(--his-primary-rgb), 0); }
  100% { box-shadow: 0 0 0 0 rgba(var(--his-primary-rgb), 0); }
}

@media (max-width: 1080px) {
  .profile-layout {
    grid-template-columns: 1fr;
    min-height: auto;
  }
}

@media (max-width: 768px) {
  .profile-main-card,
  .profile-detail-card {
    border-radius: 16px;
  }

  .profile-banner {
    height: 116px;
  }

  .profile-main-card :deep(.el-card__body),
  .profile-detail-card :deep(.el-card__body) {
    padding: 0;
  }

  .profile-detail-card {
    padding: 14px 12px 12px;
  }

  .detail-list {
    display: flex;
    flex-direction: column;
  }

  .detail-row {
    grid-template-columns: 1fr;
    gap: 6px;
  }

  .detail-row :deep(.el-button) {
    height: 26px;
    padding: 0 8px;
  }

  .detail-main {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
