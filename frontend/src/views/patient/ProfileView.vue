<template>
  <div class="profile-container">
    <el-row :gutter="20">
      <!-- 个人信息卡片 -->
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <span>个人信息</span>
          </template>
          <div v-loading="infoLoading">
            <div class="avatar-section">
              <el-avatar :size="80" icon="User" />
              <h3>{{ profile?.name || profile?.username }}</h3>
              <el-tag>{{ profile?.gender }}</el-tag>
            </div>
            <el-descriptions :column="1" border size="small" style="margin-top: 16px">
              <el-descriptions-item label="账户ID">{{ profile?.account }}</el-descriptions-item>
              <el-descriptions-item label="手机号">{{ profile?.phone }}</el-descriptions-item>
              <el-descriptions-item label="年龄">{{ profile?.age }}</el-descriptions-item>
              <el-descriptions-item label="地址">{{ profile?.address || '未填写' }}</el-descriptions-item>
              <el-descriptions-item label="就诊次数">{{ profile?.totalVisits ?? 0 }}</el-descriptions-item>
              <el-descriptions-item label="最近就诊">{{ profile?.lastVisitDate || '暂无' }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </el-card>
      </el-col>

      <!-- 修改信息 -->
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header>
            <span>修改个人信息</span>
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getPatientInfo, updateProfile } from '@/api/patient'
import type { PatientInfo, UpdateProfileDto } from '@/types'

const userStore = useUserStore()
const infoLoading = ref(false)
const updateLoading = ref(false)
const profile = ref<PatientInfo | null>(null)
const formRef = ref<FormInstance>()

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
  } finally {
    infoLoading.value = false
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
.profile-container { padding: 20px; }
.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}
</style>
