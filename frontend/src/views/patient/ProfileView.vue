<template>
  <div class="page profile-container">
    <div class="page-header">
      <div>
        <div class="page-title">个人信息</div>
        <div class="page-subtitle">资料概览与安全信息维护</div>
      </div>
    </div>
    <el-row class="fill-row" :gutter="20">
      <!-- 个人信息卡片 -->
      <el-col :xs="24" :lg="8" class="fill-col">
        <el-card class="fill-card" shadow="hover">
          <template #header>
            <div class="card-head">
              <span>资料概览</span>
              <el-tag v-if="profile" effect="light" type="primary" size="small">已登录</el-tag>
              <el-tag v-else effect="light" type="info" size="small">未登录</el-tag>
            </div>
          </template>
          <div v-loading="infoLoading">
            <div class="avatar-section">
              <div class="avatar-wrapper" @click="triggerUpload">
                <el-avatar :size="80" :icon="User" :src="avatarUrl" />
                <div class="avatar-overlay">
                  <el-icon><Camera /></el-icon>
                  <span>更换头像</span>
                </div>
              </div>
              <input
                ref="fileInputRef"
                type="file"
                accept="image/jpg,image/jpeg,image/png,image/gif,image/webp,image/bmp"
                style="display: none"
                @change="handleFileChange"
              />
              <h3>{{ profile?.name || profile?.username }}</h3>
              <div class="meta-row">
                <el-tag effect="light">{{ profile?.gender }}</el-tag>
                <el-tag effect="light" type="info">{{ profile?.age ? profile.age + ' 岁' : '年龄未填' }}</el-tag>
              </div>
            </div>
            <el-descriptions :column="1" border size="small" style="margin-top: 16px">
              <el-descriptions-item label="账户ID">{{ profile?.account }}</el-descriptions-item>
              <el-descriptions-item label="手机号">{{ profile?.phone }}</el-descriptions-item>
              <el-descriptions-item label="年龄">{{ profile?.age ? profile.age + ' 岁' : '未填写' }}</el-descriptions-item>
              <el-descriptions-item label="身份证">
                <div class="id-card-field">
                  <span>{{ displayIdCard }}</span>
                  <el-button
                    v-if="!idCardRevealed"
                    type="primary"
                    link
                    size="small"
                    @click="showIdCardDialog"
                  >
                    查看详细
                  </el-button>
                  <el-button
                    v-else
                    type="warning"
                    link
                    size="small"
                    @click="idCardRevealed = false"
                  >
                    隐藏
                  </el-button>
                </div>
              </el-descriptions-item>
              <el-descriptions-item label="地址">{{ profile?.address || '未填写' }}</el-descriptions-item>
              <el-descriptions-item label="就诊次数">{{ profile?.totalVisits ?? 0 }}</el-descriptions-item>
              <el-descriptions-item label="最近就诊">{{ profile?.lastVisitDate || '暂无' }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </el-card>
      </el-col>

      <!-- 修改信息 -->
      <el-col :xs="24" :lg="16" class="fill-col">
        <el-card class="fill-card" shadow="hover">
          <template #header>
            <div class="card-head">
              <span>资料编辑</span>
              <el-tag effect="light" type="info" size="small">需验证密码</el-tag>
            </div>
          </template>
          <el-form ref="formRef" :model="updateForm" :rules="updateRules" label-width="100px" v-loading="updateLoading">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="updateForm.username" placeholder="请输入用户名" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="updateForm.phone" placeholder="请输入新手机号" maxlength="11" />
            </el-form-item>
            <el-form-item label="地址" prop="address">
              <el-input v-model="updateForm.address" placeholder="请输入地址" />
            </el-form-item>
            <el-form-item label="当前密码" prop="password" required>
              <el-input v-model="updateForm.password" type="password" placeholder="修改信息需验证当前密码" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleUpdate">保存修改</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>

    <!-- 身份证验证密码对话框 -->
    <el-dialog
      v-model="idCardDialogVisible"
      title="身份验证"
      width="400px"
      :close-on-click-modal="false"
      @closed="idCardPassword = ''"
    >
      <p style="margin-bottom: 12px; color: #909399; font-size: 13px;">查看完整身份证号需要验证您的登录密码</p>
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
const profile = ref<PatientInfo | null>(null)
const formRef = ref<FormInstance>()
const fileInputRef = ref<HTMLInputElement>()

// 身份证相关状态
const idCardRevealed = ref(false)
const fullIdCard = ref('')
const idCardDialogVisible = ref(false)
const idCardPassword = ref('')
const idCardLoading = ref(false)

const BASE_URL = '/HIS'

// 头像缓存破坏戳，每次上传后更新
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
    // 重新加载时重置身份证显示状态
    idCardRevealed.value = false
    fullIdCard.value = ''
  } catch {
    profile.value = null
  } finally {
    infoLoading.value = false
  }
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
    // 更新时间戳，破坏浏览器缓存
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
  } finally {
    updateLoading.value = false
  }
}

onMounted(loadProfile)
</script>

<style scoped>
.profile-container { padding: 0; }
.card-head{
  display:flex;
  align-items:center;
  justify-content: space-between;
  gap: 12px;
}
.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}
.avatar-wrapper {
  position: relative;
  cursor: pointer;
  border-radius: 50%;
  overflow: hidden;
}
.avatar-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  opacity: 0;
  transition: opacity 0.3s;
  border-radius: 50%;
  font-size: 12px;
  gap: 4px;
}
.avatar-wrapper:hover .avatar-overlay {
  opacity: 1;
}
.meta-row{
  display:flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content:center;
}
.id-card-field {
  display: flex;
  align-items: center;
  gap: 8px;
}
:deep(.el-descriptions__label){
  color: rgba(15, 23, 42, 0.62);
}
</style>
